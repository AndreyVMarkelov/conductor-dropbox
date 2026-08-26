# Roadmap

This roadmap describes the current direction for `conductor-dropbox`.
Priorities may change based on feedback and integration requirements.

## v0.1 — Core Dropbox workflows

- [x] Upload and download files
- [x] Create, move, and delete files and folders
- [x] List folders
- [x] Get metadata
- [x] Search files and folders
- [x] Cursor-based pagination
- [x] OAuth refresh-token authentication
- [x] PKCE login
- [x] Structured errors and retry semantics
- [x] Dropbox Riviera Markdown extraction
- [x] Docker deployment
- [x] End-to-end workflow examples

## v0.2 — Dropbox Sign

The next priority is integrating Dropbox Sign with durable Conductor workflows.

Planned capabilities:

- [ ] Dropbox Sign authentication
- [ ] Create and send signature requests
- [ ] Get signature request status
- [ ] Download signed documents
- [ ] Cancel signature requests
- [ ] Structured Dropbox Sign errors
- [ ] Store completed signed documents in Dropbox
- [ ] End-to-end Dropbox → Sign → Dropbox workflow

Target workflow:

```text
Dropbox document
      ↓
Conductor
      ↓
Dropbox Sign
      ↓
Signature request
      ↓
Wait for completion
      ↓
Download signed document
      ↓
Dropbox
```

## v0.3 — Webhooks and durable events

Add event-driven execution for Dropbox and Dropbox Sign.

- [ ] Dropbox webhooks
- [ ] Dropbox Sign webhooks
- [ ] Conductor workflow resume from webhook events
- [ ] Dropbox change cursors
- [ ] Incremental file-change processing
- [ ] Cursor checkpointing and recovery
- [ ] Durable event-processing examples

Target workflow:

```text
Dropbox / Dropbox Sign
          ↓
       Webhook
          ↓
      Conductor
          ↓
 Resume durable workflow
```

This milestone will allow signature workflows to wait for completion without polling.

## v0.4 — Dropbox Business

- [ ] Dropbox team authentication
- [ ] Team-scoped credentials
- [ ] Member-scoped execution
- [ ] Team namespace support
- [ ] Team folder workflows

## Future

Potential future capabilities include:

- Shared links
- Revisions and restore
- File locking
- Batch operations
- Dropbox metadata
- Change-driven document processing
- AI and RAG workflow examples
- Additional Dropbox Riviera capabilities

## Orkes Connected App

The OSS integration will continue to work independently of native Orkes support.

In parallel, the project will explore:

- Native Dropbox Connected App support
- Orkes-managed Dropbox OAuth
- Native binary payload handling
- Native pagination and asynchronous operation conventions
- Dropbox Sign integration in Orkes

