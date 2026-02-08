AI NPC アーキテクチャ参照

## システム構成

```
AIBrainManager (全NPC管理・ティックループ)
  ├── NPCBrain (NPC毎のステートマシン)
  │   ├── AwarenessAgent   (環境スキャン - 16ブロック範囲)
  │   ├── MemoryAgent      (短期/長期記憶管理)
  │   ├── MovementAgent    (Pathfinder移動制御)
  │   └── ConversationAgent(プレイヤー会話管理)
  │   └── ActionQueue: [WalkTo, LookAt, Say, Emote, Idle, Wave]
  └── GeminiClient (非同期・Semaphore制御)
      ├── Flash: 行動決定 (Function Calling)
      └── Pro: 会話応答 (テキスト生成)
```

## パッケージ構成

- `com.ifmineai` - メインプラグイン・データクラス
- `com.ifmineai.ai` - NPCBrain, AIBrainManager, NPCState, DecisionContext
- `com.ifmineai.ai.action` - NPCAction, WalkTo/LookAt/Say/Emote/Idle/Wave Action
- `com.ifmineai.ai.agent` - BehaviorAgent, Awareness/Movement/Conversation/Memory Agent
- `com.ifmineai.ai.gemini` - GeminiClient, ToolRegistry, PromptBuilder, ResponseParser
- `com.ifmineai.ai.memory` - MemoryStore, MemoryEntry, MemoryType
- `com.ifmineai.ai.personality` - PersonalityProfile, PersonalityLoader
- `com.ifmineai.config` - AIConfig
- `com.ifmineai.command` - AICommandHandler, AITabCompleter

## 状態遷移 (NPCState)

IDLE → THINKING (AI判断待ち) → WALKING/TALKING/LOOKING/EMOTING → IDLE

## Gemini Function Calling ツール

walk_to, approach_player, look_at, say, emote, idle, wave, remember

## パフォーマンス設計

- Gemini API: 完全非同期 (CompletableFuture + BukkitScheduler)
- ラウンドロビン: NPC毎に30tick(1.5秒)間隔
- 同時リクエスト: Semaphore(3)
- Pathfinder再発行: 20tick毎
- グレースフルデグラデーション: AI無効時は従来テレポートパトロール
