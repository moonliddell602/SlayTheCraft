package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：出一张牌（V1 出牌界面点击时发送）。
 *
 * <p>两个下标都是 0 基；`enemyIndex` 为 −1 表示"由服务端挑第一个存活敌人"（不需要指定目标的牌）。</p>
 */
public record PlayCardPayload(int handIndex, int enemyIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayCardPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "play_card"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayCardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayCardPayload::handIndex,
            ByteBufCodecs.VAR_INT, PlayCardPayload::enemyIndex,
            PlayCardPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
