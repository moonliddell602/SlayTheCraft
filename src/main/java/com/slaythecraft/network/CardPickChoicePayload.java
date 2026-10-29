package com.slaythecraft.network;

import java.util.List;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：**选牌结果**（v0.5.1.7 A3）。
 *
 * @param indices 选中的下标（对应 {@link CardPickOfferPayload#candidates()} 的顺序）
 * @param auto    {@code true} = 放弃/超时，让服务端按"贪心"替他选（保证卡牌效果不会凭空消失）；
 *                {@code false} = 这是玩家的明确选择（可以为空，例如"弃置边陵一张都不消耗"）
 */
public record CardPickChoicePayload(List<Integer> indices, boolean auto) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CardPickChoicePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "card_pick_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardPickChoicePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), CardPickChoicePayload::indices,
                    ByteBufCodecs.BOOL, CardPickChoicePayload::auto,
                    CardPickChoicePayload::new);

    public CardPickChoicePayload {
        indices = List.copyOf(indices);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
