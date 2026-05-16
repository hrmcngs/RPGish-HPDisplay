## RPGish-HPDisplay (RPG風HP表示、ダメージ表示データパック)

マイクラにRPGの戦闘の風味を足します。

![2021-02-11_12 25 35](https://user-images.githubusercontent.com/78842898/166111524-b363d19f-2ffd-4beb-a8f7-b730401f0fd0.png)

## 機能:

  - ボスモブ(エンダードラゴン、ウィザー)以外のモブがダメージを受けた際、頭上にダメージ値を表示
  - ダメージの種類によって表示色が変化:（僕のmodもできるように）
    - mod属性: 氷=シアン / 電気=黄 / 侵食=ピンク / 聖=金
  - ボスモブ以外のモブがダメージを受けた/HPを回復した際、5秒間HPバーを表示
  - 飼いならしたオオカミはシステムから除外 (MC-193202バグ回避)
  - 毒エフェクトでHP<=0になる場合、HP=1に保持 (バニラの毒の挙動)
  - 名札などで付けた名前はHPバー表示後に復元
  - 512以上のダメージを受けた場合の処理に対応
  - **プレイヤー対応 (mod版のみ)**: 被ダメージ/被回復時にアクションバー(ホットバー上)へ
    同スタイルのHPバーを表示。頭上にもダメージ数値が出る

## ビルド & リリース

### データパック

```bash
./build.sh      # バージョン選択 + beta/release選択 → zipを生成
./release.sh    # ビルド済みzipをGitHub Releasesにアップロード
```

### Mod (Minecraft 1.20.1)

データパックと同じ機能を mod として実装したものを提供しています。
**Forge / NeoForge / Fabric** に対応します。

> **Forge と NeoForge について**
> 1.20.1 では NeoForge は Forge のフォークで API が完全互換のため、
> `mod-forge` でビルドした jar が **Forge でも NeoForge でもそのまま動きます**。
> そのため Forge と NeoForge は 1 つのビルド (`mod-forge/`) に統合しています。

#### クイックスタート

リポジトリのルート (`/Users/hiromichi/Documents/github/datapack/RPGish-HPDisplay/`) で:

```bash
# 全ローダーをビルド → dist/ に jar が並ぶ
./build.sh

# ビルド済みなら 2 回目以降はオフラインで OK
./build.sh --offline

# 開発用 Minecraft クライアントを立ち上げて手動テスト (macOS)
./run_client_mac.sh

# 同じものを Windows (WSL / Git Bash) で
./run_client_win.sh
```

利用可能なスクリプトと出力先:

| スクリプト | 役割 | 出力 |
|---|---|---|
| `./build.sh [loader...] [--offline]` | mod の jar をビルド | `dist/mh_rpgish-1.0.1-<loader>.jar` |
| `./run_client_mac.sh [loader] [--offline]` | 開発版 MC クライアントを macOS で起動 | (画面表示) |
| `./run_client_win.sh [loader] [--offline]` | 同じく WSL / Git Bash で起動 | (画面表示) |
| `RPGish-HPDisplay-datapack/build.sh` | データパックの zip をビルド | `RPGish-HPDisplay-datapack/*.zip` |

`loader` には `forge` / `fabric` (省略時は `forge`)、`./build.sh` には `all` も指定可。
`neoforge` を渡しても `forge` ビルドにフォールバックします。

#### ディレクトリ構成

```
mod-forge/       Forge 1.20.1 用 mod (jar は NeoForge 1.20.1 でもそのまま動く)
mod-fabric/      Fabric 1.20.1 用 mod (Mixin ベースで API が異なるためソース別管理)
build.sh         まとめてビルドするシェルスクリプト
dist/            ビルド成果物の集約先
```

#### 全部まとめてビルド (推奨)

リポジトリのルートで:

```bash
./build.sh              # forge / fabric を全部ビルド → dist/ に jar が並ぶ
./build.sh forge        # 個別指定も可
./build.sh forge fabric # 複数指定
./build.sh --offline    # オフラインビルド (※下記参照)
```

成果物の例:

```
dist/
├── mh_rpgish-1.0.1-forge.jar    ← Forge / NeoForge 両対応
└── mh_rpgish-1.0.1-fabric.jar
```

#### 個別にビルドする場合

```bash
cd mod-forge  && ./gradlew build   # 出力: mod-forge/build/libs/mh_rpgish-1.0.1.jar
cd mod-fabric && ./gradlew build   # 出力: mod-fabric/build/libs/mh_rpgish-1.0.1.jar
```

開発用に Minecraft クライアントを起動する場合:

ルートに OS 別ラッパースクリプトが用意してあります。

```bash
# macOS
./run_client_mac.sh                  # Forge (デフォルト)
./run_client_mac.sh fabric           # Fabric
./run_client_mac.sh --offline        # Forge + オフライン
./run_client_mac.sh fabric --offline # Fabric + オフライン

# Windows (WSL / Git Bash) — 中身は同じ引数
./run_client_win.sh
./run_client_win.sh fabric --offline
```

`run_client_win.sh` は WSL / Git Bash を自動判定して `gradlew` か `gradlew.bat` を使い分けます。
WSL 上では `DISPLAY=:0` を自動設定 (Windows 11 + WSLg を想定)。

個別に Gradle を叩いてもOK:

```bash
cd mod-forge
./gradlew runClient   # クライアント起動
./gradlew runServer   # サーバー起動
```

#### オフラインビルド

初回ビルドはネット接続が必須 (Gradle 本体・Minecraft / Forge / Fabric SDK のダウンロード)。
一度成功すれば次回以降は `--offline` でオフラインでもビルドできます:

```bash
./build.sh --offline
# もしくは個別に
cd mod-forge && ./gradlew --offline build
```

Gradle のキャッシュは `~/.gradle/caches/` に置かれます。
別マシンでオフラインビルドしたい場合はこのディレクトリごとコピーすれば移植可能。

#### VSCode + WSL でビルドする場合

このリポジトリを WSL 側のファイルシステム (例: `~/RPGish-HPDisplay/`) に置いて
VSCode の Remote-WSL で開く構成を推奨します。Windows パス (`/mnt/c/...`) 経由は
Gradle が著しく遅くなるためです。

```bash
# WSL の Ubuntu などのターミナルで
sudo apt install -y openjdk-17-jdk
cd ~/RPGish-HPDisplay
./build.sh
```

VSCode のターミナル (Remote-WSL) からそのまま `./build.sh` を実行できます。
gradlew は実行権限を自動で付与するため、Windows 経由でクローンして
権限が落ちていても動作します。

#### 動作要件

- Java 17 (Temurin / Microsoft OpenJDK / OpenJDK いずれも可)
- Minecraft 1.20.1
- 各ローダー: Forge 47.x / Fabric Loader 0.15+ / NeoForge 47.1.x
