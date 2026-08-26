package com.dropbox.conductor.dropbox;

import java.util.Map;

public record DropboxCredentials(String accessToken, String appKey, String refreshToken) {

    private static final String ACCESS_TOKEN_ENV = "DROPBOX_ACCESS_TOKEN";
    private static final String APP_KEY_ENV = "DROPBOX_APP_KEY";
    private static final String REFRESH_TOKEN_ENV = "DROPBOX_REFRESH_TOKEN";

    public static DropboxCredentials fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static DropboxCredentials fromEnvironment(Map<String, String> env) {
        return new DropboxCredentials(env.get(ACCESS_TOKEN_ENV), env.get(APP_KEY_ENV), env.get(REFRESH_TOKEN_ENV));
    }

    public boolean hasRefreshTokenAuth() {
        return refreshToken != null && !refreshToken.isBlank();
    }

    public boolean hasAccessTokenAuth() {
        return accessToken != null && !accessToken.isBlank();
    }
}
