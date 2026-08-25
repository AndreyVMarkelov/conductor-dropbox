# conductor-dropbox

Dropbox integration for Conductor and Orkes workflows.

## Authentication

`conductor-dropbox` supports two authentication modes.

### Recommended: refresh token

Use a Dropbox app key together with a long-lived refresh token:

```sh
export DROPBOX_APP_KEY=your_app_key
export DROPBOX_REFRESH_TOKEN=your_refresh_token
```

The Dropbox Java SDK automatically obtains and refreshes short-lived access tokens at runtime.

This is the recommended mode for long-running workers.

### Development: access token

For local development, you can also provide a short-lived access token directly:

```sh
export DROPBOX_ACCESS_TOKEN=your_access_token
```

If both authentication modes are configured, refresh-token authentication takes precedence.

### PKCE / OAuth login

The worker does not currently perform an interactive OAuth login flow.

A PKCE-based bootstrap flow will be added separately so users can authorize Dropbox and obtain a refresh token without embedding an app secret in the worker.

## Workflow examples

Ready-to-run workflow definitions, task definitions, and screenshots are available under [`examples/workflows`](examples/workflows/README.md).

Current examples include:

- Process Dropbox Folder — list a folder, iterate entries, skip folders, and extract Markdown from files with Dropbox Riviera.
- File Organizer — route files by extension and move them into destination folders.
