package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：问号事件**选择之后的结果文案**（v0.5.0）。
 *
 * <p>需求原文："选择选项之后弹出的文案"。以前结果只发聊天栏，现在会同时下发这个包，
 * 客户端弹一页独立的结果页（沿用"子页面一律新开页面"的约定）。</p>
 */
public record QuestionResultPayload(String eventName, String optionLabel, String text)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuestionResultPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "question_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestionResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, QuestionResultPayload::eventName,
                    ByteBufCodecs.STRING_UTF8, QuestionResultPayload::optionLabel,
                    ByteBufCodecs.STRING_UTF8, QuestionResultPayload::text,
                    QuestionResultPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
