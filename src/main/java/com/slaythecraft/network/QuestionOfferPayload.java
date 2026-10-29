package com.slaythecraft.network;

import java.util.List;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：问号事件的描述与选项（v0.0.2 V11；v0.5.0 增加事件描述与"要不要先选牌"）。
 *
 * <p>只送**展示用文本**与事件 id：玩家点选后回传的是 {@code (eventId, optionIndex, cardIndex)}，
 * 服务端重新查表应用效果，客户端无法凭空造出奖励。</p>
 */
public record QuestionOfferPayload(String eventId, String name, List<String> description, List<OptionView> options)
        implements CustomPacketPayload {

    /**
     * 一个选项的展示文本。
     *
     * @param needsCard 这一项要不要先打开"选一张牌"页面
     * @param pickHint  选牌页的说明（不需要选牌时为空串）
     */
    public record OptionView(String label, String description, boolean needsCard, String pickHint) {
        public static final StreamCodec<RegistryFriendlyByteBuf, OptionView> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, OptionView::label,
                ByteBufCodecs.STRING_UTF8, OptionView::description,
                ByteBufCodecs.BOOL, OptionView::needsCard,
                ByteBufCodecs.STRING_UTF8, OptionView::pickHint,
                OptionView::new);
    }

    public static final CustomPacketPayload.Type<QuestionOfferPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "question_offer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestionOfferPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, QuestionOfferPayload::eventId,
                    ByteBufCodecs.STRING_UTF8, QuestionOfferPayload::name,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), QuestionOfferPayload::description,
                    OptionView.STREAM_CODEC.apply(ByteBufCodecs.list()), QuestionOfferPayload::options,
                    QuestionOfferPayload::new);

    public QuestionOfferPayload {
        description = List.copyOf(description);
        options = List.copyOf(options);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
