package com.slaythecraft.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 状态与充能球的**图标 + 说明**（v0.4.6 美术需求）。
 *
 * <p>需求原文：</p>
 * <ul>
 *   <li>"所有药水效果的 logo 贴图都会以文字的同等大小显示对应状态左侧"；</li>
 *   <li>v0.4.6："斩杀线：使用试炼之兆 logo 贴图；神龟：使用抗性提升 logo 贴图；涂毒：使用中毒贴图"；</li>
 *   <li>v0.4.6.1：**中毒**用中毒药水效果 logo、**凋零**用凋零药水效果 logo、**灼烧**用烈焰弹贴图。</li>
 *   <li>v0.4.6.7：贴图按**真实像素尺寸**等比缩放居中（{@link IconFit}）——需求原文
 *       "小图标如果出现大小错误的问题，自动将图像调整到合适大小"。</li>
 * </ul>
 *
 * <p>做法：全部复用**原版状态效果贴图**（{@code minecraft:textures/mob_effect/<效果>.png}），
 * 尺寸按文字高度（8×8），画在状态文字左侧；不认识的标签就不画图标（只显示文字），
 * 这样新增状态永远不会画错图标。</p>
 */
public final class StatusIcons {

    /**
     * 状态标签 → 图标贴图。
     *
     * <p>需求（v0.4.6.1）点名了三处：中毒用**中毒药水效果** logo、凋零用**凋零药水效果** logo、
     * 灼烧用**烈焰弹**贴图 —— 所以这里存的是完整贴图位置（既可以是 {@code mob_effect/*}，
     * 也可以是物品贴图 {@code item/*}）。</p>
     */
    private static final Map<String, ResourceLocation> ICONS = new LinkedHashMap<>();
    /** 状态标签 → 功能说明（悬停 0.6 秒后显示）。 */
    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    /** 图标边长（= 原版字体的字高）。 */
    public static final int SIZE = 8;
    /** 图标与文字之间的间距。 */
    public static final int GAP = 2;

    static {
        // ---- v0.4.6.1 需求点名的三处
        effect("中毒", "poison", "每拥有 x 点中毒，出牌环节结束时受到 X 点伤害；每个新回合减少 1 层。");
        effect("凋零", "wither", "可以累加。累计大于当前生命值时，回合结束后立刻死亡。");
        item("灼烧", "minecraft:fire_charge", "可以累加。每累计一点，在下回合开始时造成对应层数的伤害。");

        effect("神龟", "resistance", "结束出牌时获得等同于层数的格挡。");
        effect("力量", "strength", "每层使你的攻击伤害 +1。与【虚弱】互相抵消。");
        // v0.5.1.4 虚弱重做：每层 -1 攻击伤害、可以累加、**不随回合消失**，且与力量互相抵消
        effect("虚弱", "weakness", "可以累加。每层使攻击伤害 -1（不随回合消失），并且与力量互相抵消："
                + "给力量 3 的目标施加虚弱 2，结果他剩力量 1。");
        // v0.5.1.4 新增：旧「虚弱」的那套机制改名成【挖掘疲劳】（需求点名用挖掘疲劳的效果小图标）
        effect("挖掘疲劳", "mining_fatigue", "可以累加。每累计 X 点，则在 X 回合内攻击伤害 -25%。"
                + "（这就是旧版「虚弱」的机制，现在叫挖掘疲劳；生物「给予虚弱」的意图也全部改成了它。）");
        effect("易伤", "instant_damage", "层数 = 剩余回合数；期间你受到的伤害 +25%。");
        effect("活力", "health_boost", "下一张攻击牌额外造成等同于层数的伤害，打完清零。");
        effect("迅捷", "speed", "每层使你获得的格挡 +1。与【缓慢】互相抵消。");
        effect("缓慢", "slowness", "每层使你获得的格挡 -1。与【敏捷】互相抵消。");
        effect("再生", "regeneration", "自己回合开始时恢复等同于层数的生命。");
        effect("缓降", "slow_falling", "免疫下一次受到的伤害，触发后减少 1 层。");
        effect("充能球", "conduit_power", "故障机器人的充能球：回合结束触发被动，激发时产生一次性效果。");
        // v0.5.0.8：需求"蓄能作为状态也应该像其他状态那样正常显示"。
        // 原版没有"蓄能"效果，借用风爆（wind_charged）贴图，语义上也是"攒着劲儿要炸"。
        effect("蓄能", "wind_charged",
                "旋风人攒下的自爆能量：被击晕后下一次敌人回合一次性打出来，之后它自己倒下。");
        // v0.5.2.5 新状态【荆棘】：需求点名用"盛开的杜鹃花丛"图标（方块，贴图在 textures/block/）
        // v0.5.5.1 美术调整（用户）："荆棘（贴图修改）：使用 缠怨藤 物品贴图"
        item("荆棘", "minecraft:twisting_vines",
                "可以累加。当你被攻击时，对攻击者反弹等同于层数的伤害（反弹伤害不吃力量/虚弱，"
                        + "但会被对方的格挡吸收）。");
        // v0.5.0.4：需求"集中（使用时钟图标）"——原版没有"集中"状态效果，所以用时钟**物品**贴图。
        // v0.5.5.1 美术调整（用户）："集中：使用 末地水晶 物品贴图"
        item("集中", "minecraft:end_crystal",
                "所有充能球的效果增加 X 点（X 为集中的层数），等离子充能球不受影响："
                        + "闪电球每回合伤害 +X、冰霜球每回合格挡 +X、黑暗球每回合成长 +X。");
    }

    /** 状态标签 → 一句话"它是谁"（图鉴详情页的**描述**；机制文本见 {@link #mechanics}）。 */
    private static final Map<String, String> FLAVOR = new LinkedHashMap<>();

    /**
     * 图鉴用的一条状态（v0.4.6.7 状态图鉴）。
     *
     * @param label     状态名（{@code 灼烧}）
     * @param texture   图标贴图（可能为 {@code null}，此时只显示文字）
     * @param flavor    描述（一句话）
     * @param mechanics 机制（怎么算、怎么结算）
     */
    public record Entry(String label, ResourceLocation texture, String flavor, String mechanics) {
    }

    static {
        flavor("中毒", "渗进血管里的东西，越拖越难受。");
        flavor("凋零", "身体正在被时间啃掉，数字比疼痛更危险。");
        flavor("灼烧", "火焰贴在身上，每一点都会在下回合咬你一口。");
        flavor("神龟", "缩进壳里：挨打的时候顺便把壳叠厚。");
        flavor("力量", "手更沉了，每一击都多一分。");
        flavor("虚弱", "手在抖，每一击都少一分 —— 攒起来的力量也能把它顶回去。");
        flavor("挖掘疲劳", "眼皮沉得像灌了铅，刀锋软了四分之一。");
        flavor("易伤", "防线露出了破绽，接下来每一击都更痛。");
        flavor("活力", "憋着一股劲，下一刀格外狠。");
        flavor("迅捷", "步子更快，格挡也堆得更快。");
        flavor("缓慢", "手脚被拖住，格挡堆不起来。");
        flavor("再生", "伤口自己合上，每回合回一点血。");
        flavor("缓降", "落到地上之前，伤害先被卸掉一次。");
        flavor("充能球", "故障机器人的球：缩在身后，等着被激发。");
        flavor("集中", "盯着钟表数拍子：球的效果跟着层数一起变强。");
        flavor("蓄能", "风在袖子里越攒越紧，只等一声响。");
        flavor("荆棘", "你身上每一根刺都记着挨过的每一下。");
    }

    private StatusIcons() {
    }

    /** 登记一条状态的"描述"（机制文本由 {@link #effect} / {@link #item} 登记）。 */
    private static void flavor(String label, String flavor) {
        FLAVOR.put(label, flavor);
    }

    /** 全部状态（按登记顺序；状态图鉴用）。 */
    public static List<Entry> all() {
        List<Entry> list = new ArrayList<>(ICONS.size());
        for (Map.Entry<String, ResourceLocation> entry : ICONS.entrySet()) {
            String label = entry.getKey();
            list.add(new Entry(label, entry.getValue(), FLAVOR.getOrDefault(label, ""),
                    DESCRIPTIONS.getOrDefault(label, "")));
        }
        return List.copyOf(list);
    }

    /** 一条状态的机制文本（查不到返回空串）。 */
    public static String mechanics(String label) {
        return DESCRIPTIONS.getOrDefault(labelOf(label), "");
    }

    /** 用原版**状态效果**的 logo（{@code textures/mob_effect/<id>.png}）。 */
    private static void effect(String label, String effectId, String description) {
        ICONS.put(label, ResourceLocation.withDefaultNamespace("textures/mob_effect/" + effectId + ".png"));
        DESCRIPTIONS.put(label, description);
    }

    /** 用某个原版**物品**的贴图（{@code minecraft:textures/item/<path>.png}，例如烈焰弹）。 */
    private static void item(String label, String itemId, String description) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        String path = id == null ? "" : id.getPath();
        ICONS.put(label, ResourceLocation.withDefaultNamespace("textures/item/" + path + ".png"));
        DESCRIPTIONS.put(label, description);
    }

    /**
     * 用某个原版**方块**的贴图（{@code minecraft:textures/block/<path>.png}）—— v0.5.2.5 加。
     *
     * <p>【荆棘】的图标是"盛开的杜鹃花丛"，它是**方块**：贴图在 {@code textures/block/} 下，
     * 用 {@link #item} 那条 {@code textures/item/} 路径会指到一个不存在的文件。</p>
     */
    private static void block(String label, String blockId, String description) {
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        String path = id == null ? "" : id.getPath();
        ICONS.put(label, ResourceLocation.withDefaultNamespace("textures/block/" + path + ".png"));
        DESCRIPTIONS.put(label, description);
    }

    /** 这条状态文本（{@code 虚弱 5}）对应的状态名（{@code 虚弱}）。 */
    public static String labelOf(String entry) {
        if (entry == null || entry.isEmpty()) {
            return "";
        }
        int space = entry.indexOf(' ');
        String label = space < 0 ? entry : entry.substring(0, space);
        // "力量+3" / "迅捷+2" 这种带加号的写法也要认出来（v0.5.5.1：从**最后一个** + 处切，
        // 老写法只在"名字以 + 结尾"时才切，所以 "力量+3" 认不出图标）
        int plus = label.lastIndexOf('+');
        if (plus > 0) {
            label = label.substring(0, plus);
        }
        return label;
    }

    /**
     * 状态的**数值部分**（v0.5.5.1）：{@code 虚弱 5} → {@code 5}、{@code 力量+3} → {@code +3}；
     * 没有数字的状态（例如【充能球】）返回空串。
     */
    public static String valueOf(String entry) {
        if (entry == null || entry.isEmpty()) {
            return "";
        }
        String label = labelOf(entry);
        String rest = entry.length() > label.length() ? entry.substring(label.length()).trim() : "";
        // "力量+3" 这种写法切掉名字后正好剩 "+3"
        return rest;
    }

    /** 战斗内**紧凑**宽度（v0.5.5.1：常态只画图标 + 数字，不画名字）。 */
    public static int compactWidth(String entry, int valueWidth) {
        return known(entry) ? SIZE + GAP + valueWidth : valueWidth;
    }

    /** 这个状态标签有没有对应的图标。 */
    public static boolean known(String entry) {
        return ICONS.containsKey(labelOf(entry));
    }

    /** 状态图标贴图；没有对应图标时返回 {@code null}。 */
    public static ResourceLocation texture(String entry) {
        return ICONS.get(labelOf(entry));
    }

    /** 状态的说明文字（悬停提示用）；没有登记时返回空列表。 */
    public static List<String> describe(String entry) {
        String label = labelOf(entry);
        List<String> lines = new ArrayList<>();
        lines.add(entry);
        String description = DESCRIPTIONS.get(label);
        if (description != null) {
            lines.addAll(BinderStyle.wrap(Minecraft.getInstance().font, description, 160));
        }
        return lines;
    }

    /** 图标 + 文字的总宽度。 */
    public static int width(String entry, int textWidth) {
        return known(entry) ? SIZE + GAP + textWidth : textWidth;
    }

    /**
     * 画一条状态：图标（如果有）在左，文字在右。
     *
     * @param x 这一条的左边缘
     * @return 画完之后的下一个 x（调用方用来排版下一条）
     */
    public static int draw(GuiGraphics graphics, String entry, int x, int y, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        int textX = x;
        ResourceLocation texture = texture(entry);
        if (texture != null) {
            // 8×8：正好和字一样高（"以文字的同等大小显示"）
            // v0.4.6.7：需求"小图标如果出现大小错误的问题，自动将图像调整到合适大小" ——
            // 交给 IconFit 按贴图真实像素尺寸等比缩放居中（8×8 / 16×16 的贴图结果与以前完全一致，
            // 作者换一张非正方形或更大尺寸的贴图时不再被压扁 / 只画左上角）
            IconFit.draw(graphics, texture, x, y, SIZE, SIZE);
            textX = x + SIZE + GAP;
        }
        TextStyle.draw(graphics, minecraft.font, entry, textX, y, color);
        return textX + minecraft.font.width(entry);
    }

    /**
     * 画一整行状态（用 {@code  · } 分隔的多条），每条都带自己的图标。
     *
     * @return 每一条的命中矩形（{@code [x, y, w, h]} 四元组，按顺序），供 0.6 秒悬停判定
     */
    public static List<int[]> drawLine(GuiGraphics graphics, List<String> entries, int rightEdge, int y,
                                       int color) {
        Minecraft minecraft = Minecraft.getInstance();
        int[] widths = new int[entries.size()];
        int total = 0;
        for (int i = 0; i < entries.size(); i++) {
            widths[i] = width(entries.get(i), minecraft.font.width(entries.get(i)));
            total += widths[i] + (i > 0 ? minecraft.font.width(" · ") : 0);
        }
        int x = Math.max(4, rightEdge - total);
        List<int[]> rects = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                TextStyle.draw(graphics, minecraft.font, " · ", x, y, color);
                x += minecraft.font.width(" · ");
            }
            int start = x;
            x = draw(graphics, entries.get(i), x, y, color);
            rects.add(new int[] {start, y, Math.max(1, x - start), 10});
        }
        return rects;
    }

    /** 把 {@code 虚弱 5 · 易伤 2} 拆成条目列表。 */
    public static List<String> split(String statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        List<String> entries = new ArrayList<>();
        for (String part : statuses.split(" · ")) {
            if (!part.isBlank()) {
                entries.add(part.trim());
            }
        }
        return List.copyOf(entries);
    }

    // ------------------------------------------------------------------ v0.5.5.1 战斗内的紧凑显示

    /**
     * 画一条**紧凑**状态：**只画图标 + 数字**，不写状态名（v0.5.5.1 美术调整）。
     *
     * <p>需求原文："战斗内的状态常态不再显示名称，只显示图标和数字。悬停显示不变。"
     * 所以悬停提示仍用**完整条目**（{@code 虚弱 5} + 说明），只有常态的排版变紧凑。</p>
     *
     * @return 画完之后的下一个 x
     */
    public static int drawCompact(GuiGraphics graphics, String entry, int x, int y, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        String value = valueOf(entry);
        int textX = x;
        ResourceLocation texture = texture(entry);
        if (texture != null) {
            IconFit.draw(graphics, texture, x, y, SIZE, SIZE);
            textX = x + SIZE + GAP;
        }
        if (value.isEmpty()) {
            return textX;
        }
        TextStyle.draw(graphics, minecraft.font, value, textX, y, color);
        return textX + minecraft.font.width(value);
    }

    /**
     * 画一整行**紧凑**状态（图标 + 数字，用 {@code  · } 分隔）。
     *
     * @param alphas 每条状态的透明度（{@code null} = 全不透明）；长度不足时按 1 处理 ——
     *               v0.5.5.1 的"获得状态时渐显"就是靠它
     * @return 每一条的命中矩形（{@code [x, y, w, h]}），顺序与 {@code entries} 一致
     */
    public static List<int[]> drawCompactLine(GuiGraphics graphics, List<String> entries, int rightEdge, int y,
                                              int color, float[] alphas) {
        Minecraft minecraft = Minecraft.getInstance();
        int[] widths = new int[entries.size()];
        int total = 0;
        for (int i = 0; i < entries.size(); i++) {
            String value = valueOf(entries.get(i));
            widths[i] = compactWidth(entries.get(i), minecraft.font.width(value));
            total += widths[i] + (i > 0 ? minecraft.font.width(" · ") : 0);
        }
        int x = Math.max(4, rightEdge - total);
        List<int[]> rects = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            float alpha = alphas == null || i >= alphas.length ? 1.0F : alphas[i];
            if (i > 0) {
                final int sepX = x;
                FadeStyle.withAlpha(alpha, () -> TextStyle.draw(graphics, minecraft.font, " · ", sepX, y, color));
                x += minecraft.font.width(" · ");
            }
            int start = x;
            final int at = x;
            final String entry = entries.get(i);
            FadeStyle.withAlpha(alpha, () -> drawCompact(graphics, entry, at, y, color));
            x += widths[i];
            rects.add(new int[] {start, y, Math.max(1, x - start), 10});
        }
        return rects;
    }
}
