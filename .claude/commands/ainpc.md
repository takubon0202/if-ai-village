AI NPC管理スキル - AI NPCシステムの状態確認・デバッグ・設定変更を支援

## 使い方
このスキルはAI NPCシステムの管理タスクを支援します。

## 対応コマンド
- `/ainpc status` - AI NPC全体の状態確認
- `/ainpc debug [uuid]` - 特定NPCのデバッグ情報
- `/ainpc personality` - 性格プロファイル一覧
- `/ainpc reload` - AI設定リロード
- `/ainpc stats` - 統計情報

## 設定ファイル
- `config.yml` の `ai` セクション - API設定・動作パラメータ
- `personalities.yml` - 性格プロファイル定義
- `memories.yml` - NPC記憶データ (自動生成)
- `counselors.yml` - NPC永続化データ (自動生成)

## トラブルシューティング
1. AIが動かない → `config.yml` の `ai.enabled: true` と `ai.gemini-api-key` を確認
2. NPCが歩かない → Pathfinderが有効か、チャンクがロードされているか確認
3. 会話が応答しない → Gemini APIキーの有効性とレート制限を確認
