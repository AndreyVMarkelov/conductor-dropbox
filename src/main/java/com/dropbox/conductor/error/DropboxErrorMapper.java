package com.dropbox.conductor.error;

import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxException;
import com.dropbox.core.RateLimitException;

public final class DropboxErrorMapper {

    private DropboxErrorMapper() {
    }

    public static DropboxError map(String operation, Exception exception) {
        if (exception instanceof RateLimitException) {
            return error(
                    DropboxErrorCode.RATE_LIMITED,
                    "Dropbox rate limit exceeded",
                    true,
                    operation
            );
        }

        if (exception instanceof DbxApiException apiException) {
            return error(
                    DropboxErrorCode.INTERNAL_ERROR,
                    safeMessage(apiException),
                    false,
                    operation
            );
        }

        if (exception instanceof DbxException dbxException) {
            return error(
                    DropboxErrorCode.NETWORK_ERROR,
                    safeMessage(dbxException),
                    true,
                    operation
            );
        }

        if (exception instanceof IllegalArgumentException) {
            return error(
                    DropboxErrorCode.INVALID_INPUT,
                    safeMessage(exception),
                    false,
                    operation
            );
        }

        return error(
                DropboxErrorCode.INTERNAL_ERROR,
                safeMessage(exception),
                false,
                operation
        );
    }

    private static DropboxError error(
            DropboxErrorCode code,
            String message,
            boolean retryable,
            String operation
    ) {
        return new DropboxError(
                code.name(),
                message,
                retryable,
                operation
        );
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }
}
