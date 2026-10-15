
#!/usr/bin/env python3
"""Package Android source code and build configuration for AI review."""

from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "GameVault-source.zip"

# Directories containing source code, tests, and Android resources.
INCLUDE_DIRS = [
    "app/src",
    "gradle",
]

# Build configurations and dependency declarations.
INCLUDE_FILES = [
    "settings.gradle.kts",
    "build.gradle.kts",
    "app/build.gradle.kts",
    "gradle/libs.versions.toml",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradle/gradle-daemon-jvm.properties",
    "gradlew",
    "README.md",
]

# Generated content, IDE state, and caches.
EXCLUDE_DIRS = {
    "build", ".gradle", ".git", ".idea",
    ".kotlin", ".cxx", "out", "node_modules",
    "__pycache__", "generated",
}

# Files that may contain credentials or machine-specific settings.
EXCLUDE_FILES = {
    "google-services.json",
    "local.properties",
    "gradle.properties",
    ".env",
    "key.properties",
}

EXCLUDE_SUFFIXES = {".jar", ".apk", ".aab", ".jks", ".keystore", ".class"}

def allowed(path: Path) -> bool:
    """Check whether a file is safe and relevant to package."""
    relative = path.relative_to(ROOT)

    if not path.is_file() or path.is_symlink():
        return False
    if any(part in EXCLUDE_DIRS for part in relative.parts):
        return False
    if path.name in EXCLUDE_FILES:
        return False
    if path.suffix.lower() in EXCLUDE_SUFFIXES:
        return False
    if any(word in path.name.lower() for word in
           ("secret", "credential", "service-account", "private-key")):
        return False

    return True


files = set()

for directory in INCLUDE_DIRS:
    source = ROOT / directory
    if source.exists():
        files.update(p for p in source.rglob("*") if allowed(p))

for filename in INCLUDE_FILES:
    path = ROOT / filename
    if path.exists() and allowed(path):
        files.add(path)

with ZipFile(OUTPUT, "w", ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(files):
        archive.write(path, path.relative_to(ROOT))

print(f"Created: {OUTPUT}")
print(f"Files included: {len(files)}")
print(f"ZIP size: {OUTPUT.stat().st_size / 1024:.1f} KB")
