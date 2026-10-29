package com.slaythecraft.combat.mc;

import com.slaythecraft.combat.core.Combatant;
import com.slaythecraft.combat.core.LifePool;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家的战斗化身（v0.0.2 V9 起使用**独立生命值**；v0.2.1 起与玩家生命值**完全解耦**）。
 *
 * <p>测试反馈："独立生命值和玩家生命值无关，不应该把独立生命值和玩家生命值关联。"
 * 因此这个类现在**不碰原版血条**：</p>
 * <ul>
 *   <li>战斗血量来自玩家档案里的 {@link LifePool}（默认上限 75），战斗结束时由
 *       {@code CombatManager} 写回档案；</li>
 *   <li>{@link #onHpChanged} 不写原版血量（原版掉血也不进战斗）；</li>
 *   <li>{@link #onDefeated} **不再**通过原版伤害把玩家打死 —— 独立生命值归零只是"这一局失败"，
 *       原版角色不受影响（以前那条 {@code defeatKillsPlayer} 联动已经删掉）。</li>
 * </ul>
 */
public final class PlayerCombatant extends Combatant {
    private final ServerPlayer player;

    public PlayerCombatant(ServerPlayer player) {
        this(player, StcAttachments.dataOf(player).life());
    }

    public PlayerCombatant(ServerPlayer player, LifePool life) {
        super(player.getName().getString(), life.maxHp(), life.hp());
        this.player = player;
    }

    public ServerPlayer player() {
        return player;
    }

    @Override
    protected void onHpChanged(int previousHp, int newHp) {
        // 与玩家生命值无关：刻意什么都不做（原版血条既不受战斗影响，也不影响战斗）。
    }

    @Override
    protected void onDefeated() {
        // 与玩家生命值无关：独立生命值归零不杀原版角色。
        // "本局结束 / 生命值回满或保留 1 点"由 CombatManager 在战斗结束时处理。
    }

    /** 便于调试指令查看档案里的生命值。 */
    public PlayerCardData data() {
        return StcAttachments.dataOf(player);
    }
}
