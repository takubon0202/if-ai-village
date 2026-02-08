package com.ifmineai.ai.gemini;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;

import java.util.Map;

/**
 * Gemini Function Calling ツール定義
 * NPCが実行可能なアクションをFunction Callingツールとして定義
 */
public class GeminiToolRegistry {

    private final Tool behaviorTools;

    public GeminiToolRegistry() {
        this.behaviorTools = buildBehaviorTools();
    }

    public Tool getBehaviorTools() {
        return behaviorTools;
    }

    private Tool buildBehaviorTools() {
        FunctionDeclaration walkTo = FunctionDeclaration.builder()
                .name("walk_to")
                .description("ホーム地点からの相対座標へ歩いて移動する")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "offset_x", Schema.builder().type("NUMBER").description("X方向オフセット (-32〜32)").build(),
                                "offset_z", Schema.builder().type("NUMBER").description("Z方向オフセット (-32〜32)").build(),
                                "speed", Schema.builder().type("NUMBER").description("移動速度 (0.5〜1.5, デフォルト1.0)").build()
                        ))
                        .required(java.util.List.of("offset_x", "offset_z"))
                        .build())
                .build();

        FunctionDeclaration approachPlayer = FunctionDeclaration.builder()
                .name("approach_player")
                .description("指定プレイヤーに近づく")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "player_name", Schema.builder().type("STRING").description("プレイヤー名").build(),
                                "stop_distance", Schema.builder().type("NUMBER").description("停止距離 (デフォルト2.0)").build()
                        ))
                        .required(java.util.List.of("player_name"))
                        .build())
                .build();

        FunctionDeclaration lookAt = FunctionDeclaration.builder()
                .name("look_at")
                .description("指定対象を見つめる")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "target_name", Schema.builder().type("STRING").description("対象名 (プレイヤー名またはエンティティ名)").build(),
                                "duration_seconds", Schema.builder().type("NUMBER").description("見つめる時間 (秒, デフォルト2.0)").build()
                        ))
                        .required(java.util.List.of("target_name"))
                        .build())
                .build();

        FunctionDeclaration say = FunctionDeclaration.builder()
                .name("say")
                .description("周囲のプレイヤーにメッセージを話す (最大200文字)")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "message", Schema.builder().type("STRING").description("発言内容").build(),
                                "radius", Schema.builder().type("NUMBER").description("聞こえる範囲 (ブロック, デフォルト16)").build()
                        ))
                        .required(java.util.List.of("message"))
                        .build())
                .build();

        FunctionDeclaration emote = FunctionDeclaration.builder()
                .name("emote")
                .description("感情をパーティクルで表現する")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "emotion", Schema.builder()
                                        .type("STRING")
                                        .description("感情 (happy, sad, curious, angry, surprised)")
                                        .build()
                        ))
                        .required(java.util.List.of("emotion"))
                        .build())
                .build();

        FunctionDeclaration idle = FunctionDeclaration.builder()
                .name("idle")
                .description("その場で待機する")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "duration_seconds", Schema.builder().type("NUMBER").description("待機時間 (秒, 1〜10)").build()
                        ))
                        .required(java.util.List.of("duration_seconds"))
                        .build())
                .build();

        FunctionDeclaration wave = FunctionDeclaration.builder()
                .name("wave")
                .description("指定対象に手を振る")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "target_name", Schema.builder().type("STRING").description("手を振る対象の名前").build()
                        ))
                        .required(java.util.List.of("target_name"))
                        .build())
                .build();

        FunctionDeclaration remember = FunctionDeclaration.builder()
                .name("remember")
                .description("重要な出来事を記憶に保存する")
                .parameters(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "content", Schema.builder().type("STRING").description("記憶する内容").build(),
                                "importance", Schema.builder().type("INTEGER").description("重要度 (1-5)").build()
                        ))
                        .required(java.util.List.of("content", "importance"))
                        .build())
                .build();

        return Tool.builder()
                .functionDeclarations(java.util.List.of(
                        walkTo, approachPlayer, lookAt, say, emote, idle, wave, remember
                ))
                .build();
    }
}
