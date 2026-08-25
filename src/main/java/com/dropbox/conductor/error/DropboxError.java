package com.dropbox.conductor.error;

public record DropboxError(
        String code,
        String message,
        boolean retryable,
        String operation
) {
}
