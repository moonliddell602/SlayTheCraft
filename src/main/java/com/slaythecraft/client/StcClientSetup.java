package com.slaythecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.slaythecraft.Config;
import com.slaythecraft.SlayTheCraft;
import com.slaythecraft.binder.StcMenus;
import com.slaythecraft.merchant.StcEntities;
import com.slaythecraft.network.OpenBinderPayload;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端接线：快捷键 + 界面注册。
 *
 * <p>本类只在客户端加载（{@code value = Dist.CLIENT}），主类里也做了 dist 判断，
 * 专用服务器不会触碰这些客户端类型。</p>
 *
 * <p>mod 总线事件（按键注册、界面注册）在主类构造里显式注册；
 * 游戏总线事件（客户端 tick）用注解自动注册——这样就不必用到已标记待删除的
 * {@code @EventBusSubscriber(bus = …)}。</p>
 */
@EventBusSubscriber(modid = SlayTheCraft.MODID, value = Dist.CLIENT)
public final class StcClientSetup {

    /** 默认 B 键；名称与分类都是 lang key，玩家可在按键设置里改。 */
    public static final KeyMapping OPEN_BINDER = new KeyMapping(
            "key.stc.open_binder", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.stc");

    /** V3：战斗期间强制过的视角，战斗结束要还原成玩家原来的设置。 */
    private static CameraType previousCameraType;
    private static boolean forcedThirdPerson;

    private StcClientSetup() {
    }

    /** 由主类在客户端侧调用。 */
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(StcClientSetup::onRegisterKeys);
        modEventBus.addListener(StcClientSetup::onRegisterScreens);
        modEventBus.addListener(StcClientSetup::onRegisterGuiLayers);
        modEventBus.addListener(StcClientSetup::onRegisterRenderers);
    }

    /** 卡牌商人直接复用原版村民的渲染器（外观就是村民，不需要美术资源）。 */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(StcEntities.CARD_MERCHANT.get(), VillagerRenderer::new);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BINDER);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(StcMenus.CARD_BINDER.get(), CardBinderScreen::new);
        event.register(StcMenus.RECYCLER.get(), CardBinderScreen::new);
        event.register(StcMenus.SPIRE_GATE.get(), SpireGateScreen::new);
        event.register(StcMenus.REWARD.get(), RewardScreen::new);
        // v0.5.0.20：战斗结算页与三选一共用同一个界面类（按菜单模式画不同页面）
        event.register(StcMenus.BATTLE_RESULT.get(), RewardScreen::new);
        event.register(StcMenus.CARD_SHOP.get(), CardShopScreen::new);
        event.register(StcMenus.CAMPFIRE.get(), CampfireScreen::new);
    }

    /** 右下角"新卡牌奖励可用"提示（设计文档 §8.4）+ 物品栏上方的药水槽位（v0.0.2 V8）+ 充能球悬停说明。 */
    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(RewardHudLayer.ID, new RewardHudLayer());
        event.registerAboveAll(PotionHudLayer.ID, new PotionHudLayer());
        // v0.4.6.3：「战斗开始 / 第 X 回合」改用原版 /title 大标题（服务端下发），
        // 客户端自绘的战斗横幅层已删除（见 BattleTitleCheck）。
        // v0.4.6：充能球的悬停说明（鼠标停在球上 0.6 秒）
        event.registerAboveAll(OrbTooltipLayer.ID, new OrbTooltipLayer());
    }

    /**
     * 退出世界 / 断开连接时清掉客户端缓存的**开发者模式开关**（v0.5.1.2）。
     *
     * <p>不清的话，切到别的服务器（或回到主菜单）时还会记着上一个服务器的状态，
     * 图鉴里会平白多出两个按了会被服务端拒绝的按钮。</p>
     */
    @SubscribeEvent
    public static void onLoggingOut(
            net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDevState.clear();
        // v0.5.5.0（A6-1）：断开连接后把卡池还原成内置定义，别让上一个世界 / 服务器的
        // 数据包卡泄漏到主菜单或下一个服务器
        appliedCardRegistry = null;
        registryWaitTicks = 0;
        com.slaythecraft.combat.core.CardLibrary.resetExternal();
    }

    /** v0.5.5.0（A6-1）：已经灌过 stc:card 的那条连接（同一条连接只灌一次）。 */
    private static Object appliedCardRegistry;
    private static int registryWaitTicks;

    /**
     * 把**同步下来的**卡牌注册表灌进客户端卡池（图鉴 / 卡面 / 商店都读同一个覆盖层）。
     *
     * <p>客户端拿不到服务端的数据包文件，只有同步过来的注册表条目；同步在登录阶段完成，
     * 这里等它出现再灌一次（最多等 200 tick，之后无论有没有都按"没有数据包卡"处理，
     * 免得一直空转）。</p>
     */
    private static void syncCardRegistry(Minecraft minecraft) {
        net.minecraft.client.multiplayer.ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            appliedCardRegistry = null;
            registryWaitTicks = 0;
            return;
        }
        if (appliedCardRegistry == connection) {
            return;
        }
        boolean ready = connection.registryAccess() != null
                && connection.registryAccess()
                        .registry(com.slaythecraft.cardreg.StcCardRegistry.CARDS).isPresent();
        if (!ready && registryWaitTicks++ < 200) {
            return;
        }
        appliedCardRegistry = connection;
        com.slaythecraft.cardreg.CardRegistryLoader.Result result =
                com.slaythecraft.cardreg.CardRegistryLoader.apply(connection.registryAccess());
        if (!result.clean()) {
            for (String error : result.errors()) {
                SlayTheCraft.LOGGER.warn("[STC] 客户端 stc:card：{}", error);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {        Minecraft minecraft = Minecraft.getInstance();
        // 服务端会再校验一次"是否携带收纳包"，客户端按键只是请求
        while (OPEN_BINDER.consumeClick()) {
            PacketDistributor.sendToServer(OpenBinderPayload.INSTANCE);
        }
        if (minecraft.player == null) {
            return;
        }
        // v0.5.5.0（A6-1）：把同步下来的 stc:card 灌进客户端卡池（同一条连接只做一次）
        syncCardRegistry(minecraft);
        // V1：自己的回合自动弹出手牌界面；不是自己回合/战斗结束时自动收起
        // v0.4.2：战斗里的"查看牌组/牌堆"子页面不要被顶掉
        // v0.5.1.7 A3：选牌页开着的时候**不要**把出牌界面顶回来（本来就会每 tick 重设一次）
        boolean pickPending = ClientCardPickState.offer() != null;
        if (ClientBattleState.shouldShowScreen() && !pickPending) {
            if (!(minecraft.screen instanceof BattleScreen) && !(minecraft.screen instanceof BattleInfoScreen)) {
                minecraft.setScreen(new BattleScreen());
            }
        } else if (!pickPending && minecraft.screen instanceof BattleScreen) {
            minecraft.setScreen(null);
        }
        // V3：战斗期间自动第三人称（结束后恢复玩家原来的视角设置）
        updateCamera(minecraft);
        // v0.4.5：BOSS 战音乐（每层可用 StcApi.registerBossMusic 定制；没配就不动原版音乐）
        BossMusic.tick(minecraft);
        // V11：服务端下发了问号事件时按顺序弹界面：结果页 → 选牌页 → 选项页
        if (ClientQuestionState.resultText() != null) {
            if (!(minecraft.screen instanceof QuestionResultScreen)) {
                minecraft.setScreen(new QuestionResultScreen());
            }
        } else if (ClientQuestionState.pendingPick() != null) {
            if (!(minecraft.screen instanceof QuestionCardPickScreen)) {
                minecraft.setScreen(new QuestionCardPickScreen(ClientQuestionState.pendingPick()));
            }
        } else if (ClientQuestionState.offer() != null && !(minecraft.screen instanceof QuestionScreen)) {
            minecraft.setScreen(new QuestionScreen());
        }
        // v0.5.1.7 A3：卡牌要求选牌时弹出选牌页（废料回收 / 应急储备 / 弃置边陵 / 回收）
        if (ClientCardPickState.offer() != null && !(minecraft.screen instanceof BattleCardPickScreen)) {
            minecraft.setScreen(new BattleCardPickScreen(ClientCardPickState.offer()));
        }
    }

    /** V3：进入战斗切到第三人称背面视角，离开战斗恢复原设置。 */
    private static void updateCamera(Minecraft minecraft) {
        boolean inBattle = ClientBattleState.view().active();
        if (Config.forceThirdPerson()) {
            if (inBattle && !forcedThirdPerson) {
                previousCameraType = minecraft.options.getCameraType();
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                forcedThirdPerson = true;
            } else if (!inBattle && forcedThirdPerson) {
                minecraft.options.setCameraType(previousCameraType == null ? CameraType.FIRST_PERSON : previousCameraType);
                forcedThirdPerson = false;
                previousCameraType = null;
            }
        } else if (forcedThirdPerson) {
            minecraft.options.setCameraType(previousCameraType == null ? CameraType.FIRST_PERSON : previousCameraType);
            forcedThirdPerson = false;
            previousCameraType = null;
        }
        // 测试反馈修正：战斗期间把视角压到俯瞰 45 度 —— 平视时玩家模型会挡住怪物。
        // 0 = 不干预（玩家自己转视角）；只锁俯仰角，左右仍可自由转。
        if (inBattle && minecraft.player != null) {
            int pitch = Config.battleCameraPitch();
            if (pitch > 0 && Math.abs(minecraft.player.getXRot() - pitch) > 0.5F) {
                minecraft.player.setXRot(pitch);
            }
        }
    }
}
