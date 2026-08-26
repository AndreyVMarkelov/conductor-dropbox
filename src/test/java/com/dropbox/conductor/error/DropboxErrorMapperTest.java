package com.dropbox.conductor.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.dropbox.core.RetryException;
import com.dropbox.core.ServerException;
import com.dropbox.core.v2.files.DownloadError;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.GetMetadataError;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderContinueError;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.LookupError;
import com.dropbox.core.v2.files.RelocationError;
import com.dropbox.core.v2.files.RelocationErrorException;
import com.dropbox.core.v2.files.SearchError;
import com.dropbox.core.v2.files.SearchErrorException;
import com.dropbox.core.v2.files.UploadError;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.UploadWriteFailed;
import com.dropbox.core.v2.files.WriteConflictError;
import com.dropbox.core.v2.files.WriteError;
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
    void mapsSdkRetryAndServerErrorsAsRetryableTemporaryUnavailable() {
        DropboxError retryError = DropboxErrorMapper.map("upload_file", new RetryException("request-id", "retry"));
        DropboxError serverError = DropboxErrorMapper.map("upload_file", new ServerException("request-id", "server"));

        assertEquals(DropboxErrorCode.TEMPORARY_UNAVAILABLE.name(), retryError.code());
        assertTrue(retryError.retryable());
        assertEquals(DropboxErrorCode.TEMPORARY_UNAVAILABLE.name(), serverError.code());
        assertTrue(serverError.retryable());
    }

    @Test
    void mapsDownloadPathNotFound() {
        DownloadErrorException exception = new DownloadErrorException(
                "request-id", "path/not_found/", null, DownloadError.path(LookupError.NOT_FOUND));

        DropboxError error = DropboxErrorMapper.map("download_file", exception);

        assertEquals(DropboxErrorCode.PATH_NOT_FOUND.name(), error.code());
        assertEquals("Dropbox download path does not exist", error.message());
        assertFalse(error.retryable());
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

    @Test
    void mapsInvalidAccessTokenAsNonRetryableAuthError() {
        InvalidAccessTokenException exception = mock(InvalidAccessTokenException.class);

        DropboxError error = DropboxErrorMapper.map("list_folder", exception);

        assertEquals(DropboxErrorCode.AUTH_ERROR.name(), error.code());
        assertEquals("Dropbox access token is invalid or expired", error.message());
        assertFalse(error.retryable());
        assertEquals("list_folder", error.operation());
    }

    @Test
    void mapsGetMetadataNotFoundAsNonRetryablePathNotFound() {
        GetMetadataErrorException exception = new GetMetadataErrorException(
                "request-id", "path/not_found/", null, GetMetadataError.path(LookupError.NOT_FOUND));

        DropboxError error = DropboxErrorMapper.map("get_metadata", exception);

        assertEquals(DropboxErrorCode.PATH_NOT_FOUND.name(), error.code());
        assertEquals("Dropbox path does not exist", error.message());
        assertFalse(error.retryable());
        assertEquals("get_metadata", error.operation());
    }

    @Test
    void mapsInvalidSearchArgumentAsNonRetryableInvalidInput() {
        SearchErrorException exception = new SearchErrorException(
                "request-id", "invalid_argument/", null, SearchError.invalidArgument("invalid search"));

        DropboxError error = DropboxErrorMapper.map("search", exception);

        assertEquals(DropboxErrorCode.INVALID_INPUT.name(), error.code());
        assertEquals("Invalid Dropbox search arguments", error.message());
        assertFalse(error.retryable());
        assertEquals("search", error.operation());
    }

    @Test
    void mapsSearchInternalErrorAsRetryableTemporaryUnavailable() {
        SearchErrorException exception =
                new SearchErrorException("request-id", "internal_error/", null, SearchError.INTERNAL_ERROR);

        DropboxError error = DropboxErrorMapper.map("search", exception);

        assertEquals(DropboxErrorCode.TEMPORARY_UNAVAILABLE.name(), error.code());
        assertEquals("Dropbox search failed with a temporary internal error", error.message());
        assertTrue(error.retryable());
        assertEquals("search", error.operation());
    }

    @Test
    void mapsListFolderCursorResetAsNonRetryableInvalidInput() {
        ListFolderContinueErrorException exception =
                new ListFolderContinueErrorException("request-id", "reset/", null, ListFolderContinueError.RESET);

        DropboxError error = DropboxErrorMapper.map("list_folder", exception);

        assertEquals(DropboxErrorCode.INVALID_INPUT.name(), error.code());
        assertEquals("Dropbox folder cursor is no longer valid and the listing must be restarted", error.message());
        assertFalse(error.retryable());
        assertEquals("list_folder", error.operation());
    }

    private static RelocationErrorException relocationException(RelocationError error) {
        return new RelocationErrorException("2/files/move_v2", "test-request-id", null, error);
    }

    private static UploadErrorException uploadException(UploadError error) {
        return new UploadErrorException("2/files/upload", "test-request-id", null, error);
    }
}
