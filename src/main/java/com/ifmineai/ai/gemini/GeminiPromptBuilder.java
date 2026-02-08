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

        sb.append("行動ルール:\n");
        sb.append("- 提供されたツール(関数)を使って行動してください\n");
        sb.append("- 1回の応答で1〜2個のツールコールを行ってください\n");
        sb.append("- ホーム地点から").append(brain.getData().getRange() * 2).append("ブロック以内で活動してください\n");
        sb.append("- 基本行動は walk_to と idle の組み合わせです。散歩→立ち止まる→散歩を繰り返してください\n\n");

        sb.append("発言ルール (最重要):\n");
        sb.append("- sayツールは絶対に使わないでください。あなたは受け身のキャラクターです\n");
        sb.append("- プレイヤーとの会話はプレイヤーが右クリックで開始します。自発的に話しかけないでください\n");
        sb.append("- プレイヤーが近くにいる場合は、look_at や wave だけにしてください\n");
        sb.append("- 行動は walk_to, idle, look_at, wave, emote のみ使ってください\n");

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

        // sayは常に禁止
        sb.append("- 重要: sayツールは使用禁止です。walk_to, idle, look_at, waveのみ使ってください\n");

        if (!ctx.nearbyPlayers().isEmpty()) {
            sb.append("\n近くのプレイヤー:\n");
            for (DecisionContext.NearbyPlayer p : ctx.nearbyPlayers()) {
                sb.append("- ").append(p.name())
                        .append(" (距離: ").append(String.format("%.1f", p.distance())).append("ブロック");
                if (p.isSneaking()) sb.append(", スニーク中");
                if (p.isSprinting()) sb.append(", ダッシュ中");
                // 挨拶済みかどうか
                if (brain.hasGreeted(p.name())) {
                    sb.append(", 既に挨拶済み");
                }
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

        sb.append("\n次に何をしますか？walk_toかidleを使ってください。sayは使わないでください。");

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

        sb.append("会話スタイル (厳守):\n");
        sb.append("- 受け身で会話してください。プレイヤーの話を聞いて、それに答えるスタイルです\n");
        sb.append("- 自分から質問攻めにしないでください。相手が何か言うまで待つ姿勢です\n");
        sb.append("- 応答は短く自然に。1〜2文で十分です (最大100文字程度)\n");
        sb.append("- 定型的な丁寧語の羅列は避けてください (「何かお困りですか？」「ご相談に乗りますよ」等の繰り返しはNG)\n");
        sb.append("- プレイヤーの発言に直接答えてください。話をそらさないでください\n");
        sb.append("- 相手が「はい」「うん」等の短い返事をした場合、それに合った短い返答をしてください\n");
        sb.append("- 人間同士の自然な雑談のように会話してください\n");

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
            int start = Math.max(0, history.size() - 5);
            for (int i = start; i < history.size(); i++) {
                ConversationAgent.ChatMessage msg = history.get(i);
                String role = msg.role().equals("user") ? playerName : "あなた";
                sb.append(role).append(": ").append(msg.content()).append("\n");
            }
            sb.append("\n");
        }

        sb.append(playerName).append(": ").append(latestMessage).append("\n");
        sb.append("短く自然に応答してください。質問返しは不要です。");

        return sb.toString();
    }
}
