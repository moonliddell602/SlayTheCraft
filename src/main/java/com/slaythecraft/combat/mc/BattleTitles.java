package com.slaythecraft.combat.mc;

import java.util.Collection;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * 战斗开场的**原版大标题**（v0.4.6.3）。
 *
 * <p>需求原文："战斗开始和第 X 回合的呈现动画可以使用原版 /title 大标题浮现。"</p>
 *
 * <p>所以「战斗开始」与「第 X 回合」不再由客户端自绘横幅（{@code BattleBanners} /
 * {@code BattleBannerLayer}，v0.4.6.3 已删除），改为服务端直接下发原版 title 包 ——
 * 与 {@code /title} 指令完全同一条路径，淡入淡出由原版客户端负责，多人时每个参战玩家各收一份。</p>
 *
 * <p>时长与旧的客户端横幅对齐（{@link com.slaythecraft.combat.core.FadeTimeline}）：
 * 「战斗开始」6+18+6 = 30 tick（1.5 秒），「第 X 回合」6+12+6 = 24 tick（1.2 秒）。</p>
 *
 * <p>注意：原版 {@code Gui.setTitle} 每次都会**重置**标题计时器，所以背靠背发两条标题只会看到后一条 ——
 * 「战斗开始」与「第 1 回合」因此做成"主标题 + 副标题"同屏出现，而不是排队播放。</p>
 */
public final class BattleTitles {

    /** 战斗开始：淡入 6 tick / 停留 18 tick / 淡出 6 tick = 1.5 秒。 */
    private static final ClientboundSetTitlesAnimationPacket BATTLE_START_TIMES =
            new ClientboundSetTitlesAnimationPacket(6, 18, 6);
    /** 回合开始：6 / 12 / 6 = 1.2 秒。 */
    private static final ClientboundSetTitlesAnimationPacket TURN_TIMES =
            new ClientboundSetTitlesAnimationPacket(6, 12, 6);

    private BattleTitles() {
    }

    /**
     * 开战：主标题「战斗开始」，副标题「第 1 回合」。
     *
     * <p>包是只读的，可以安全地发给多个玩家（原版 {@code TitleCommand} 也是这么做的）。</p>
     */
    public static void battleStart(Collection<ServerPlayer> roster) {
        for (ServerPlayer member : roster) {
            if (member.connection == null) {
                continue;
            }
            member.connection.send(BATTLE_START_TIMES);
            member.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("stc.battle.banner.turn", 1)));
            member.connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("stc.battle.banner.start")));
        }
    }

    /** 新回合：主标题「第 X 回合」。 */
    public static void turn(Collection<ServerPlayer> roster, int turn) {
        for (ServerPlayer member : roster) {
            if (member.connection == null) {
                continue;
            }
            member.connection.send(TURN_TIMES);
            member.connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("stc.battle.banner.turn", turn)));
        }
    }

    /** 战斗结束：把可能还在淡出的标题收掉（否则「第 3 回合」会飘到奖励界面上）。 */
    public static void clear(Collection<ServerPlayer> roster) {
        for (ServerPlayer member : roster) {
            if (member.connection != null) {
                member.connection.send(new ClientboundClearTitlesPacket(false));
            }
        }
    }
}
