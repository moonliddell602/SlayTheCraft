package com.slaythecraft.data;

import java.util.function.Supplier;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 注册在玩家身上的持久化数据（设计文档 §5.2）。
 *
 * <p>用 NeoForge 1.21 的 {@link AttachmentType} 而不是旧 Capability：以玩家为单位、随存档持久化、
 * 可选同步到客户端（UI 与右下角 HUD 要读金币 / 待领奖励）。</p>
 */
public final class StcAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SlayTheCraft.MODID);

    /** 玩家卡牌档案：持久化 + 跨死亡保留 + 同步客户端。 */
    public static final Supplier<AttachmentType<PlayerCardData>> CARD_DATA = ATTACHMENT_TYPES.register("card_data",
            () -> AttachmentType.builder(PlayerCardData::new)
                    .serialize(PlayerCardData.CODEC)
                    .copyOnDeath()
                    .sync(PlayerCardData.STREAM_CODEC)
                    .build());

    private StcAttachments() {
    }

    /** 取（必要时创建）玩家的卡牌档案。 */
    public static PlayerCardData dataOf(Player player) {
        return player.getData(CARD_DATA);
    }

    /**
     * 服务端改完数据后调用：把档案推送给该玩家的客户端。
     * Attachment 不会自动同步，所有写操作之后都必须显式调用。
     */
    public static void syncTo(Player player) {
        player.syncData(CARD_DATA);
    }
}
