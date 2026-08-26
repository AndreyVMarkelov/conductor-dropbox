package com.dropbox.conductor.error;

import com.dropbox.core.AccessErrorException;
import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.RetryException;
import com.dropbox.core.ServerException;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.DeleteErrorException;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.LookupError;
import com.dropbox.core.v2.files.RelocationErrorException;
import com.dropbox.core.v2.files.SearchErrorException;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.riviera.GetMarkdownAsyncError;
import com.dropbox.core.v2.riviera.MarkdownConversionApiV2Error;

public final class DropboxErrorMapper {

    private DropboxErrorMapper() {}

    public static DropboxError map(String operation, Exception exception) {
        if (exception instanceof DownloadErrorException downloadException) {
            var error = downloadException.errorValue;
            if (error.isPath()) {
                return mapLookupError(operation, error.getPathValue(), "Dropbox download path");
            }
            if (error.isUnsupportedFile()) {
                return error(
                        DropboxErrorCode.OPERATION_NOT_SUPPORTED,
                        "Dropbox does not support downloading this file",
                        false,
                        operation);
            }
        }

        if (exception instanceof DeleteErrorException deleteException) {
            if (deleteException.errorValue.isPathLookup()
                    && deleteException.errorValue.getPathLookupValue().isNotFound()) {
                return error(DropboxErrorCode.PATH_NOT_FOUND, "Dropbox path does not exist", false, operation);
            }
        }

        if (exception instanceof CreateFolderErrorException createFolderException) {
            if (createFolderException.errorValue.isPath()
                    && createFolderException.errorValue.getPathValue().isConflict()) {
                return error(DropboxErrorCode.PATH_CONFLICT, "Dropbox path already exists", false, operation);
            }
        }

        if (exception instanceof RateLimitException) {
            return error(DropboxErrorCode.RATE_LIMITED, "Dropbox rate limit exceeded", true, operation);
        }

        if (exception instanceof RetryException || exception instanceof ServerException) {
            return error(DropboxErrorCode.TEMPORARY_UNAVAILABLE, "Dropbox is temporarily unavailable", true, operation);
        }

        if (exception instanceof AccessErrorException) {
            return error(DropboxErrorCode.PERMISSION_DENIED, "Dropbox access is denied", false, operation);
        }

        if (exception instanceof RelocationErrorException relocationException) {
            var error = relocationException.errorValue;
            if (error.isFromLookup() && error.getFromLookupValue().isNotFound()) {
                return error(DropboxErrorCode.PATH_NOT_FOUND, "Dropbox source path does not exist", false, operation);
            }

            if (error.isTo() && error.getToValue().isConflict()) {
                return error(
                        DropboxErrorCode.PATH_CONFLICT, "Dropbox destination path already exists", false, operation);
            }

            if ((error.isFromWrite() && error.getFromWriteValue().isNoWritePermission())
                    || (error.isTo() && error.getToValue().isNoWritePermission())) {
                return error(DropboxErrorCode.PERMISSION_DENIED, "Dropbox path permission denied", false, operation);
            }

            if (error.isInternalError()) {
                return error(
                        DropboxErrorCode.TEMPORARY_UNAVAILABLE,
                        "Dropbox move failed with a temporary internal error",
                        true,
                        operation);
            }

            if (error.isInsufficientQuota()) {
                return error(DropboxErrorCode.QUOTA_EXCEEDED, "Insufficient Dropbox storage quota", false, operation);
            }

            if (error.isTooManyFiles()) {
                return error(DropboxErrorCode.INVALID_INPUT, "Move contains too many files", false, operation);
            }

            if (error.isCantMoveFolderIntoItself() || error.isDuplicatedOrNestedPaths()) {
                return error(DropboxErrorCode.INVALID_INPUT, "Invalid Dropbox move", false, operation);
            }

            if (error.isCantCopySharedFolder()
                    || error.isCantNestSharedFolder()
                    || error.isCantTransferOwnership()
                    || error.isCantMoveSharedFolder()
                    || error.isCantMoveIntoVault()
                    || error.isCantMoveIntoFamily()) {
                return error(
                        DropboxErrorCode.OPERATION_NOT_SUPPORTED,
                        "Dropbox does not allow this move operation",
                        false,
                        operation);
            }
        }

        if (exception instanceof UploadErrorException uploadException) {
            var uploadError = uploadException.errorValue;

            if (uploadError.isPath()) {
                var writeError = uploadError.getPathValue().getReason();

                if (writeError.isConflict()) {
                    return error(
                            DropboxErrorCode.PATH_CONFLICT,
                            "Dropbox destination path already exists",
                            false,
                            operation);
                }

                if (writeError.isNoWritePermission()) {
                    return error(
                            DropboxErrorCode.PERMISSION_DENIED,
                            "No permission to write Dropbox destination path",
                            false,
                            operation);
                }

                if (writeError.isInsufficientSpace()) {
                    return error(
                            DropboxErrorCode.QUOTA_EXCEEDED, "Insufficient Dropbox storage quota", false, operation);
                }

                if (writeError.isTooManyWriteOperations()) {
                    return error(
                            DropboxErrorCode.TEMPORARY_UNAVAILABLE,
                            "Too many Dropbox write operations",
                            true,
                            operation);
                }

                if (writeError.isMalformedPath()) {
                    return error(DropboxErrorCode.INVALID_INPUT, "Invalid Dropbox destination path", false, operation);
                }
            }

            if (uploadError.isPayloadTooLarge()) {
                return error(DropboxErrorCode.INVALID_INPUT, "Upload payload is too large", false, operation);
            }

            if (uploadError.isContentHashMismatch()) {
                return error(DropboxErrorCode.INVALID_INPUT, "Upload content hash mismatch", false, operation);
            }

            if (uploadError.isEncryptionNotSupported()) {
                return error(
                        DropboxErrorCode.OPERATION_NOT_SUPPORTED,
                        "Dropbox API does not support the required file encryption",
                        false,
                        operation);
            }

            if (uploadError.isPropertiesError()) {
                return error(DropboxErrorCode.INVALID_INPUT, "Invalid Dropbox file properties", false, operation);
            }
        }

        if (exception instanceof ListFolderErrorException listFolderException) {
            var error = listFolderException.errorValue;

            if (error.isPath() && error.getPathValue().isNotFound()) {
                return error(DropboxErrorCode.PATH_NOT_FOUND, "Dropbox folder does not exist", false, operation);
            }

            if (error.isPath() && error.getPathValue().isNotFolder()) {
                return error(DropboxErrorCode.INVALID_INPUT, "Dropbox path is not a folder", false, operation);
            }
        }

        if (exception instanceof GetMetadataErrorException metadataException) {
            var error = metadataException.errorValue;
            if (error.isPath()) {
                var lookup = error.getPathValue();
                if (lookup.isNotFound()) {
                    return error(DropboxErrorCode.PATH_NOT_FOUND, "Dropbox path does not exist", false, operation);
                }

                if (lookup.isRestrictedContent()) {
                    return error(DropboxErrorCode.PERMISSION_DENIED, "Dropbox content is restricted", false, operation);
                }
            }
        }

        if (exception instanceof SearchErrorException searchException) {
            var error = searchException.errorValue;

            if (error.isPath()) {
                var lookup = error.getPathValue();

                if (lookup.isNotFound()) {
                    return error(
                            DropboxErrorCode.PATH_NOT_FOUND, "Dropbox search path does not exist", false, operation);
                }

                if (lookup.isNotFolder()) {
                    return error(
                            DropboxErrorCode.INVALID_INPUT, "Dropbox search path is not a folder", false, operation);
                }

                if (lookup.isRestrictedContent()) {
                    return error(
                            DropboxErrorCode.PERMISSION_DENIED, "Dropbox search path is restricted", false, operation);
                }
            }

            if (error.isInvalidArgument()) {
                return error(DropboxErrorCode.INVALID_INPUT, "Invalid Dropbox search arguments", false, operation);
            }

            if (error.isInternalError()) {
                return error(
                        DropboxErrorCode.TEMPORARY_UNAVAILABLE,
                        "Dropbox search failed with a temporary internal error",
                        true,
                        operation);
            }
        }

        if (exception instanceof ListFolderContinueErrorException continueException) {
            var error = continueException.errorValue;

            if (error.isPath()) {
                var lookup = error.getPathValue();

                if (lookup.isNotFound()) {
                    return error(DropboxErrorCode.PATH_NOT_FOUND, "Dropbox folder does not exist", false, operation);
                }

                if (lookup.isNotFolder()) {
                    return error(DropboxErrorCode.INVALID_INPUT, "Dropbox path is not a folder", false, operation);
                }

                if (lookup.isRestrictedContent()) {
                    return error(
                            DropboxErrorCode.PERMISSION_DENIED,
                            "Dropbox folder content is restricted",
                            false,
                            operation);
                }
            }

            if (error.isReset()) {
                return error(
                        DropboxErrorCode.INVALID_INPUT,
                        "Dropbox folder cursor is no longer valid and the listing must be restarted",
                        false,
                        operation);
            }
        }

        if (exception instanceof InvalidAccessTokenException) {
            return error(DropboxErrorCode.AUTH_ERROR, "Dropbox access token is invalid or expired", false, operation);
        }

        if (exception instanceof DbxApiException apiException) {
            return error(DropboxErrorCode.INTERNAL_ERROR, safeMessage(apiException), false, operation);
        }

        if (exception instanceof DbxException dbxException) {
            return error(DropboxErrorCode.NETWORK_ERROR, safeMessage(dbxException), true, operation);
        }

        if (exception instanceof IllegalArgumentException) {
            return error(DropboxErrorCode.INVALID_INPUT, safeMessage(exception), false, operation);
        }

        return error(DropboxErrorCode.INTERNAL_ERROR, safeMessage(exception), false, operation);
    }

    public static DropboxError mapRivieraError(String operation, GetMarkdownAsyncError error) {
        MarkdownConversionApiV2Error details = error.getErrorDetails();
        if (details != null) {
            if (details.isServerError()) {
                return error(
                        DropboxErrorCode.TEMPORARY_UNAVAILABLE,
                        "Dropbox markdown conversion is temporarily unavailable",
                        true,
                        operation);
            }
            if (details.isUserError() || details.isLimitExceededError()) {
                return error(DropboxErrorCode.INVALID_INPUT, "Invalid Dropbox markdown request", false, operation);
            }
            if (details.isUnsupportedFormatError()) {
                return error(
                        DropboxErrorCode.OPERATION_NOT_SUPPORTED,
                        "Dropbox does not support this file format for markdown extraction",
                        false,
                        operation);
            }
            if (details.isLinkDownloadDisabledError() || details.isSharedLinkPasswordProtected()) {
                return error(DropboxErrorCode.PERMISSION_DENIED, "Dropbox file cannot be accessed", false, operation);
            }
        }

        return switch (error.getErrorCode()) {
            case BAD_REQUEST ->
                error(DropboxErrorCode.INVALID_INPUT, "Invalid Dropbox markdown request", false, operation);
            case ACCESS_ERROR ->
                error(DropboxErrorCode.PERMISSION_DENIED, "Dropbox file cannot be accessed", false, operation);
            case RATELIMIT_ERROR ->
                error(DropboxErrorCode.RATE_LIMITED, "Dropbox rate limit exceeded", true, operation);
            case UNAVAILABLE ->
                error(
                        DropboxErrorCode.TEMPORARY_UNAVAILABLE,
                        "Dropbox markdown conversion is temporarily unavailable",
                        true,
                        operation);
            case API_ERROR, UNKNOWN_ERROR, OTHER ->
                error(DropboxErrorCode.INTERNAL_ERROR, "Dropbox markdown conversion failed", false, operation);
        };
    }

    private static DropboxError mapLookupError(String operation, LookupError error, String subject) {
        if (error.isNotFound()) {
            return error(DropboxErrorCode.PATH_NOT_FOUND, subject + " does not exist", false, operation);
        }
        if (error.isNotFile() || error.isNotFolder() || error.isMalformedPath()) {
            return error(DropboxErrorCode.INVALID_INPUT, "Invalid " + subject.toLowerCase(), false, operation);
        }
        if (error.isRestrictedContent()) {
            return error(DropboxErrorCode.PERMISSION_DENIED, subject + " is restricted", false, operation);
        }
        if (error.isLocked()) {
            return error(DropboxErrorCode.TEMPORARY_UNAVAILABLE, subject + " is locked", true, operation);
        }
        return error(DropboxErrorCode.INTERNAL_ERROR, "Dropbox lookup failed", false, operation);
    }

    private static DropboxError error(DropboxErrorCode code, String message, boolean retryable, String operation) {
        return new DropboxError(code.name(), message, retryable, operation);
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }
}
