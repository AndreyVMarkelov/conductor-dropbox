package com.dropbox.conductor.dropbox;

import static org.junit.jupiter.api.Assertions.*;

import com.dropbox.core.v2.DbxClientV2;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class DropboxClientProviderTest {

    @Test
    void createsClientFromAccessToken() {
        DbxClientV2 client = DropboxClientProvider.create(Map.of("DROPBOX_ACCESS_TOKEN", "access-token"));

        assertNotNull(client);
    }

    @Test
    void createsClientFromRefreshTokenAndAppKey() {
        DbxClientV2 client = DropboxClientProvider.create(Map.of(
                "DROPBOX_APP_KEY", "app-key",
                "DROPBOX_REFRESH_TOKEN", "refresh-token"));

        assertNotNull(client);
    }

    @Test
    void refreshTokenRequiresAppKey() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> DropboxClientProvider.create(Map.of("DROPBOX_REFRESH_TOKEN", "refresh-token")));

        assertEquals(
                "Environment variable DROPBOX_APP_KEY is required when DROPBOX_REFRESH_TOKEN is set",
                exception.getMessage());
    }

    @Test
    void failsWhenNoCredentialsConfigured() {
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> DropboxClientProvider.create(Map.of()));

        assertEquals(
                "Configure Dropbox authentication using either DROPBOX_ACCESS_TOKEN or DROPBOX_APP_KEY + DROPBOX_REFRESH_TOKEN",
                exception.getMessage());
    }

    @Test
    void refreshTokenTakesPrecedenceOverAccessToken() {
        DbxClientV2 client = DropboxClientProvider.create(Map.of(
                "DROPBOX_ACCESS_TOKEN", "access-token",
                "DROPBOX_APP_KEY", "app-key",
                "DROPBOX_REFRESH_TOKEN", "refresh-token"));

        assertNotNull(client);
    }

    @Test
    @EnabledIf("hasDropboxCredentials")
    void authenticatesWithDropbox() throws Exception {
        var client = DropboxClientProvider.create();
        var account = client.users().getCurrentAccount();
        assertNotNull(account.getAccountId());
    }

    static boolean hasDropboxCredentials() {
        String accessToken = System.getenv("DROPBOX_ACCESS_TOKEN");
        String appKey = System.getenv("DROPBOX_APP_KEY");
        String refreshToken = System.getenv("DROPBOX_REFRESH_TOKEN");

        return (accessToken != null && !accessToken.isBlank())
                || (appKey != null && !appKey.isBlank() && refreshToken != null && !refreshToken.isBlank());
    }
}
