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

`conductor-dropbox` includes a PKCE-based login helper that can obtain a long-lived Dropbox refresh token without using an app secret.

Set your Dropbox app key:

```sh
export DROPBOX_APP_KEY=your_app_key
```

Run:

```sh
./gradlew dropboxLogin
```

The command prints a Dropbox authorization URL.

Open the URL in your browser, authorize the app, copy the authorization code, and paste it back into the terminal.

After a successful login, the command prints:

```sh
export DROPBOX_APP_KEY=...
export DROPBOX_REFRESH_TOKEN=...
```

Export those values and start the worker:

```sh
unset DROPBOX_ACCESS_TOKEN

export DROPBOX_APP_KEY=...
export DROPBOX_REFRESH_TOKEN=...

./gradlew run
```

The Dropbox Java SDK will automatically obtain and refresh short-lived access tokens at runtime.

## Workflow examples

Ready-to-run workflow definitions, task definitions, and screenshots are available under [`examples/workflows`](examples/workflows/README.md).

Current examples include:

- Process Dropbox Folder — list a folder, iterate entries, skip folders, and extract Markdown from files with Dropbox Riviera.
- File Organizer — route files by extension and move them into destination folders.
