package com.slaythecraft.network;

import java.util.List;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：篝火准备状态（v0.0.3 W5）。
 *
 * <p>火堆的已用次数/上限客户端自己能从档案与配置里读到，所以这里只送"等待名单 / 队友名单 / 是否就位"。</p>
 *
 * @param containerId 菜单 id（客户端据此判断是不是自己的篝火界面）
 * @param waiting     还没进入准备界面的队友名字（多人时必须都进来才能开始）
 * @param allies      附近的队友名字（"包扎"要用；客户端不能自己算队伍）
 * @param ready       是否所有人都已就位（就位后按钮才可用）
 */
public record CampfireStatePayload(int containerId, List<String> waiting, List<String> allies, boolean ready)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CampfireStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "campfire_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CampfireStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CampfireStatePayload::containerId,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CampfireStatePayload::waiting,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CampfireStatePayload::allies,
                    ByteBufCodecs.BOOL, CampfireStatePayload::ready,
                    CampfireStatePayload::new);

    public CampfireStatePayload {
        waiting = List.copyOf(waiting);
        allies = List.copyOf(allies);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
