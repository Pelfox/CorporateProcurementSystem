package com.team.corporate.backend.security;

import com.team.corporate.backend.config.AuthRateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Set;
import java.util.stream.Collectors;

/** Exact proxy IPs only; forwarded headers from a direct client have no authority. */
@Component
public class ClientAddressResolver {
    private final Set<String> trusted;

    public ClientAddressResolver(AuthRateLimitProperties properties) {
        trusted = properties.trustedProxies().stream().map(ClientAddressResolver::literal).collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String peer = literal(request.getRemoteAddr());
        String forwarded = request.getHeader("X-Forwarded-For");
        if (!trusted.contains(peer) || forwarded == null || forwarded.length() > 512) {
            return peer;
        }
        String[] chain = forwarded.split(",", -1);
        if (chain.length > 8) {
            return peer;
        }
        try {
            String current = peer;
            for (int i = chain.length - 1; i >= 0 && trusted.contains(current); i--) {
                current = literal(chain[i].strip());
            }
            return current;
        } catch (IllegalArgumentException invalid) {
            return peer;
        }
    }

    private static String literal(String address) {
        return InetAddress.ofLiteral(address).getHostAddress();
    }
}
