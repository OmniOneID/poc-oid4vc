# OID4VP File-based Configuration

The files in this directory are used only when `oid4vp.repository.type` is set to `file`.

## File Descriptions

| File | Description |
|------|------|
| `oid4vp-config.json` | Default OID4VP settings |
| `dcql-scope-mappings.json` | DCQL Scope mapping configurations |

## How to Activate

Configure the following in your `application.yml` file:

```yaml
oid4vp:
  repository:
    type: file