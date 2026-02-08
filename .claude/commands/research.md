# /research - Minecraft 情報調査スキル (Claude + Gemini + Codex 3AI連携)

Claude Code（メイン分析）、Gemini CLI（Web検索）、Codex CLI（コード構造化）の3つのAIを連携して、Minecraft関連の最新情報を多角的に調査します。

## 実行フロー

### Step 1: Gemini で Web検索・最新情報を収集

Gemini CLIのGoogle検索グラウンディングを活用して、最新の公式情報を取得:

```bash
gemini -m gemini-3-pro-preview "今日は2026年2月7日です。Minecraft Java Edition 1.21.11 $ARGUMENTS について、最新の仕様・変更点・使い方を調査してください。公式Wiki(minecraft.wiki)とPaper API docs(docs.papermc.io)を優先的に参照してください。"
```

### Step 2: Claude で分析・検証・補完

Claude Code自身のツール（WebFetch、WebSearch、Read）を使って、Geminiの調査結果を検証・補完:

1. **WebFetch** で公式ソースを直接確認:
   - https://minecraft.wiki/
   - https://docs.papermc.io/
   - https://jd.papermc.io/paper/

2. **分析・統合**: Geminiの結果と公式ソースを照合し、矛盾や不足を特定

3. **補完**: 不足情報をClaudeの知識とWebSearchで補う

### Step 3: Codex でコード化・構造化

調査結果をJavaコード、JSON、または構造化データとしてまとめる:

```bash
echo "今日は2026年2月7日です。プロジェクト: IF MineAI (Minecraft Plugin, Java 21, Paper API 1.21.11)。以下のMinecraft情報をJavaコードとして構造化してください: $ARGUMENTS" | codex exec - --sandbox read-only
```

### Step 4: 3AI クロスチェック・最終統合

3つのAIの結果を統合:

| AI | 役割 | 強み |
|----|------|------|
| **Gemini** | Web検索・情報収集 | Google検索グラウンディング、最新情報へのアクセス |
| **Claude** | 分析・検証・統合 | 深い推論、コード理解、公式ドキュメント解析 |
| **Codex** | コード構造化・実装 | Java コード生成、データ構造設計 |

## 調査対象

- Minecraft バージョン情報・変更点
- コマンド構文と仕様
- Paper API の使い方・変更点
- プラグイン開発ベストプラクティス
- エンチャント・エフェクト・アイテムデータ
- Minecraft 1.21.11 固有の仕様

## 調査時の優先リソース

1. Minecraft Wiki (minecraft.wiki)
2. Paper Docs (docs.papermc.io)
3. Paper Javadoc (jd.papermc.io/paper/)
4. SpigotMC (spigotmc.org)
5. PaperMC GitHub (github.com/PaperMC)

## 実行後

3つのAI（Claude、Gemini、Codex）の結果を統合し、
矛盾がないか確認した上でユーザーに報告してください。
情報の信頼度を【高/中/低】で明示してください。
