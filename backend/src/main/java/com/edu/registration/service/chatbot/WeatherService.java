package com.edu.registration.service.chatbot;

import com.edu.registration.config.ChatbotWeatherProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tra cứu thời tiết hiện tại qua Open-Meteo (miễn phí, không cần API key).
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Pattern CITY_AFTER_PREPOSITION = Pattern.compile("(?:^|\\s)(?:o|tai)\\s+([a-z0-9 ]{2,40})");

    private static final List<Map.Entry<String, String>> CITY_ALIASES = buildCityAliases();

    private final ChatbotWeatherProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WeatherService(ChatbotWeatherProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * Trả về báo cáo thời tiết đã định dạng cho thành phố trong câu hỏi,
     * hoặc rỗng nếu không lấy được dữ liệu.
     */
    public Optional<String> getWeatherReport(String message) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        String city = resolveCity(message);
        try {
            GeoLocation location = geocode(city);
            if (location == null) {
                return Optional.empty();
            }
            JsonNode forecast = fetchForecast(location);
            return Optional.of(formatReport(location, forecast));
        } catch (Exception e) {
            log.warn("Không lấy được dữ liệu thời tiết cho '{}': {}", city, e.getMessage());
            return Optional.empty();
        }
    }

    private GeoLocation geocode(String city) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getGeocodingUrl())
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "vi")
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();
        String body = restTemplate.getForObject(uri, String.class);
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode results = objectMapper.readTree(body).path("results");
        if (!results.isArray() || results.isEmpty()) {
            return null;
        }
        JsonNode first = results.get(0);
        String name = first.path("name").asText(city);
        return new GeoLocation(name, first.path("latitude").asDouble(), first.path("longitude").asDouble());
    }

    private JsonNode fetchForecast(GeoLocation location) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getForecastUrl())
                .queryParam("latitude", location.latitude())
                .queryParam("longitude", location.longitude())
                .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m")
                .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                .queryParam("timezone", "auto")
                .queryParam("forecast_days", 1)
                .build()
                .encode()
                .toUri();
        String body = restTemplate.getForObject(uri, String.class);
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Open-Meteo trả về rỗng");
        }
        return objectMapper.readTree(body);
    }

    private String formatReport(GeoLocation location, JsonNode forecast) {
        JsonNode current = forecast.path("current");
        JsonNode daily = forecast.path("daily");

        double temperature = current.path("temperature_2m").asDouble(0);
        double feelsLike = current.path("apparent_temperature").asDouble(0);
        int humidity = current.path("relative_humidity_2m").asInt(0);
        double windSpeed = current.path("wind_speed_10m").asDouble(0);
        int weatherCode = current.path("weather_code").asInt(-1);

        double minTemp = daily.path("temperature_2m_min").path(0).asDouble(0);
        double maxTemp = daily.path("temperature_2m_max").path(0).asDouble(0);
        int rainProbability = daily.path("precipitation_probability_max").path(0).asInt(0);

        String observedAt = formatTime(current.path("time").asText(""));

        return String.format(Locale.ROOT,
                "🌤️ **Thời tiết %s** (%s)\n" +
                        "- Nhiệt độ hiện tại: **%.1f°C** (cảm giác như %.1f°C)\n" +
                        "- %s, độ ẩm %d%%, gió %.0f km/h\n" +
                        "- Hôm nay: %.1f°C - %.1f°C, khả năng mưa %d%%\n\n" +
                        "(Nguồn: Open-Meteo)",
                location.name(), observedAt, temperature, feelsLike,
                weatherDescription(weatherCode), humidity, windSpeed,
                minTemp, maxTemp, rainProbability);
    }

    private String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isBlank()) {
            return "hiện tại";
        }
        try {
            return TIME_FORMATTER.format(LocalDateTime.parse(isoTime));
        } catch (Exception ignored) {
            return isoTime;
        }
    }

    private String weatherDescription(int code) {
        return switch (code) {
            case 0 -> "Trời quang";
            case 1 -> "Ít mây";
            case 2 -> "Có mây";
            case 3 -> "Nhiều mây";
            case 45, 48 -> "Sương mù";
            case 51, 53, 55 -> "Mưa phùn";
            case 56, 57 -> "Mưa phùn đóng băng";
            case 61 -> "Mưa nhỏ";
            case 63 -> "Mưa vừa";
            case 65 -> "Mưa to";
            case 66, 67 -> "Mưa đóng băng";
            case 71, 73, 75 -> "Tuyết rơi";
            case 77 -> "Hạt tuyết";
            case 80, 81, 82 -> "Mưa rào";
            case 85, 86 -> "Mưa tuyết";
            case 95 -> "Dông";
            case 96, 99 -> "Dông kèm mưa đá";
            default -> "Không xác định";
        };
    }

    private String resolveCity(String message) {
        String normalized = removeDiacritics(message == null ? "" : message).toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> alias : CITY_ALIASES) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(alias.getKey()) + "\\b");
            if (pattern.matcher(normalized).find()) {
                return alias.getValue();
            }
        }
        Matcher matcher = CITY_AFTER_PREPOSITION.matcher(normalized);
        if (matcher.find()) {
            String candidate = matcher.group(1)
                    .replaceAll("\\b(?:hom nay|ngay mai|bay gio|hien tai|ra sao|the nao|khong|nhi|nhe|nha|a)\\b.*$", "")
                    .trim();
            if (candidate.length() >= 2) {
                return candidate;
            }
        }
        return properties.getDefaultCity();
    }

    private static List<Map.Entry<String, String>> buildCityAliases() {
        Map<String, String> cities = new LinkedHashMap<>();
        cities.put("ho chi minh", "Hồ Chí Minh");
        cities.put("tp hcm", "Hồ Chí Minh");
        cities.put("sai gon", "Hồ Chí Minh");
        cities.put("hcm", "Hồ Chí Minh");
        cities.put("ha noi", "Hà Nội");
        cities.put("hai phong", "Hải Phòng");
        cities.put("da nang", "Đà Nẵng");
        cities.put("can tho", "Cần Thơ");
        cities.put("nha trang", "Nha Trang");
        cities.put("da lat", "Đà Lạt");
        cities.put("vung tau", "Vũng Tàu");
        cities.put("quy nhon", "Quy Nhơn");
        cities.put("bien hoa", "Biên Hòa");
        cities.put("buon ma thuot", "Buôn Ma Thuột");
        cities.put("thai nguyen", "Thái Nguyên");
        cities.put("nam dinh", "Nam Định");
        cities.put("ha long", "Hạ Long");
        cities.put("thanh hoa", "Thanh Hóa");
        cities.put("vinh long", "Vĩnh Long");
        cities.put("vinh phuc", "Vĩnh Phúc");
        cities.put("vinh yen", "Vĩnh Yên");
        cities.put("hue", "Huế");
        cities.put("vinh", "Vinh");
        List<Map.Entry<String, String>> aliases = new ArrayList<>(cities.entrySet());
        aliases.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());
        return aliases;
    }

    private String removeDiacritics(String str) {
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private record GeoLocation(String name, double latitude, double longitude) {
    }
}
