package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình tra cứu thời tiết qua Open-Meteo (miễn phí, không cần API key).
 */
@Component
@ConfigurationProperties(prefix = "app.chatbot.weather")
public class ChatbotWeatherProperties {

    private boolean enabled = true;
    private String defaultCity = "Hà Nội";
    private String geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search";
    private String forecastUrl = "https://api.open-meteo.com/v1/forecast";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultCity() {
        return defaultCity;
    }

    public void setDefaultCity(String defaultCity) {
        this.defaultCity = defaultCity;
    }

    public String getGeocodingUrl() {
        return geocodingUrl;
    }

    public void setGeocodingUrl(String geocodingUrl) {
        this.geocodingUrl = geocodingUrl;
    }

    public String getForecastUrl() {
        return forecastUrl;
    }

    public void setForecastUrl(String forecastUrl) {
        this.forecastUrl = forecastUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
