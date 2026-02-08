# /gemini - Google Gemini CLI 連携スキル

Gemini CLIを使用してタスクを実行します。
Web検索、情報収集、API仕様調査に特化しています。

## 実行方法

以下のコマンドでGemini CLIにタスクを委譲してください:

```bash
gemini -m gemini-3-pro-preview "今日は2026年2月7日です。プロジェクト: IF MineAI (Minecraft Plugin, Java 21, Paper API 1.21.11)。$ARGUMENTS"
```

高速処理が必要な場合はFlashモデルを使用:

```bash
gemini -m gemini-3-flash-preview "今日は2026年2月7日です。$ARGUMENTS"
```

## 使用モデル

- **推奨**: `gemini-3-pro-preview` (高品質)
- **高速**: `gemini-3-flash-preview` (軽量タスク向け)

## 重要: モデル制限

**Gemini 3系のみ使用すること**
- `gemini-3-pro-preview` ✅
- `gemini-3-flash-preview` ✅
- `gemini-2.5-*` ❌ 使用禁止

**Gemini 2.5系へのフォールバックは絶対に行わないこと**

## Web検索機能

Gemini CLIはGoogle検索グラウンディングが組み込まれています:
- 直接Web検索が可能
- URL取得・解析が可能
- ClaudeのWebSearch toolの代わりに使用可能

## 主な用途

- **Minecraft Wiki調査**: コマンド構文、エンチャント、アイテムID
- **Paper API調査**: APIドキュメント、Javadoc確認
- **バージョン情報**: 最新MCバージョン、API変更点
- **プラグイン開発情報**: SpigotMC、Paper フォーラム
- **Java 21機能**: 最新Java仕様の確認

## 調査時の優先リソース

1. Minecraft Wiki (minecraft.wiki)
2. Paper Docs (docs.papermc.io)
3. Paper Javadoc (jd.papermc.io)
4. SpigotMC (spigotmc.org)

## 実行後

Gemini CLIの出力を確認し、結果をユーザーに報告してください。
取得した情報の信頼性を検証してください。
