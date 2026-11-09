package com.edu.registration.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretSanitizerTest {

    @Test
    void shouldSanitizePasswordsAndSecrets() {
        String input = "Connection error: password=superSecretPassword123 with secret=myApiKeySecret";
        String sanitized = SecretSanitizer.sanitize(input);

        assertFalse(sanitized.contains("superSecretPassword123"));
        assertFalse(sanitized.contains("myApiKeySecret"));
        assertTrue(sanitized.contains("password=***REDACTED***"));
        assertTrue(sanitized.contains("secret=***REDACTED***"));
    }

    @Test
    void shouldSanitizeBearerTokensAndJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakSignatureHere123";
        String input = "Request failed with header Authorization: Bearer " + jwt;
        String sanitized = SecretSanitizer.sanitize(input);

        assertFalse(sanitized.contains("doNotLeakSignatureHere123"));
        assertTrue(sanitized.contains("Bearer ***REDACTED***"));
    }

    @Test
    void shouldSanitizeDatabaseCredentials() {
        String input = "Failed to connect to jdbc:mysql://dbuser:superSecretDbPass@mysql:3306/db";
        String sanitized = SecretSanitizer.sanitize(input);

        assertFalse(sanitized.contains("superSecretDbPass"));
        assertTrue(sanitized.contains("dbuser:***REDACTED***@"));
    }

    @Test
    void shouldTruncateExcessivelyLongErrors() {
        String longError = "A".repeat(2500);
        String sanitized = SecretSanitizer.sanitize(longError, 500);

        assertEquals(500, sanitized.length());
        assertTrue(sanitized.endsWith("..."));
    }

    @Test
    void shouldHandleNullOrBlankGracefully() {
        assertNull(SecretSanitizer.sanitize(null));
        assertEquals("", SecretSanitizer.sanitize(""));
        assertEquals("   ", SecretSanitizer.sanitize("   "));
    }
}
