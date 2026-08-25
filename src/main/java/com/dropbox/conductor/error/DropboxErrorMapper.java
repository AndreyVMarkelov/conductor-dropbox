package com.dropbox.conductor.error;

import com.dropbox.core.DbxApiException;
import com.dropbox.core.DbxException;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.DeleteErrorException;
import com.dropbox.core.v2.files.RelocationErrorException;
import com.dropbox.core.v2.files.UploadErrorException;

public final class DropboxErrorMapper {

    private DropboxErrorMapper() {}

    public static DropboxError map(String operation, Exception exception) {
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
