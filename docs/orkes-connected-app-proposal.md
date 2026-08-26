# Dropbox Connected App for Orkes

## Overview

This proposal describes a native Dropbox Connected App for Orkes.

The integration would allow Orkes workflows to work directly with Dropbox files, folders, and document content using Orkes-managed authentication.

A working open-source reference implementation already exists in `conductor-dropbox`. It provides Dropbox workers, OAuth refresh-token support, PKCE authorization, structured error handling, retry-aware behavior, Dropbox Riviera Markdown extraction, and end-to-end workflow examples.

Repository: https://github.com/AndreyVMarkelov/conductor-dropbox

## Proposed Category

**Productivity**

## Goals

The Connected App should:

- make Dropbox operations available directly in the Orkes workflow builder
- use Orkes-managed authentication and credentials
- expose workflow-oriented operations rather than Dropbox SDK implementation details
- provide predictable inputs, outputs, errors, retries, and pagination
- support document-processing and AI workflows through Dropbox Riviera

The goal is not to require Orkes users to deploy the existing `conductor-dropbox` worker. The OSS project serves as a working reference implementation and validation environment for the native integration.

## Authentication

Dropbox supports OAuth 2.0 with short-lived access tokens and long-lived refresh tokens.

The existing OSS implementation supports OAuth 2.0, offline access, automatic access-token refresh, and PKCE authorization without an application secret.

For the native Connected App, authentication should follow Orkes OAuth conventions. The exact client configuration, redirect handling, credential ownership, and refresh-token storage should be agreed with Orkes.

```text
User
  ↓
Orkes OAuth flow
  ↓
Dropbox authorization
  ↓
Orkes-managed credentials
  ↓
Dropbox Connected App operations
```

The user should not need to manually manage short-lived Dropbox access tokens.

## MVP Operations

| Orkes operation | Reference implementation | Purpose |
| --- | --- | --- |
| List Folder | `dropbox_list_folder` | List files and folders at a Dropbox path |
| Get Metadata | planned `dropbox_get_metadata` | Retrieve file or folder metadata |
| Create Folder | `dropbox_create_folder` | Create a Dropbox folder |
| Move File or Folder | `dropbox_move` | Move or rename a file or folder |
| Delete File or Folder | `dropbox_delete` | Delete a file or folder |
| Upload File From Base64 | upload worker | Upload file content |
| Download File Base64 | download worker | Download file content |
| Search Files and Folders | planned search worker | Search Dropbox |
| Extract Markdown | `dropbox_extract_markdown` | Extract Markdown using Dropbox Riviera |

## Operation Contracts

### List Folder

Inputs:

- `path`
- `recursive`
- `includeDeleted`

Outputs:

- `entries`
- `cursor`
- `hasMore`

Entry metadata may include `type`, `id`, `name`, `pathLower`, `pathDisplay`, `rev`, `size`, and `contentHash`.

Dropbox cursors should remain pagination state rather than requiring a separate user-facing Continue Listing operation unless that matches Orkes conventions.

### Get Metadata

Inputs:

- `path`

Potential future support: file ID.

Outputs may include `type`, `id`, `name`, `pathLower`, `pathDisplay`, `rev`, `size`, and `contentHash`.

### Create Folder

Input: `path`.

Output: created folder metadata.

### Move File or Folder

Inputs:

- `fromPath`
- `toPath`

Output: destination metadata.

### Delete File or Folder

Input: `path`.

Output: deleted resource metadata.

### Upload File From Base64

Inputs:

- `path`
- Base64-encoded content
- optional write mode

Output: uploaded file metadata.

Base64 provides a simple MVP contract consistent with file-oriented Connected Apps. For large files, a platform-supported payload/content-reference mechanism may be preferable. Dropbox upload-session details should remain internal where possible.

### Download File Base64

Inputs:

- `path` or file ID

Outputs:

- Base64-encoded content
- file metadata

For large files, a payload/reference mechanism should be considered to avoid unnecessary Base64 overhead.

### Search Files and Folders

Inputs:

- `query`
- optional path/scope
- pagination options where supported

Outputs:

- matching entries
- pagination state

### Extract Markdown

Input: Dropbox file ID.

Outputs:

- `markdown`
- `asyncJobId`

This operation uses Dropbox Riviera and enables document-processing and AI workflows without requiring users to download and locally parse supported documents.

Riviera extraction may be asynchronous. Retry behavior should preserve the underlying extraction job rather than blindly creating a new job.

## Error Model

Proposed stable workflow-level categories:

- `AUTH_ERROR`
- `RATE_LIMITED`
- `NOT_FOUND`
- `CONFLICT`
- `PERMISSION_DENIED`
- `INVALID_INPUT`
- `NETWORK_ERROR`

Errors should indicate whether retrying is safe. Dropbox rate-limit responses should respect server-provided retry guidance.

The OSS reference implementation already maps Dropbox failures into structured workflow errors.

## Retry and Idempotency

Read operations such as List Folder, Get Metadata, Search, and Download are generally safe to retry.

Mutation operations require operation-specific retry behavior. The integration should avoid retries that duplicate side effects or turn a successful mutation into a later conflict.

Asynchronous Riviera extraction should not automatically start duplicate jobs.

## Example Use Cases

### Document Processing

```text
Dropbox Folder
      ↓
List Folder
      ↓
For Each File
      ↓
Extract Markdown
      ↓
AI / LLM Processing
      ↓
Store / Route Result
```

### File Organizer

```text
Incoming
    ↓
List Folder
    ↓
For Each File
    ↓
Route by Type
   ↙    ↓     ↘
Data Documents Other
   ↓    ↓      ↓
Move File or Folder
```

Both patterns are represented by end-to-end tested workflows in the OSS project.

### Document Intake

```text
Incoming Documents
        ↓
Dropbox Metadata
        ↓
Classification
        ↓
Extract Markdown
        ↓
Business Workflow
```

Possible applications include invoice processing, support workflows, knowledge ingestion, classification, summarization, and RAG pipelines.

## Differentiator

In addition to standard file-management primitives—list, metadata, create, move, delete, upload, download, and search—the integration exposes:

**Extract Markdown with Dropbox Riviera**

This makes Dropbox useful as a document-processing primitive for AI and RAG workflows, not only as a filesystem connector.

## Existing Reference Implementation

`conductor-dropbox` currently provides:

- Dropbox file and folder workers
- OAuth refresh-token authentication
- automatic short-lived access-token refresh
- PKCE authorization without an app secret
- credential-provider abstraction
- structured Dropbox error mapping
- retry-aware task behavior
- Dropbox Riviera Markdown extraction
- sequential folder processing
- file routing and organization
- end-to-end Orkes/Conductor examples and screenshots

## OSS and Native Connected App Relationship

### OSS `conductor-dropbox`

```text
Environment / Secret Manager
          ↓
DropboxCredentialsProvider
          ↓
DropboxClientProvider
          ↓
Conductor Workers
          ↓
Dropbox API
```

Useful for self-hosted Conductor and users who operate their own workers.

### Native Orkes Connected App

```text
Orkes
  ↓
Managed OAuth Credentials
  ↓
Dropbox Connected App
  ↓
Dropbox API
```

This would expose Dropbox directly in the Orkes workflow builder without requiring worker deployment.

## Future Scope

- shared links
- revisions and restore
- batch operations
- file locking
- Dropbox metadata
- change detection
- richer Riviera/document-processing operations
- Dropbox team authentication
- member-scoped execution
- team namespace support

Team authentication should be a separate milestone from individual-account OAuth.

## Open Questions for Orkes

1. What is the preferred process for proposing and contributing a new Connected App?
2. Should Dropbox OAuth use an Orkes-managed OAuth application or customer-provided Dropbox app credentials?
3. What OAuth redirect and credential-storage conventions should Dropbox follow?
4. Does the Connected App framework support PKCE, or should Dropbox follow the same confidential-client model as existing OAuth integrations?
5. What is the preferred representation for uploaded and downloaded binary content?
6. Should Base64 be used for the MVP, or is there a platform payload/reference mechanism better suited to large files?
7. What pagination convention should List Folder and Search follow?
8. What retry and rate-limit conventions should Connected Apps use?
9. How should asynchronous operations such as Riviera extraction be represented?
10. Should the initial integration support individual Dropbox accounts only, or individual and Dropbox team accounts?
11. What requirements apply to documentation, testing, security review, and marketplace publication?

## Proposed Initial Scope

```text
Dropbox — Productivity

Authentication
└── OAuth 2.0

Files and Folders
├── List Folder
├── Get Metadata
├── Create Folder
├── Move File or Folder
├── Delete File or Folder
├── Upload File From Base64
├── Download File Base64
└── Search Files and Folders

Document Processing
└── Extract Markdown
```

This scope provides complete basic file automation while also exposing a differentiated Dropbox document-processing capability for AI workflows.
