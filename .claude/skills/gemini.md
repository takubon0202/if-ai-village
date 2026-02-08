# Gemini CLI スキル拡張

## インストール済み拡張機能

### 1. code-review (v0.1.0)
- 自動コードレビュー
- ベストプラクティスチェック
- 問題検出

### 2. conductor (v0.2.0)
- 機能計画
- 実装管理
- トラックベースワークフロー

### 3. gemini-cli-jules (v0.1.0)
- 非同期コーディングエージェント
- バックグラウンドコード実行
- **重要**: Git操作は行わない（Claudeのみが担当）

### 4. gemini-cli-security (v0.4.0)
- 脆弱性検出
- カテゴリスキャン
- 重要度レベル分類

### 5. nanobanana (v1.0.10)
- アイコン生成 (`/icon`)
- パターン生成 (`/pattern`)
- ダイアグラム作成 (`/diagram`)

## コマンド例

```bash
/code-review                     # 自動コードレビュー
/jules [task]                    # バックグラウンドコーディング
/security:analyze                # 脆弱性スキャン
/icon "description" --count=3    # アイコン生成
```

## 重要事項

- Gemini 3系のみ使用
- Gemini 2.5系へのフォールバック禁止
- Git操作はClaude Codeのみが実行
