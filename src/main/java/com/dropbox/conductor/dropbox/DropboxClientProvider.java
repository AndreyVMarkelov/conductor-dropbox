package com.dropbox.conductor.dropbox;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import java.util.Map;

public final class DropboxClientProvider {

    private static final String ACCESS_TOKEN_ENV = "DROPBOX_ACCESS_TOKEN";
    private static final String APP_KEY_ENV = "DROPBOX_APP_KEY";
    private static final String REFRESH_TOKEN_ENV = "DROPBOX_REFRESH_TOKEN";

    private DropboxClientProvider() {}

    public static DbxClientV2 create() {
        return create(System.getenv());
    }

    static DbxClientV2 create(Map<String, String> env) {
        DbxRequestConfig config =
                DbxRequestConfig.newBuilder("conductor-dropbox/0.1").build();

        String refreshToken = env.get(REFRESH_TOKEN_ENV);
        String appKey = env.get(APP_KEY_ENV);

        if (refreshToken != null && !refreshToken.isBlank()) {
            if (appKey == null || appKey.isBlank()) {
                throw new IllegalStateException(
                        "Environment variable " + APP_KEY_ENV + " is required when " + REFRESH_TOKEN_ENV + " is set");
            }

            DbxCredential credential = new DbxCredential("", -1L, refreshToken, appKey);

            return new DbxClientV2(config, credential);
        }

        String accessToken = env.get(ACCESS_TOKEN_ENV);

        if (accessToken != null && !accessToken.isBlank()) {
            return new DbxClientV2(config, accessToken);
        }

        throw new IllegalStateException("Configure Dropbox authentication using either "
                + ACCESS_TOKEN_ENV
                + " or "
                + APP_KEY_ENV
                + " + "
                + REFRESH_TOKEN_ENV);
    }
}
