package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：**开发者模式的牌组编辑请求**（v0.5.1.0）。
 *
 * <p>需求（开发者模式）："可以在图鉴里直接向卡组添加卡牌。可以直接在查看卡组页面删牌。"
 * 图鉴与"查看我的牌组"都是**只读的查看页面**，客户端不能直接改档案 —— 所以这里发一个请求，
 * 由服务端在 {@code DevModeHandler} 里校验"开发者模式已开启"之后再动档案
 * （客户端永远不被信任）。</p>
 *
 * @param action   {@code add} / {@code remove}
 * @param cardId   卡牌 id（不带命名空间，例如 {@code strike}）
 */
public record DevDeckPayload(String action, String cardId) implements CustomPacketPayload {

    public static final String ACTION_ADD = "add";
    public static final String ACTION_REMOVE = "remove";

    public static final CustomPacketPayload.Type<DevDeckPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "dev_deck"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DevDeckPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DevDeckPayload::action,
                    ByteBufCodecs.STRING_UTF8, DevDeckPayload::cardId,
                    DevDeckPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
