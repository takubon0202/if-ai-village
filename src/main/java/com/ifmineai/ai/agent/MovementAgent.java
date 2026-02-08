package com.ifmineai.ai.agent;

import com.ifmineai.ai.NPCBrain;
import com.ifmineai.ai.action.NPCAction;
import com.ifmineai.ai.action.WalkToAction;
import com.ifmineai.config.AIConfig;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
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

        // Y座標を地面に合わせる (屋根・木の上を回避)
        target.setY(findGroundY(npc.getWorld(), target.getBlockX(), target.getBlockZ(), npc.getLocation().getBlockY() + 10));

        // ホーム範囲内か確認
        if (target.distanceSquared(home) > radius * radius) {
            // 範囲外なら範囲内に収める
            target = home.clone().add(offsetX * 0.3, 0, offsetZ * 0.3);
            target.setY(findGroundY(npc.getWorld(), target.getBlockX(), target.getBlockZ(), npc.getLocation().getBlockY() + 10));
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
        target.setY(findGroundY(npc.getWorld(), target.getBlockX(), target.getBlockZ(), npc.getLocation().getBlockY() + 10));
        return new WalkToAction(target, 1.2);
    }

    /**
     * 指定座標の地面Y座標を検出する。
     * getHighestBlockYAt() は屋根・木の上を返すため、NPCの現在Y付近から下方向に走査して
     * 固体ブロック+上が非固体の位置を返す。
     */
    public static int findGroundY(World world, int x, int z, int startY) {
        int maxY = Math.min(startY, world.getMaxHeight() - 1);
        int minY = world.getMinHeight();

        for (int y = maxY; y > minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            if (block.getType().isSolid() && !above.getType().isSolid()) {
                return y + 1;
            }
        }

        // 見つからなかった場合はフォールバック
        return world.getHighestBlockYAt(x, z) + 1;
    }
}
