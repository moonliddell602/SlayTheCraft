package com.slaythecraft.relic;

import com.slaythecraft.combat.core.CardEnchantment;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.combat.core.CardType;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.server.level.ServerPlayer;

/**
 * **获得遗物时的"立刻生效"效果**（v0.5.3.0）。
 *
 * <p>为什么单独一个服务：`PlayerCardData.grantRelic` 只能改档案数据（生命 / 栏位），
 * 而【制图台】要**发 5 次卡牌奖励**（需要 ServerPlayer 与奖励队列）。
 * 所以拿遗物之后统一走一趟这里，UI/指令/藏宝箱三条路都能覆盖。</p>
 *
 * <p>已经在 `grantRelic` 里处理的（最大生命、药水栏位）不再重复。</p>
 */
public final class RelicObtainService {

    private RelicObtainService() {
    }

    /**
     * 结算一件遗物的"获得时"效果。
     *
     * @return 给玩家看的说明；没有额外效果时返回空串
     */
    public static String apply(ServerPlayer player, String relicId) {
        if (player == null || relicId == null) {
            return "";
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        return switch (relicId) {
            case "worn_flint" -> enchantTwoAttacks(player, data);
            case "cartography_table" -> fiveCardRewards(player, data);
            default -> "";
        };
    }

    /** 【破旧打火石】：为两张**攻击牌**附魔【火焰附加】。 */
    private static String enchantTwoAttacks(ServerPlayer player, PlayerCardData data) {
        int done = data.enchantFirstMatching(CardEnchantment.FIRE_ASPECT, 2, instance -> {
            var definition = CardLibrary.byId(instance.id().getPath());
            return definition != null && definition.type() == CardType.ATTACK;
        });
        if (done <= 0) {
            return "牌组里没有可以附魔的攻击牌";
        }
        StcAttachments.syncTo(player);
        return "为 " + done + " 张攻击牌附上了【火焰附加】";
    }

    /** 【制图台】：立刻获得 5 次卡牌奖励（进待领队列，HUD 角标会 +5）。 */
    private static String fiveCardRewards(ServerPlayer player, PlayerCardData data) {
        data.addPendingRewards(5);
        StcAttachments.syncTo(player);
        return "立刻获得 5 次卡牌奖励（当前待领 " + data.pendingRewards() + " 次）";
    }
}
