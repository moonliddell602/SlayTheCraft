package com.slaythecraft.network;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 → 客户端：**开发者模式开关状态**（v0.5.1.2）。
 *
 * <p>需求（开发者模式）："可以在图鉴里直接向卡组添加卡牌。可以直接在查看卡组页面删牌。"
 * 这两个按钮**只在开发者模式开着的时候显示** —— 否则普通玩家的图鉴里会平白多出两个按不动的按钮。
 * 开关状态只存在服务端内存里，所以必须同步给客户端一份。</p>
 *
 * <p>客户端拿它**只用来决定按钮显示与否**：真正的校验永远在服务端
 * （{@code DevDeckService.handle} 会再查一次 {@code DevMode.isEnabled}），
 * 所以就算客户端被改成"永远显示按钮"也改不动牌组。</p>
 *
 * @param enabled 开发者模式是否已开启
 */
public record DevStatePayload(boolean enabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DevStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "dev_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DevStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, DevStatePayload::enabled,
                    DevStatePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
