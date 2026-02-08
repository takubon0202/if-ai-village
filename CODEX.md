# Codex CLI コンテキスト - IF MineAI

## プロジェクト情報

- **名前**: IF MineAI
- **種類**: Minecraft Java Edition プラグイン
- **言語**: Java 21
- **ビルド**: Gradle 8.12 (Kotlin DSL)
- **API**: Paper API 1.21.11
- **使用モデル**: gpt-5.3-codex

## Codex CLIの主な用途

1. **Javaコード生成**: プラグインクラス、コマンドハンドラー、イベントリスナー
2. **バグ修正・デバッグ**: コンパイルエラー、ランタイムエラーの解決
3. **最適化**: パフォーマンス改善、メモリ効率化
4. **Paper API活用**: API仕様に基づいたコード実装

## 重要な規約

- Paper APIのイベントシステムを使用
- BukkitSchedulerでの非同期処理
- config.ymlによる設定管理
- plugin.ymlの正確な記述
- NMS直接アクセスは避ける

## ソースディレクトリ

- メインコード: `src/main/java/com/ifmineai/`
- リソース: `src/main/resources/`
- テスト: `src/test/java/com/ifmineai/`
