package com.slaythecraft.achievement;

import com.slaythecraft.SlayTheCraft;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * 本模组进度（成就）的授予与登记。
 *
 * <p>基于**原版成就模块**：全部条目都是 {@code data/stc/advancement/stc/*.json}，
 * 挂在根成就【方块尖塔】之下（根成就的背景是**白色羊毛纹理**），
 * 在游戏里和原版成就一起显示、一起弹 toast。</p>
 *
 * <p><b>为什么根叫【方块尖塔】而不是【尖塔在等着你】</b>（v0.5.0.11）：
 * 原版 {@code AdvancementTab} 的标签名直接取**根节点 display 的 title**
 * （{@code AdvancementTab.title = display.getTitle()}），所以屏幕上那行字是"分支名"。
 * 需求里那行字应该是【方块尖塔】，因此根承担分支名，而**【尖塔在等着你】下沉为它的第一条子成就**。
 * 这样还顺带修好了 toast：根成就的完成在原版里有特殊分支
 * （{@code PlayerAdvancements} 对 {@code isRoot()} 的自动授予与可见性处理都不同），
 * 挂在根上的 {@code show_toast} 不会可靠地弹出来，挂在子成就上才会。</p>
 *
 * <p>成就树（{@code →} 读作"是它的前置"）：</p>
 * <pre>
 * 方块尖塔（根，标签页名 + 白色羊毛背景；不弹 toast、不进聊天栏）
 *  └─ 尖塔在等着你（进游戏即有；**这条会弹 toast**）
 *      ├─ 我进塔！（合成尖塔入口）
 *      ├─ ！？收纳袋？！（获得卡牌收纳包）─ 再玩小卡组找人弄你（第一次删牌）
 *      ├─ 看起来不太聪明（第一次击败常规敌怪）─ 犹豫就会败北（第一次战败）
 *      └─ 人形宝可梦出击（获得战斗军旗）
 *          ├─ 集中冰才对（有集中时生成冰球）
 *          └─ 回响形态了 回响形态（隐藏：连续打出 4 张同名手牌）
 * </pre>
 *
 * <p>授予路径有两条：<b>原版触发器</b>（{@code inventory_changed} / {@code tick} 自动完成那四条）与
 * <b>代码授予</b>（帧里没有对应触发器的那些走 {@code minecraft:impossible} + 本类的方法）。</p>
 */
public final class AchievementService {

    /** 根成就【方块尖塔】：成就树的标签页（名字就是屏幕上那行字），本身不留节点。 */
    public static final ResourceLocation ROOT = id("root");
    /** 尖塔在等着你 —— 第一次进入游戏（原版 {@code minecraft:tick} 触发，会弹 toast）。 */
    public static final ResourceLocation ENTERED_GAME = id("entered_game");
    /** 我进塔！—— 合成尖塔入口（原版 inventory_changed）。 */
    public static final ResourceLocation SPIRE_GATE = id("spire_gate");
    /** ！？收纳袋？！—— 第一次获得卡牌收纳包（原版 inventory_changed）。 */
    public static final ResourceLocation CARD_BINDER = id("card_binder");
    /** 再玩小卡组找人弄你 —— 第一次删牌（代码授予）。 */
    public static final ResourceLocation CARD_DELETE = id("card_delete");
    /** 人形宝可梦出击 —— 第一次获得战斗军旗（原版 inventory_changed）。 */
    public static final ResourceLocation BATTLE_STANDARD = id("battle_standard");
    /** 看起来不太聪明 —— 第一次击败常规敌怪（代码授予）。 */
    public static final ResourceLocation FIRST_BLOOD = id("first_blood");
    /** 犹豫就会败北 —— 第一次战败（代码授予）。 */
    public static final ResourceLocation FIRST_DEFEAT = id("first_defeat");
    /** 集中冰才对 —— 拥有集中时生成冰球（代码授予）。 */
    public static final ResourceLocation FOCUS_FROST = id("focus_frost");
    /** 回响形态了 回响形态 —— 连续打出 4 张同名手牌（**隐藏成就**，代码授予）。 */
    public static final ResourceLocation ECHO_FORM = id("echo_form");

    /**
     * 一条进度的登记项（自检用它逐条校验 JSON 结构与成就树）。
     *
     * @param id        进度 id
     * @param file      相对 {@code data/stc/advancement} 的文件路径（不带 .json）
     * @param parent    前置进度的 id；根成就为 {@code null}
     * @param trigger   criteria 里要求的触发器
     * @param itemId    物品类进度必须点名的物品 id（其余为 {@code null}）
     * @param hidden    是不是隐藏成就
     */
    public record Entry(ResourceLocation id, String file, String parent, String trigger, String itemId,
                        boolean hidden) {
    }

    private static final String INVENTORY = "minecraft:inventory_changed";
    private static final String IMPOSSIBLE = "minecraft:impossible";
    private static final String TICK = "minecraft:tick";

    /** 全部进度（顺序固定，供自检逐条校验）。 */
    public static final Entry[] ENTRIES = {
        new Entry(ROOT, "stc/root", null, TICK, null, false),
        new Entry(ENTERED_GAME, "stc/entered_game", "stc:root", TICK, null, false),
        new Entry(SPIRE_GATE, "stc/spire_gate", "stc:entered_game", INVENTORY, "stc:spire_gate", false),
        new Entry(CARD_BINDER, "stc/card_binder", "stc:entered_game", INVENTORY, "stc:card_binder", false),
        new Entry(CARD_DELETE, "stc/card_delete", "stc:card_binder", IMPOSSIBLE, null, false),
        new Entry(BATTLE_STANDARD, "stc/battle_standard", "stc:entered_game", INVENTORY,
                "stc:battle_standard", false),
        new Entry(FIRST_BLOOD, "stc/first_blood", "stc:entered_game", IMPOSSIBLE, null, false),
        new Entry(FIRST_DEFEAT, "stc/first_defeat", "stc:first_blood", IMPOSSIBLE, null, false),
        new Entry(FOCUS_FROST, "stc/focus_frost", "stc:battle_standard", IMPOSSIBLE, null, false),
        new Entry(ECHO_FORM, "stc/echo_form", "stc:battle_standard", IMPOSSIBLE, null, true)
    };

    /** 成就树的**背景贴图**（需求："成就的背景贴图应为：白色羊毛纹理。"）。 */
    public static final String BACKGROUND = "minecraft:textures/block/white_wool.png";

    /**
     * 根成就（标签页）的标题翻译键。
     *
     * <p>它同时是**屏幕上那行字**（原版 {@code AdvancementTab.title = display.getTitle()}），
     * 需求要求那行字是【方块尖塔】，所以这里用常量固定住，
     * 由自检查中英语言文件确实把它翻成「方块尖塔」/「Block Spire」。</p>
     */
    public static final String ROOT_TITLE_KEY = "advancements.stc.root.title";

    /**
     * 根成就**不弹 toast、不进聊天栏**。
     *
     * <p>根是成就树的容器（原版所有自带根成就也都是 {@code show_toast:false} /
     * {@code announce_to_chat:false}）；真正"第一次进游戏"的提示挂在子成就
     * {@link #ENTERED_GAME} 上，那条才是玩家看到的 toast。</p>
     */
    public static final boolean ROOT_SILENT = true;

    private AchievementService() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SlayTheCraft.MODID, path);
    }

    /**
     * 给某名玩家授予一个进度。
     *
     * @return true = 这次真的授予了（之前没完成过）；false = 已经完成过 / 找不到这个进度
     */
    public static boolean grant(ServerPlayer player, ResourceLocation advancement) {
        if (player == null || advancement == null || player.getServer() == null) {
            return false;
        }
        ServerAdvancementManager manager = player.getServer().getAdvancements();
        AdvancementHolder holder = manager.get(advancement);
        if (holder == null) {
            // 进度文件没被加载（多半是写错了路径 / JSON 格式坏了）——留一条日志便于排查
            SlayTheCraft.LOGGER.warn("[STC] 找不到进度 {}，无法授予", advancement);
            return false;
        }
        // 必须带上 criterion 名：JSON 里每条 criteria 的名字（见 data/stc/advancement/stc/*.json）
        String criterion = holder.value().criteria().keySet().stream().findFirst().orElse("");
        if (criterion.isEmpty()) {
            return false;
        }
        return player.getAdvancements().award(holder, criterion);
    }

    /** 第一次进入游戏（【尖塔在等着你】）。JSON 里有 {@code minecraft:tick} 触发，这里再显式授予一次。 */
    public static boolean grantEnteredGame(ServerPlayer player) {
        return grant(player, ENTERED_GAME);
    }

    /** 第一次击败常规敌怪。 */
    public static boolean grantFirstBlood(ServerPlayer player) {
        return grant(player, FIRST_BLOOD);
    }

    /** 第一次战败。 */
    public static boolean grantFirstDefeat(ServerPlayer player) {
        return grant(player, FIRST_DEFEAT);
    }

    /** 第一次删牌。 */
    public static boolean grantCardDelete(ServerPlayer player) {
        return grant(player, CARD_DELETE);
    }

    /** 拥有集中时生成冰球。 */
    public static boolean grantFocusFrost(ServerPlayer player) {
        return grant(player, FOCUS_FROST);
    }

    /** 连续打出 4 张同名手牌（隐藏成就）。 */
    public static boolean grantEchoForm(ServerPlayer player) {
        return grant(player, ECHO_FORM);
    }
}
