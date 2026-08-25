package com.dropbox.conductor.dropbox;

import com.dropbox.core.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class DropboxPkceLogin {

    private static final String APP_KEY_ENV = "DROPBOX_APP_KEY";

    private DropboxPkceLogin() {}

    public static void main(String[] args) throws IOException {
        String appKey = System.getenv(APP_KEY_ENV);

        if (appKey == null || appKey.isBlank()) {
            throw new IllegalStateException("Environment variable " + APP_KEY_ENV + " is required");
        }

        DbxRequestConfig requestConfig =
                DbxRequestConfig.newBuilder("conductor-dropbox/0.1").build();

        DbxAppInfo appInfo = new DbxAppInfo(appKey);

        DbxPKCEWebAuth webAuth = new DbxPKCEWebAuth(requestConfig, appInfo);

        DbxWebAuth.Request request = DbxWebAuth.newRequestBuilder()
                .withNoRedirect()
                .withTokenAccessType(TokenAccessType.OFFLINE)
                .build();

        String authorizeUrl = webAuth.authorize(request);

        System.out.println("Open this URL in your browser:");
        System.out.println();
        System.out.println(authorizeUrl);
        System.out.println();
        System.out.println("Authorize the Dropbox app and copy the authorization code.");
        System.out.println();
        System.out.print("Authorization code: ");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        String code = reader.readLine();

        if (code == null || code.isBlank()) {
            throw new IllegalStateException("Authorization code is required");
        }

        try {
            DbxAuthFinish authFinish = webAuth.finishFromCode(code.trim());

            System.out.println();
            System.out.println("Authorization successful.");
            System.out.println();
            System.out.println("Set these environment variables:");
            System.out.println();
            System.out.println("export DROPBOX_APP_KEY=" + appKey);
            System.out.println("export DROPBOX_REFRESH_TOKEN=" + authFinish.getRefreshToken());

        } catch (DbxException e) {
            throw new IllegalStateException("Dropbox OAuth authorization failed", e);
        }
    }
}
