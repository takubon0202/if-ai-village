package com.ifmineai.ai.action;

import org.bukkit.entity.Mob;

public interface NPCAction {

    /**
     * アクション開始時に呼ばれる
     */
    void start(Mob npc);

    /**
     * 毎tick呼ばれる
     * @return true = 完了, false = 継続
     */
    boolean tick(Mob npc);

    /**
     * アクション中断時に呼ばれる
     */
    void cancel(Mob npc);

    /**
     * アクションの説明文
     */
    String describe();
}
