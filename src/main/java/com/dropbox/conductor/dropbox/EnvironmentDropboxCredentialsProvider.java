package com.dropbox.conductor.dropbox;

public final class EnvironmentDropboxCredentialsProvider implements DropboxCredentialsProvider {
    @Override
    public DropboxCredentials get() {
        return DropboxCredentials.fromEnvironment();
    }
}
