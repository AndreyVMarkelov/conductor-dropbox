# Workflow Examples

## Process Dropbox Folder

`process-folder.json` demonstrates how to:

- list a Dropbox folder
- iterate entries sequentially
- skip folders
- extract Markdown from files with Dropbox Riviera

### Workflow

![Process Dropbox folder workflow](../../docs/images/process-folder-workflow.png)

### Successful execution

![Successful folder processing execution](../../docs/images/process-folder-execution.png)

### Register task definitions

```sh
jq -s '.' \
  examples/taskdefs/dropbox-list-folder.json \
  examples/taskdefs/dropbox-extract-markdown.json \
  | curl -s -X POST \
      http://localhost:8080/api/metadata/taskdefs \
      -H 'Content-Type: application/json' \
      -d @-
```

### Register workflow

```sh
curl -s -X POST \
  http://localhost:8080/api/metadata/workflow \
  -H 'Content-Type: application/json' \
  -d @examples/workflows/process-folder.json
```

### Run

```sh
curl -s -X POST \
  'http://localhost:8080/api/workflow/dropbox_process_folder?version=1' \
  -H 'Content-Type: application/json' \
  -d '{
    "path": "/conductor-dropbox-tests"
  }'
```

The workflow processes entries one by one. Folder entries are skipped. File entries are passed to `dropbox_extract_markdown`.

`dropbox_extract_markdown` uses `retryCount: 0` because retrying the whole task can launch a new Riviera extraction job.
