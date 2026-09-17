#!/bin/bash
set -eu
# 401 demonstra API/Security respondendo; a saúde do PostgreSQL é separada.
exec 3<>/dev/tcp/127.0.0.1/8080
printf 'GET /api/usuarios/me HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3
IFS= read -r status <&3
[[ "$status" == HTTP/*" 401 "* ]]
