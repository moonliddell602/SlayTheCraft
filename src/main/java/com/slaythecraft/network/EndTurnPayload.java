package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：结束回合（V1 出牌界面的「结束回合」按钮）。 */
public record EndTurnPayload() implements CustomPacketPayload {

    public static final EndTurnPayload INSTANCE = new EndTurnPayload();

    public static final CustomPacketPayload.Type<EndTurnPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "end_turn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EndTurnPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
