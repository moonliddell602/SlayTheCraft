package com.slaythecraft.combat.mc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.slaythecraft.combat.core.CombatContext;
import com.slaythecraft.combat.core.CombatFxEvent;
import com.slaythecraft.network.BattleFxPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 战斗表现层事件的**服务器侧总线**（v0.3.0）。
 *
 * <p>引擎只管广播 {@link CombatFxEvent}，这里负责把它转成 {@link BattleFxPayload}
 * 发给这场战斗的所有参战玩家。名单在开战时一次性记住（引擎在结算过程中只认识 {@code Combatant}）。</p>
 */
public final class CombatFxBus {

    private static final Map<CombatContext, List<ServerPlayer>> ROSTERS = new HashMap<>();

    private CombatFxBus() {
    }

    /** 开战时挂上：记住参战名单，并把引擎的广播接到网络包上。 */
    public static void attach(CombatContext context, List<ServerPlayer> roster) {
        List<ServerPlayer> members = List.copyOf(roster);
        ROSTERS.put(context, members);
        context.setFxSink(event -> dispatch(members, event));
    }

    /** 战斗结束时摘掉（同时清空引擎的接收器，避免野指针）。 */
    public static void detach(CombatContext context) {
        if (context == null) {
            return;
        }
        ROSTERS.remove(context);
        context.setFxSink(null);
    }

    /** 当前挂着的战斗数量（自检/调试用）。 */
    public static int attachedCount() {
        return ROSTERS.size();
    }

    private static void dispatch(List<ServerPlayer> roster, CombatFxEvent event) {
        if (event == null || event.kind() == null) {
            return;
        }
        // v0.5.0.10 成就：这两个是"战斗中发生过某件事"，帧里没有对应的原版触发器，
        // 所以在这里直接授予（对应 JSON 里是 minecraft:impossible）。
        switch (event.kind()) {
            case FROST_CHANNELLED -> {
                for (ServerPlayer member : roster) {
                    com.slaythecraft.achievement.AchievementService.grantFocusFrost(member);
                }
            }
            case FOUR_SAME_CARDS -> {
                for (ServerPlayer member : roster) {
                    com.slaythecraft.achievement.AchievementService.grantEchoForm(member);
                }
            }
            default -> {
                // 其余事件只走"表现层"，不涉及成就
            }
        }
        BattleFxPayload payload = BattleFxPayload.of(event);
        for (ServerPlayer member : roster) {
            if (member.connection != null) {
                PacketDistributor.sendToPlayer(member, payload);
            }
        }
    }
}
