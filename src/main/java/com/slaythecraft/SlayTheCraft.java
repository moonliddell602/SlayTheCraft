package com.slaythecraft;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.slaythecraft.binder.CardBinderItem;
import com.slaythecraft.binder.StcMenus;
import com.slaythecraft.client.StcClientSetup;
import com.slaythecraft.data.StcAttachments;
import com.slaythecraft.gate.SpireGateBlock;
import com.slaythecraft.item.BattleStandardItem;
import com.slaythecraft.merchant.CardMerchantSpawnEggItem;
import com.slaythecraft.merchant.StcEntities;
import com.slaythecraft.binder.BinderResolver;
import com.slaythecraft.network.StcPayloads;
import com.slaythecraft.station.CardRecyclerBlock;
import com.slaythecraft.villager.StcVillagers;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * SlayTheCraft — a Slay the Spire style card battle layer for Minecraft.
 *
 * <p>Current milestone: combat mechanics only (energy, block, damage, intents, piles). Card art,
 * a card GUI and map/run progression are intentionally not part of this stage.</p>
 */
@Mod(SlayTheCraft.MODID)
public class SlayTheCraft {
    /** Must match {@code mod_id} in gradle.properties and the {@code @Mod} value. */
    public static final String MODID = "stc";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** 尖塔入口方块：一切尖塔内容都由它触发，不劫持原版玩法。 */
    public static final DeferredBlock<SpireGateBlock> SPIRE_GATE = BLOCKS.registerBlock("spire_gate",
            SpireGateBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> SPIRE_GATE_ITEM = ITEMS.registerSimpleBlockItem("spire_gate", SPIRE_GATE);

    /** 【卡牌回收站】方块：花金币删卡的另一条途径。 */
    public static final DeferredBlock<CardRecyclerBlock> CARD_RECYCLER = BLOCKS.registerBlock("card_recycler",
            CardRecyclerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CARD_RECYCLER_ITEM = ITEMS.registerSimpleBlockItem("card_recycler", CARD_RECYCLER);

    /** 卡牌商人生成蛋（调试 / 摆摊位用）。 */
    public static final DeferredItem<CardMerchantSpawnEggItem> CARD_MERCHANT_SPAWN_EGG = ITEMS.register(
            "card_merchant_spawn_egg", () -> new CardMerchantSpawnEggItem(new Item.Properties().stacksTo(16)));

    /** 右键可查看档案；放进 Curios 饰品槽后才有资格开战。 */
    public static final DeferredItem<CardBinderItem> CARD_BINDER = ITEMS.register("card_binder",
            () -> new CardBinderItem(new Item.Properties().stacksTo(1)));

    /** Right-click a hostile mob to start a card battle; right-click the air to end your turn. */
    public static final DeferredItem<BattleStandardItem> BATTLE_STANDARD = ITEMS.register("battle_standard",
            () -> new BattleStandardItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COMBAT_TAB = CREATIVE_MODE_TABS.register("combat",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.stc.combat"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> CARD_BINDER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CARD_BINDER.get());
                        output.accept(BATTLE_STANDARD.get());
                        output.accept(SPIRE_GATE_ITEM.get());
                        output.accept(CARD_RECYCLER_ITEM.get());
                        output.accept(CARD_MERCHANT_SPAWN_EGG.get());
                    })
                    .build());

    public SlayTheCraft(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        StcEntities.ENTITIES.register(modEventBus);
        // v0.4.5：【卡牌大师】村民职业 + 它的工作站点（= 尖塔入口方块）
        StcVillagers.POI_TYPES.register(modEventBus);
        StcVillagers.PROFESSIONS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        StcAttachments.ATTACHMENT_TYPES.register(modEventBus);
        StcMenus.MENUS.register(modEventBus);
        modEventBus.addListener(StcPayloads::register);
        modEventBus.addListener(StcEntities::registerAttributes);
        // v0.5.5.0（A6-1）卡牌数据包注册表 stc:card：注册表在 **mod 总线**上登记
        modEventBus.addListener(com.slaythecraft.cardreg.StcCardRegistry::registerDatapackRegistry);
        // v0.2.4 修复：把【卡牌收纳包】注册成 Curios 饰品。
        // 少了这一步，收纳包没有 CuriosCapability.ITEM，**根本放不进饰品槽**，
        // 于是所有"必须携带收纳包"的判定都会失败（界面打不开、开战被拒）。
        modEventBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
                BinderResolver.registerCuriosItem());
        // 客户端专属接线（快捷键 / 界面）：专用服务器不能触碰这些客户端类型
        if (FMLEnvironment.dist == Dist.CLIENT) {
            StcClientSetup.init(modEventBus);
        }
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("[STC] SlayTheCraft loaded — card battle mechanics prototype");
    }
}
