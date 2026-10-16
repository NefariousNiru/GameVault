#!/usr/bin/env bash
# Checks only for presence of release signing secrets. Never print credentials.
set -euo pipefail

for key in GV_RELEASE_STORE_FILE GV_RELEASE_STORE_PASSWORD GV_RELEASE_KEY_ALIAS GV_RELEASE_KEY_PASSWORD; do
  if [[ -z "${!key:-}" ]]; then
    printf 'Release signing missing: %s\n' "$key" >&2
    exit 1
  fi
done

if [[ ! -s "$GV_RELEASE_STORE_FILE" ]]; then
  printf 'Release signing keystore does not exist or is empty.\n' >&2
  exit 1
fi

printf 'Release signing configuration is present.\n'
