# API (Ktor implementation)

## Server

Rate limit is 120 requests per minute.

### Routes

```json
{
  "routes": {
    "GET": [
      "/",
      "/counts",
      "/players/{uuid}"
    ]
  }
}
```

### Implemented features

- Add `?pretty` at the end of the URL to get pretty JSON instead of minified JSON.
- [#1](https://github.com/AzisabaNetwork/api/issues/1)
- [#2](https://github.com/AzisabaNetwork/api/issues/2)

### Authenticating

Use the header `Authorization: Bearer <token>` to authenticate.
