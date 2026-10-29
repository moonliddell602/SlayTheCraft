package com.slaythecraft.character;

import com.slaythecraft.Config;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 角色选择的 Minecraft 侧包装：调用纯逻辑 {@link CharacterService}，再把角色的最大生命写进玩家属性，
 * 并把档案同步给客户端（界面要显示"当前角色"）。
 *
 * <p>v0.0.2 测试反馈修正后：本局没结束之前选别的角色会被拒绝（{@link CharacterService.Result#LOCKED}），
 * 换角色 = 开新的一局（牌组/遗物/药水/进度都重置）。</p>
 */
public final class CharacterSelection {

    private CharacterSelection() {
    }

    /**
     * 选择 / 切换角色（或开新的一局）。
     *
     * @return 给玩家看的提示文案
     */
    public static String select(ServerPlayer player, CharacterProfile profile) {
        PlayerCardData data = StcAttachments.dataOf(player);
        CharacterService.Result result = CharacterService.select(data, profile);

        if (result == CharacterService.Result.LOCKED) {
            return "本局还在进行（第 " + data.floor() + " 层）：不能换角色。"
                    + "打完三层 BOSS 或战败之后才能换；想从头开始就用「重开本局」。";
        }
        if (result == CharacterService.Result.ALREADY_ACTIVE) {
            return "当前角色已经是【" + name(profile) + "】。";
        }

        // v0.2.1：独立生命值与玩家生命值**完全解耦**，这里不再改任何原版血量/属性。
        StcAttachments.syncTo(player);

        String name = name(profile);
        String relic = profile.startingRelicDefinition() == null ? ""
                : "，初始遗物【" + profile.startingRelicDefinition().name() + "】";
        return switch (result) {
            case STARTED -> "已成为【" + name + "】：独立生命上限 " + Config.maxPlayerHp()
                    + "、每回合能量 " + profile.energyPerTurn() + "，起始牌组 "
                    + profile.startingDeck().size() + " 张已写入收纳包" + relic + "。";
            case RESTARTED -> "开始新的一局【" + name + "】：牌组重置为 " + profile.startingDeck().size()
                    + " 张起始牌，遗物 / 药水 / 进度已清空，独立生命值 " + data.life().describe()
                    + "，金币重置为 " + data.gold() + relic + "。";
            default -> "当前角色已经是【" + name + "】。";
        };
    }

    private static String name(CharacterProfile profile) {
        return Component.translatable(profile.nameKey()).getString();
    }
}
