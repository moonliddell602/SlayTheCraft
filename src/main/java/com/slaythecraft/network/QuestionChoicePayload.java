package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：玩家选中的问号事件选项（v0.0.2 V11；v0.5.0 增加选中的牌下标）。
 *
 * @param eventId     事件 id（服务端据此重新查表）
 * @param optionIndex 0 基选项下标
 * @param cardIndex   需要选牌时玩家选的牌下标（0 基；不需要选牌时是 -1）
 */
public record QuestionChoicePayload(String eventId, int optionIndex, int cardIndex)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuestionChoicePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "question_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestionChoicePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, QuestionChoicePayload::eventId,
                    ByteBufCodecs.VAR_INT, QuestionChoicePayload::optionIndex,
                    ByteBufCodecs.VAR_INT, QuestionChoicePayload::cardIndex,
                    QuestionChoicePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
