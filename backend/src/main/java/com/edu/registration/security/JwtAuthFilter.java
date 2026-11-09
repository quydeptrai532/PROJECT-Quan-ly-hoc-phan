package com.edu.registration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAuthCache userAuthCache;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, UserAuthCache userAuthCache, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userAuthCache = userAuthCache;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            try {
                if (jwtService.isValid(token)) {
                    String username = jwtService.extractUsername(token);
                    Optional<UserAuthCache.AuthUser> authUserOpt = userAuthCache.find(username);
                    if (authUserOpt.isPresent()) {
                        UserAuthCache.AuthUser user = authUserOpt.get();
                        if (!user.active()) {
                            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                    "Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo");
                            return;
                        }
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user.username(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("message", message));
    }
}
