package com.ifmineai.ai.agent;

import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.action.NPCAction;
import com.ifmineai.ai.action.WalkToAction;
import com.ifmineai.config.AIConfig;

import org.bukkit.Location;
import org.bukkit.entity.Mob;

import java.util.Random;

/**
 * 移動判断エージェント - ホーム範囲内でのランダム歩行やプレイヤーへの接近を管理
 */
public class MovementAgent implements BehaviorAgent {

    private final AIConfig config;
    private final Random random = new Random();

    public MovementAgent(AIConfig config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "MovementAgent";
    }

    /**
     * ホーム範囲内でランダムな歩行先を生成
     */
    public NPCAction generateRandomWalk(Mob npc, NPCBrain brain) {
        Location home = brain.getHomeLocation();
        if (home == null) return null;

        double radius = config.getHomeRadius();
        double offsetX = (random.nextDouble() * 2 - 1) * radius * 0.5;
        double offsetZ = (random.nextDouble() * 2 - 1) * radius * 0.5;

        Location target = home.clone().add(offsetX, 0, offsetZ);

        // Y座標を地面に合わせる
        target.setY(npc.getWorld().getHighestBlockYAt(target.getBlockX(), target.getBlockZ()));

        // ホーム範囲内か確認
        if (target.distanceSquared(home) > radius * radius) {
            // 範囲外なら範囲内に収める
            target = home.clone().add(offsetX * 0.3, 0, offsetZ * 0.3);
            target.setY(npc.getWorld().getHighestBlockYAt(target.getBlockX(), target.getBlockZ()));
        }

        return new WalkToAction(target, 1.0);
    }

    /**
     * 指定座標への歩行アクションを生成 (AI指示からの呼び出し用)
     */
    public NPCAction walkTo(Location target, double speed) {
        return new WalkToAction(target, speed);
    }

    /**
     * プレイヤーに近づくアクションを生成
     */
    public NPCAction approachPlayer(Mob npc, Location playerLoc, double stopDistance) {
        // プレイヤーの手前で止まるようにオフセット
        double dx = npc.getLocation().getX() - playerLoc.getX();
        double dz = npc.getLocation().getZ() - playerLoc.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist <= stopDistance) {
            return null; // すでに近い
        }

        double ratio = stopDistance / dist;
        Location target = playerLoc.clone().add(dx * ratio, 0, dz * ratio);
        return new WalkToAction(target, 1.2);
    }
}
