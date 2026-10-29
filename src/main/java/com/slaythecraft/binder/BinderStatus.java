package com.slaythecraft.binder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.slaythecraft.combat.mc.CombatManager;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;

import net.minecraft.resources.ResourceLocation;

/**
 * 档案的可读摘要。S2 的收纳包 UI 出现之前，它是右键收纳包与 {@code /stc binder status} 的共同数据源，
 * 避免同一份信息在两个地方各写一遍。
 */
public final class BinderStatus {

    private BinderStatus() {
    }

    public static List<String> lines(PlayerCardData data, boolean hasBinder, String binderLocation, boolean inCombat) {
        List<String> lines = new ArrayList<>();
        lines.add("金币：" + data.gold() + "　·　卡牌：" + data.cardCount() + " 张　·　待领奖励：" + data.pendingRewards());
        lines.add("角色：" + (data.character() == null ? "未选择" : data.character().toString())
                + "　·　收纳包：" + (hasBinder ? "已携带（" + binderLocation + "）" : "未携带"));
        lines.add("随身状态：" + (inCombat ? "战斗中" : "空闲")
                + "　·　战斗统计：击杀 " + data.stats().kills()
                + " / 胜 " + data.stats().battlesWon()
                + " / 败 " + data.stats().battlesLost()
                + " / 已删卡 " + data.stats().cardsDeleted());
        if (data.cardCount() == 0) {
            lines.add("卡牌：（空）");
        } else {
            lines.add("卡牌：" + summarize(data.cards()));
        }
        Map<ResourceLocation, Integer> progress = data.killProgress();
        if (!progress.isEmpty()) {
            StringBuilder sb = new StringBuilder("击杀进度：");
            boolean first = true;
            for (Map.Entry<ResourceLocation, Integer> entry : progress.entrySet()) {
                if (!first) {
                    sb.append("　");
                }
                sb.append(entry.getKey().getPath()).append(' ').append(entry.getValue());
                first = false;
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    public static List<String> linesFor(net.minecraft.server.level.ServerPlayer player) {
        // v0.4.6.3：这里报"身上有没有包"（背包里也算），与"能不能打开界面"保持一致
        List<String> lines = lines(StcAttachments.dataOf(player),
                BinderResolver.carried(player),
                BinderResolver.binderLocation(player),
                CombatManager.inCombat(player));
        if (BinderResolver.onlyInBackpack(player)) {
            lines.add("收纳包在背包里：B 键 / 右键都能打开界面，但**开始尖塔对战**要先拿到主手或副手。");
        }
        return lines;
    }

    private static String summarize(List<CardInstance> cards) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) {
                sb.append("　");
            }
            CardInstance card = cards.get(i);
            sb.append(i + 1).append('.').append(card.id().getPath());
            if (card.upgraded()) {
                sb.append('+').append(card.upgrades());
            }
            if (card.enchanted()) {
                sb.append('[').append(card.enchantmentLabel()).append(']');
            }
        }
        return sb.toString();
    }
}
