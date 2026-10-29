package com.slaythecraft.question;

import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.binder.BinderResolver;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * v0.4.6 两个新事件的**触发条件**（需求原文写得很具体）。
 *
 * <ul>
 *   <li>蘑菇岛事件：**副手【卡牌收纳包】+ 主手【战斗军旗】+ 右键哞菇牛**；</li>
 *   <li>烟熏蜜蜂事件：**副手【卡牌收纳包】+ 主手剪刀 + 右键蜂巢**。</li>
 * </ul>
 *
 * <p>两个条件都要求"副手拿着收纳包"，这既是需求，也顺带保证玩家确实在玩尖塔内容
 * （没带收纳包的人右键哞菇/蜂巢仍然是原版行为）。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class EventTriggers {

    private EventTriggers() {
    }

    /** 蘑菇岛：主手军旗 + 副手收纳包 + 哞菇牛。 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof MushroomCow)) {
            return;
        }
        if (!holdsBinderOffhand(player) || !player.getMainHandItem().is(SlayTheCraft.BATTLE_STANDARD.get())) {
            return;
        }
        event.setCanceled(true);
        open(player, QuestionLibrary.MUSHROOM_ISLAND);
    }

    /** 烟熏蜜蜂：主手剪刀 + 副手收纳包 + 蜂巢方块。 */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.BEEHIVE) && !state.is(Blocks.BEE_NEST)) {
            return;
        }
        if (!holdsBinderOffhand(player) || !player.getMainHandItem().is(Items.SHEARS)) {
            return;
        }
        event.setCanceled(true);
        open(player, QuestionLibrary.SMOKED_BEEHIVE);
    }

    /** 副手 / 主手任一持有收纳包（v0.4.6 需求写的是"副手卡牌收纳包"）。 */
    private static boolean holdsBinderOffhand(ServerPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(offhand.getItem());
        if (id != null && id.getNamespace().equals(SlayTheCraft.MODID) && id.getPath().equals("card_binder")) {
            return true;
        }
        // 兼容 Curios：装备在饰品槽时也算"带着收纳包"
        return BinderResolver.hasBinder(player);
    }

    /** 直接打开一个事件（与 Shift+右键村民共用 QuestionService 的校验与流程）。 */
    private static void open(ServerPlayer player, QuestionEvent event) {
        String reason = QuestionService.openDirect(player, event);
        if (reason != null) {
            player.displayClientMessage(Component.literal("[STC] " + reason), true);
        }
    }
}
