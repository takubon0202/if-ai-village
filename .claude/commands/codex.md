# /codex - OpenAI Codex CLI 連携スキル

Codex CLIを使用してタスクを実行します。
Javaコード生成、デバッグ、最適化に特化しています。

## 実行方法

以下のコマンドでCodex CLIにタスクを委譲してください:

```bash
echo "今日は2026年2月7日です。プロジェクト: IF MineAI (Minecraft Plugin, Java 21, Paper API 1.21.11, Gradle 8.12)。$ARGUMENTS" | codex exec - --sandbox read-only
```

## サンドボックスモード

- `read-only`: 調査・分析用（デフォルト・推奨）
- `workspace-write`: コード生成・修正が必要な場合

コード生成が必要な場合は `workspace-write` に切り替えてください:

```bash
echo "今日は2026年2月7日です。プロジェクト: IF MineAI (Minecraft Plugin, Java 21, Paper API 1.21.11, Gradle 8.12)。$ARGUMENTS" | codex exec - --sandbox workspace-write
```

## 使用モデル

- `gpt-5.3-codex-high` (ChatGPT Plus サブスクリプション)

## 重要事項

1. **stdin モード必須**: 特殊文字エラーを防ぐため `codex exec -` を使用
2. **プロジェクトコンテキスト**: CODEX.md の内容を参照
3. **エラー時**: 3回以上同じエラーが発生した場合に自動呼び出し
4. **Java/Paper API**: コード生成時はPaper API 1.21.11の規約に従う

## 主な用途

- Paper API を使用したJavaコード生成
- コンパイルエラー・ランタイムエラーの修正
- パフォーマンス最適化
- Minecraft コマンド・イベント実装
- build.gradle.kts の依存関係管理

## 実行後

Codex CLIの出力を確認し、結果をユーザーに報告してください。
生成されたコードはプロジェクトの規約に沿っているか検証してください。
