package com.slaythecraft.binder;

import com.slaythecraft.Config;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;

import net.minecraft.resources.ResourceLocation;

/**
 * 删卡服务：**收纳包 UI、卡牌回收站方块与卡牌大师商店共用同一套逻辑与价格**（设计文档 §6.3）。
 *
 * <p>v0.4.6 需求："此版本开始，每次删牌会导致下一次删牌需要花费增加 50。"
 * 所以价格不再是固定值，而是 {@code 基础价 + 50 × 已经删过的张数}
 * （已删张数记在档案的统计里，跨存档保留）。</p>
 *
 * <p>纯数据逻辑，不依赖 Minecraft 实体，因此可以无头校验（见 {@code CardDeleteServiceCheck}）。</p>
 */
public final class CardDeleteService {

    /** 删卡结果，UI 与方块按它给出不同提示。 */
    public enum Result {
        OK,
        NOT_ENOUGH_GOLD,
        NO_SUCH_CARD
    }

    private CardDeleteService() {
    }

    /** 基础删卡价（配置，默认 100）。 */
    public static int baseCost() {
        return Config.cardDeleteCost();
    }

    /** 这名玩家**这一次**删卡要花多少钱（基础价 + 递增）。 */
    public static int cost(PlayerCardData data) {
        int deleted = data == null ? 0 : data.stats().cardsDeleted();
        return baseCost() + Config.cardDeleteCostIncrease() * Math.max(0, deleted);
    }

    /** 删过 {@code deletedCount} 张时的价格（给界面显示"下次会涨到多少"用）。 */
    public static int costAfter(int deletedCount) {
        return baseCost() + Config.cardDeleteCostIncrease() * Math.max(0, deletedCount);
    }

    /**
     * 删除档案里第 {@code index} 张卡并扣金币。
     * 先校验卡牌存在与余额充足，**任一不满足都不做任何修改**。
     */
    public static Result deleteAt(PlayerCardData data, int index) {
        if (index < 0 || index >= data.cardCount()) {
            return Result.NO_SUCH_CARD;
        }
        int cost = cost(data);
        if (data.gold() < cost) {
            return Result.NOT_ENOUGH_GOLD;
        }
        CardInstance removed = data.removeCardAt(index).orElse(null);
        if (removed == null) {
            return Result.NO_SUCH_CARD;
        }
        // 余额已在上面校验过，这里必然成功
        data.spendGold(cost);
        return Result.OK;
    }

    /** 按卡牌 id 删一张（回收站/指令用；删除的是第一张同名卡）。 */
    public static Result deleteById(PlayerCardData data, ResourceLocation cardId) {
        int index = data.indexOfCard(cardId);
        if (index < 0) {
            return Result.NO_SUCH_CARD;
        }
        return deleteAt(data, index);
    }
}
