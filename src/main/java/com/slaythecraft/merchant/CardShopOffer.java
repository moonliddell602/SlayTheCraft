package com.slaythecraft.merchant;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * 商店里的一件商品：哪张卡 + 多少金币。
 *
 * <p>不落盘（店面每次打开现抽），所以只需要流编解码器，不需要存档 Codec。</p>
 */
public record CardShopOffer(ResourceLocation cardId, int price) {

    public static final StreamCodec<RegistryFriendlyByteBuf, CardShopOffer> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CardShopOffer::cardId,
            ByteBufCodecs.VAR_INT, CardShopOffer::price,
            CardShopOffer::new);
}
