#!/bin/bash
set -e

REPO="hrmcngs/RPGish-HPDisplay"

# --- 言語選択 ---
echo "Language / 言語:"
echo "1) 日本語"
echo "2) English"
read -rp "選択/Select [1]: " LANG_CHOICE

if [ "${LANG_CHOICE:-1}" = "2" ]; then
    L_CHECKING="Checking GitHub Releases..."
    L_NO_BUILDS="Error: No zip files in builds/. Run ./build.sh first."
    L_ALL_UPLOADED="No unpublished builds. Everything is already uploaded."
    L_UNPUBLISHED="Unpublished builds:"
    L_CANCEL="Cancel"
    L_SELECT_UPLOAD="Select file to upload: "
    L_CANCELLED="Cancelled."
    L_INVALID="Invalid selection"
    L_FILE="File"
    L_TAG="Tag"
    L_CREATING="Creating new release"
    L_DONE="Done!"
else
    L_CHECKING="GitHub Releases を確認中..."
    L_NO_BUILDS="Error: builds/ にzipファイルがありません。先に ./build.sh を実行してください。"
    L_ALL_UPLOADED="未公開のビルドはありません。すべてアップロード済みです。"
    L_UNPUBLISHED="未公開のビルド:"
    L_CANCEL="キャンセル"
    L_SELECT_UPLOAD="アップロードするファイルを選択: "
    L_CANCELLED="キャンセルしました"
    L_INVALID="無効な選択"
    L_FILE="ファイル"
    L_TAG="タグ"
    L_CREATING="新しいリリースを作成"
    L_DONE="完了!"
fi

# --- builds/ 内のzipを一覧 ---
if [ ! -d builds ] || [ -z "$(ls builds/*.zip 2>/dev/null)" ]; then
    echo "$L_NO_BUILDS"
    exit 1
fi

# --- 公開済みリリースのタグ一覧を取得 ---
echo "$L_CHECKING"
EXISTING_TAGS=$(gh release list --repo "$REPO" --limit 100 2>/dev/null | awk -F'\t' '{print $3}')

# --- 未公開のzipを抽出 ---
CANDIDATES=()
for f in builds/*.zip; do
    BASENAME=$(basename "$f" .zip)
    FOUND=false
    for tag in $EXISTING_TAGS; do
        if [ "$BASENAME" = "$tag" ]; then
            FOUND=true
            break
        fi
    done
    if [ "$FOUND" = false ]; then
        CANDIDATES+=("$f")
    fi
done

if [ ${#CANDIDATES[@]} -eq 0 ]; then
    echo "$L_ALL_UPLOADED"
    exit 0
fi

# --- 選択メニュー ---
echo ""
echo "$L_UNPUBLISHED"
for i in "${!CANDIDATES[@]}"; do
    echo "$((i + 1))) $(basename "${CANDIDATES[$i]}")"
done
echo "0) ${L_CANCEL}"
read -rp "$L_SELECT_UPLOAD" CHOICE

if [ "$CHOICE" = "0" ] || [ -z "$CHOICE" ]; then
    echo "$L_CANCELLED"
    exit 0
fi

INDEX=$((CHOICE - 1))
if [ $INDEX -lt 0 ] || [ $INDEX -ge ${#CANDIDATES[@]} ]; then
    echo "$L_INVALID"
    exit 1
fi

ZIP="${CANDIDATES[$INDEX]}"
BASENAME=$(basename "$ZIP" .zip)
TAG="$BASENAME"

# --- beta/release判定 ---
PRERELEASE_FLAG=""
if [[ "$BASENAME" == *-beta ]]; then
    PRERELEASE_FLAG="--prerelease"
fi

echo ""
echo "${L_FILE}: $(basename "$ZIP")"
echo "${L_TAG}:     ${TAG}"
echo ""

echo "${L_CREATING}: ${TAG}"
gh release create "$TAG" "$ZIP" --title "$BASENAME" $PRERELEASE_FLAG --repo "$REPO"

echo "${L_DONE} $(basename "$ZIP") -> ${TAG}"
