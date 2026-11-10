package com.edu.registration.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {

    private final List<String> trustedProxies = List.of(
            "127.0.0.1",
            "::1",
            "0:0:0:0:0:0:0:1",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16"
    );

    @Test
    @DisplayName("Bỏ qua X-Forwarded-For khi request đến từ IP không nằm trong danh sách Proxy tin cậy")
    void shouldIgnoreXForwardedForWhenRemoteAddrIsNotTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10"); // IP công cộng bên ngoài, không phải proxy tin cậy
        request.addHeader("X-Forwarded-For", "1.1.1.1, 8.8.8.8");
        request.addHeader("X-Real-IP", "9.9.9.9");

        String resolvedIp = ClientIpResolver.resolveClientIp(request, trustedProxies);
        // Phải trả về đúng remoteAddr, tuyệt đối không tin header do client giả lập
        assertEquals("203.0.113.10", resolvedIp);
    }

    @Test
    @DisplayName("Trích xuất IP thật từ X-Forwarded-For khi request đến từ Proxy tin cậy (127.0.0.1)")
    void shouldResolveClientIpFromXForwardedForWhenRemoteAddrIsTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); // Reverse proxy nội bộ tin cậy
        request.addHeader("X-Forwarded-For", "203.0.113.55");

        String resolvedIp = ClientIpResolver.resolveClientIp(request, trustedProxies);
        assertEquals("203.0.113.55", resolvedIp);
    }

    @Test
    @DisplayName("Duyệt ngược chuỗi proxy đa tầng để tìm client IP thực sự (bỏ qua các intermediate proxy tin cậy)")
    void shouldResolveOriginalClientInMultiHopProxyChain() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1"); // Gateway proxy
        // Chuỗi: Client thực -> Proxy biên (10.0.1.5) -> Gateway
        request.addHeader("X-Forwarded-For", "198.51.100.77, 10.0.1.5");

        String resolvedIp = ClientIpResolver.resolveClientIp(request, trustedProxies);
        assertEquals("198.51.100.77", resolvedIp);
    }

    @Test
    @DisplayName("Sử dụng X-Real-IP nếu X-Forwarded-For trống và request đến từ proxy tin cậy")
    void shouldUseXRealIpWhenXForwardedForIsEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.2"); // Docker network thuộc dải 172.16.0.0/12
        request.addHeader("X-Real-IP", "198.51.100.88");

        String resolvedIp = ClientIpResolver.resolveClientIp(request, trustedProxies);
        assertEquals("198.51.100.88", resolvedIp);
    }

    @Test
    @DisplayName("Trả về remoteAddr nếu không có header forward nào từ proxy tin cậy")
    void shouldFallbackToRemoteAddrWhenHeadersMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String resolvedIp = ClientIpResolver.resolveClientIp(request, trustedProxies);
        assertEquals("127.0.0.1", resolvedIp);
    }

    @Test
    @DisplayName("Nhận diện chính xác các dải CIDR tin cậy (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)")
    void shouldMatchCidrRangesCorrectly() {
        assertTrue(ClientIpResolver.isTrustedProxy("10.5.20.1", trustedProxies));
        assertTrue(ClientIpResolver.isTrustedProxy("172.20.10.2", trustedProxies));
        assertTrue(ClientIpResolver.isTrustedProxy("192.168.1.100", trustedProxies));
        assertTrue(ClientIpResolver.isTrustedProxy("127.0.0.1", trustedProxies));
        assertTrue(ClientIpResolver.isTrustedProxy("::1", trustedProxies));

        assertFalse(ClientIpResolver.isTrustedProxy("8.8.8.8", trustedProxies));
        assertFalse(ClientIpResolver.isTrustedProxy("203.0.113.1", trustedProxies));
    }
}
