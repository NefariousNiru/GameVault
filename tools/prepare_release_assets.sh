#!/usr/bin/env bash
# Prepare a directly-downloadable GitHub Release APK, checksum and human-readable notes.
# Invoked only after apksigner successfully verifies the APK.
set -euo pipefail

if (( $# != 2 )); then
  echo 'Usage: prepare_release_assets.sh v1.0.0 path/to/signed.apk' >&2
  exit 2
fi
release_tag="$1"
source_apk="$2"
if [[ ! "$release_tag" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo 'Invalid release tag (expected vMAJOR.MINOR.PATCH)' >&2
  exit 2
fi
if [[ ! -s "$source_apk" || "${source_apk##*.}" != apk ]]; then
  echo 'Signed APK is missing or empty' >&2
  exit 2
fi

apk_name="GameVault-${release_tag}.apk"
release_dir="${GV_RELEASE_DIST_DIR:-dist}"
repository="${GITHUB_REPOSITORY:-NefariousNiru/GameVault}"
mkdir -p "$release_dir"
cp "$source_apk" "${release_dir}/${apk_name}"

(
  cd "$release_dir"
  sha256sum "$apk_name" > SHA256SUMS.txt
  sha256sum --check --status SHA256SUMS.txt
)
sha256="$(awk 'NR == 1 {print $1}' "${release_dir}/SHA256SUMS.txt")"
release_url="https://github.com/${repository}/releases/download/${release_tag}"

cat > "${release_dir}/release-notes.md" <<NOTES
## GameVault ${release_tag}

Keep your PC and PS5 game library, personal ratings, statuses and shared Play Queue in one place.

### Download

**[Download GameVault for Android (.apk)](${release_url}/${apk_name})**

Install the **APK directly** on your Android device (no ZIP extraction needed).

**SHA-256** (of the APK):

\`\`\`text
${sha256}
\`\`\`

[Download SHA256SUMS.txt](${release_url}/SHA256SUMS.txt) to verify the downloaded APK:

\`\`\`bash
sha256sum -c SHA256SUMS.txt
\`\`\`

Run the verification command in the directory containing both the APK and checksum file.

**Note:** The APK is signed with the permanent release certificate indicating it came from this repository

For screenshots, features and setup instructions, see the [GameVault README](https://github.com/${repository}#readme).
NOTES
printf 'Prepared %s and SHA256SUMS.txt (SHA-256: %s)\n' "$apk_name" "$sha256"
