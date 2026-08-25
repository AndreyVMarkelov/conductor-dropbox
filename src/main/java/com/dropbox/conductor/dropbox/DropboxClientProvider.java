package com.dropbox.conductor.dropbox;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

public final class DropboxClientProvider {
    private static final String TOKEN_ENV = "DROPBOX_ACCESS_TOKEN";

    private DropboxClientProvider() {
    }

    public static DbxClientV2 create() {
        String token = System.getenv(TOKEN_ENV);

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Environment variable " + TOKEN_ENV + " is required"
            );
        }

        DbxRequestConfig config = DbxRequestConfig.newBuilder(
                "conductor-dropbox/0.1"
        ).build();

        return new DbxClientV2(config, token);
    }
}
