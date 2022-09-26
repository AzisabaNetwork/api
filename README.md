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
      "/servers/life/spawners",
      "/players/{uuid}/punishments",
      "/punishments/{id}"
    ]
  }
}
```

### Other routes and parameters

- `/servers/life/auctions?includeExpired=true` - Get all auctions (`includeExpired` defaults to false)
- `/servers/life/spawners?child_server=lifepve1` - Get spawners on a specific child server (`child_server` defaults to all servers)

### Implemented features

- Add `?pretty` at the end of the URL to get pretty JSON instead of minified JSON.
- [#1](https://github.com/AzisabaNetwork/api/issues/1)
- [#2](https://github.com/AzisabaNetwork/api/issues/2)
- [#3](https://github.com/AzisabaNetwork/api/issues/3)

### Authenticating

Use the header `Authorization: Bearer <token>` to authenticate.
