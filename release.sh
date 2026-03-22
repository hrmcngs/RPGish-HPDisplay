#!/bin/bash
set -e

MC_VERSION="1.20.1"
VERSION=$(cat VERSION)

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

NAME="RPGish-HPDisplay-${MC_VERSION}-${VERSION}-${RELEASE_TYPE}"
ZIP="${NAME}.zip"
TAG="RPGish-HPDisplay-${MC_VERSION}-${RELEASE_TYPE}"

if [ ! -f "$ZIP" ]; then
    echo "Error: ${ZIP} が見つかりません。先に ./build.sh で同じタイプをビルドしてください。"
    exit 1
fi

PRERELEASE_FLAG=""
if [ "$RELEASE_TYPE" = "beta" ]; then
    PRERELEASE_FLAG="--prerelease"
fi

if gh release view "$TAG" --repo hrmcngs/RPGish-HPDisplay > /dev/null 2>&1; then
    echo "Updating existing release: ${TAG}"
    gh release upload "$TAG" "$ZIP" --clobber --repo hrmcngs/RPGish-HPDisplay
    gh release edit "$TAG" --title "$ZIP" --repo hrmcngs/RPGish-HPDisplay
else
    echo "Creating new release: ${TAG}"
    gh release create "$TAG" "$ZIP" --title "$ZIP" $PRERELEASE_FLAG --repo hrmcngs/RPGish-HPDisplay
fi

echo "Done! ${ZIP} -> ${TAG}"
