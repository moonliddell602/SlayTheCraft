package com.slaythecraft.villager;

import com.slaythecraft.chat.StcChat;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.merchant.CardShopMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 卡牌大师村民的运行时接线（v0.4.5）。
 *
 * <ul>
 *   <li>**右键**卡牌大师 → 打开本模组的**商店界面**（v0.4.6 需求："可以售卖卡牌，遗物。药水。
 *       用本模组的独立 ui 代替原版交易面板"），不再弹原版交易界面
 *       （这个职业没有交易表，弹出来也是空的）；</li>
 *   <li>定期把卡牌大师的**群系外观**统一成沙漠村民（贴图占位：需求允许"使用其他群系的村民贴图"），
 *       否则会出现"本地群系衣服 + 沙漠村民身体"两层贴图叠加的怪样子。</li>
 * </ul>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID)
public final class CardMasterEvents {

    /** 每隔多少 tick 检查一次"新成为卡牌大师的村民"（性能友好，200 = 10 秒）。 */
    private static final int SCAN_INTERVAL = 200;

    /** 外观统一成哪个群系（贴图占位 = 沙漠村民）。 */
    public static final VillagerType LOOK = VillagerType.DESERT;

    private static int tickCounter;

    private CardMasterEvents() {
    }

    /** 右键卡牌大师 = 打开本模组商店（主手 / 副手都接）。 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof Villager villager) || !StcVillagers.isCardMaster(villager)) {
            return;
        }
        event.setCanceled(true);
        CardShopMenu.open(player);
    }

    /**
     * 定期把"刚认领了尖塔入口的村民"调成卡牌大师外观，并给附近的玩家一句提示。
     *
     * <p>职业本身由原版逻辑赋值（{@code AssignProfessionFromJobSite}），这里只做**外观与提示**，
     * 所以就算这里一次也没跑，村民照样是卡牌大师、照样能打开入口界面。</p>
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < SCAN_INTERVAL) {
            return;
        }
        tickCounter = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            for (Villager villager : level.getEntitiesOfClass(Villager.class,
                    player.getBoundingBox().inflate(48.0D), StcVillagers::isCardMaster)) {
                if (villager.getVillagerData().getType() != LOOK) {
                    villager.setVillagerData(villager.getVillagerData().setType(LOOK));
                }
                if (!villager.getPersistentData().getBoolean(ANNOUNCED_TAG)) {
                    villager.getPersistentData().putBoolean(ANNOUNCED_TAG, true);
                    StcChat.tell(player, Component.literal("[STC] 一名村民成为了【卡牌大师】——"
                            + "右键他就能打开商店（卡牌 / 遗物 / 药水 / 删牌）。"));
                }
            }
        }
    }

    /** "已经提示过"的标记（写在实体持久数据里，重启存档不会重复提示）。 */
    private static final String ANNOUNCED_TAG = "stc_card_master_announced";
}
