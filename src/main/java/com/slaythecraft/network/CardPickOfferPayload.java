package com.slaythecraft.network;

import java.util.List;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：**卡牌让你选牌**（v0.5.1.7 A3）。
 *
 * <p>需求："A3：应该让玩家自己选择。" —— 废料回收 / 应急储备 / 弃置边陵 / 回收 这四张牌的卡面
 * 本来就写着"选择…"，但实现一直是自动挑费用最高的。现在引擎会把这次选择**挂起**，
 * 由这个包把"选什么、最多选几张、候选是哪些"发给客户端。</p>
 *
 * <p>候选是**服务端快照**：客户端只负责显示与回传下标，不能自己从手牌/弃牌堆里算 ——
 * 否则打出的那张牌落堆的时机会让下标错位。</p>
 *
 * @param prompt     给玩家看的一句话（服务端按效果种类生成）
 * @param maxPicks   最多选几张（弃置边陵是"最多 N 张"，其余是恰好 1 张）
 * @param candidates 候选牌（顺序 = 回传下标的含义）
 */
public record CardPickOfferPayload(String prompt, int maxPicks, List<Choice> candidates)
        implements CustomPacketPayload {

    /** 候选里的一张牌（客户端用 id 去卡池取名字/费用）。 */
    public record Choice(ResourceLocation id, boolean upgraded) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Choice> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Choice::id,
                ByteBufCodecs.BOOL, Choice::upgraded,
                Choice::new);
    }

    public static final CustomPacketPayload.Type<CardPickOfferPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "card_pick_offer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CardPickOfferPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CardPickOfferPayload::prompt,
                    ByteBufCodecs.VAR_INT, CardPickOfferPayload::maxPicks,
                    Choice.STREAM_CODEC.apply(ByteBufCodecs.list()), CardPickOfferPayload::candidates,
                    CardPickOfferPayload::new);

    public CardPickOfferPayload {
        candidates = List.copyOf(candidates);
    }

    /** 这次请求是不是"真的要选"（服务端用一条空请求表示"已经处理完了，把界面收掉"）。 */
    public boolean active() {
        return !prompt.isEmpty();
    }

    /** "没有待选牌了"（服务端在玩家选完 / 超时兜底之后发一条，客户端据此收掉选牌页）。 */
    public static CardPickOfferPayload cleared() {
        return new CardPickOfferPayload("", 0, List.of());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
