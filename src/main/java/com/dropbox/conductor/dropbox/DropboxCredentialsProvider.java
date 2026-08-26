package com.dropbox.conductor.dropbox;

@FunctionalInterface
public interface DropboxCredentialsProvider {
    DropboxCredentials get();
}
