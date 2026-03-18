package com.example.bff.common.security;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class JwtTokenParser {

    private JwtTokenParser() {
    }

    public static Map<String, Object> parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            return jwt.getJWTClaimsSet().getClaims();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public static Set<String> extractRealmRoles(Map<String, Object> claims) {
        Object realmAccess = claims.get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmMap)) {
            return Collections.emptySet();
        }
        Object rolesObj = realmMap.get("roles");
        if (!(rolesObj instanceof List<?> rolesList)) {
            return Collections.emptySet();
        }
        return rolesList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                .collect(Collectors.toSet());
    }
}

