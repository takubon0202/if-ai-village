# IF MineAI - Minecraft Plugin 開発プロジェクト

## プロジェクト概要

Minecraft Java Edition向けプラグイン開発プロジェクト。
Paper API をベースにプラグインを構築する。

## AI役割分担

| AI | 役割 | 得意分野 |
|----|------|----------|
| **Claude Code** (メイン) | コア開発・設計・統合 | アーキテクチャ設計、コードレビュー、リファクタリング |
| **Codex CLI** (補助) | コード生成・デバッグ | Java コード生成、バグ修正、最適化 |
| **Gemini CLI** (補助) | リサーチ・情報収集 | Web検索、Minecraft Wiki調査、最新情報取得 |

## 技術スタック

- **言語**: Java 21 (LTS)
- **ビルドツール**: Gradle 8.12 (Kotlin DSL)
- **API**: Paper API 1.21.11
- **ターゲット**: Minecraft Java Edition

## ディレクトリ構造

```
if-mineai/
├── src/
│   ├── main/
│   │   ├── java/com/ifmineai/    # Javaソースコード
│   │   └── resources/             # plugin.yml等のリソース
│   └── test/
│       └── java/com/ifmineai/    # テストコード
├── gradle/wrapper/                # Gradle Wrapper
├── scripts/                       # ヘルパースクリプト
├── .claude/                       # Claude Code設定
│   ├── settings.json
│   ├── commands/                  # スキル定義
│   └── skills/                    # 拡張スキル
├── build.gradle.kts               # ビルド設定
├── settings.gradle.kts            # プロジェクト設定
├── CLAUDE.md                      # このファイル
├── CODEX.md                       # Codex CLIコンテキスト
└── GEMINI.md                      # Gemini CLIコンテキスト
```

## 開発コマンド

```bash
./gradlew build          # ビルド
./gradlew clean          # クリーン
./gradlew jar            # JAR作成
./gradlew test           # テスト実行
./gradlew dependencies   # 依存関係確認
```

## 重要ルール

### Gemini CLI使用時
- **Gemini 3系のみ使用**: `gemini-3-pro-preview` または `gemini-3-flash-preview`
- Gemini 2.5系へのフォールバックは**禁止**

### 使用量上限
- 使用量上限に達した場合は即座に報告
- 該当CLIの使用を停止し、代替CLIを提案
- 追加課金を防ぐためのルール

### コード品質
- Paper API の規約に従う
- NMS (net.minecraft.server) の直接使用は避ける
- プラグインのメインクラスは JavaPlugin を継承
- コマンド、リスナー、設定は適切に分離

## サブスクリプション情報

| AI | サブスク | モデル |
|----|---------|--------|
| Claude Code | Max ($200/月) | Claude Opus 4.6 |
| Codex CLI | ChatGPT Plus ($20/月) | GPT-5.3 Codex High |
| Gemini CLI | Gemini AI Pro ($19.99/月) | Gemini 3 Pro Preview / Flash Preview |
