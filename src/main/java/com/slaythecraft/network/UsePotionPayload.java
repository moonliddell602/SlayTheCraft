package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：使用某个槽位的药水（v0.0.2 V8；v0.4.6 支持对自己 / 队友使用）。
 *
 * @param slot   0 基槽位下标
 * @param target 目标：{@code -1} = 自己；{@code 0..n-1} = 敌人下标；
 *               {@code ALLY_BASE + i} = 第 i 名参战玩家（正面药水可以给队友用）
 */
public record UsePotionPayload(int slot, int target) implements CustomPacketPayload {

    /** 队友目标的起始下标（避开敌人下标，服务端按同一常量解析）。 */
    public static final int ALLY_BASE = 100;

    public static final CustomPacketPayload.Type<UsePotionPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "use_potion"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UsePotionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UsePotionPayload::slot,
            ByteBufCodecs.VAR_INT, UsePotionPayload::target,
            UsePotionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
