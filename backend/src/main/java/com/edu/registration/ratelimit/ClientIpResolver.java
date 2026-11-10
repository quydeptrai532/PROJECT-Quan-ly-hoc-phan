package com.edu.registration.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tiện ích phân giải địa chỉ IP máy khách an toàn qua Reverse Proxy (QLSV-102 / TASK-OP-07B).
 * Chỉ tin tưởng các header X-Forwarded-For / X-Real-IP nếu yêu cầu thực sự đến từ Proxy được tin cậy.
 */
public final class ClientIpResolver {

    private static final Pattern VALID_IP_PATTERN = Pattern.compile("^[0-9a-fA-F.:]{3,45}$");

    private ClientIpResolver() {
    }

    /**
     * Xác định IP gốc của client.
     *
     * @param request        HttpServletRequest hiện tại
     * @param trustedProxies Danh sách các địa chỉ IP hoặc dải mạng CIDR của Proxy tin cậy
     * @return Địa chỉ IP đã được xác thực an toàn
     */
    public static String resolveClientIp(HttpServletRequest request, List<String> trustedProxies) {
        if (request == null) {
            return "127.0.0.1";
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            remoteAddr = "127.0.0.1";
        } else {
            remoteAddr = remoteAddr.trim();
        }

        // Nếu request KHÔNG đến từ proxy tin cậy -> Tuyệt đối không đọc header do client tự gửi!
        if (!isTrustedProxy(remoteAddr, trustedProxies)) {
            return sanitizeIp(remoteAddr);
        }

        // Request đến từ proxy tin cậy: bóc tách X-Forwarded-For
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String resolved = extractFromForwardedFor(xForwardedFor, trustedProxies);
            if (resolved != null && !resolved.isBlank()) {
                return sanitizeIp(resolved);
            }
        }

        // Kiểm tra tiếp X-Real-IP nếu X-Forwarded-For không có
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            String candidate = xRealIp.trim();
            if (isValidIpFormat(candidate)) {
                return sanitizeIp(candidate);
            }
        }

        return sanitizeIp(remoteAddr);
    }

    /**
     * Duyệt chuỗi X-Forwarded-For từ phải qua trái để tìm IP untrusted đầu tiên.
     */
    private static String extractFromForwardedFor(String xForwardedFor, List<String> trustedProxies) {
        String[] parts = xForwardedFor.split(",");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && isValidIpFormat(trimmed)) {
                list.add(trimmed);
            }
        }

        if (list.isEmpty()) {
            return null;
        }

        // Đi ngược từ gần máy chủ nhất (phải qua trái)
        for (int i = list.size() - 1; i >= 0; i--) {
            String ip = list.get(i);
            if (!isTrustedProxy(ip, trustedProxies)) {
                return ip;
            }
        }

        // Nếu tất cả các IP trong chuỗi đều thuộc proxy tin cậy, trả về IP xa nhất (bên trái cùng)
        return list.get(0);
    }

    /**
     * Kiểm tra xem một IP có nằm trong danh sách Proxy tin cậy hay không.
     */
    public static boolean isTrustedProxy(String ip, List<String> trustedProxies) {
        if (ip == null || ip.isBlank() || trustedProxies == null || trustedProxies.isEmpty()) {
            return false;
        }

        String target = ip.trim();
        InetAddress targetAddress;
        try {
            targetAddress = InetAddress.getByName(target);
        } catch (UnknownHostException e) {
            return false;
        }

        for (String rule : trustedProxies) {
            if (rule == null || rule.isBlank()) {
                continue;
            }
            String trimmedRule = rule.trim();

            // Khớp CIDR (vd: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 127.0.0.0/8)
            if (trimmedRule.contains("/")) {
                if (matchIpv4Cidr(targetAddress, trimmedRule)) {
                    return true;
                }
            } else {
                // Khớp chính xác IP
                try {
                    InetAddress ruleAddress = InetAddress.getByName(trimmedRule);
                    if (targetAddress.equals(ruleAddress)) {
                        return true;
                    }
                    if (targetAddress.isLoopbackAddress() && ruleAddress.isLoopbackAddress()) {
                        return true;
                    }
                } catch (UnknownHostException ignored) {
                }
            }
        }

        return false;
    }

    private static boolean matchIpv4Cidr(InetAddress address, String cidr) {
        byte[] bytes = address.getAddress();
        if (bytes.length != 4) {
            return false; // Chỉ áp dụng CIDR cho IPv4
        }
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            InetAddress subnetAddress = InetAddress.getByName(parts[0].trim());
            byte[] subnetBytes = subnetAddress.getAddress();
            if (subnetBytes.length != 4) {
                return false;
            }

            int prefix = Integer.parseInt(parts[1].trim());
            if (prefix < 0 || prefix > 32) {
                return false;
            }

            int ipInt = bytesToInt(bytes);
            int subnetInt = bytesToInt(subnetBytes);
            int mask = prefix == 0 ? 0 : ~((1 << (32 - prefix)) - 1);

            return (ipInt & mask) == (subnetInt & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
    }

    private static boolean isValidIpFormat(String ip) {
        return ip != null && VALID_IP_PATTERN.matcher(ip).matches();
    }

    private static String sanitizeIp(String ip) {
        if (!isValidIpFormat(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
