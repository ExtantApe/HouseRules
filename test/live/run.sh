#!/usr/bin/env bash
# Builds one throwaway Paper server on 127.0.0.1, runs the HouseRules acceptance tests, and deletes it, even when
# a test fails. See README.md in this folder.
# Usage: HR_ACCEPT_EULA=1 test/live/run.sh <paper version>
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$HERE/../.." && pwd)
WORK=$(mkdir -p "${HR_LIVE_DIR:-$HERE/work}" && cd "${HR_LIVE_DIR:-$HERE/work}" && pwd)
PORT=${HR_PORT:-25619}
VERSION=${1:?Usage: run.sh <paper version>}

if [ "${HR_ACCEPT_EULA:-}" != "1" ]; then
  echo "This starts a Minecraft server, which means accepting the Minecraft EULA (https://aka.ms/MinecraftEULA)."
  echo "Set HR_ACCEPT_EULA=1 to accept it."
  exit 2
fi
JAR=$(ls "$REPO"/build/libs/HouseRules-*.jar 2>/dev/null | head -1)
HELPER=$REPO/test/live/helper/build/libs/LiveTestHelper.jar
# Which share stand-in to install: "new" (named BlueMinimap, the default) or "old" (named BlueMapHUD, 0.1.x).
export HR_SHARE_PLUGIN=${HR_SHARE_PLUGIN:-new}
case "$HR_SHARE_PLUGIN" in
  new) FAKEHUD=$REPO/test/live/fakeminimap/build/libs/LiveTestFakeMinimap.jar ;;
  old) FAKEHUD=$REPO/test/live/fakehud/build/libs/LiveTestFakeHud.jar ;;
  *) echo "HR_SHARE_PLUGIN must be new or old"; exit 2 ;;
esac
missing=0
for f in "$WORK/paper-$VERSION.jar" "$WORK/ViaVersion.jar" "$WORK/ViaBackwards.jar" "$WORK/LuckPerms.jar"; do
  [ -f "$f" ] || { echo "Missing $f. Run test/live/fetch.sh $VERSION first."; missing=1; }
done
for f in "$JAR" "$HELPER" "$FAKEHUD"; do [ -n "$f" ] && [ -f "$f" ] || { echo "Missing a built jar. Run ./gradlew build first."; missing=1; }; done
[ -d "$HERE/node_modules/mineflayer" ] || { echo "Run npm ci in test/live first."; missing=1; }
[ $missing = 0 ] || exit 2
if pgrep -f "server-[0-9.]*/.*paper-|paper-.*\.jar nogui" >/dev/null && [ -z "${HR_ALLOW_OTHER:-}" ]; then
  echo "Note: another Paper server seems to be running on this machine. Port $PORT is used for this one."
fi

NAME=server-$VERSION
SERVER=$WORK/$NAME
remove_server() { case "$SERVER" in "$WORK"/server-*) rm -rf -- "$SERVER" ;; esac; }
trap remove_server EXIT
remove_server

echo "== Building a throwaway Paper $VERSION server in $SERVER"
mkdir -p "$SERVER/plugins"
echo "eula=true" > "$SERVER/eula.txt"
cat > "$SERVER/server.properties" <<PROPS
server-ip=127.0.0.1
server-port=$PORT
query.port=$PORT
online-mode=false
enforce-secure-profile=false
white-list=false
max-players=40
spawn-protection=0
difficulty=peaceful
spawn-monsters=false
level-type=minecraft\:flat
generate-structures=false
view-distance=4
simulation-distance=4
motd=HouseRules throwaway test server
PROPS
cp "$WORK/ViaVersion.jar" "$WORK/ViaBackwards.jar" "$WORK/LuckPerms.jar" "$HELPER" "$FAKEHUD" "$SERVER/plugins/"
cp "$JAR" "$SERVER/plugins/HouseRules.jar"
cd "$HERE"
LOG=$WORK/$NAME.log
echo "== Running the suite (log: $LOG)"
status=0
node suite.js "$NAME" "paper-$VERSION.jar" "$PORT" > "$LOG" 2>&1 || status=1
grep -E "^(FAIL|SKIP)|^SUMMARY|CRASHED" "$LOG" || true
echo "== Deleting $SERVER"
exit $status
