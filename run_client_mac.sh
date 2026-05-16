#!/usr/bin/env bash
# run_client_mac.sh — macOS 用、開発版 Minecraft クライアントを起動する
#
# Usage:
#   ./run_client_mac.sh                  # forge (デフォルト) で起動
#   ./run_client_mac.sh fabric           # Fabric で起動
#   ./run_client_mac.sh forge --offline  # オフラインモード
#   ./run_client_mac.sh --offline        # forge + offline (省略形)
#
# Notes:
#   - 1.20.1 では NeoForge も forge ビルドで動作する (neoforge 指定は forge にフォールバック)
#   - 初回起動時はネット必須 (Gradle 本体 + Minecraft 資産ダウンロード)
#   - 二度目以降は --offline で起動可能
#   - 起動後の Minecraft 内で「Singleplayer → 新規ワールド作成」して動作確認できる

set -euo pipefail

SCRIPT_PATH="${BASH_SOURCE[0]:-$0}"
ROOT="$(cd "$(dirname "$SCRIPT_PATH")" && pwd)"

LOADER="forge"
OFFLINE=""

for arg in "$@"; do
    case "$arg" in
        forge|fabric)         LOADER="$arg" ;;
        neoforge)
            echo "INFO: NeoForge 1.20.1 は forge ビルドで起動します。" >&2
            LOADER="forge"
            ;;
        --offline|-o|offline) OFFLINE="--offline" ;;
        -h|--help)
            sed -n '2,15p' "$SCRIPT_PATH" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *) echo "Unknown argument: $arg" >&2; exit 2 ;;
    esac
done

DIR="$ROOT/mod-$LOADER"
if [ ! -d "$DIR" ]; then
    echo "ERROR: $DIR が見つかりません" >&2
    exit 1
fi

if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: java が見つかりません。JDK 17 をインストールしてください。" >&2
    echo "  brew install --cask temurin@17" >&2
    exit 1
fi

cd "$DIR"
[ -x ./gradlew ] || chmod +x ./gradlew

echo "==================================================================="
echo " Minecraft クライアント起動: $LOADER (macOS) ${OFFLINE:+[OFFLINE]}"
echo "==================================================================="

# macOS では LWJGL が -XstartOnFirstThread を必要とするが、ForgeGradle/Loom が自動で付ける
exec ./gradlew $OFFLINE runClient
