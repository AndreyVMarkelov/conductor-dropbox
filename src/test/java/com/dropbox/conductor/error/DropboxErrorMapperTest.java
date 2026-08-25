package com.dropbox.conductor.error;

import com.dropbox.core.DbxException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropboxErrorMapperTest {

    @Test
    void mapsIllegalArgumentExceptionToInvalidInput() {
        DropboxError error = DropboxErrorMapper.map(
                "upload_file",
                new IllegalArgumentException("bad mode")
        );

        assertEquals("INVALID_INPUT", error.code());
        assertEquals("bad mode", error.message());
        assertFalse(error.retryable());
        assertEquals("upload_file", error.operation());
    }

    @Test
    void mapsDbxExceptionToRetryableNetworkError() {
        DropboxError error = DropboxErrorMapper.map(
                "download_file",
                new DbxException("network failure")
        );

        assertEquals("NETWORK_ERROR", error.code());
        assertEquals("network failure", error.message());
        assertTrue(error.retryable());
        assertEquals("download_file", error.operation());
    }

    @Test
    void mapsUnknownExceptionToInternalError() {
        DropboxError error = DropboxErrorMapper.map(
                "delete",
                new RuntimeException("boom")
        );

        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals("boom", error.message());
        assertFalse(error.retryable());
        assertEquals("delete", error.operation());
    }

    @Test
    void usesExceptionClassNameWhenMessageMissing() {
        DropboxError error = DropboxErrorMapper.map(
                "move",
                new RuntimeException()
        );

        assertEquals("INTERNAL_ERROR", error.code());
        assertEquals("RuntimeException", error.message());
        assertFalse(error.retryable());
    }
}
