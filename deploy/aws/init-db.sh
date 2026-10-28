#!/bin/sh
set -eu

existing=$(psql -X -v ON_ERROR_STOP=1 -Atc "SELECT to_regclass('public.clients') IS NOT NULL")
if [ "$existing" = t ]; then
    echo "sLEAPy schema already exists; refusing to reseed existing accounts or trades."
    exit 0
fi

psql -X -v ON_ERROR_STOP=1 --single-transaction \
    -f /seed/tables.sql -f /seed/mockdata.sql -f /seed/analystSchema.sql
echo "sLEAPy demo schema and mock data initialized."
