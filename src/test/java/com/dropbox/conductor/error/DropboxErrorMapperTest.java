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

    private static RelocationErrorException relocationException(RelocationError error) {
        return new RelocationErrorException("2/files/move_v2", "test-request-id", null, error);
    }
}
