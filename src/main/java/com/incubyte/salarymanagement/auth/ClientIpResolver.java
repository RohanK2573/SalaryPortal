package com.incubyte.salarymanagement.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
    private final Set<String> trustedProxies;

    public ClientIpResolver(@Value("${app.security.trusted-proxy-addresses:}") String configuredProxies) {
        trustedProxies = Arrays.stream(configuredProxies.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddress)) return remoteAddress;
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) return remoteAddress;
        // The trusted ingress must replace this header with the original client address.
        return forwardedFor.split(",", 2)[0].trim();
    }
}
