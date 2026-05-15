#!/bin/bash
set -e

# --- 設定 ---
MC_VERSION="1.20.1"
VERSION_FILE="VERSION"

# --- 言語選択 ---
echo "Language / 言語:"
echo "1) 日本語"
echo "2) English"
read -rp "選択/Select [1]: " LANG_CHOICE

if [ "${LANG_CHOICE:-1}" = "2" ]; then
    L_CURRENT="Current version"
    L_VERSION="Version:"
    L_CANCEL="Cancel"
    L_SELECT="Select"
    L_RELEASE_TYPE="Release type:"
    L_CANCELLED="Cancelled."
    L_INVALID="Invalid selection"
else
    L_CURRENT="現在のバージョン"
    L_VERSION="バージョン:"
    L_CANCEL="キャンセル"
    L_SELECT="選択"
    L_RELEASE_TYPE="リリースタイプ:"
    L_CANCELLED="キャンセルしました"
    L_INVALID="無効な選択"
fi

# --- バージョン読み込み ---
if [ ! -f "$VERSION_FILE" ]; then
    echo "1.0.0" > "$VERSION_FILE"
fi

CURRENT=$(cat "$VERSION_FILE")
MAJOR=$(echo "$CURRENT" | cut -d. -f1)
MINOR=$(echo "$CURRENT" | cut -d. -f2)
PATCH=$(echo "$CURRENT" | cut -d. -f3)

echo ""
echo "${L_CURRENT}: ${CURRENT}"
echo ""
echo "${L_VERSION}"
echo "1) patch  (${MAJOR}.${MINOR}.$((PATCH + 1)))"
echo "2) minor  (${MAJOR}.$((MINOR + 1)).0)"
echo "3) major  ($((MAJOR + 1)).0.0)"
echo "0) ${L_CANCEL}"
read -rp "${L_SELECT} [1]: " CHOICE

case "${CHOICE:-1}" in
    0) echo "$L_CANCELLED"; exit 0 ;;
    1) PATCH=$((PATCH + 1)) ;;
    2) MINOR=$((MINOR + 1)); PATCH=0 ;;
    3) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
    *) echo "$L_INVALID"; exit 1 ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"

echo ""
echo "${L_RELEASE_TYPE}"
echo "1) beta"
echo "2) release"
echo "0) ${L_CANCEL}"
read -rp "${L_SELECT} [1]: " TYPE_CHOICE

case "${TYPE_CHOICE:-1}" in
    0) echo "$L_CANCELLED"; exit 0 ;;
    1) RELEASE_TYPE="beta" ;;
    2) RELEASE_TYPE="release" ;;
    *) echo "$L_INVALID"; exit 1 ;;
esac

echo "$NEW_VERSION" > "$VERSION_FILE"

# --- ビルド ---
NAME="RPGish-HPDisplay-${MC_VERSION}-${NEW_VERSION}-${RELEASE_TYPE}"
ZIP="${NAME}.zip"
mkdir -p builds
rm -f "builds/${ZIP}"
zip -r "builds/${ZIP}" pack.mcmeta pack.png data/ -x "*.git*"
echo "Created builds/${ZIP} (v${NEW_VERSION})"
