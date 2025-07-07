package com.mumuk.global.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String email;

    /**
     * Creates a new authenticated JWT-based authentication token with the specified email as the principal.
     *
     * @param email the email address to be used as the principal for this authentication token
     */
    public JwtAuthenticationToken(String email) {
        super(null);
        this.email = email;
        setAuthenticated(true);
    }

    /**
     * Returns the email address associated with this authentication token as the principal.
     *
     * @return the email address used as the principal
     */
    @Override
    public Object getPrincipal() {
        return email; // email이 principal
    }

    /**
     * Returns {@code null} to indicate that this authentication token does not store credentials.
     *
     * @return always {@code null}
     */
    @Override
    public Object getCredentials() {
        return null;
    }
}
