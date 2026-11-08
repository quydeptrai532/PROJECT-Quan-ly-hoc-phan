package com.edu.registration.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "audit_alert_rules")
public class AuditAlertRule {

    @Id
    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int threshold;

    @Column(name = "window_minutes", nullable = false)
    private int windowMinutes;

    @Column(nullable = false, length = 20)
    private String severity;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) {
        this.severity = severity;
    }
}