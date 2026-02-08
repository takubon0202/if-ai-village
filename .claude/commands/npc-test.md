NPC テストスキル - AI NPCシステムのテスト手順

## テスト手順

### 基本テスト (AI無効)
1. `./gradlew clean build` でコンパイル成功を確認
2. サーバーにJARをデプロイ
3. `/counselor spawn north 10` でNPCスポーン
4. NPCがテレポートパトロールすることを確認
5. 右クリックで固定メッセージが表示されることを確認

### AIテスト (AI有効)
1. `config.yml` で `ai.enabled: true` と `ai.gemini-api-key` を設定
2. サーバーを再起動
3. `/counselor spawn north 10 counselor` でAI NPC生成
4. NPCが自然に歩き回ることを確認 (Pathfinder使用)
5. 近づくとNPCが振り向き挨拶するか確認
6. 右クリックでGemini経由の会話が開始するか確認
7. チャットでNPCに話しかけてAI応答を確認

### コマンドテスト
- `/ainpc status` - AI状態表示
- `/ainpc debug` - 全NPCデバッグ概要
- `/ainpc personality` - 性格プロファイル一覧
- `/ainpc stats` - 統計情報

### 性格テスト
- `/counselor spawn north 10 guard` - 衛兵性格
- `/counselor spawn east 10 merchant` - 商人性格
- `/counselor spawn south 10 explorer` - 探検家性格
