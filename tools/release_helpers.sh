#!/usr/bin/env bash
# Shared release verification helpers. Source from scripts; avoid printing secrets.
locate_release_apk() {
  local apk
  apk="$(find app/build/outputs/apk/release -maxdepth 1 -name '*.apk' -type f -print -quit 2>/dev/null)"
  if [[ -z "$apk" ]]; then echo 'No release APK found' >&2; return 1; fi
  printf '%s\n' "$apk"
}

verify_release_signature() {
  local apk="$1" signer="" candidates=""
  if [[ -n "${ANDROID_HOME:-}" ]]; then candidates="${ANDROID_HOME}/build-tools";
  elif [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then candidates="${ANDROID_SDK_ROOT}/build-tools"; fi
  if [[ -n "$candidates" && -d "$candidates" ]]; then
    signer="$(find "$candidates" -name apksigner -type f -print | sort -V | tail -n 1)"
  fi
  if [[ -z "$signer" ]]; then signer="$(command -v apksigner || true)"; fi
  if [[ -z "$signer" ]]; then
    echo 'apksigner not found (set ANDROID_HOME or ANDROID_SDK_ROOT)' >&2; return 1
  fi
  "$signer" verify --verbose --print-certs "$apk"
}
