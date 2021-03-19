package com.azure.identity.extensions;

import com.azure.core.credential.AccessToken;
import com.azure.identity.extensions.implementation.util.ValidationUtil;

import java.time.OffsetDateTime;
import java.util.HashMap;

/**
 * Fluent credential builder for instantiating a {@link StaticTokenCredential}.
 *
 * @see StaticTokenCredential
 */
public class StaticTokenCredentialBuilder extends CredentialBuilderBase<StaticTokenCredentialBuilder> {

    private String tokenString;

    private OffsetDateTime expiresAt;

    private AccessToken accessToken;

    /**
     * Sets the prefetched token string for the token.
     *
     * @param tokenString The string of prefetched token
     *
     * @return The updated StaticTokenCredentialBuilder object.
     */
    public StaticTokenCredentialBuilder tokenString(String tokenString) {
        this.tokenString = tokenString;
        return this;
    }

    /**
     * Sets the prefetched token string for the token.
     *
     * @param expiresAt The string of prefetched token
     *
     * @return The updated StaticTokenCredentialBuilder object.
     */
    public StaticTokenCredentialBuilder expiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    /**
     * Sets the prefetched access token for the access token.
     *
     * @param accessToken The prefetched token
     *
     * @return The updated StaticTokenCredentialBuilder object.
     */
    public StaticTokenCredentialBuilder accessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    /**
     * Creates a new {@link StaticTokenCredentialBuilder} with the current configurations.
     *
     * @return a {@link StaticTokenCredentialBuilder} with the current configurations.
     */
    public StaticTokenCredential build() {
        ValidationUtil.validateAllEmpty(getClass().getSimpleName(), new HashMap<String, Object>() {{
            put("tokenString", tokenString);
            put("accessToken", accessToken);
        }});
        if ( accessToken == null ) {
            ValidationUtil.validateAllEmpty(getClass().getSimpleName(), new HashMap<String, Object>() {{
                put("tokenString", tokenString);
                put("expiresAt", expiresAt);
            }});
        }
        return new StaticTokenCredential(tokenString, expiresAt, accessToken);
    }

}
