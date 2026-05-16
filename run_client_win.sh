#!/usr/bin/env bash
# run_client_win.sh — Windows (WSL / Git Bash) 用、開発版 Minecraft クライアントを起動する
#
# Usage:
#   ./run_client_win.sh                  # forge (デフォルト) で起動
#   ./run_client_win.sh neoforge         # NeoForge ランタイムで起動
#   ./run_client_win.sh fabric           # Fabric で起動
#   ./run_client_win.sh forge --offline  # オフラインモード
#   ./run_client_win.sh --offline        # forge + offline (省略形)
#
# Notes:
#   - forge / neoforge は同じ mod-forge/ を使い、Gradle の -Pplatform で SDK を切替
#   - WSL 上で動かす場合、Windows 11 + WSLg なら追加設定不要でウィンドウが出る
#     (Windows 10 + WSL2 は X サーバー (VcXsrv 等) のセットアップが必要)
#   - Git Bash の場合は自動的に gradlew.bat を使用
#   - 初回起動時はネット必須、二度目以降は --offline で OK
#   - リポジトリは WSL のネイティブファイルシステム
#     (例: ~/RPGish-HPDisplay/) に置くのを推奨。
#     /mnt/c/... 経由は Gradle が著しく遅くなる。

set -euo pipefail

SCRIPT_PATH="${BASH_SOURCE[0]:-$0}"
ROOT="$(cd "$(dirname "$SCRIPT_PATH")" && pwd)"

LOADER="forge"
OFFLINE=""

for arg in "$@"; do
    case "$arg" in
        forge|neoforge|fabric) LOADER="$arg" ;;
        --offline|-o|offline)  OFFLINE="--offline" ;;
        -h|--help)
            sed -n '2,19p' "$SCRIPT_PATH" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *) echo "Unknown argument: $arg" >&2; exit 2 ;;
    esac
done

# ローダー名 → ビルドディレクトリ + Gradle 追加引数
case "$LOADER" in
    forge)    DIR="$ROOT/mod-forge";  PLATFORM_ARG="-Pplatform=forge" ;;
    neoforge) DIR="$ROOT/mod-forge";  PLATFORM_ARG="-Pplatform=neoforge" ;;
    fabric)   DIR="$ROOT/mod-fabric"; PLATFORM_ARG="" ;;
esac

if [ ! -d "$DIR" ]; then
    echo "ERROR: $DIR が見つかりません" >&2
    exit 1
fi

# --- プラットフォーム判定 ---------------------------------------------------
UNAME_S="$(uname -s)"
IS_WSL=0
IS_GITBASH=0
case "$UNAME_S" in
    Linux*)
        # WSL かどうかは /proc/version で判定
        if grep -qiE 'microsoft|wsl' /proc/version 2>/dev/null; then
            IS_WSL=1
        fi
        ;;
    MINGW*|MSYS*|CYGWIN*) IS_GITBASH=1 ;;
esac

# --- WSL 用: WSLg / X サーバー DISPLAY のチェック ---------------------------
if [ "$IS_WSL" = "1" ] && [ -z "${DISPLAY:-}" ]; then
    # WSLg の既定値 (Windows 11) は :0
    export DISPLAY=":0"
    echo "INFO: DISPLAY 未設定だったので :0 にしました (WSLg を想定)" >&2
fi

# --- Java チェック ----------------------------------------------------------
if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: java が見つかりません。JDK 17 をインストールしてください。" >&2
    if [ "$IS_WSL" = "1" ]; then
        echo "  sudo apt install -y openjdk-17-jdk" >&2
    else
        echo "  https://adoptium.net/ から Temurin 17 をインストール" >&2
    fi
    exit 1
fi

cd "$DIR"

# --- gradlew の選択 --------------------------------------------------------
if [ "$IS_GITBASH" = "1" ]; then
    GRADLEW="./gradlew.bat"
else
    GRADLEW="./gradlew"
    [ -x "$GRADLEW" ] || chmod +x "$GRADLEW"
fi

echo "==================================================================="
echo " Minecraft クライアント起動: $LOADER ${IS_WSL:+(WSL)}${IS_GITBASH:+(Git Bash)} ${OFFLINE:+[OFFLINE]}"
echo "==================================================================="

exec "$GRADLEW" $OFFLINE $PLATFORM_ARG runClient
