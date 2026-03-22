#!/bin/bash
set -e

# --- 設定 ---
MC_VERSION="1.20.1"
VERSION_FILE="VERSION"

# --- バージョン読み込み ---
if [ ! -f "$VERSION_FILE" ]; then
    echo "1.0.0" > "$VERSION_FILE"
fi

CURRENT=$(cat "$VERSION_FILE")
MAJOR=$(echo "$CURRENT" | cut -d. -f1)
MINOR=$(echo "$CURRENT" | cut -d. -f2)
PATCH=$(echo "$CURRENT" | cut -d. -f3)

echo "現在のバージョン: ${CURRENT}"
echo ""
echo "バージョン:"
echo "1) patch  (${MAJOR}.${MINOR}.$((PATCH + 1)))"
echo "2) minor  (${MAJOR}.$((MINOR + 1)).0)"
echo "3) major  ($((MAJOR + 1)).0.0)"
echo "0) キャンセル"
read -rp "選択 [1]: " CHOICE

case "${CHOICE:-1}" in
    0) echo "キャンセルしました"; exit 0 ;;
    1) PATCH=$((PATCH + 1)) ;;
    2) MINOR=$((MINOR + 1)); PATCH=0 ;;
    3) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
    *) echo "無効な選択"; exit 1 ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"

echo ""
echo "リリースタイプ:"
echo "1) beta"
echo "2) release"
echo "0) キャンセル"
read -rp "選択 [1]: " TYPE_CHOICE

case "${TYPE_CHOICE:-1}" in
    0) echo "キャンセルしました"; exit 0 ;;
    1) RELEASE_TYPE="beta" ;;
    2) RELEASE_TYPE="release" ;;
    *) echo "無効な選択"; exit 1 ;;
esac

echo "$NEW_VERSION" > "$VERSION_FILE"

# --- ビルド ---
NAME="RPGish-HPDisplay-${MC_VERSION}-${NEW_VERSION}-${RELEASE_TYPE}"
ZIP="${NAME}.zip"
rm -f "$ZIP"
zip -r "$ZIP" pack.mcmeta pack.png data/ -x "*.git*"
echo "Created ${ZIP} (v${NEW_VERSION})"
