package com.dropbox.conductor.error;

import static org.junit.jupiter.api.Assertions.*;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.*;
import org.junit.jupiter.api.Test;

class DropboxErrorMapperTest {

    @Test
    void mapsIllegalArgumentExceptionToInvalidInput() {
        DropboxError error = DropboxErrorMapper.map("upload_file", new IllegalArgumentException("bad mode"));

        assertEquals("INVALID_INPUT", error.code());
        assertEquals("bad mode", error.message());
        assertFalse(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    @Test
    void mapsDbxExceptionToRetryableNetworkError() {
        DropboxError error = DropboxErrorMapper.map("download_file", new DbxException("network failure"));

        assertEquals("NETWORK_ERROR", error.code());
        assertEquals("network failure", error.message());
        assertTrue(error.retryable());
        assertEquals("download_file", error.operation());
    }

    @Test
    void mapsUnknownExceptionToInternalError() {
        DropboxError error = DropboxErrorMapper.map("delete", new RuntimeException("boom"));

        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals("boom", error.message());
        assertFalse(error.retryable());
        assertEquals("delete", error.operation());
    }

    @Test
    void usesExceptionClassNameWhenMessageMissing() {
        DropboxError error = DropboxErrorMapper.map("move", new RuntimeException());

        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals("RuntimeException", error.message());
        assertFalse(error.retryable());
    }

    @Test
    void mapsMoveSourceNotFound() {
        RelocationErrorException exception = relocationException(RelocationError.fromLookup(LookupError.NOT_FOUND));

        DropboxError error = DropboxErrorMapper.map("move", exception);

        assertEquals("PATH_NOT_FOUND", error.code());
        assertEquals("Dropbox source path does not exist", error.message());
        assertFalse(error.retryable());
        assertEquals("move", error.operation());
    }

    @Test
    void mapsMoveDestinationConflict() {
        RelocationErrorException exception =
                relocationException(RelocationError.to(WriteError.conflict(WriteConflictError.FILE)));

        DropboxError error = DropboxErrorMapper.map("move", exception);

        assertEquals("PATH_CONFLICT", error.code());
        assertEquals("Dropbox destination path already exists", error.message());
        assertFalse(error.retryable());
        assertEquals("move", error.operation());
    }

    @Test
    void mapsMoveInternalErrorAsRetryable() {
        RelocationErrorException exception = relocationException(RelocationError.INTERNAL_ERROR);

        DropboxError error = DropboxErrorMapper.map("move", exception);

        assertEquals("TEMPORARY_UNAVAILABLE", error.code());
        assertEquals("Dropbox move failed with a temporary internal error", error.message());
        assertTrue(error.retryable());
        assertEquals("move", error.operation());
    }

    @Test
    void mapsMovePermissionDenied() {
        RelocationErrorException exception = relocationException(RelocationError.to(WriteError.NO_WRITE_PERMISSION));

        DropboxError error = DropboxErrorMapper.map("move", exception);

        assertEquals("PERMISSION_DENIED", error.code());
        assertEquals("Dropbox path permission denied", error.message());
        assertFalse(error.retryable());
        assertEquals("move", error.operation());
    }

    @Test
    void mapsMoveInsufficientQuota() {
        RelocationErrorException exception = relocationException(RelocationError.INSUFFICIENT_QUOTA);

        DropboxError error = DropboxErrorMapper.map("move", exception);

        assertEquals("QUOTA_EXCEEDED", error.code());
        assertEquals("Insufficient Dropbox storage quota", error.message());
        assertFalse(error.retryable());
        assertEquals("move", error.operation());
    }

    @Test
    void mapsMoveSourcePermissionDenied() {
        RelocationErrorException exception =
                relocationException(RelocationError.fromWrite(WriteError.NO_WRITE_PERMISSION));

        DropboxError error = DropboxErrorMapper.map("move", exception);

        assertEquals("PERMISSION_DENIED", error.code());
        assertFalse(error.retryable());
    }

    @Test
    void mapsUploadPathConflict() {
        UploadErrorException exception = uploadException(UploadError.path(
                new UploadWriteFailed(WriteError.conflict(WriteConflictError.FILE), "test-session-id")));

        DropboxError error = DropboxErrorMapper.map("upload_file", exception);

        assertEquals("PATH_CONFLICT", error.code());
        assertEquals("Dropbox destination path already exists", error.message());
        assertFalse(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    @Test
    void mapsUploadPermissionDenied() {
        UploadErrorException exception = uploadException(
                UploadError.path(new UploadWriteFailed(WriteError.NO_WRITE_PERMISSION, "test-session-id")));

        DropboxError error = DropboxErrorMapper.map("upload_file", exception);

        assertEquals("PERMISSION_DENIED", error.code());
        assertEquals("No permission to write Dropbox destination path", error.message());
        assertFalse(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    @Test
    void mapsUploadInsufficientSpace() {
        UploadErrorException exception = uploadException(
                UploadError.path(new UploadWriteFailed(WriteError.INSUFFICIENT_SPACE, "test-session-id")));

        DropboxError error = DropboxErrorMapper.map("upload_file", exception);

        assertEquals("QUOTA_EXCEEDED", error.code());
        assertEquals("Insufficient Dropbox storage quota", error.message());
        assertFalse(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    @Test
    void mapsUploadTooManyWriteOperationsAsRetryable() {
        UploadErrorException exception = uploadException(
                UploadError.path(new UploadWriteFailed(WriteError.TOO_MANY_WRITE_OPERATIONS, "test-session-id")));

        DropboxError error = DropboxErrorMapper.map("upload_file", exception);

        assertEquals("TEMPORARY_UNAVAILABLE", error.code());
        assertEquals("Too many Dropbox write operations", error.message());
        assertTrue(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    @Test
    void mapsUploadPayloadTooLarge() {
        UploadErrorException exception = uploadException(UploadError.PAYLOAD_TOO_LARGE);

        DropboxError error = DropboxErrorMapper.map("upload_file", exception);

        assertEquals("INVALID_INPUT", error.code());
        assertEquals("Upload payload is too large", error.message());
        assertFalse(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    @Test
    void mapsUploadContentHashMismatch() {
        UploadErrorException exception = uploadException(UploadError.CONTENT_HASH_MISMATCH);

        DropboxError error = DropboxErrorMapper.map("upload_file", exception);

        assertEquals("INVALID_INPUT", error.code());
        assertEquals("Upload content hash mismatch", error.message());
        assertFalse(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    @Test
    void mapsUploadEncryptionNotSupported() {
        UploadErrorException exception = uploadException(UploadError.ENCRYPTION_NOT_SUPPORTED);

        DropboxError error = DropboxErrorMapper.map("upload_file", exception);

        assertEquals("OPERATION_NOT_SUPPORTED", error.code());
        assertEquals("Dropbox API does not support the required file encryption", error.message());
        assertFalse(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    private static RelocationErrorException relocationException(RelocationError error) {
        return new RelocationErrorException("2/files/move_v2", "test-request-id", null, error);
    }

    private static UploadErrorException uploadException(UploadError error) {
        return new UploadErrorException("2/files/upload", "test-request-id", null, error);
    }
}
