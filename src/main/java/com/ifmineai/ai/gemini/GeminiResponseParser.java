package com.ifmineai.ai.gemini;

import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.action.*;
import com.ifmineai.ai.agent.MemoryAgent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Geminiレスポンスのツールコールをパースし、NPCActionに変換
 */
public class GeminiResponseParser {

    private static final Logger LOGGER = Logger.getLogger(GeminiResponseParser.class.getName());

    public List<NPCAction> parse(GenerateContentResponse response, Mob npc, NPCBrain brain) {
        List<NPCAction> actions = new ArrayList<>();

        try {
            if (response == null) return actions;

            Optional<List<Candidate>> candidatesOpt = response.candidates();
            if (candidatesOpt.isEmpty()) return actions;

            for (Candidate candidate : candidatesOpt.get()) {
                Optional<Content> contentOpt = candidate.content();
                if (contentOpt.isEmpty()) continue;

                Optional<List<Part>> partsOpt = contentOpt.get().parts();
                if (partsOpt.isEmpty()) continue;

                for (Part part : partsOpt.get()) {
                    Optional<FunctionCall> fcOpt = part.functionCall();
                    if (fcOpt.isEmpty()) continue;

                    NPCAction action = convertFunctionCall(fcOpt.get(), npc, brain);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "レスポンスパース失敗", e);
        }

        return actions;
    }

    private NPCAction convertFunctionCall(FunctionCall fc, Mob npc, NPCBrain brain) {
        Optional<String> nameOpt = fc.name();
        Optional<Map<String, Object>> argsOpt = fc.args();
        if (nameOpt.isEmpty() || argsOpt.isEmpty()) return null;

        String name = nameOpt.get();
        Map<String, Object> args = argsOpt.get();

        try {
            return switch (name) {
                case "walk_to" -> parseWalkTo(args, npc, brain);
                case "approach_player" -> parseApproachPlayer(args, npc);
                case "look_at" -> parseLookAt(args, npc);
                case "say" -> parseSay(args, brain);
                case "emote" -> parseEmote(args);
                case "idle" -> parseIdle(args);
                case "wave" -> parseWave(args, npc);
                case "remember" -> parseRemember(args, brain);
                default -> {
                    LOGGER.warning("未知のツール: " + name);
                    yield null;
                }
            };
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "ツールコール変換失敗: " + name, e);
            return null;
        }
    }

    private NPCAction parseWalkTo(Map<String, Object> args, Mob npc, NPCBrain brain) {
        double offsetX = getDouble(args, "offset_x", 0);
        double offsetZ = getDouble(args, "offset_z", 0);
        double speed = getDouble(args, "speed", 1.0);

        Location home = brain.getHomeLocation();
        if (home == null) return null;

        Location target = home.clone().add(offsetX, 0, offsetZ);
        target.setY(npc.getWorld().getHighestBlockYAt(target.getBlockX(), target.getBlockZ()));

        return new WalkToAction(target, speed);
    }

    private NPCAction parseApproachPlayer(Map<String, Object> args, Mob npc) {
        String playerName = getString(args, "player_name", null);
        double stopDistance = getDouble(args, "stop_distance", 2.0);
        if (playerName == null) return null;

        Player player = Bukkit.getPlayer(playerName);
        if (player == null || !player.isOnline()) return null;

        Location playerLoc = player.getLocation();
        double dx = npc.getLocation().getX() - playerLoc.getX();
        double dz = npc.getLocation().getZ() - playerLoc.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist <= stopDistance) return null;

        double ratio = stopDistance / dist;
        Location target = playerLoc.clone().add(dx * ratio, 0, dz * ratio);
        return new WalkToAction(target, 1.2);
    }

    private NPCAction parseLookAt(Map<String, Object> args, Mob npc) {
        String targetName = getString(args, "target_name", null);
        double durationSec = getDouble(args, "duration_seconds", 2.0);
        if (targetName == null) return null;

        Entity target = findEntity(npc, targetName);
        if (target == null) return null;

        return new LookAtAction(target, (int) (durationSec * 20));
    }

    private NPCAction parseSay(Map<String, Object> args, NPCBrain brain) {
        String message = getString(args, "message", null);
        double radius = getDouble(args, "radius", 16.0);
        if (message == null) return null;

        return new SayAction(message, radius, brain.getData().getPersonalityType());
    }

    private NPCAction parseEmote(Map<String, Object> args) {
        String emotionStr = getString(args, "emotion", "happy");

        EmoteAction.Emotion emotion;
        try {
            emotion = EmoteAction.Emotion.valueOf(emotionStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            emotion = EmoteAction.Emotion.HAPPY;
        }

        return new EmoteAction(emotion);
    }

    private NPCAction parseIdle(Map<String, Object> args) {
        double durationSec = getDouble(args, "duration_seconds", 3.0);
        durationSec = Math.max(1.0, Math.min(10.0, durationSec));
        return new IdleAction((int) (durationSec * 20));
    }

    private NPCAction parseWave(Map<String, Object> args, Mob npc) {
        String targetName = getString(args, "target_name", null);
        Entity target = targetName != null ? findEntity(npc, targetName) : null;
        return new WaveAction(target);
    }

    private NPCAction parseRemember(Map<String, Object> args, NPCBrain brain) {
        // rememberはアクションではなく記憶操作 - nullを返して別途処理
        // ここでは記憶をMemoryAgentに直接追加する代わりに、
        // 軽量な Idle を返す
        return new IdleAction(5);
    }

    private Entity findEntity(Mob npc, String name) {
        // まずプレイヤーを探す
        Player player = Bukkit.getPlayer(name);
        if (player != null && player.isOnline()) {
            return player;
        }

        // 近くのエンティティから名前で探す
        for (Entity entity : npc.getLocation().getNearbyEntities(16, 16, 16)) {
            if (entity.getName().equalsIgnoreCase(name)) {
                return entity;
            }
        }

        return null;
    }

    private double getDouble(Map<String, Object> args, String key, double def) {
        Object val = args.get(key);
        if (val instanceof Number num) return num.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private String getString(Map<String, Object> args, String key, String def) {
        Object val = args.get(key);
        if (val instanceof String s) return s;
        if (val != null) return val.toString();
        return def;
    }

    private int getInt(Map<String, Object> args, String key, int def) {
        Object val = args.get(key);
        if (val instanceof Number num) return num.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
