package com.slaythecraft.network;

import java.util.List;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.merchant.CardShopOffer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：商店当前的全部货架（v0.0.2 V8；v0.4.6 扩充成"卡牌 + 遗物 + 药水 + 删牌"）。
 *
 * <p>商店的货架是**菜单本地状态**（不像玩家档案那样常驻同步），所以这里需要一个专门的包；
 * 打开商店、买走一件、刷新商品之后都会重发一次。</p>
 *
 * <p>字段分组打包成 {@link ShopStock} 与 {@link ShopPrices}：{@code StreamCodec.composite}
 * 最多支持 6 个字段，分组之后既躲开上限也更易读。</p>
 */
public record ShopOffersPayload(int containerId, ShopStock stock, ShopPrices prices, int refreshLeft)
        implements CustomPacketPayload {

    /** 货架内容：卡牌 + 遗物 + 药水。 */
    public record ShopStock(List<CardShopOffer> offers, List<String> relics, List<String> potions) {

        public static final StreamCodec<RegistryFriendlyByteBuf, ShopStock> STREAM_CODEC =
                StreamCodec.composite(
                        CardShopOffer.STREAM_CODEC.apply(ByteBufCodecs.list()), ShopStock::offers,
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ShopStock::relics,
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ShopStock::potions,
                        ShopStock::new);

        public ShopStock {
            offers = List.copyOf(offers);
            relics = List.copyOf(relics);
            potions = List.copyOf(potions);
        }
    }

    /**
     * 价格表。
     *
     * @param refreshCost 刷新一次的价格
     * @param relicPrice  遗物售价
     * @param potionPrice 药水售价
     * @param deleteCost  这一次删牌的价格（每次删牌后会上涨）
     */
    public record ShopPrices(int refreshCost, int relicPrice, int potionPrice, int deleteCost) {

        public static final StreamCodec<RegistryFriendlyByteBuf, ShopPrices> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, ShopPrices::refreshCost,
                        ByteBufCodecs.VAR_INT, ShopPrices::relicPrice,
                        ByteBufCodecs.VAR_INT, ShopPrices::potionPrice,
                        ByteBufCodecs.VAR_INT, ShopPrices::deleteCost,
                        ShopPrices::new);
    }

    public static final CustomPacketPayload.Type<ShopOffersPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "shop_offers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShopOffersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShopOffersPayload::containerId,
            ShopStock.STREAM_CODEC, ShopOffersPayload::stock,
            ShopPrices.STREAM_CODEC, ShopOffersPayload::prices,
            ByteBufCodecs.VAR_INT, ShopOffersPayload::refreshLeft,
            ShopOffersPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
