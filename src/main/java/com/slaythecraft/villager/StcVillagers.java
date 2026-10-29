package com.slaythecraft.villager;

import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.slaythecraft.SlayTheCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * **卡牌大师村民**（v0.4.5）。
 *
 * <p>需求原文："卡牌大师村民：通过无业村民（未就业的原版村民）把【尖塔入口】当作工作方块绑定而成"。</p>
 *
 * <p>所以这里注册的是**真正的原版村民职业系统**，而不是自造实体：</p>
 * <ol>
 *   <li>{@link #CARD_MASTER_POI}：一个兴趣点（POI），匹配【尖塔入口】方块的全部状态；</li>
 *   <li>{@link #CARD_MASTER}：一个 {@link VillagerProfession}，它的 {@code heldJobSite} 就是上面这个兴趣点；</li>
 *   <li>{@code data/minecraft/tags/point_of_interest_type/acquirable_job_site.json} 把该兴趣点加入
 *       "可就业工作站点"标签 —— 无业村民才会主动去找它。</li>
 * </ol>
 *
 * <p>于是：一个**无业村民**走到【尖塔入口】旁边 → 认领工作站点 → 原版逻辑
 * （{@code AssignProfessionFromJobSite}）在职业表里找到"工作站点匹配"的第一个职业 →
 * 变成 {@code stc:card_master} 卡牌大师。整个过程没有自定义实体、没有 Mixin。</p>
 *
 * <p>贴图：需求允许"直接使用其他群系的村民贴图"，所以
 * {@code assets/stc/textures/entity/villager/profession/card_master.png} 放的就是沙漠村民的贴图
 * （64×64 整套身体），卡牌大师的外观因此是**另一个群系的村民**；整合包想换美术只要替换这一个文件
 * （见 README 的"美术占位"小节）。</p>
 */
public final class StcVillagers {

    /** 兴趣点注册表。 */
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, SlayTheCraft.MODID);

    /** 村民职业注册表。 */
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, SlayTheCraft.MODID);

    /** 职业 id：{@code stc:card_master}。 */
    public static final ResourceLocation CARD_MASTER_ID =
            ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, "card_master");

    /** 职业的注册键（自检 / 指令用）。 */
    public static final ResourceKey<VillagerProfession> CARD_MASTER_KEY =
            ResourceKey.create(Registries.VILLAGER_PROFESSION, CARD_MASTER_ID);

    /**
     * 【尖塔入口】作为工作站点。
     *
     * <p>匹配方块的全部状态（目前只有默认状态），村民的寻路/认领逻辑就靠它。</p>
     */
    public static final DeferredHolder<PoiType, PoiType> CARD_MASTER_POI = POI_TYPES.register("card_master",
            () -> new PoiType(
                    ImmutableSet.copyOf(SlayTheCraft.SPIRE_GATE.get().getStateDefinition().getPossibleStates()),
                    1,   // 一个村民占一个工作站点
                    1)); // 一个工作站点只服务一个村民

    /** 卡牌大师职业（工作站点 = 尖塔入口；工作音效沿用原版制图师，免得再做一个音频资源）。 */
    public static final DeferredHolder<VillagerProfession, VillagerProfession> CARD_MASTER =
            PROFESSIONS.register("card_master", () -> new VillagerProfession(
                    "card_master",
                    holder -> holder.is(CARD_MASTER_POI.getKey()),
                    holder -> holder.is(CARD_MASTER_POI.getKey()),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

    private StcVillagers() {
    }

    /** 供自检/指令使用：这个村民是不是卡牌大师。 */
    public static boolean isCardMaster(net.minecraft.world.entity.npc.Villager villager) {
        return villager.getVillagerData().getProfession() == CARD_MASTER.get();
    }
}
