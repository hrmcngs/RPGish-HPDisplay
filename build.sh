#!/usr/bin/env bash
# build.sh — RPGish HP Display mod を Forge / Fabric 向けにビルドする
#
# Usage:
#   ./build.sh                        # 全ローダーをビルド (デフォルト)
#   ./build.sh forge                  # Forge だけ
#   ./build.sh fabric                 # Fabric だけ
#   ./build.sh all --offline          # 全部、オフラインモード (キャッシュ済み依存のみ使用)
#   ./build.sh forge fabric --offline # 複数指定も OK
#
# Notes:
#   - 1.20.1 では Forge と NeoForge は互換のため、mod-forge の jar が両方で動く
#     (neoforge を引数に渡しても forge ビルドにフォールバックする)
#   - 初回ビルドはネット接続が必要 (Gradle 本体・Forge/Fabric SDK のダウンロード)
#   - 一度成功すれば次回以降は --offline を付けてオフラインでも動く
#   - macOS / Linux / WSL (Windows Subsystem for Linux) で動作
#   - 生成 jar は <repo>/dist/ にコピーされる

set -euo pipefail

# --- リポジトリルート (このスクリプトがある場所) を解決 -----------------------
SCRIPT_PATH="${BASH_SOURCE[0]:-$0}"
ROOT="$(cd "$(dirname "$SCRIPT_PATH")" && pwd)"
DIST="$ROOT/dist"

# --- 引数パース -------------------------------------------------------------
OFFLINE=""
LOADERS=()

for arg in "$@"; do
    case "$arg" in
        --offline|-o)   OFFLINE="--offline" ;;
        forge|fabric)   LOADERS+=("$arg") ;;
        neoforge)
            echo "INFO: NeoForge 1.20.1 は forge ビルドの jar で動作します。forge をビルドします。" >&2
            LOADERS+=("forge")
            ;;
        all)            LOADERS=("forge" "fabric") ;;
        -h|--help)
            sed -n '2,17p' "$SCRIPT_PATH" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *) echo "Unknown argument: $arg" >&2; exit 2 ;;
    esac
done

# 引数なしなら全ローダー
if [ ${#LOADERS[@]} -eq 0 ]; then
    LOADERS=("forge" "fabric")
fi

# --- 環境チェック -----------------------------------------------------------
if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: java が見つかりません。JDK 17 をインストールしてください。" >&2
    exit 1
fi

JAVA_MAJOR="$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')"
if [ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -lt 17 ] 2>/dev/null; then
    echo "WARNING: Java $JAVA_MAJOR が検出されました。Java 17+ を推奨します。" >&2
fi

mkdir -p "$DIST"

# --- ビルド関数 -------------------------------------------------------------
build_one() {
    local name="$1"
    local dir="$ROOT/mod-$name"

    if [ ! -d "$dir" ]; then
        echo "skip $name: $dir が存在しません" >&2
        return 0
    fi

    echo "==================================================================="
    echo " ビルド: $name  ($dir)  ${OFFLINE:+[OFFLINE]}"
    echo "==================================================================="

    (
        cd "$dir"
        # WSL/Linux で gradlew に実行権限が無い場合に備えて付与
        [ -x ./gradlew ] || chmod +x ./gradlew
        ./gradlew $OFFLINE build --no-daemon
    )

    # 生成 jar (sources/dev/javadoc 以外) を dist/ にコピー
    local jar
    jar="$(find "$dir/build/libs" -maxdepth 1 -type f -name '*.jar' \
        ! -name '*-sources.jar' ! -name '*-dev.jar' ! -name '*-javadoc.jar' \
        -print0 2>/dev/null | xargs -0 ls -t 2>/dev/null | head -1)"
    if [ -n "$jar" ]; then
        local base
        base="$(basename "$jar" .jar)"
        local out="$DIST/${base}-${name}.jar"
        cp "$jar" "$out"
        echo "  -> $out"
    else
        echo "  WARNING: $name の jar が見つかりませんでした" >&2
    fi
}

# --- 実行 -------------------------------------------------------------------
for loader in "${LOADERS[@]}"; do
    build_one "$loader"
done

echo
echo "完了。成果物: $DIST"
ls -la "$DIST"
