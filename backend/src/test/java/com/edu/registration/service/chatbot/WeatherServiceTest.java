package com.edu.registration.service.chatbot;

import com.edu.registration.config.ChatbotWeatherProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherServiceTest {

    @Test
    @DisplayName("Lấy thời tiết theo thành phố trong câu hỏi và định dạng báo cáo")
    void shouldReturnFormattedWeatherForCityInMessage() throws Exception {
        TestServer testServer = startServer();
        try {
            WeatherService service = new WeatherService(properties(testServer.port()), new ObjectMapper());

            Optional<String> report = service.getWeatherReport("Thời tiết Hà Nội hôm nay");

            assertTrue(report.isPresent());
            assertTrue(report.get().contains("Hà Nội"));
            assertTrue(report.get().contains("25.4"));
            assertTrue(report.get().contains("Có mây"));
            assertTrue(report.get().contains("Open-Meteo"));
            assertEquals("Hà Nội", testServer.lastQueriedCity().get());
        } finally {
            testServer.server().stop(0);
        }
    }

    @Test
    @DisplayName("Không nêu thành phố thì dùng thành phố mặc định")
    void shouldUseDefaultCityWhenMessageHasNoCity() throws Exception {
        TestServer testServer = startServer();
        try {
            ChatbotWeatherProperties properties = properties(testServer.port());
            properties.setDefaultCity("Hà Nội");
            WeatherService service = new WeatherService(properties, new ObjectMapper());

            Optional<String> report = service.getWeatherReport("Nhiệt độ hôm nay thế nào?");

            assertTrue(report.isPresent());
            assertEquals("Hà Nội", testServer.lastQueriedCity().get());
        } finally {
            testServer.server().stop(0);
        }
    }

    @Test
    @DisplayName("Nhận diện thành phố có dấu trong câu hỏi")
    void shouldResolveAccentedCityName() throws Exception {
        TestServer testServer = startServer();
        try {
            WeatherService service = new WeatherService(properties(testServer.port()), new ObjectMapper());

            Optional<String> report = service.getWeatherReport("Thời tiết Đà Nẵng hôm nay");

            assertTrue(report.isPresent());
            assertEquals("Đà Nẵng", testServer.lastQueriedCity().get());
        } finally {
            testServer.server().stop(0);
        }
    }

    @Test
    @DisplayName("Không có kết quả địa danh thì trả về rỗng")
    void shouldReturnEmptyWhenGeocodingHasNoResults() throws Exception {
        TestServer testServer = startServer();
        try {
            testServer.emptyResults().set(true);
            WeatherService service = new WeatherService(properties(testServer.port()), new ObjectMapper());

            assertTrue(service.getWeatherReport("Thời tiết Hà Nội").isEmpty());
        } finally {
            testServer.server().stop(0);
        }
    }

    private ChatbotWeatherProperties properties(int port) {
        ChatbotWeatherProperties properties = new ChatbotWeatherProperties();
        properties.setEnabled(true);
        properties.setDefaultCity("Hà Nội");
        properties.setGeocodingUrl("http://127.0.0.1:" + port + "/search");
        properties.setForecastUrl("http://127.0.0.1:" + port + "/forecast");
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(3000);
        return properties;
    }

    private TestServer startServer() throws IOException {
        AtomicReference<String> lastQueriedCity = new AtomicReference<>();
        AtomicBoolean emptyResults = new AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/search", exchange -> {
            String name = queryParam(exchange, "name");
            lastQueriedCity.set(name);
            String body = emptyResults.get()
                    ? "{\"results\":[]}"
                    : "{\"results\":[{\"name\":\"" + name + "\",\"latitude\":21.03,\"longitude\":105.85}]}";
            writeJson(exchange, body);
        });

        server.createContext("/forecast", exchange -> writeJson(exchange,
                "{\"current\":{\"time\":\"2026-11-10T14:30\",\"temperature_2m\":25.4,"
                        + "\"relative_humidity_2m\":70,\"apparent_temperature\":27.1,"
                        + "\"weather_code\":2,\"wind_speed_10m\":8.3},"
                        + "\"daily\":{\"temperature_2m_max\":[28.5],\"temperature_2m_min\":[21.2],"
                        + "\"precipitation_probability_max\":[20]}}"));

        server.start();
        return new TestServer(server, lastQueriedCity, emptyResults);
    }

    private void writeJson(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String queryParam(com.sun.net.httpserver.HttpExchange exchange, String key) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0 && key.equals(pair.substring(0, separator))) {
                return URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private record TestServer(HttpServer server, AtomicReference<String> lastQueriedCity, AtomicBoolean emptyResults) {
        int port() {
            return server.getAddress().getPort();
        }
    }
}
