package com.ifmineai.ai.gemini;

import com.ifmineai.ai.DecisionContext;
import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.agent.ConversationAgent;
import com.ifmineai.ai.personality.PersonalityLoader;
import com.ifmineai.ai.personality.PersonalityProfile;

import java.util.List;

/**
 * Gemini APIへのプロンプトを構築
 */
public class GeminiPromptBuilder {

    private final PersonalityLoader personalityLoader;

    public GeminiPromptBuilder(PersonalityLoader personalityLoader) {
        this.personalityLoader = personalityLoader;
    }

    /**
     * 行動決定用システムプロンプト
     */
    public String buildSystemPrompt(PersonalityProfile profile, NPCBrain brain) {
        StringBuilder sb = new StringBuilder();
        sb.append("あなたはMinecraftワールドに住むNPCです。\n");
        sb.append("名前: ").append(profile.displayName()).append("\n");
        sb.append("性格タイプ: ").append(profile.type()).append("\n");
        sb.append("性格説明: ").append(profile.description()).append("\n\n");

        sb.append("性格パラメータ:\n");
        sb.append("- 親切さ: ").append(profile.friendliness()).append("/10\n");
        sb.append("- 好奇心: ").append(profile.curiosity()).append("/10\n");
        sb.append("- 社交性: ").append(profile.sociability()).append("/10\n");
        sb.append("- 話し方: ").append(profile.speechStyle()).append("\n\n");

        sb.append("ルール:\n");
        sb.append("- 提供されたツール(関数)を使って行動してください\n");
        sb.append("- 1回の応答で1〜3個のツールコールを行ってください\n");
        sb.append("- ホーム地点から").append(brain.getData().getRange() * 2).append("ブロック以内で活動してください\n");
        sb.append("- プレイヤーが近くにいたら興味を示してください\n");
        sb.append("- 自然で人間らしい行動パターンを心がけてください\n");
        sb.append("- 同じ行動を繰り返さず、バリエーションを持たせてください\n");

        return sb.toString();
    }

    /**
     * 行動決定用ユーザープロンプト
     */
    public String buildUserPrompt(DecisionContext ctx, NPCBrain brain) {
        StringBuilder sb = new StringBuilder();
        sb.append("現在の状況:\n");
        sb.append("- 状態: ").append(ctx.currentState()).append("\n");
        sb.append("- 時間帯: ").append(ctx.timeOfDay()).append("\n");
        sb.append("- 天気: ").append(ctx.weather()).append("\n");
        sb.append("- バイオーム: ").append(ctx.biome()).append("\n");

        if (!ctx.nearbyPlayers().isEmpty()) {
            sb.append("\n近くのプレイヤー:\n");
            for (DecisionContext.NearbyPlayer p : ctx.nearbyPlayers()) {
                sb.append("- ").append(p.name())
                        .append(" (距離: ").append(String.format("%.1f", p.distance())).append("ブロック");
                if (p.isSneaking()) sb.append(", スニーク中");
                if (p.isSprinting()) sb.append(", ダッシュ中");
                sb.append(")\n");
            }
        } else {
            sb.append("\n近くにプレイヤーはいません。\n");
        }

        if (!ctx.nearbyEntities().isEmpty()) {
            sb.append("\n近くのエンティティ:\n");
            for (DecisionContext.NearbyEntity e : ctx.nearbyEntities()) {
                sb.append("- ").append(e.type())
                        .append(" (距離: ").append(String.format("%.1f", e.distance())).append(")\n");
            }
        }

        if (!ctx.recentMemories().isEmpty()) {
            sb.append("\n最近の記憶:\n");
            for (String mem : ctx.recentMemories()) {
                sb.append("- ").append(mem).append("\n");
            }
        }

        sb.append("\n次に何をしますか？ツールを使って行動してください。");

        return sb.toString();
    }

    /**
     * 会話用システムプロンプト
     */
    public String buildConversationSystemPrompt(NPCBrain brain, String playerName) {
        PersonalityProfile profile = personalityLoader.getProfile(brain.getData().getPersonalityType());

        StringBuilder sb = new StringBuilder();
        sb.append("あなたはMinecraftワールドに住むNPC「").append(profile.displayName()).append("」です。\n");
        sb.append("性格: ").append(profile.description()).append("\n");
        sb.append("話し方: ").append(profile.speechStyle()).append("\n\n");
        sb.append("ルール:\n");
        sb.append("- プレイヤー「").append(playerName).append("」と会話しています\n");
        sb.append("- 日本語で自然に応答してください (最大200文字程度)\n");
        sb.append("- キャラクターの性格を反映した口調で話してください\n");
        sb.append("- Minecraftの世界観を崩さないでください\n");

        return sb.toString();
    }

    /**
     * 会話用ユーザープロンプト (履歴込み)
     */
    public String buildConversationUserPrompt(
            List<ConversationAgent.ChatMessage> history,
            String latestMessage, String playerName) {

        StringBuilder sb = new StringBuilder();

        if (history.size() > 1) {
            sb.append("会話履歴:\n");
            // 最新5件のみ
            int start = Math.max(0, history.size() - 5);
            for (int i = start; i < history.size(); i++) {
                ConversationAgent.ChatMessage msg = history.get(i);
                String role = msg.role().equals("user") ? playerName : "あなた";
                sb.append(role).append(": ").append(msg.content()).append("\n");
            }
            sb.append("\n");
        }

        sb.append(playerName).append("の最新メッセージ: ").append(latestMessage).append("\n");
        sb.append("応答してください。");

        return sb.toString();
    }
}
