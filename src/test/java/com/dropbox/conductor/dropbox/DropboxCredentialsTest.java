package com.dropbox.conductor.dropbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DropboxCredentialsTest {

    @Test
    void readsAccessTokenFromEnvironment() {
        DropboxCredentials credentials =
                DropboxCredentials.fromEnvironment(Map.of("DROPBOX_ACCESS_TOKEN", "access-token"));

        assertEquals("access-token", credentials.accessToken());
        assertNull(credentials.appKey());
        assertNull(credentials.refreshToken());

        assertTrue(credentials.hasAccessTokenAuth());
        assertFalse(credentials.hasRefreshTokenAuth());
    }

    @Test
    void readsRefreshTokenCredentialsFromEnvironment() {
        DropboxCredentials credentials = DropboxCredentials.fromEnvironment(Map.of(
                "DROPBOX_APP_KEY", "app-key",
                "DROPBOX_REFRESH_TOKEN", "refresh-token"));

        assertNull(credentials.accessToken());
        assertEquals("app-key", credentials.appKey());
        assertEquals("refresh-token", credentials.refreshToken());

        assertFalse(credentials.hasAccessTokenAuth());
        assertTrue(credentials.hasRefreshTokenAuth());
    }

    @Test
    void readsBothAuthenticationModes() {
        DropboxCredentials credentials = DropboxCredentials.fromEnvironment(Map.of(
                "DROPBOX_ACCESS_TOKEN", "access-token",
                "DROPBOX_APP_KEY", "app-key",
                "DROPBOX_REFRESH_TOKEN", "refresh-token"));

        assertTrue(credentials.hasAccessTokenAuth());
        assertTrue(credentials.hasRefreshTokenAuth());
    }

    @Test
    void emptyEnvironmentHasNoAuthentication() {
        DropboxCredentials credentials = DropboxCredentials.fromEnvironment(Map.of());

        assertFalse(credentials.hasAccessTokenAuth());
        assertFalse(credentials.hasRefreshTokenAuth());
    }

    @Test
    void blankTokensAreNotAuthentication() {
        DropboxCredentials credentials = DropboxCredentials.fromEnvironment(Map.of(
                "DROPBOX_ACCESS_TOKEN", " ",
                "DROPBOX_APP_KEY", "app-key",
                "DROPBOX_REFRESH_TOKEN", ""));

        assertFalse(credentials.hasAccessTokenAuth());
        assertFalse(credentials.hasRefreshTokenAuth());
    }
}
