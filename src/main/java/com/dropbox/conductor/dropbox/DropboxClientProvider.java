package com.dropbox.conductor.dropbox;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;

public final class DropboxClientProvider {

    private DropboxClientProvider() {}

    public static DbxClientV2 create() {
        return create(new EnvironmentDropboxCredentialsProvider());
    }

    static DbxClientV2 create(DropboxCredentialsProvider provider) {
        return create(provider.get());
    }

    static DbxClientV2 create(DropboxCredentials credentials) {
        DbxRequestConfig config =
                DbxRequestConfig.newBuilder("conductor-dropbox/0.1").build();

        if (credentials.hasRefreshTokenAuth()) {
            if (credentials.appKey() == null || credentials.appKey().isBlank()) {
                throw new IllegalStateException(
                        "Dropbox app key is required when refresh token authentication is configured");
            }

            DbxCredential credential = new DbxCredential("", -1L, credentials.refreshToken(), credentials.appKey());

            return new DbxClientV2(config, credential);
        }

        if (credentials.hasAccessTokenAuth()) {
            return new DbxClientV2(config, credentials.accessToken());
        }

        throw new IllegalStateException(
                "Configure Dropbox authentication using either an access token " + "or an app key with refresh token");
    }
}
