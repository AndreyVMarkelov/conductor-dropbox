# Idempotency

Conductor tasks may be executed more than once because of retries, timeouts,
worker restarts, or delivery failures.

Dropbox mutation workers therefore define explicit retry semantics.

## create_folder

Creating the requested folder successfully and then retrying the same task may
result in a path conflict.

A retry is considered successful if the requested path already exists and is a
folder representing the expected destination.

Future implementation should distinguish this case from conflicts with an
unexpected object.

## upload_file

### ADD

`ADD` is not inherently idempotent.

If the first upload succeeds but the worker fails before reporting completion,
retrying may produce a conflict or, when autorename is enabled, create another
file.

Workflows requiring deterministic retries should prefer `OVERWRITE` or a
revision-aware strategy.

### OVERWRITE

Uploading to the same deterministic path with `OVERWRITE` is effectively
idempotent with respect to final file location and contents.

## move

A move may succeed in Dropbox while the worker fails before returning its
result.

On retry:

- the source path may no longer exist;
- the destination path may already contain the moved object.

A future retry-aware implementation should verify destination state before
treating this condition as failure.

## delete

Delete is logically idempotent for workflow cleanup.

If the target has already been deleted by a previous attempt, a retry may treat
the missing path as a successful final state.

## download_file

Read-only and naturally idempotent.

