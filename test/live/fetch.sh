#!/usr/bin/env bash
# Downloads the official Paper build and test plugins into work/, and checks each against its published checksum.
# Usage: test/live/fetch.sh <paper version>...   e.g. test/live/fetch.sh 26.2 26.3
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
WORK=${MAYOR_LIVE_DIR:-$HERE/work}
mkdir -p "$WORK/downloads"
UA="mayor-live-tests"

get() { # url, file, algorithm, expected hash
  local url=$1 file=$2 algo=$3 want=$4 got
  if [ -f "$WORK/$file" ]; then
    got=$(shasum -a "$algo" "$WORK/$file" | cut -d' ' -f1)
    [ "$got" = "$want" ] && { echo "have $file"; return; }
  fi
  curl -fsSL -A "$UA" -o "$WORK/downloads/$file" "$url"
  got=$(shasum -a "$algo" "$WORK/downloads/$file" | cut -d' ' -f1)
  if [ "$got" != "$want" ]; then
    rm -f "$WORK/downloads/$file"
    echo "Checksum mismatch for $file: expected $want, got $got"; exit 1
  fi
  mv "$WORK/downloads/$file" "$WORK/$file"
  echo "fetched $file (sha$algo checked)"
}

for v in "$@"; do
  read -r url sha < <(curl -fsSL -A "$UA" "https://fill.papermc.io/v3/projects/paper/versions/$v/builds/latest" \
    | python3 -c 'import json,sys; s=json.load(sys.stdin)["downloads"]["server:default"]; print(s["url"], s["checksums"]["sha256"])')
  get "$url" "paper-$v.jar" 256 "$sha"
done

for p in ViaVersion ViaBackwards; do
  v=$(curl -fsSL -A "$UA" "https://hangar.papermc.io/api/v1/projects/$p/latestrelease")
  read -r url sha < <(curl -fsSL -A "$UA" "https://hangar.papermc.io/api/v1/projects/$p/versions/$v" \
    | python3 -c 'import json,sys; d=json.load(sys.stdin)["downloads"]["PAPER"]; print(d["downloadUrl"], d["fileInfo"]["sha256Hash"])')
  get "$url" "$p.jar" 256 "$sha"
done

# LuckPerms from its official Modrinth project, which publishes SHA-512 hashes.
read -r url sha < <(curl -fsSL -A "$UA" 'https://api.modrinth.com/v2/project/luckperms/version?loaders=%5B%22paper%22%2C%22bukkit%22%5D' \
  | python3 -c 'import json,sys; v=json.load(sys.stdin)[0]; f=[f for f in v["files"] if f["primary"]][0]; print(f["url"], f["hashes"]["sha512"])')
get "$url" "LuckPerms.jar" 512 "$sha"
rmdir "$WORK/downloads" 2>/dev/null || true
