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
      "/players/{uuid}",
      "/players/by-name/{name}",
      "/servers/life/auctions",
      "/servers/life/auctions/{id}",
      "/players/{uuid}/punishments",
      "/punishments/{id}"
    ]
  }
}
```

- `/servers/life/auctions?includeExpired=true` - Get all auctions (`includeExpired` defaults to false)

### Implemented features

- Add `?pretty` at the end of the URL to get pretty JSON instead of minified JSON.
- [#1](https://github.com/AzisabaNetwork/api/issues/1)
- [#2](https://github.com/AzisabaNetwork/api/issues/2)
- [#3](https://github.com/AzisabaNetwork/api/issues/3)

### Authenticating

Use the header `Authorization: Bearer <token>` to authenticate.
