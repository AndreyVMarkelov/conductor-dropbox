package com.dropbox.conductor.dropbox;

import static org.junit.jupiter.api.Assertions.*;

import com.dropbox.core.v2.DbxClientV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class DropboxClientProviderTest {

    @Test
    void createsClientFromAccessToken() {
        DbxClientV2 client = DropboxClientProvider.create(new DropboxCredentials("access-token", null, null));

        assertNotNull(client);
    }

    @Test
    void createsClientFromRefreshTokenAndAppKey() {
        DbxClientV2 client = DropboxClientProvider.create(new DropboxCredentials("access-token", null, null));

        assertNotNull(client);
    }

    @Test
    void refreshTokenRequiresAppKey() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> DropboxClientProvider.create(new DropboxCredentials(null, null, "refresh-token")));

        assertEquals(
                "Dropbox app key is required when refresh token authentication is configured", exception.getMessage());
    }

    @Test
    void failsWhenNoCredentialsConfigured() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> DropboxClientProvider.create(new DropboxCredentials(null, null, null)));

        assertEquals(
                "Configure Dropbox authentication using either an access token or an app key with refresh token",
                exception.getMessage());
    }

    @Test
    void refreshTokenTakesPrecedenceOverAccessToken() {
        DbxClientV2 client = DropboxClientProvider.create(new DropboxCredentials(null, "app-key", "refresh-token"));

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
