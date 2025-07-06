package com.mumuk.global.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String email;

    public JwtAuthenticationToken(String email) {
        super(null);
        this.email = email;
        setAuthenticated(true);
    }

    @Override
    public Object getPrincipal() {
        return email; // email이 principal
    }

    @Override
    public Object getCredentials() {
        return null;
    }
}
