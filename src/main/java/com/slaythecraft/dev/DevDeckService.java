package com.slaythecraft.dev;

import com.slaythecraft.combat.core.Card;
import com.slaythecraft.combat.core.CardLibrary;
import com.slaythecraft.data.CardIds;
import com.slaythecraft.data.CardInstance;
import com.slaythecraft.data.PlayerCardData;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.network.DevDeckPayload;

import net.minecraft.server.level.ServerPlayer;

/**
 * 开发者模式的**牌组编辑服务**（v0.5.1.0）。
 *
 * <p>需求（开发者模式）："可以在图鉴里直接向卡组添加卡牌。可以直接在查看卡组页面删牌。"</p>
 *
 * <p>图鉴与"查看我的牌组"都是只读页面，客户端不能直接改档案；客户端只发
 * {@link DevDeckPayload} 请求，真正的校验与写入都在这里 —— **客户端永远不被信任**。</p>
 */
public final class DevDeckService {

    private DevDeckService() {
    }

    /**
     * 处理一次加卡 / 删卡请求。
     *
     * @return 给玩家看的提示
     */
    public static String handle(ServerPlayer player, String action, String cardId) {
        if (!DevMode.isEnabled(player)) {
            return "牌组编辑需要开发者模式（用 /stc dev on 开启）。";
        }
        if (cardId == null || cardId.isBlank()) {
            return "卡牌 id 为空。";
        }
        Card definition = CardLibrary.byId(cardId);
        if (definition == null) {
            return "卡池里没有这张卡：" + cardId + "。";
        }
        PlayerCardData data = StcAttachments.dataOf(player);
        boolean add = DevDeckPayload.ACTION_ADD.equals(action);
        if (add) {
            data.addCard(CardInstance.of(CardIds.of(definition)));
            StcAttachments.syncTo(player);
            return "【开发者模式】已把【" + definition.name() + "】加入牌组（现有 "
                    + data.cardCount() + " 张）。";
        }
        if (DevDeckPayload.ACTION_REMOVE.equals(action)) {
            if (data.removeFirstCard(CardIds.of(definition))) {
                StcAttachments.syncTo(player);
                return "【开发者模式】已从牌组移除一张【" + definition.name() + "】（现有 "
                        + data.cardCount() + " 张）。";
            }
            return "【开发者模式】牌组里没有【" + definition.name() + "】。";
        }
        return "未知的牌组编辑动作：" + action + "。";
    }
}
