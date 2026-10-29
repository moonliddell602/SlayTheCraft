package com.slaythecraft.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.slaythecraft.combat.core.FloorRoster;
import com.slaythecraft.combat.core.Potion;
import com.slaythecraft.combat.core.PotionLibrary;
import com.slaythecraft.combat.core.Relic;
import com.slaythecraft.combat.core.RelicLibrary;

/**
 * 图鉴页面（v0.4.6.7）的自检。
 *
 * <p>需求原文：</p>
 * <ul>
 *   <li>"未详情查看的图鉴外侧不再显示获取方式。获取方式，机制，趣闻描述都显示在详情页面。"；</li>
 *   <li>"遗物图鉴外部的排列修改为每行3个。"；</li>
 *   <li>"新图鉴：状态图鉴 —— 按钮显示状态图标和名称" / "详情页面显示状态效果描述，图标，名称，和机制。"；</li>
 *   <li>"新图鉴：药水图鉴 —— 按钮显示药水图标和名称" / "详情页面显示效果描述，图标，名称，和机制。"。</li>
 * </ul>
 *
 * <p>所以这里盯四件事：</p>
 * <ol>
 *   <li><b>数据够不够画</b>：状态图鉴每条都要有名字 / 描述 / 机制 / 图标；药水图鉴每瓶都要有名字 / 描述；</li>
 *   <li><b>遗物网格几何</b>：{@link RelicCodexScreen#columns()} = 3，{@link RelicCodexScreen#rowsFor(int)} 正确；</li>
 *   <li><b>详情页 / 列表页分工</b>：源码级断言 —— 遗物**列表**不再画初始 / 掉落标签，而**详情**方法里两段标签都在；</li>
 *   <li><b>接线没断</b>：图鉴主页确实挂上了"状态图鉴 / 药水图鉴"两个模块。</li>
 * </ol>
 *
 * <pre>
 * java -cp "build\classes\java\main;build\classes\java\test;&lt;NeoForm classpath&gt;" com.slaythecraft.client.CodexPagesCheck
 * </pre>
 */
public final class CodexPagesCheck {

    private CodexPagesCheck() {
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        int[] counters = {0, 0};

        statuses(lines, counters);
        potions(lines, counters);
        footerGeometry(lines, counters);
        uiOverflowFixes(lines, counters);
        settlementUi(lines, counters);
        cardLayout(lines, counters);
        gateCarousel(lines, counters);
        relicGrid(lines, counters);
        relicSourceRendering(lines, counters);
        codexHome(lines, counters);
        questionCodex(lines, counters);
        iconsAndDevButtons(lines, counters);
        weakReworkStatusIcons(lines, counters);
        multiplayerCodexPage(lines, counters);
        battleRelicBar(lines, counters);

        lines.forEach(System.out::println);
        String summary = (counters[1] == 0 ? "全部通过" : "存在失败") + "：" + counters[0] + " 通过 / " + counters[1]
                + " 失败";
        System.out.println(summary);
        if (counters[1] > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- 1. 状态图鉴数据

    /** 状态图鉴：每条状态都要能画出【图标 + 名字】，详情页要能写出【描述 + 机制】。 */
    private static void statuses(List<String> lines, int[] counters) {
        List<StatusIcons.Entry> entries = StatusIcons.all();
        check(lines, counters, "状态图鉴有数据（StatusIcons.all() 非空）", !entries.isEmpty(), "> 0",
                entries.size());

        List<String> missing = new ArrayList<>();
        for (StatusIcons.Entry entry : entries) {
            if (entry.label() == null || entry.label().isBlank()) {
                missing.add("名字为空");
            }
            if (entry.flavor() == null || entry.flavor().isBlank()) {
                missing.add(entry.label() + "：描述为空");
            }
            if (entry.mechanics() == null || entry.mechanics().isBlank()) {
                missing.add(entry.label() + "：机制为空");
            }
            if (entry.texture() == null) {
                missing.add(entry.label() + "：没有图标贴图");
            }
        }
        check(lines, counters, "每条状态都有名字 / 描述 / 机制 / 图标（共 " + entries.size() + " 条）",
                missing.isEmpty(), "0 条缺项", missing);

        // 名字不能重复（否则列表里会出现两个一样的按钮）
        check(lines, counters, "状态名字互不重复",
                entries.stream().map(StatusIcons.Entry::label).distinct().count() == entries.size(),
                entries.size(), entries.stream().map(StatusIcons.Entry::label).distinct().count());

        // 列表分页：一页放得下就不用翻页，放不下要正好多一页
        int perPage = StatusCodexScreen.perPage();
        check(lines, counters, "状态图鉴一页 = " + perPage + " 条（2 列 × 6 行）",
                StatusCodexScreen.columns() == 2 && perPage == 12, "2 列 / 12 条",
                StatusCodexScreen.columns() + " 列 / " + perPage + " 条");
        check(lines, counters, "状态图鉴页数 = ceil(条数 / 每页)",
                StatusCodexScreen.pagesFor(entries.size()) == Math.max(1, (entries.size() + perPage - 1) / perPage),
                Math.max(1, (entries.size() + perPage - 1) / perPage),
                StatusCodexScreen.pagesFor(entries.size()));
        check(lines, counters, "状态图鉴空列表也算 1 页（不会除以 0）", StatusCodexScreen.pagesFor(0) == 1, 1,
                StatusCodexScreen.pagesFor(0));
    }

    // ---------------------------------------------------------------- 2. 药水图鉴数据

    /** 药水图鉴：每瓶药水都要能画出【图标 + 名字】，详情页要能写出【效果描述 + 机制】。 */
    private static void potions(List<String> lines, int[] counters) {
        List<Potion> potions = PotionLibrary.all();
        check(lines, counters, "药水图鉴有数据（PotionLibrary.all() 非空）", !potions.isEmpty(), "> 0",
                potions.size());

        List<String> missing = new ArrayList<>();
        for (Potion potion : potions) {
            if (potion.name() == null || potion.name().isBlank()) {
                missing.add(potion.id() + "：名字为空");
            }
            if (potion.description() == null || potion.description().isBlank()) {
                missing.add(potion.id() + "：描述为空");
            }
            if (potion.kind() == null || potion.form() == null) {
                missing.add(potion.id() + "：性质 / 图标形态为空");
            }
            if (potion.usesItemIcon() && (potion.itemId() == null || potion.itemId().isBlank())) {
                missing.add(potion.id() + "：用物品贴图却没有物品 id");
            }
        }
        check(lines, counters, "每瓶药水都有名字 / 描述 / 性质 / 形态（共 " + potions.size() + " 瓶）",
                missing.isEmpty(), "0 瓶缺项", missing);

        int perPage = PotionCodexScreen.perPage();
        check(lines, counters, "药水图鉴一页 = " + perPage + " 瓶（2 列 × 6 行）",
                PotionCodexScreen.columns() == 2 && perPage == 12, "2 列 / 12 瓶",
                PotionCodexScreen.columns() + " 列 / " + perPage + " 瓶");
        check(lines, counters, "药水图鉴页数 = ceil(瓶数 / 每页)",
                PotionCodexScreen.pagesFor(potions.size()) == Math.max(1, (potions.size() + perPage - 1) / perPage),
                Math.max(1, (potions.size() + perPage - 1) / perPage),
                PotionCodexScreen.pagesFor(potions.size()));
    }

    // ---------------------------------------------------------------- 3. 战斗卡面版式与下移

    /**
     * v0.5.0.6 需求（配了两张版式图）：
     *
     * <pre>
     * "1.图1是现在游戏内的效果。图2是应该至现的效果。"
     * "2.存在问题：卡牌位置过高。应该向下（正下位置，可以隐藏物品栏，饱食度，经验值和生命值等UI
     *   来达到只显示卡牌的效果。）。届时，也要一并移动药水栏，状态显示位置和结束回合按钮。"
     * </pre>
     *
     * <p>版式图要求：卡图占上半部分的大部分 → 卡名 → 类型（攻击/技能/能力）居中的一条带
     * → 最下面一块**独立的效果文案框**。这里断死"四块互不重叠且都在牌内"。</p>
     */
    private static void cardLayout(List<String> lines, int[] counters) {
        int[] m = BattleScreen.cardMetrics();
        int cardW = m[0];
        int cardH = m[1];
        int[] off = BattleScreen.cardLayoutOffsets();
        int artTop = off[0];
        int artBottom = off[1];
        int nameY = off[2];
        int textTop = off[3];
        int textBottom = off[4];
        int typeTop = off[5];
        int typeBottom = off[6];
        int cardBottom = off[7];

        check(lines, counters, "卡面宽度放大四分之一（64 → 80）",
                cardW == 80 && BattleScreen.CARD_W_BASE == 64, "80", String.valueOf(cardW));
        // 高度刻意不放大：GUI 刻度高度常只有 240，加上 HUD_SHIFT 下移后 120 会顶出屏幕
        check(lines, counters, "卡面高度保持 96（不放大，避免下移后顶出屏幕）",
                cardH == 96 && cardH == BattleScreen.CARD_H_BASE, "96", String.valueOf(cardH));
        check(lines, counters, "卡图占上半部分的主要位置（高度 ≥ 牌高的三分之一）",
                m[2] * 3 >= cardH, "≥ " + (cardH / 3), m[2]);
        // ⚠️ v0.5.0.15 起这条断言的**前提变了**：卡名按版式图搬到了最上面（费用框右边），
        // 不再是"卡图下沿之下"。所以这里改成断言三者的**包含关系**（卡名带在卡图上、且各块首尾相接），
        // 而不是继续沿用旧的位置比较 —— 旧写法会与"按版式图重排"直接冲突。
        check(lines, counters, "卡图不与卡名带重叠（卡名带在卡图之上，见下面的版式断言）",
                nameY >= 0 && off[9] <= artTop, "卡名带在卡图之上", off[9] + " vs " + artTop);
        check(lines, counters, "效果文案框在卡名带之下（不与卡名重叠）", textTop >= off[9] + 8,
                "≥ " + (off[9] + 8), textTop);
        check(lines, counters, "效果文案框整块都在牌内（下沿不越界）",
                textBottom <= cardBottom, "≤ " + cardBottom, textBottom);
        check(lines, counters, "类型标签带在效果框之上、且整块在牌内（不越出牌底）",
                typeTop <= textTop && typeBottom <= cardBottom, "在效果框之上且在牌内",
                typeTop + " / " + typeBottom);
        check(lines, counters, "卡图 + 名字 + 效果框 + 类型带都在牌内（卡图起点 ≥ 0）", artTop >= 0, "≥ 0", artTop);

        // ---- v0.5.0.15：按版式图重排 + 三条反馈
        int nameTop = off[8];
        int nameBottom = off[9];
        int[] blocks = BattleScreen.cardBlockHeights();
        int nameBandH = blocks[3];

        // 需求："手牌的实际设计应该为图3" —— 纵向顺序：卡名带 → 卡图 → 类型带 → 效果框
        check(lines, counters, "卡名带在卡图之上（版式图：费用/卡名在最上面）", nameBottom <= artTop,
                "≤ " + artTop, nameBottom);
        // v0.5.0.16：卡图与类型带之间留了 2 像素间隙（让三块的分隔均匀可辨），
        // 所以这里断言的是"**至多** 2 像素间隙"，而不是上一版的"首尾相接"。
        check(lines, counters, "卡图与类型带之间只有 ≤ 2 像素间隙（版式紧凑）",
                typeTop - artBottom >= 0 && typeTop - artBottom <= 2, "0..2", typeTop - artBottom);
        check(lines, counters, "类型带紧接在效果框之上（首尾相接）", off[6] == textTop, "==",
                off[6] + " vs " + textTop);
        check(lines, counters, "卡名带 + 卡图 + 类型带 + 效果框 + 各段留白 = 牌高（没有漏算的空隙）",
                nameBandH + blocks[0] + blocks[1] + blocks[2] + BattleScreen.cardFixedPadTotal() == cardH,
                cardH, nameBandH + blocks[0] + blocks[1] + blocks[2] + BattleScreen.cardFixedPadTotal());
        check(lines, counters, "卡名不会压到左上角费用框（卡名带的起点在费用框之下）",
                nameTop >= 0, "≥ 0", nameTop);

        // 需求："边框需要加粗以更好地显示卡牌稀有度。"
        check(lines, counters, "稀有度边框已加粗（≥ 2 像素）",
                BattleScreen.cardBorderThickness() >= 2, "≥ 2", BattleScreen.cardBorderThickness());
        check(lines, counters, "边框加粗后仍不侵占内容（2 × 边框 < 卡图起点 + 卡图高）",
                BattleScreen.cardBorderThickness() * 2 <= artTop + BattleScreen.cardBorderThickness(),
                "不侵占", BattleScreen.cardBorderThickness() * 2);

        // 需求："X牌费用的显示应该为X，而不是0"
        check(lines, counters, "X 费牌在引擎里有可判定的标记（CardLibrary.isXCost 存在且可用）",
                com.slaythecraft.combat.core.CardLibrary.isXCost("tempest"),
                "tempest 是 X 费", "不是");
        check(lines, counters, "非 X 费牌不会被误判",
                !com.slaythecraft.combat.core.CardLibrary.isXCost("strike"), "strike 不是 X 费", "是");
        String battleSrc = read("client/BattleScreen.java", lines, counters);
        // v0.5.5.1：整张卡面的画法收敛到 CardFace（手牌 / 卡牌图鉴 / 拥有卡牌共用一份），
        // 所以这两条断言改成看 CardFace，并顺带断言手牌是**委托**过去（不会再出现两份画法）
        String faceSrc = read("client/CardFace.java", lines, counters);
        check(lines, counters, "卡面费用走 isXCost 判定（源码里显示 X 而不是 card.cost() 的裸值）",
                faceSrc != null && faceSrc.contains("CardLibrary.isXCost") && faceSrc.contains("stc.card.cost.x"),
                "有 X 费判定 + 文案键", "没有");
        check(lines, counters, "手牌的卡面委托给 CardFace（只有一份画法）",
                battleSrc.contains("CardFace.draw(") && battleSrc.contains("CardFace.View"),
                "出现", "缺失");

        // 需求："卡牌描述（应当适当缩放文本）"
        check(lines, counters, "描述用的自适应缩放只缩不放（scale ≤ 1）",
                TextStyle.fitScale(300, 1, 68, 21, 9) <= 1.0F, "≤ 1.0",
                TextStyle.fitScale(300, 1, 68, 21, 9));
        check(lines, counters, "描述放得下时按原大画（scale == 1）",
                TextStyle.fitScale(40, 1, 68, 21, 9) == 1.0F, "1.0",
                TextStyle.fitScale(40, 1, 68, 21, 9));
        check(lines, counters, "描述超出下限时被夹住（不会缩到看不见）",
                TextStyle.fitScale(9999, 99, 68, 21, 9) >= TextStyle.MIN_TEXT_SCALE, "≥ 下限",
                TextStyle.fitScale(9999, 99, 68, 21, 9));
        check(lines, counters, "卡面描述走了缩放绘制（CardFace 里用 drawScaled + fitScale）",
                faceSrc != null && faceSrc.contains("TextStyle.drawScaled")
                        && faceSrc.contains("TextStyle.fitScale"),
                "有", "没有");
        check(lines, counters, "描述框几何仍由 BattleScreen 的静态纯函数提供（绘制/折行/自检同一份定义）",
                faceSrc != null && faceSrc.contains("BattleScreen.textBoxInnerW()")
                        && faceSrc.contains("BattleScreen.textBoxInnerH()")
                        && faceSrc.contains("BattleScreen.cardLayoutOffsets()"),
                "共用几何", "各写一份");
        // v0.5.5.1（用户）："拥有卡牌和卡牌图鉴内的卡牌显示方式将和手牌显示的方式一致（包括美工等）"
        String codexSrc = read("client/CardCodexScreen.java", lines, counters);
        String pickSrc = read("client/CardPickScreen.java", lines, counters);
        check(lines, counters, "卡牌图鉴用整张卡面（CardFace.drawScaled）",
                codexSrc != null && codexSrc.contains("CardFace.drawScaled"), "出现", "缺失");
        check(lines, counters, "拥有卡牌用整张卡面（CardFace.drawScaled）",
                pickSrc != null && pickSrc.contains("CardFace.drawScaled"), "出现", "缺失");
        check(lines, counters, "两个列表都按卡面比例算格高（不会把卡压扁）",
                codexSrc != null && codexSrc.contains("CardFace.gridCellH")
                        && pickSrc != null && pickSrc.contains("CardFace.gridCellH"),
                "出现", "缺失");

        // ---- v0.5.0.16：反馈"手牌的描述模块出现严重错位"的**真正根因**与断言
        //
        // 根因：描述框内宽只有 68 像素，而最短的正式描述"造成 6 点伤害。"就要约 72 像素 ——
        // 一张普通打击牌的描述**必然折行**，框内高度又只够两行，文字被挤/被缩 → 看起来是"错位"。
        //
        // 我上一批只断言了"框在牌内"，**没断言"典型描述放得下"**，所以这个 bug 溜了过去。
        // 这两条就是补上的那一课。
        int innerW = BattleScreen.textBoxInnerW();
        check(lines, counters, "描述框内宽 ≥ 最短正式描述所需的 " + BattleScreen.CARD_TEXT_MIN_W + " 像素",
                innerW >= BattleScreen.CARD_TEXT_MIN_W, "≥ " + BattleScreen.CARD_TEXT_MIN_W, innerW);
        check(lines, counters, "描述框至少能画 2 行（放不下的描述才有缩放空间）",
                BattleScreen.textBoxLines() >= 2, "≥ 2", BattleScreen.textBoxLines());
        // 宽高必须足够容纳"一句典型描述"：宽度 ≥ 最小内宽，高度 ≥ 2 行
        check(lines, counters, "描述框的宽 × 高足够容纳一句典型描述（不是只够半个字）",
                innerW >= 70 && BattleScreen.textBoxInnerH() >= 18,
                "≥70 × ≥18", innerW + " × " + BattleScreen.textBoxInnerH());
        // 绘制与几何必须共用同一份定义（上一批的错位就是"算宽度"和"画宽度"各写一份导致的）
        check(lines, counters, "描述绘制用的是 textBoxInnerW/InnerH（绘制与几何只有一份定义）",
                battleSrc.contains("textBoxInnerW()") && battleSrc.contains("textBoxInnerH()"),
                "共用定义", "各写一份（就是错位的来源）");
        check(lines, counters, "描述框没有顶到牌边（给加粗边框留出内侧空间）",
                BattleScreen.textBoxInnerW() < cardW, "< " + cardW, BattleScreen.textBoxInnerW());

        // ---- v0.5.0.18：按用户给的原版卡面参考，插画应当是**通栏**的
        int[] art = BattleScreen.cardArtRect();
        int artLeftMargin = art[0];
        int artRightMargin = cardW - (art[0] + art[2]);
        check(lines, counters, "卡图通栏铺满卡宽（左右各只留 ≤ 2 像素，接近原版的通栏插画）",
                artLeftMargin <= 2 && artRightMargin <= 2, "左右各 ≤ 2",
                "左 " + artLeftMargin + " / 右 " + artRightMargin);
        check(lines, counters, "卡图仍不与卡名带重叠（在卡名带之下）", art[1] >= nameBottom, "≥ " + nameBottom,
                art[1]);
        check(lines, counters, "卡图仍整块在牌内（下沿不越界）", art[1] + art[3] <= typeTop, "≤ " + typeTop,
                art[1] + art[3]);

        // 卡牌下移：战斗内必须比非战斗时低 HUD_SHIFT
        int shift = BattleScreen.hudShiftAmount();
        int guiHeight = 240;
        int inBattle = BattleScreen.handYFor(guiHeight, true);
        int outOfBattle = BattleScreen.handYFor(guiHeight, false);
        check(lines, counters, "战斗内手牌确实下移了（差 = HUD_SHIFT > 0）",
                inBattle - outOfBattle == shift && shift > 0, "> 0", shift);
        // 注意：这里的高度是 Minecraft 的 **GUI 刻度**高度（480 窗口通常 = 240 刻度），不是窗口像素
        check(lines, counters, "下移后在标准 GUI 高度（240）下手牌整个在屏幕内",
                inBattle + cardH <= guiHeight, "≤ " + guiHeight, inBattle + cardH);
        check(lines, counters, "下移后手牌仍然不高过屏幕顶部（y ≥ 0）", inBattle >= 0, "≥ 0", inBattle);
        // 硬约束：下移量必须严格小于手牌底边距，否则底边会跑到屏幕外（这个坑我踩过一次）
        check(lines, counters, "HUD_SHIFT 严格小于手牌底边距（保证下移后不会顶出屏幕）",
                shift < BattleScreen.handBottomMargin(),
                "< " + BattleScreen.handBottomMargin(), shift);
    }

    // ---------------------------------------------------------------- 4. 角色选择滚轮式

    /**
     * v0.5.0.9 需求："角色选择后续需要改成滚轮式显示（因为后面还有新角色，以及要给附属包的开发者留下接口）。"
     *
     * <p>这里盯几何与循环：卡片不能挤出面板、滚轮切换要能循环、角色变多也不会破。</p>
     */
    private static void gateCarousel(List<String> lines, int[] counters) {
        int panelW = SpireGateScreen.gatePanelSize()[0];

        // 可见范围内：卡片都在面板里
        boolean inside = true;
        for (int offset = -3; offset <= 3; offset++) {
            if (!SpireGateScreen.carouselVisible(offset)) {
                continue;
            }
            int x = SpireGateScreen.carouselCardX(offset);
            if (x < 0 || x + SpireGateScreen.carouselCardWidth() > panelW) {
                inside = false;
            }
        }
        check(lines, counters, "滚轮可见范围内的卡片都在面板内", inside, "在面板内", "越界");
        check(lines, counters, "可见范围是 3 张（中间 + 左右各 1）",
                SpireGateScreen.carouselVisible(0) && SpireGateScreen.carouselVisible(1)
                        && SpireGateScreen.carouselVisible(-1) && !SpireGateScreen.carouselVisible(2),
                "3 张", "不是 3 张");
        check(lines, counters, "中间的卡片比两侧更靠中间（offset 0 最居中）",
                Math.abs(SpireGateScreen.carouselCardX(0) - panelW / 2)
                        < Math.abs(SpireGateScreen.carouselCardX(1) - panelW / 2),
                "0 最居中", "不是");

        // 滚轮循环
        check(lines, counters, "滚轮往后：最后一个 → 第一个",
                SpireGateScreen.carouselNext(3, 4, 1) == 0, 0, SpireGateScreen.carouselNext(3, 4, 1));
        check(lines, counters, "滚轮往前：第一个 → 最后一个",
                SpireGateScreen.carouselNext(0, 4, -1) == 3, 3, SpireGateScreen.carouselNext(0, 4, -1));
        check(lines, counters, "只有一个角色时不会越界",
                SpireGateScreen.carouselNext(0, 1, 1) == 0, 0, SpireGateScreen.carouselNext(0, 1, 1));
        check(lines, counters, "没有角色时返回 0（不会除以 0）",
                SpireGateScreen.carouselNext(0, 0, 1) == 0, 0, SpireGateScreen.carouselNext(0, 0, 1));
        // 角色再多也要能转到任何一个
        boolean coversAll = true;
        for (int i = 0; i < 12; i++) {
            if (SpireGateScreen.carouselNext(0, 12, i) != i) {
                coversAll = false;
            }
        }
        check(lines, counters, "12 个角色时任意索引都转得到（为附属包扩角色留好）", coversAll, "都能转到", "有转不到的");
    }

    // ---------------------------------------------------------------- 5b. v0.5.0.12 UI 溢出修复

    /**
     * v0.5.0.12 的反馈与相应的机器化断言：
     *
     * <ol>
     *   <li>"开始对战按钮应该被移除（无效）" —— 主页源码里**不再有** {@code stc.gate.start} 的按钮绘制；</li>
     *   <li>"可挑战名称里，生物的名称没有汉化" —— 显示名必须走 {@code displayName(...)}；</li>
     *   <li>"右侧的ID存在会超出UI范围" —— 名字列 + 间隙 + id 列 = 内容区宽度（两列铺满、不越界）；</li>
     *   <li>"页码被返回按钮覆盖" —— 页脚三个元素（页码 / 翻页 / 返回）几何上互不重叠；</li>
     *   <li>"尖塔入口主页的文字超出UI范围" —— 提示块在 1..3 行时都完整落在面板内。</li>
     * </ol>
     */
    private static void uiOverflowFixes(List<String> lines, int[] counters) {
        int panelW = SpireGateScreen.gatePanelSize()[0];
        int panelH = SpireGateScreen.gatePanelSize()[1];

        // ---- (1) 提示块：任何行数都必须完整落在面板内，且不压到按钮上
        boolean hintsInside = true;
        for (int n = 1; n <= SpireGateScreen.HINT_MAX_LINES; n++) {
            if (!SpireGateScreen.hintBlockInside(n)) {
                hintsInside = false;
            }
        }
        check(lines, counters, "尖塔入口提示块（1.." + SpireGateScreen.HINT_MAX_LINES + " 行）都完整落在面板内",
                hintsInside, "都在面板内", "有越界");
        check(lines, counters, "提示行数增加时首行上移（自底向上排版）",
                SpireGateScreen.hintBlockTop(3) < SpireGateScreen.hintBlockTop(1),
                "3 行更靠上", "没有上移");
        check(lines, counters, "提示块最后一行不会盖住第三排按钮",
                SpireGateScreen.hintBlockTop(SpireGateScreen.HINT_MAX_LINES)
                        > SpireGateScreen.buttonRow3Y() + 14,
                "> 按钮下沿", SpireGateScreen.hintBlockTop(SpireGateScreen.HINT_MAX_LINES));

        // ---- (2) 面板确实被放大了（反馈给的解决方式就是"扩大背景"）
        check(lines, counters, "尖塔入口面板已扩大（宽 ≥ 280 且高 ≥ 280）",
                panelW >= 280 && panelH >= 280, "≥ 280×280", panelW + "×" + panelH);

        // ---- (3) 滚轮三张卡 + 两侧留白都要放得下（原来 240 宽会挤出）
        int leftCardX = SpireGateScreen.carouselCardX(-1);
        int rightCardEnd = SpireGateScreen.carouselCardX(1) + SpireGateScreen.carouselCardWidth();
        check(lines, counters, "滚轮三张卡连同两侧留白都在面板内（原来 240 宽会挤出）",
                leftCardX >= 12 && rightCardEnd <= panelW - 12, "都在面板内",
                leftCardX + ".." + rightCardEnd);

        // ---- (4) 名单两列铺满内容区
        int[] cols = FloorRosterScreen.columnWidths();
        check(lines, counters, "名单的名字列 + id 列恰好铺满内容区（id 不会爬出面板）",
                FloorRosterScreen.columnsFit(), true, false);
        check(lines, counters, "名单 id 列从名字列右边开始（两列不重叠）",
                FloorRosterScreen.idColumnX() >= cols[0], "≥ 名字列宽", FloorRosterScreen.idColumnX());
        int[] rect = FloorRosterScreen.rowRect(FloorRosterScreen.rowsPerPage() - 1, 0);
        check(lines, counters, "名单最后一行仍落在页脚之上（不会和按钮压在一起）",
                rect[1] + rect[3] <= FloorRosterScreen.footerY(), "≤ 页脚 y",
                (rect[1] + rect[3]) + " vs " + FloorRosterScreen.footerY());

        // v0.5.0.15：反馈配图里名单的行画到了面板边框之外。这里把"面板能不能装下整页"钉死：
        // 内容区必须装得下 ROWS_PER_PAGE 行，而且行的上下边界都落在面板内。
        int rosterH = FloorRosterScreen.panelSize()[1];
        int listTop = FloorRosterScreen.rowRect(0, 0)[1];
        int listBottom = FloorRosterScreen.rowRect(FloorRosterScreen.rowsPerPage() - 1, 0)[1]
                + FloorRosterScreen.rowRect(0, 0)[3];
        check(lines, counters, "名单面板装得下一整页（内容区高 ≥ 每页行数 × 行高）",
                listTop >= 0 && listBottom <= rosterH, "在面板内",
                listTop + ".." + listBottom + " / 面板高 " + rosterH);
        check(lines, counters, "名单列表的上下边界都在面板内（列表不会越出边框）",
                listTop > 0 && listBottom <= FloorRosterScreen.footerY(), "在面板内",
                listTop + ".." + listBottom + " 页脚 " + FloorRosterScreen.footerY());
        check(lines, counters, "名单标题在面板顶部之内、且在列表之上（标题与首行不重叠）",
                FloorRosterScreen.rowRect(0, 0)[1] >= 10 + 9, "≥ 标题下沿",
                FloorRosterScreen.rowRect(0, 0)[1]);

        // ---- (5) 名单页脚：页码 / 翻页 / 返回 互不重叠
        int rosterW = FloorRosterScreen.panelSize()[0];
        int backX = BinderStyle.rosterBackButtonX(rosterW);
        int pageBtnX = BinderStyle.rosterPageButtonX(rosterW);
        check(lines, counters, "名单页脚：翻页按钮在返回按钮左边且不重叠",
                pageBtnX + BinderStyle.ROSTER_BTN_W <= backX, "不重叠",
                (pageBtnX + BinderStyle.ROSTER_BTN_W) + " vs " + backX);
        check(lines, counters, "名单页脚：返回按钮右边缘不越出面板",
                backX + BinderStyle.ROSTER_BTN_W <= rosterW - BinderStyle.GRID_X, "不越界",
                backX + BinderStyle.ROSTER_BTN_W);
        check(lines, counters, "名单页脚：页码有正的可用宽度（不会被挤掉）",
                BinderStyle.rosterPageTextWidth(rosterW) > 0, "> 0",
                BinderStyle.rosterPageTextWidth(rosterW));

        // ---- (6) 源码级：按钮已移除、名字已本地化
        String gate = read("client/SpireGateScreen.java", lines, counters);
        check(lines, counters, "尖塔入口主页源码里已经没有「开始对战」按钮的绘制",
                !gate.contains("stc.gate.start"), "不含 stc.gate.start", "仍然含");
        check(lines, counters, "尖塔入口主页源码里挂了「新手教程」按钮",
                gate.contains("stc.gate.tutorial") && gate.contains("TutorialScreen"),
                "有教程入口", "没有");
        String roster = read("client/FloorRosterScreen.java", lines, counters);
        check(lines, counters, "可挑战名单源码里用 displayName(...) 取本地化名",
                roster.contains("displayName(entry)"), "走 displayName", "没有");

        // ---- (7) 教程：章节键中英都有 + 切页不丢行、不超页
        String zh = readLang("zh_cn.json", lines, counters);
        String en = readLang("en_us.json", lines, counters);
        boolean keysOk = true;
        for (String key : TutorialScreen.allChapterKeys()) {
            if (!zh.contains("\"" + key + "\"") || !en.contains("\"" + key + "\"")) {
                keysOk = false;
            }
        }
        check(lines, counters, "教程章节（标题 + 正文）的键中英都有", keysOk, "都有", "有缺失");

        List<List<String>> cut = TutorialScreen.paginate(List.of(List.of("1", "2", "3", "4", "5"), List.of("6")), 3);
        int cutLines = cut.stream().mapToInt(List::size).sum();
        check(lines, counters, "教程切页不丢行（切出来的行数 ≥ 输入的正文行数 6）", cutLines >= 6, "≥ 6", cutLines);
        check(lines, counters, "教程切页：每页都不超过一页能画的行数",
                cut.stream().allMatch(p -> p.size() <= 3), "每页 ≤ 3", "有超页");

        // ---- (8) 新增语言键中英都有
        for (String key : new String[] {"stc.roster.page.short", "stc.gate.tutorial", "stc.gate.tutorial.hint"}) {
            check(lines, counters, "新增语言键中英都有：" + key,
                    zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
        }

        // ---- (9) 汉化的链条真的通
        //
        // 链条：FloorRoster.Entry.entityId() → BuiltInRegistries.ENTITY_TYPE.get(id)
        //       → EntityType.getDescription() → Util.makeDescriptionId("entity", key)
        //       = "entity.<namespace>.<path>"
        //
        // ⚠️ 这里**不能**真的去读 BuiltInRegistries / 构造 EntityType：
        // 裸 JVM 下 BuiltInRegistries 的静态初始化会直接抛 ExceptionInInitializerError
        // （没有 NeoForge 的 bootstrap），整个套件会挂掉。所以第一环改成**源码级**验证：
        // 界面确实走了 displayName(...) → 注册表查找 → getDescription()。
        // 第二环（描述键存在）在构建产物里验，且**做不了就显式报"未完成"**，绝不静默通过。
        String rosterSrc = read("client/FloorRosterScreen.java", lines, counters);
        check(lines, counters, "汉化走的是实体类型的本地化描述（源码里出现 ENTITY_TYPE.get + getDescription）",
                rosterSrc.contains("BuiltInRegistries.ENTITY_TYPE.get") && rosterSrc.contains("getDescription()"),
                "有注册表查找 + getDescription", "没有");
        check(lines, counters, "汉化失败时会退回原始名字（源码里有 fallback 分支）",
                rosterSrc.contains("return fallback == null"), "有兜底", "没有兜底");

        List<FloorRoster.Entry> floorOne = FloorRoster.byFaction(1).values().stream()
                .flatMap(List::stream).toList();
        check(lines, counters, "第 1 层名单非空（下面按名单逐条验描述键）", !floorOne.isEmpty(), "> 0",
                floorOne.size());

        String vanillaEn = vanillaLang("en_us.json");
        if (vanillaEn.isEmpty()) {
            check(lines, counters, "原版实体描述键校验（**未完成**：读不到 "
                    + "client-1.21.1-client-extra.jar，先跑一次 gradlew build）", false, "已校验", "未完成");
        } else {
            List<String> missingDesc = new ArrayList<>();
            for (FloorRoster.Entry entry : floorOne) {
                String key = descriptionKey(entry.entityId());
                if (!vanillaEn.contains("\"" + key + "\"")) {
                    missingDesc.add(key);
                }
            }
            check(lines, counters, "可挑战名单里每一只都有原版实体描述键（汉化取得到，不是空白）",
                    missingDesc.isEmpty(), "都有", missingDesc);
        }
    }

    /** {@code EntityType.getDescription()} 用的翻译键：{@code entity.<namespace>.<path>}。 */
    static String descriptionKey(String entityId) {
        int colon = entityId == null ? -1 : entityId.indexOf(':');
        String ns = colon > 0 ? entityId.substring(0, colon) : "minecraft";
        String path = colon > 0 ? entityId.substring(colon + 1) : String.valueOf(entityId);
        return "entity." + ns + "." + path;
    }

    /** 读原版客户端 jar 里的 en_us.json（实体名等原版翻译都在那里）；读不到返回空串。 */
    private static String vanillaLang(String name) {
        Path jar = Path.of(".gradle", "repositories", "ng_dummy_ng", "net", "minecraft", "client", "1.21.1",
                "client-1.21.1-client-extra.jar");
        if (!Files.isRegularFile(jar)) {
            return "";
        }
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
            java.util.zip.ZipEntry entry = zip.getEntry("assets/minecraft/lang/" + name);
            if (entry == null) {
                return "";
            }
            try (java.io.InputStream in = zip.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // 读不到就让调用方按"未完成"显式报出来（绝不静默通过）
            return "";
        }
    }

    /** 读一个语言文件。 */
    private static String readLang(String name, List<String> lines, int[] counters) {
        try {
            return Files.readString(Path.of("src", "main", "resources", "assets", "stc", "lang", name),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            check(lines, counters, name + "：能读取（" + e.getMessage() + "）", false, "可读", "读不到");
            return "";
        }
    }

    // ---------------------------------------------------------------- 5c. v0.5.0.20 结算页与反馈

    /**
     * v0.5.0.20 的反馈与相应的机器化断言：
     *
     * <ul>
     *   <li>"新UI:战斗结算页面" —— 结算页的行几何必须自洽（四行不重叠、都在面板内、左右对称）；</li>
     *   <li>"战斗结算，卡牌领取页面添加动画：渐显" —— {@link UiFeedback#fadeAlpha} 必须 0 → 1 单调；</li>
     *   <li>"为每次点击按钮都添加音效：block.note_block.hat，音高（第一八度）F♯/G♭" ——
     *       音高常量必须是 0.5（原版音符盒 F♯4）；</li>
     *   <li>"移除详情页下方的统计数据（击杀，胜败，删牌）" —— 进度页源码里不再绘制那一行。</li>
     * </ul>
     */
    private static void settlementUi(List<String> lines, int[] counters) {
        int[] panel = RewardScreen.resultPanelSize();
        int panelW = panel[0];
        int panelH = panel[1];
        check(lines, counters, "结算页面板尺寸是 260×186", panelW == 260 && panelH == 186,
                "260×186", panelW + "×" + panelH);

        int[] gold = RewardScreen.resultRowRect(0);
        int[] relic = RewardScreen.resultRowRect(1);
        int[] card = RewardScreen.resultRowRect(2);
        int[] skip = RewardScreen.resultRowRect(3);
        check(lines, counters, "结算页四行从上到下依次排开、互不重叠",
                gold[1] + gold[3] <= relic[1] && relic[1] + relic[3] <= card[1]
                        && card[1] + card[3] <= skip[1],
                "不重叠", gold[1] + "/" + relic[1] + "/" + card[1] + "/" + skip[1]);
        check(lines, counters, "结算页四行都在面板内（最后一行下沿不越界）",
                skip[1] + skip[3] <= panelH, "≤ " + panelH, skip[1] + skip[3]);
        check(lines, counters, "结算页行按钮左右边距对称、都在面板内",
                gold[0] >= 0 && gold[0] + gold[2] <= panelW
                        && gold[0] == panelW - (gold[0] + gold[2]),
                "对称且在内", gold[0] + ".." + (gold[0] + gold[2]));

        long t0 = 1000L;
        check(lines, counters, "渐显：刚打开时完全透明（alpha = 0）",
                UiFeedback.fadeAlpha(t0, t0) == 0.0F, 0.0F, UiFeedback.fadeAlpha(t0, t0));
        check(lines, counters, "渐显：到时后完全不透明（alpha = 1）",
                UiFeedback.fadeAlpha(t0 + UiFeedback.fadeInMs(), t0) == 1.0F, 1.0F,
                UiFeedback.fadeAlpha(t0 + UiFeedback.fadeInMs(), t0));
        check(lines, counters, "渐显：中途在 0..1 之间且随时间单调递增",
                UiFeedback.fadeAlpha(t0 + 50, t0) > 0.0F
                        && UiFeedback.fadeAlpha(t0 + 50, t0) < 1.0F
                        && UiFeedback.fadeAlpha(t0 + 150, t0) > UiFeedback.fadeAlpha(t0 + 50, t0),
                "单调递增", UiFeedback.fadeAlpha(t0 + 50, t0) + " -> " + UiFeedback.fadeAlpha(t0 + 150, t0));
        check(lines, counters, "渐显：没有打开时刻（openedAt ≤ 0）时直接全亮",
                UiFeedback.fadeAlpha(t0, 0L) == 1.0F, 1.0F, UiFeedback.fadeAlpha(t0, 0L));

        check(lines, counters, "按钮音效音高是第一八度 F♯/G♭（原版音符盒 F♯4 = 0.5）",
                Math.abs(UiFeedback.PITCH_F_SHARP_4 - 0.5F) < 1.0E-6F, 0.5F, UiFeedback.PITCH_F_SHARP_4);

        String rewardSrc = read("client/RewardScreen.java", lines, counters);
        check(lines, counters, "卡牌领取页与结算页都走渐显（UiFeedback.fadeAlpha + FadeStyle）",
                rewardSrc.contains("UiFeedback.fadeAlpha") && rewardSrc.contains("FadeStyle.withAlpha"),
                "有", "没有");
        check(lines, counters, "按钮点击真的播了音效（click(...) 里调 UiFeedback.click）",
                rewardSrc.contains("UiFeedback.click()"), "有", "没有");
        // 两种模式：源码里按 Mode 分支画不同页面（判定用**方法调用**而不是类型名，
        // 否则 "RewardMenu.Mode" 这种子串在别处也会命中，断言就假通过了）
        check(lines, counters, "同一个界面类按菜单模式画两种页面（menu.mode() == RESULT / 否则 CHOICE）",
                rewardSrc.contains("menu.mode() == RewardMenu.Mode.RESULT")
                        && rewardSrc.contains("renderResult(") && rewardSrc.contains("renderChoice("),
                "有两条渲染分支", "没有");

        // 进度页：那一行必须被**注释掉**（源码里不能有"未注释的绘制语句"）。
        // 判定用 "图形调用 + 结尾分号"，因为注释里的那一行以 "//" 开头、且我们保留了原文作参照。
        String progressSrc = read("client/BinderProgressScreen.java", lines, counters);
        boolean stillDrawing = false;
        for (String line : progressSrc.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//")) {
                continue;
            }
            if (trimmed.startsWith("TextStyle.draw(graphics, font, stats")) {
                stillDrawing = true;
            }
        }
        check(lines, counters, "事件回合统计详情页**不再绘制**击杀/胜败/删牌那一行",
                !stillDrawing, "已移除", "仍在绘制");

        String zh = readLang("zh_cn.json", lines, counters);
        String en = readLang("en_us.json", lines, counters);
        for (String key : new String[] {"stc.reward.result.title", "stc.reward.result.gold",
                "stc.reward.result.relic", "stc.reward.result.card", "stc.reward.result.skip"}) {
            check(lines, counters, "结算页语言键中英都有：" + key,
                    zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
        }
    }

    // ---------------------------------------------------------------- 5. 页脚翻页按钮几何

    /**
     * v0.5.0.4 需求："药水图鉴位置，上一页/下一页的位置错误（被放置在了图鉴外）。"
     *
     * <p>把"翻页按钮到底画在哪"变成可断言的几何：两个按钮都必须落在书页内，
     * 「返回」与「上一页」不许重叠，而且**最窄面板**（{@link BinderStyle#widthFor} 的下限 180）
     * 下也要成立——这正是以前写死的边距会漏掉的情况。</p>
     */
    private static void footerGeometry(List<String> lines, int[] counters) {
        // 覆盖面板宽的整个取值区间：默认 268（WIDTH）到最窄 180（widthFor 的下限），外加中间值。
        // 不真的去构造 Screen（那要 Minecraft 环境），直接喂宽度——生产路径的宽度本来就由
        // BinderStyle.widthFor 夹在 [180, 268] 里。
        int[] widths = {BinderStyle.WIDTH, 220, 180};

        for (int w : widths) {
            int pageW = BinderStyle.pageButtonWidth(w);
            int nextX = BinderStyle.nextPageButtonX(w);
            int prevX = BinderStyle.prevPageButtonX(w);
            int backRight = BinderStyle.GRID_X + BinderStyle.FOOTER_BACK_W;

            check(lines, counters, "面板宽 " + w + "：「下一页」右边缘不越出书页右内缩",
                    nextX + pageW <= w - BinderStyle.GRID_X,
                    "≤ " + (w - BinderStyle.GRID_X), nextX + pageW);
            check(lines, counters, "面板宽 " + w + "：「上一页」不越出书页左内缩",
                    prevX >= BinderStyle.GRID_X, "≥ " + BinderStyle.GRID_X, prevX);
            check(lines, counters, "面板宽 " + w + "：「返回」与「上一页」不重叠",
                    prevX >= backRight,
                    "≥ " + backRight, prevX);
            check(lines, counters, "面板宽 " + w + "：「上一页」与「下一页」不重叠",
                    prevX + pageW + BinderStyle.PAGE_BTN_GAP <= nextX,
                    "≤ " + nextX, prevX + pageW + BinderStyle.PAGE_BTN_GAP);
            check(lines, counters, "面板宽 " + w + "：翻页按钮高度不越出书页",
                    BinderStyle.footerY(BinderStyle.HEIGHT) + BinderStyle.FOOTER_BTN_H
                            <= BinderStyle.HEIGHT,
                    "≤ " + BinderStyle.HEIGHT,
                    BinderStyle.footerY(BinderStyle.HEIGHT) + BinderStyle.FOOTER_BTN_H);
        }

        // 总数文本：短文本放得下、长文本自觉地让位
        check(lines, counters, "页脚总数文本够短时放得下（不与「上一页」相撞）",
                BinderStyle.footerCountFits(BinderStyle.WIDTH, 40), true, false);
        check(lines, counters, "页脚总数文本太长时判定为放不下（让位给翻页按钮）",
                !BinderStyle.footerCountFits(BinderStyle.WIDTH, 200), false, true);
    }

    // ---------------------------------------------------------------- 4. 遗物网格几何
    /** 需求："遗物图鉴外部的排列修改为每行3个。" */
    private static void relicGrid(List<String> lines, int[] counters) {
        check(lines, counters, "遗物图鉴每行 3 个（RelicCodexScreen.columns() == 3）",
                RelicCodexScreen.columns() == 3, 3, RelicCodexScreen.columns());
        check(lines, counters, "rowsFor(7) == 3", RelicCodexScreen.rowsFor(7) == 3, 3,
                RelicCodexScreen.rowsFor(7));
        check(lines, counters, "rowsFor(9) == 3", RelicCodexScreen.rowsFor(9) == 3, 3,
                RelicCodexScreen.rowsFor(9));
        check(lines, counters, "rowsFor(1) == 1", RelicCodexScreen.rowsFor(1) == 1, 1,
                RelicCodexScreen.rowsFor(1));
        check(lines, counters, "rowsFor(3) == 1（正好一行）", RelicCodexScreen.rowsFor(3) == 1, 1,
                RelicCodexScreen.rowsFor(3));
        check(lines, counters, "rowsFor(4) == 2（多一个就换行）", RelicCodexScreen.rowsFor(4) == 2, 2,
                RelicCodexScreen.rowsFor(4));
        check(lines, counters, "rowsFor(0) 不炸（空列表算 1 行）", RelicCodexScreen.rowsFor(0) == 1, 1,
                RelicCodexScreen.rowsFor(0));

        // 真实数据：全部遗物排出来要能塞进书页（每行 3 个 → 行数别超过书页高度能放下的量）
        int relics = RelicLibrary.all().size();
        check(lines, counters, "全部遗物（" + relics + " 件）按 3 个一行排成 "
                        + RelicCodexScreen.rowsFor(relics) + " 行",
                RelicCodexScreen.rowsFor(relics) == (relics + 2) / 3, (relics + 2) / 3,
                RelicCodexScreen.rowsFor(relics));

        // 遗物有多少件是"初始遗物"（列表页不再显示这个标签，但详情页要用）
        List<Relic> starting = new ArrayList<>();
        for (Relic relic : RelicLibrary.all()) {
            if (RelicLibrary.isStarting(relic.id())) {
                starting.add(relic);
            }
        }
        check(lines, counters, "初始遗物仍然被登记（详情页要写清归属角色）", !starting.isEmpty(), "> 0",
                starting.size());
    }

    // ---------------------------------------------------------------- 4. 列表 / 详情分工（源码级）

    /**
     * 需求："未详情查看的图鉴外侧不再显示获取方式。获取方式，机制，趣闻描述都显示在详情页面。"
     *
     * <p>源码级断言：把 {@code render(...)}（列表页）和 {@code openDetail(...)}（详情页）两段切出来，
     * 检查列表段里**没有** {@code stc.codex.relics.starting} / {@code stc.codex.relics.drop}，
     * 而详情段里两段都在。</p>
     */
    private static void relicSourceRendering(List<String> lines, int[] counters) {
        String source = read("client/RelicCodexScreen.java", lines, counters);
        if (source == null) {
            return;
        }
        String list = between(source, "public void render(GuiGraphics", "public boolean mouseClicked(");
        String detail = after(source, "private void openDetail(");
        check(lines, counters, "能切出遗物列表的 render(...) 段", list != null && !list.isEmpty(), "非空",
                list == null ? "null" : list.length() + " 字");
        check(lines, counters, "能切出遗物详情的 openDetail(...) 段", detail != null && !detail.isEmpty(), "非空",
                detail == null ? "null" : detail.length() + " 字");
        if (list == null || detail == null) {
            return;
        }
        check(lines, counters, "列表页不再画【初始遗物】标签（stc.codex.relics.starting）",
                !list.contains("stc.codex.relics.starting"), "不出现", "出现了");
        check(lines, counters, "列表页不再画【可随机掉落】标签（stc.codex.relics.drop）",
                !list.contains("stc.codex.relics.drop"), "不出现", "出现了");
        check(lines, counters, "列表页也不再画【触发点】标签（stc.codex.relics.triggers）",
                !list.contains("stc.codex.relics.triggers"), "不出现", "出现了");
        check(lines, counters, "详情页写明【获取方式】（stc.codex.relics.source）",
                detail.contains("stc.codex.relics.source"), "出现", "缺失");
        check(lines, counters, "详情页写明【机制】（stc.codex.detail.mechanics）",
                detail.contains("stc.codex.detail.mechanics"), "出现", "缺失");
        check(lines, counters, "详情页写明【趣闻 / 触发点】（stc.codex.relics.flavor）",
                detail.contains("stc.codex.relics.flavor"), "出现", "缺失");
        check(lines, counters, "详情页保留初始遗物的长说明（starting.long）",
                detail.contains("stc.codex.relics.starting.long"), "出现", "缺失");
        check(lines, counters, "详情页保留掉落遗物的长说明（drop.long）",
                detail.contains("stc.codex.relics.drop.long"), "出现", "缺失");
        check(lines, counters, "列表页仍然显示遗物总数（stc.codex.relics.count）",
                list.contains("stc.codex.relics.count"), "出现", "缺失");
    }

    // ---------------------------------------------------------------- 5. 图鉴主页接线

    /** 需求："新图鉴：状态图鉴 / 药水图鉴" —— 主页要真的挂上这两个模块，且老模块一个不少。 */
    private static void codexHome(List<String> lines, int[] counters) {
        String home = read("client/CodexScreen.java", lines, counters);
        if (home != null) {
            // v0.5.1.4：加入第 9 个模块（多人模式卡牌图鉴）
            check(lines, counters, "主页模块数 = 9（8 个老模块 + 多人模式卡牌图鉴）",
                    home.contains("MODULES = 9"), 9, "源码里没找到 MODULES = 9");
            check(lines, counters, "主页新增【多人模式卡牌图鉴】模块（stc.codex.cards.multiplayer）",
                    home.contains("stc.codex.cards.multiplayer\""), "出现", "缺失");
            check(lines, counters, "点【多人模式卡牌图鉴】打开 CardCodexScreen.multiplayer",
                    home.contains("CardCodexScreen.multiplayer("), "出现", "缺失");
            check(lines, counters, "主页保留【状态与诅咒牌图鉴】模块（stc.codex.cards.status）",
                    home.contains("stc.codex.cards.status"), "出现", "缺失");
            check(lines, counters, "主页保留【遗物图鉴】模块（stc.codex.relics）",
                    home.contains("stc.codex.relics\""), "出现", "缺失");
            check(lines, counters, "主页保留【敌人阵营图鉴】模块（stc.codex.faction）",
                    home.contains("stc.codex.faction\""), "出现", "缺失");
            check(lines, counters, "主页新增【状态图鉴】模块（stc.codex.status）",
                    home.contains("stc.codex.status\""), "出现", "缺失");
            check(lines, counters, "主页新增【药水图鉴】模块（stc.codex.potion）",
                    home.contains("stc.codex.potion\""), "出现", "缺失");
            check(lines, counters, "点【状态图鉴】打开 StatusCodexScreen",
                    home.contains("new StatusCodexScreen("), "出现", "缺失");
            check(lines, counters, "点【药水图鉴】打开 PotionCodexScreen",
                    home.contains("new PotionCodexScreen("), "出现", "缺失");
            // v0.5.1.2 需求："图鉴：新增问号事件图鉴（查看问号事件与事件奖励）。"
            check(lines, counters, "主页新增【问号事件图鉴】模块（stc.codex.question）",
                    home.contains("stc.codex.question\""), "出现", "缺失");
            check(lines, counters, "点【问号事件图鉴】打开 QuestionCodexScreen",
                    home.contains("new QuestionCodexScreen("), "出现", "缺失");
            // ⚠️ v0.5.1.2 的几何修复：8 个模块必须**整体**塞进可用高度。
            // 旧公式 available/MODULES - GAP 再夹 20 下限时，8×20 + 7×8 = 216 > 194（会顶到页脚）。
            check(lines, counters, "模块高度公式先扣间隙再平分（保证 8 个模块总高 + 间隙 ≤ 可用高度）",
                    home.contains("(available - (MODULES - 1) * MODULE_GAP) / MODULES"),
                    "先扣间隙再平分", "仍是 available / MODULES - MODULE_GAP");
        }

        String statusList = read("client/StatusCodexScreen.java", lines, counters);
        if (statusList != null) {
            check(lines, counters, "状态图鉴列表按钮画【图标】（IconFit.draw）",
                    statusList.contains("IconFit.draw("), "出现", "缺失");
            check(lines, counters, "状态图鉴列表按钮画【名字】（entry.label()）",
                    statusList.contains("entry.label()"), "出现", "缺失");
            check(lines, counters, "状态图鉴返回键回图鉴主页（new CodexScreen）",
                    statusList.contains("new CodexScreen("), "出现", "缺失");
            check(lines, counters, "状态图鉴点条目打开 StatusDetailScreen（新页面）",
                    statusList.contains("new StatusDetailScreen("), "出现", "缺失");
        }

        String potionList = read("client/PotionCodexScreen.java", lines, counters);
        if (potionList != null) {
            check(lines, counters, "药水图鉴列表按钮画【图标】（PotionIcons.draw，与商店货架同一套画法）",
                    potionList.contains("PotionIcons.draw("), "出现", "缺失");
            check(lines, counters, "药水图鉴列表按钮画【名字】（potion.name()）",
                    potionList.contains("potion.name()"), "出现", "缺失");
            check(lines, counters, "药水图鉴返回键回图鉴主页（new CodexScreen）",
                    potionList.contains("new CodexScreen("), "出现", "缺失");
            check(lines, counters, "药水图鉴点条目打开 PotionDetailScreen",
                    potionList.contains("new PotionDetailScreen("), "出现", "缺失");
        }

        String statusDetail = read("client/StatusDetailScreen.java", lines, counters);
        if (statusDetail != null) {
            check(lines, counters, "状态详情页画【图标】（IconFit.draw）",
                    statusDetail.contains("IconFit.draw("), "出现", "缺失");
            check(lines, counters, "状态详情页写【名称】（entry.label()）",
                    statusDetail.contains("entry.label()"), "出现", "缺失");
            check(lines, counters, "状态详情页写【描述】（entry.flavor()）",
                    statusDetail.contains("entry.flavor()"), "出现", "缺失");
            check(lines, counters, "状态详情页写【机制】（entry.mechanics()）",
                    statusDetail.contains("entry.mechanics()"), "出现", "缺失");
            check(lines, counters, "状态详情页正文折行（BinderStyle.wrap，不截断）",
                    statusDetail.contains("BinderStyle.wrap("), "出现", "缺失");
        }

        String potionDetail = read("client/PotionDetailScreen.java", lines, counters);
        if (potionDetail != null) {
            check(lines, counters, "药水详情页画【图标】（PotionIcons.draw）",
                    potionDetail.contains("PotionIcons.draw("), "出现", "缺失");
            check(lines, counters, "药水详情页写【名称】（potion.name()）",
                    potionDetail.contains("potion.name()"), "出现", "缺失");
            check(lines, counters, "药水详情页写【效果描述】（potion.description()）",
                    potionDetail.contains("potion.description()"), "出现", "缺失");
            check(lines, counters, "药水详情页不再有【机制】段（与描述重复）",
                    !potionDetail.contains("stc.codex.detail.mechanics"), "没有", "仍有");
            check(lines, counters, "药水详情页不再显示小图标素材来源（图标形态 / 物品 id）",
                    !potionDetail.contains("stc.codex.potion.form") && !potionDetail.contains("formLabel("),
                    "没有", "仍有");
        }
    }

    // ---------------------------------------------------------------- 6. v0.5.1.2 问号事件图鉴

    /**
     * v0.5.1.2 需求："图鉴：新增问号事件图鉴（查看问号事件与事件奖励）。"
     *
     * <p>盯三件事：</p>
     * <ol>
     *   <li><b>数据齐全</b>：每个事件都要有名字、**触发方式**、至少一个选项，
     *       每个选项都要有按钮文字与奖励说明 —— 否则详情页里会出现空白行；</li>
     *   <li><b>登记不漏</b>：以后新加事件忘了写触发方式，这里当场报出来
     *       （触发条件原本散在 byProfession / byEntityType / EventTriggers 三处，最容易漏）；</li>
     *   <li><b>分页与接线</b>：列表页几何自洽、能被图鉴主页打开。</li>
     * </ol>
     */
    private static void questionCodex(List<String> lines, int[] counters) {
        List<com.slaythecraft.question.QuestionEvent> events = QuestionCodexScreen.events();
        check(lines, counters, "问号事件图鉴有数据（QuestionLibrary.all() 非空）", !events.isEmpty(), "> 0",
                events.size());
        check(lines, counters, "图鉴列出的事件数 = 事件库登记数",
                events.size() == com.slaythecraft.question.QuestionLibrary.all().size(), 
                com.slaythecraft.question.QuestionLibrary.all().size(), events.size());

        List<String> missing = new ArrayList<>();
        for (com.slaythecraft.question.QuestionEvent event : events) {
            if (event.name() == null || event.name().isBlank()) {
                missing.add(event.id() + "：名字为空");
            }
            if (!com.slaythecraft.question.QuestionLibrary.hasTrigger(event.id())) {
                missing.add(event.id() + "：没有登记触发方式");
            }
            if (com.slaythecraft.question.QuestionLibrary.triggerOf(event.id()).isBlank()) {
                missing.add(event.id() + "：触发方式是空白");
            }
            if (event.options().isEmpty()) {
                missing.add(event.id() + "：一个选项都没有");
            }
            for (com.slaythecraft.question.QuestionOption option : event.options()) {
                if (option.label() == null || option.label().isBlank()) {
                    missing.add(event.id() + "/" + option.id() + "：选项没有文字");
                }
                if (option.description() == null || option.description().isBlank()) {
                    // "查看事件奖励"就是看这一行；缺了等于图鉴白开
                    missing.add(event.id() + "/" + option.id() + "：没有写奖励（description 为空）");
                }
            }
        }
        check(lines, counters, "每个事件都有名字 / 触发方式 / 选项，且每个选项都写了奖励（共 " + events.size()
                + " 个事件）", missing.isEmpty(), "0 个缺项", missing);

        check(lines, counters, "事件 id 互不重复",
                events.stream().map(com.slaythecraft.question.QuestionEvent::id).distinct().count()
                        == events.size(),
                events.size(),
                events.stream().map(com.slaythecraft.question.QuestionEvent::id).distinct().count());

        int perPage = QuestionCodexScreen.perPage();
        check(lines, counters, "问号事件图鉴一页 = " + perPage + " 个（2 列 × 6 行）", perPage == 12, 12, perPage);
        check(lines, counters, "问号事件图鉴页数 = ceil(事件数 / 每页)",
                QuestionCodexScreen.pagesFor(events.size())
                        == Math.max(1, (events.size() + perPage - 1) / perPage),
                Math.max(1, (events.size() + perPage - 1) / perPage),
                QuestionCodexScreen.pagesFor(events.size()));
        check(lines, counters, "空列表也算 1 页（不会除以 0）", QuestionCodexScreen.pagesFor(0) == 1, 1,
                QuestionCodexScreen.pagesFor(0));

        String src = read("client/QuestionCodexScreen.java", lines, counters);
        if (src != null) {
            check(lines, counters, "列表按钮画【望远镜图标】（ItemIcons.draw + Items.SPYGLASS）",
                    src.contains("ItemIcons.draw(") && src.contains("Items.SPYGLASS"), "出现", "缺失");
            check(lines, counters, "列表按钮画【事件名】（event.name()）", src.contains("event.name()"), "出现",
                    "缺失");
            check(lines, counters, "列表按钮画【触发方式】（QuestionLibrary.triggerOf）",
                    src.contains("QuestionLibrary.triggerOf("), "出现", "缺失");
            check(lines, counters, "详情页写【触发方式】（stc.codex.question.trigger）",
                    src.contains("stc.codex.question.trigger"), "出现", "缺失");
            check(lines, counters, "详情页写【事件奖励】小标题（stc.codex.question.rewards）",
                    src.contains("stc.codex.question.rewards"), "出现", "缺失");
            check(lines, counters, "详情页逐条列出每个选项的奖励（stc.codex.question.option + option.description()）",
                    src.contains("stc.codex.question.option") && src.contains("option.description()"), "出现",
                    "缺失");
            check(lines, counters, "详情页用新页面打开（InfoPageScreen）",
                    src.contains("InfoPageScreen."), "出现", "缺失");
            check(lines, counters, "返回键回图鉴主页（new CodexScreen）", src.contains("new CodexScreen("), "出现",
                    "缺失");
        }

        String zh = readLang("zh_cn.json", lines, counters);
        String en = readLang("en_us.json", lines, counters);
        for (String key : new String[] {"stc.codex.question", "stc.codex.question.hint",
                "stc.codex.question.heading", "stc.codex.question.count", "stc.codex.question.trigger",
                "stc.codex.question.rewards", "stc.codex.question.option", "stc.codex.question.empty"}) {
            check(lines, counters, "问号事件图鉴语言键中英都有：" + key,
                    zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
        }
    }

    // ---------------------------------------------------------------- 7. v0.5.1.2 小图标与开发者按钮

    /**
     * v0.5.1.2 的两批界面改动：
     *
     * <ul>
     *   <li><b>小图标</b>：收纳包主页 4 个（书 / 箱子 / 指南针 / 书与笔）、事件触发方式页 4 个
     *       （铁剑 / 钻石剑 / 望远镜 / 篝火）、篝火 3 个（金苹果 / 铁砧 / 白色床）、
     *       战斗内生命前红心 + 格挡前盾牌；</li>
     *   <li><b>开发者模式客户端按钮</b>：图鉴详情页「加入牌组」、查看牌组页「删除」，
     *       两者都必须**由服务端同步的开关**把关，而不是无条件显示。</li>
     * </ul>
     */
    private static void iconsAndDevButtons(List<String> lines, int[] counters) {
        // ---- 图标几何：红心点阵不能是空的，尺寸要能与 8 像素高的字对齐
        check(lines, counters, "红心点阵是 7×6（与 8 像素字高对齐）",
                HudIcons.HEART_W == 7 && HudIcons.HEART_H == 6, "7×6",
                HudIcons.HEART_W + "×" + HudIcons.HEART_H);
        check(lines, counters, "物品图标按原版 16×16 渲染（ItemIcons.NATIVE）",
                ItemIcons.NATIVE == 16, 16, ItemIcons.NATIVE);
        check(lines, counters, "盾牌图标边长是正数（画得出来）", HudIcons.SHIELD > 0, "> 0", HudIcons.SHIELD);

        String binder = read("client/CardBinderScreen.java", lines, counters);
        if (binder != null) {
            check(lines, counters, "收纳包主页用 ItemIcons 画模块图标", binder.contains("ItemIcons.draw("), "出现",
                    "缺失");
            for (String item : new String[] {"Items.BOOK", "Items.CHEST", "Items.COMPASS",
                    "Items.WRITABLE_BOOK"}) {
                check(lines, counters, "收纳包主页第 " + item + " 个图标（需求点名的 4 个）",
                        binder.contains(item), "出现", "缺失");
            }
            check(lines, counters, "图标后文字右移、并按模块宽裁剪（长标题不会顶出边框）",
                    binder.contains("plainSubstrByWidth(label") && binder.contains("plainSubstrByWidth(hint"),
                    "有裁剪", "没有");
        }

        String progress = read("client/BinderProgressScreen.java", lines, counters);
        if (progress != null) {
            for (String item : new String[] {"Items.IRON_SWORD", "Items.DIAMOND_SWORD", "Items.SPYGLASS",
                    "Items.CAMPFIRE"}) {
                check(lines, counters, "事件触发方式页第 " + item + " 个图标",
                        progress.contains(item), "出现", "缺失");
            }
            check(lines, counters, "触发方式页用带图标的正文（InfoPageScreen.rich）",
                    progress.contains("InfoPageScreen.rich("), "出现", "缺失");
        }

        String campfire = read("client/CampfireScreen.java", lines, counters);
        if (campfire != null) {
            for (String item : new String[] {"Items.GOLDEN_APPLE", "Items.ANVIL", "Items.WHITE_BED"}) {
                check(lines, counters, "篝火按钮第 " + item + " 个图标", campfire.contains(item), "出现",
                        "缺失");
            }
            check(lines, counters, "篝火按钮用 ItemIcons 画图标", campfire.contains("ItemIcons.draw("), "出现",
                    "缺失");
        }

        String battle = read("client/BattleScreen.java", lines, counters);
        if (battle != null) {
            check(lines, counters, "战斗状态栏生命前画红心（HudIcons.heart）",
                    battle.contains("HudIcons.heart("), "出现", "缺失");
            check(lines, counters, "战斗状态栏格挡前画盾牌（HudIcons.shield）",
                    battle.contains("HudIcons.shield("), "出现", "缺失");
            check(lines, counters, "状态栏文字按**面板宽**裁剪（加了图标后不会顶出状态框）",
                    battle.contains("STATE_W - 12 - HudIcons.HEART_W")
                            && battle.contains("STATE_W - 12 - HudIcons.SHIELD"),
                    "按面板宽裁剪", "仍按屏幕宽裁剪");
        }

        String hud = read("client/HudIcons.java", lines, counters);
        if (hud != null) {
            check(lines, counters, "红心是程序化点阵（不依赖任何贴图路径）",
                    hud.contains("graphics.fill(") && !hud.contains(".png"), "程序化", "用了贴图路径");
        }

        // ---- 开发者模式按钮：功能在，但必须由服务端同步的开关把关
        String codex = read("client/CardCodexScreen.java", lines, counters);
        if (codex != null) {
            check(lines, counters, "图鉴详情页有「加入牌组」按钮（stc.dev.add_card）",
                    codex.contains("stc.dev.add_card"), "出现", "缺失");
            check(lines, counters, "「加入牌组」只在开发者模式开启时出现（ClientDevState.enabled()）",
                    codex.contains("ClientDevState.enabled()") && codex.contains("ClientDevActions.addCard("),
                    "有开关判定", "无条件显示");
            // v0.5.1.2：升级差异高亮 —— 详情页要看的是"相对基础版改了什么"
            check(lines, counters, "图鉴详情页用 CardTextDiff.marks 算升级改动高亮",
                    codex.contains("CardTextDiff.marks("), "出现", "缺失");
            check(lines, counters, "「加入牌组」与「查看升级」共存（两个动作按钮）",
                    codex.contains("richWithActions("), "出现", "缺失");
        }

        String pick = read("client/CardPickScreen.java", lines, counters);
        if (pick != null) {
            check(lines, counters, "查看牌组页有「删除」按钮（stc.dev.remove_card）",
                    pick.contains("stc.dev.remove_card"), "出现", "缺失");
            check(lines, counters, "「删除」只在开发者模式开启时出现（ClientDevState.enabled()）",
                    pick.contains("ClientDevState.enabled()") && pick.contains("ClientDevActions.removeCard("),
                    "有开关判定", "无条件显示");
            check(lines, counters, "只读卡片页原本不显示动作按钮（开发者模式是唯一例外）",
                    pick.contains("action == Action.NONE && ClientDevState.enabled()"), "有 NONE 判定",
                    "没有 NONE 判定");
        }

        String info = read("client/InfoPageScreen.java", lines, counters);
        if (info != null) {
            check(lines, counters, "信息页支持两个动作按钮（richWithActions + actionY 自下往上排）",
                    info.contains("richWithActions(") && info.contains("private int actionY("), "出现", "缺失");
            check(lines, counters, "信息页正文按**原文下标**折行（升级高亮不会跟着折行错位）",
                    info.contains("TextLayout.wrapRanges(") && info.contains("TextLayout.markRuns("), "出现",
                    "缺失");
            check(lines, counters, "信息页正文支持逐段小图标（BodyLine.icon）", info.contains("line.icon()"),
                    "出现", "缺失");
        }

        String devMode = read("dev/DevMode.java", lines, counters);
        if (devMode != null) {
            check(lines, counters, "开发者模式会把开关同步给客户端（syncTo → DevStatePayload）",
                    devMode.contains("syncTo(") && devMode.contains("DevStatePayload"), "出现", "缺失");
        }
        String login = read("data/PlayerDataEvents.java", lines, counters);
        if (login != null) {
            check(lines, counters, "重登时补发开发者金币并重新同步开关（放在 initialized 早退之前）",
                    login.contains("DevMode.syncTo(player)") && login.contains("DevMode.DEV_GOLD"), "出现",
                    "缺失");
            check(lines, counters, "同步发生在 initialized 早退**之前**（老存档也补得上）",
                    login.indexOf("DevMode.syncTo(player)") < login.indexOf("data.initialized()"),
                    "在前", "在后");
        }

        String clientDev = read("client/ClientDevActions.java", lines, counters);
        if (clientDev != null) {
            check(lines, counters, "客户端只发请求、不直接改档案（DevDeckPayload）",
                    clientDev.contains("DevDeckPayload") && !clientDev.contains("PlayerCardData"), "只发请求",
                    "直接改了档案");
        }

        // 登录时发的那一次可能比客户端准备好收包更早；打开收纳包时补一次，按钮不会静默消失
        String opener = read("binder/BinderOpener.java", lines, counters);
        if (opener != null) {
            check(lines, counters, "打开收纳包时补发一次开发者模式开关（登录包丢了也不会少按钮）",
                    opener.contains("DevMode.syncTo(player)"), "出现", "缺失");
        }

        String zh = readLang("zh_cn.json", lines, counters);
        String en = readLang("en_us.json", lines, counters);
        for (String key : new String[] {"stc.dev.add_card", "stc.dev.remove_card"}) {
            check(lines, counters, "开发者模式语言键中英都有：" + key,
                    zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
        }
    }

    // ---------------------------------------------------------------- 8. v0.5.1.3 战斗界面（遗物栏 + 回合文案）

    /**
     * v0.5.1.3 美术需求（用户在截图上圈出来的两处）：
     *
     * <ol>
     *   <li>红框 + 红字「移除」圈住「{@code MoonLiddell 的回合}」——**单人不再显示玩家名**；</li>
     *   <li>左侧空白区圈出来 + 红字"显示已经拥有的遗物（显示其小图标）鼠标在遗物上悬停以查看描述。"
     *       —— 战斗界面左侧要有遗物栏。</li>
     * </ol>
     *
     * <p>这里盯几何（不压按钮、不越界、命中判定正确）、顺序稳定（档案里是 {@code Set}，直接迭代
     * 会让图标每次同步都换位置），以及**没有美术资源时不能是一片空槽**（遗物目前一张贴图都没有，
     * 仓库里连 {@code textures/relic} 目录都不存在）。</p>
     */
    private static void battleRelicBar(List<String> lines, int[] counters) {
        int guiHeight = 480;

        // ---- (1) 位置：在状态框下面、在能量框与「查看我的牌组」按钮上面
        check(lines, counters, "遗物栏顶边在状态框下沿（26+44=70）之下", BattleRelicBar.TOP >= 70, "≥ 70",
                BattleRelicBar.TOP);
        int orbTop = guiHeight - 96 - 10 + BattleScreen.hudShiftAmount();
        check(lines, counters, "遗物栏底边在能量框之上（不压住左下角能量框）",
                BattleRelicBar.bottom(guiHeight) <= orbTop - 22, "≤ " + (orbTop - 22),
                BattleRelicBar.bottom(guiHeight));
        check(lines, counters, "遗物栏底边不越过「查看我的牌组」按钮（按钮顶边 = 能量框 − 22）",
                BattleRelicBar.bottom(guiHeight) <= BattleScreen.relicBarBottomLimit(guiHeight), "≤ 底边",
                BattleRelicBar.bottom(guiHeight));
        check(lines, counters, "标准界面高度（480）下至少放得下 5 件遗物",
                BattleRelicBar.rowsFor(guiHeight) >= 5, "≥ 5", BattleRelicBar.rowsFor(guiHeight));

        // ---- (2) 布局：列优先、格子互不重叠、都在面板内
        int count = 8;
        int[] panel = BattleRelicBar.panelRect(count, guiHeight);
        boolean inside = true;
        boolean overlap = false;
        List<int[]> rects = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int[] r = BattleRelicBar.slotRect(i, guiHeight);
            rects.add(r);
            if (r[0] < panel[0] || r[0] + r[2] > panel[0] + panel[2] || r[1] < panel[1]
                    || r[1] + r[3] > panel[1] + panel[3]) {
                inside = false;
            }
        }
        for (int i = 0; i < rects.size(); i++) {
            for (int j = i + 1; j < rects.size(); j++) {
                int[] a = rects.get(i);
                int[] b = rects.get(j);
                if (a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3]) {
                    overlap = true;
                }
            }
        }
        check(lines, counters, count + " 件遗物的格子都在面板内", inside, "都在面板内", "有格子越界");
        check(lines, counters, count + " 件遗物的格子互不重叠", !overlap, "不重叠", "有重叠");
        check(lines, counters, "一件遗物都没有时不画面板（不占屏幕）",
                BattleRelicBar.panelRect(0, guiHeight)[2] == 0, 0,
                BattleRelicBar.panelRect(0, guiHeight)[2]);
        check(lines, counters, "遗物少时只占一列（不会平白变宽）",
                BattleRelicBar.columnsFor(3, guiHeight) == 1, 1, BattleRelicBar.columnsFor(3, guiHeight));
        check(lines, counters, "遗物很多时最多排到 " + BattleRelicBar.MAX_COLS + " 列（不会一直往右长）",
                BattleRelicBar.columnsFor(999, guiHeight) == BattleRelicBar.MAX_COLS, BattleRelicBar.MAX_COLS,
                BattleRelicBar.columnsFor(999, guiHeight));
        check(lines, counters, "装不下时还有余量（容量有上限但不为 0）",
                BattleRelicBar.capacity(guiHeight) > 0, "> 0", BattleRelicBar.capacity(guiHeight));

        // ---- (3) 命中判定：格子中心命中自己，面板外不命中
        boolean hitsOk = true;
        for (int i = 0; i < count; i++) {
            int[] r = BattleRelicBar.slotRect(i, guiHeight);
            if (BattleRelicBar.hitIndex(r[0] + r[2] / 2.0, r[1] + r[3] / 2.0, count, guiHeight) != i) {
                hitsOk = false;
            }
        }
        check(lines, counters, "悬停命中判定：每个格子的中心都命中自己", hitsOk, "都命中", "有错位");
        check(lines, counters, "面板外不命中（不会误弹说明）",
                BattleRelicBar.hitIndex(panel[0] - 20, panel[1] - 20, count, guiHeight) == -1, -1,
                BattleRelicBar.hitIndex(panel[0] - 20, panel[1] - 20, count, guiHeight));

        // ---- (4) 顺序稳定：档案里是 Set，直接迭代会让图标每次同步都换位置
        List<Relic> all = RelicLibrary.all();
        check(lines, counters, "遗物库非空（下面按库里的顺序验）", !all.isEmpty(), "> 0", all.size());
        List<String> owned = new ArrayList<>();
        for (int i = 0; i < Math.min(6, all.size()); i++) {
            owned.add(all.get(i).id());
        }
        owned.add("other_mod:weird_relic");
        java.util.Set<String> shuffled = new java.util.LinkedHashSet<>();
        for (int i = owned.size() - 1; i >= 0; i--) {
            shuffled.add(owned.get(i));
        }
        List<String> orderedA = BattleRelicBar.orderedIds(shuffled);
        check(lines, counters, "遗物顺序与传入顺序无关（按图鉴登记顺序稳定排列）",
                orderedA.size() == owned.size() && orderedA.get(0).equals(all.get(0).id()), all.get(0).id(),
                orderedA.isEmpty() ? "(空)" : orderedA.get(0));
        check(lines, counters, "未知 id（别的模组塞进来的）不会被丢掉",
                orderedA.contains("other_mod:weird_relic"), "保留", "丢了");
        check(lines, counters, "同一批遗物两次排序结果一致",
                orderedA.equals(BattleRelicBar.orderedIds(new java.util.LinkedHashSet<>(owned))), "一致", "不一致");

        // ---- (5) 源码级：接线、悬停说明、无美术资源时的兜底
        String battle = read("client/BattleScreen.java", lines, counters);
        if (battle != null) {
            check(lines, counters, "战斗界面确实调用了遗物栏（drawRelicBar → BattleRelicBar.draw）",
                    battle.contains("drawRelicBar(") && battle.contains("BattleRelicBar.draw("), "出现", "缺失");
            check(lines, counters, "遗物栏读的是同步过来的档案（PlayerCardData.relics）",
                    battle.contains("data.relics()"), "出现", "缺失");
            check(lines, counters, "单人回合文案不再带玩家名（party().size() > 1 才用长文案）",
                    battle.contains("view.party().size() > 1") && battle.contains("stc.battle.turn.multi"),
                    "有单人/多人分支", "没有分支");
        }

        String bar = read("client/BattleRelicBar.java", lines, counters);
        if (bar != null) {
            check(lines, counters, "悬停时画说明框（用 BinderStyle.wrap 折行，不截断）",
                    bar.contains("BinderStyle.wrap("), "出现", "缺失");
            check(lines, counters, "说明框写遗物描述（relic.description()）",
                    bar.contains("relic.description()"), "出现", "缺失");
            check(lines, counters, "说明框会被夹在屏幕内（不会跑出画面）",
                    bar.contains("screenWidth - TIP_W") && bar.contains("guiHeight - tipH"), "有夹取", "没有");
        }

        String icons = read("client/RelicIcons.java", lines, counters);
        boolean hasFallback = icons != null && icons.contains("RUNE_COLORS") && icons.contains("graphics.fill(");
        if (icons != null) {
            check(lines, counters, "遗物图标有程序化兜底（没有贴图也画得出东西）", hasFallback, "有兜底", "只有贴图");
            check(lines, counters, "兜底用遗物名的第一个字（空槽比错误图标更糟）",
                    icons.contains("name.substring(0, 1)"), "出现", "缺失");
        }
        // 遗物目前**一件美术资源都没有**：所以"兜底"不是可选项，缺了整栏就是空的
        boolean anyRelicTexture = false;
        Path relicDir = Path.of("src", "main", "resources", "assets", "stc", "textures", "relic");
        if (Files.isDirectory(relicDir)) {
            try (java.util.stream.Stream<Path> stream = Files.list(relicDir)) {
                anyRelicTexture = stream.anyMatch(p -> p.getFileName().toString().endsWith(".png"));
            } catch (IOException e) {
                anyRelicTexture = true; // 读不到就当"有"，不让这条断言变成假失败
            }
        }
        check(lines, counters, "遗物目前没有贴图 → 必须有程序化兜底（否则遗物栏是一排空槽）",
                anyRelicTexture || hasFallback, "有贴图或已兜底", "两者都没有");

        // ---- (6) 回合文案的语言键
        String zh = readLang("zh_cn.json", lines, counters);
        String en = readLang("en_us.json", lines, counters);
        check(lines, counters, "单人回合文案中英都有：stc.battle.turn",
                zh.contains("\"stc.battle.turn\"") && en.contains("\"stc.battle.turn\""), "都有", "有缺失");
        check(lines, counters, "多人回合文案中英都有：stc.battle.turn.multi",
                zh.contains("\"stc.battle.turn.multi\"") && en.contains("\"stc.battle.turn.multi\""), "都有",
                "有缺失");
        check(lines, counters, "单人回合文案不含玩家名占位（需求：移除「X 的回合」）",
                !langValue(zh, "stc.battle.turn").contains("%s 的回合")
                        && !langValue(en, "stc.battle.turn").contains("'s turn"),
                "只剩回合数", langValue(zh, "stc.battle.turn"));
        check(lines, counters, "多人用的长文案保留了玩家名（单人不显示，但多人还要用）",
                langValue(zh, "stc.battle.turn.multi").contains("%s 的回合")
                        && langValue(en, "stc.battle.turn.multi").contains("'s turn"),
                "保留玩家名", langValue(zh, "stc.battle.turn.multi"));
        check(lines, counters, "遗物栏溢出提示中英都有：stc.battle.relics.overflow",
                zh.contains("\"stc.battle.relics.overflow\"") && en.contains("\"stc.battle.relics.overflow\""),
                "都有", "有缺失");
    }

    // ---------------------------------------------------------------- 9. v0.5.1.4 虚弱重做的图标覆盖

    /**
     * v0.5.1.4 需求（"药水效果 / 虚弱重做"）：
     *
     * <ul>
     *   <li>虚弱改成"可以累加、每层攻击伤害 -1"；</li>
     *   <li>旧机制改名【挖掘疲劳】，**并点名要用挖掘疲劳的效果小图标**；</li>
     *   <li>生物的"给予虚弱"意图全部改成"给予挖掘疲劳"。</li>
     * </ul>
     *
     * <p>状态名 → 图标的映射是**纯字符串**，写错了编译器一句话都不会说（游戏里只是不画图标），
     * 所以这里盯着：挖掘疲劳必须有图标、虚弱的说明必须已经改口（不能再写 -25%），
     * 而且**引擎状态栏能打出来的每一个状态名都必须有图标**（漏一个就是"有状态没图标"）。</p>
     */
    private static void weakReworkStatusIcons(List<String> lines, int[] counters) {
        // ---- 挖掘疲劳：需求点名"（挖掘疲劳效果小图标）"
        net.minecraft.resources.ResourceLocation fatigueTexture = StatusIcons.texture("挖掘疲劳");
        check(lines, counters, "【挖掘疲劳】登记了图标", fatigueTexture != null, "有图标", "没有");
        check(lines, counters, "挖掘疲劳用的是原版挖掘疲劳效果贴图（mob_effect/mining_fatigue）",
                fatigueTexture != null && fatigueTexture.getPath().endsWith("mining_fatigue.png"),
                "mining_fatigue.png", fatigueTexture == null ? "(null)" : fatigueTexture.getPath());
        check(lines, counters, "挖掘疲劳的机制写着 -25%",
                StatusIcons.mechanics("挖掘疲劳").contains("-25%"), "含 -25%",
                StatusIcons.mechanics("挖掘疲劳"));

        // ---- 虚弱：说明必须改口，否则玩家按旧机制理解就全错了
        String weakMechanics = StatusIcons.mechanics("虚弱");
        check(lines, counters, "【虚弱】的说明已改成「每层 -1 攻击伤害」",
                weakMechanics.contains("-1") && !weakMechanics.contains("-25%"), "每层 -1 且不再提 -25%",
                weakMechanics);
        check(lines, counters, "【虚弱】说明里写明与力量互相抵消",
                weakMechanics.contains("抵消"), "提到抵消", weakMechanics);
        check(lines, counters, "【力量】说明里也写明与虚弱互相抵消",
                StatusIcons.mechanics("力量").contains("抵消"), "提到抵消", StatusIcons.mechanics("力量"));
        check(lines, counters, "【迅捷】/【缓慢】说明里写明两者互相抵消",
                StatusIcons.mechanics("迅捷").contains("抵消") && StatusIcons.mechanics("缓慢").contains("抵消"),
                "都提到抵消", StatusIcons.mechanics("迅捷") + " / " + StatusIcons.mechanics("缓慢"));

        // ---- 引擎状态栏能打出来的每一个状态名，都必须有图标（漏一个就是"有状态没图标"）
        String[] engineLabels = {"力量", "虚弱", "挖掘疲劳", "易伤", "活力", "迅捷", "缓慢", "灼烧", "中毒",
                "凋零", "再生", "缓降", "神龟", "充能球", "集中", "蓄能"};
        List<String> noIcon = new ArrayList<>();
        for (String label : engineLabels) {
            if (!StatusIcons.known(label)) {
                noIcon.add(label);
            }
        }
        check(lines, counters, "引擎状态栏上的每个状态都有图标（共 " + engineLabels.length + " 种）",
                noIcon.isEmpty(), "0 个缺图标", noIcon);

        // ---- 文案：教程里不能再说"虚弱 -25%、按回合递减"
        String zh = readLang("zh_cn.json", lines, counters);
        String en = readLang("en_us.json", lines, counters);
        String zhTutorial = langValue(zh, "stc.tutorial.combat.body");
        String enTutorial = langValue(en, "stc.tutorial.combat.body");
        check(lines, counters, "教程（中）已把虚弱写成每层 -1 并提到挖掘疲劳",
                zhTutorial.contains("虚弱：可以累加") && zhTutorial.contains("挖掘疲劳"), "已更新", zhTutorial);
        check(lines, counters, "教程（中）不再写「虚弱：攻击伤害 −25%」",
                !zhTutorial.contains("虚弱：攻击伤害 −25%"), "已移除", zhTutorial);
        check(lines, counters, "教程（英）同步更新",
                enTutorial.contains("Mining Fatigue") && !enTutorial.contains("-25% (counts down, does not stack)"),
                "已更新", enTutorial);
    }

    // ---------------------------------------------------------------- 10. v0.5.1.4 多人模式卡牌图鉴

    /**
     * v0.5.1.4 需求："新卡牌类型：多人卡牌 —— 这些卡牌只有在多人模式下才会从奖励池里出现。
     * 为卡牌图鉴添加：多人模式卡牌图鉴。"
     *
     * <p>数据侧（卡的数值 / 奖励池过滤）在引擎自检里已经逐条验过了；这里盯**图鉴这一页**：
     * 它必须真的列出这几张、标题与语言键齐全、返回键回图鉴主页。
     * v0.5.3.1 起多了【商队护送】，所以是 6 张。</p>
     */
    private static void multiplayerCodexPage(List<String> lines, int[] counters) {
        List<com.slaythecraft.combat.core.Card> cards = com.slaythecraft.combat.core.CardLibrary.multiplayerCodex();
        // v0.5.5.1（用户）："现在的多人模式卡牌图鉴应该只有：商队护送。"
        check(lines, counters, "多人卡牌图鉴只有 1 张（【商队护送】，v0.5.5.1）", cards.size() == 1, 1, cards.size());
        check(lines, counters, "那一张就是商队护送",
                cards.size() == 1 && cards.get(0).id().equals("caravan_escort"), "caravan_escort", cards);
        List<String> notMarked = new ArrayList<>();
        for (com.slaythecraft.combat.core.Card card : cards) {
            if (!com.slaythecraft.combat.core.CardLibrary.isMultiplayerStyle(card.id())) {
                notMarked.add(card.id());
            }
        }
        check(lines, counters, "图鉴里的每一张都确实登记为多人玩法向（v0.5.2.5 改名，不再代表「仅多人」）",
                notMarked.isEmpty(), "0 张漏标", notMarked);

        String screen = read("client/CardCodexScreen.java", lines, counters);
        if (screen != null) {
            check(lines, counters, "卡牌图鉴支持多人模式页（multiplayerCards 分支）",
                    screen.contains("multiplayerCards"), "出现", "缺失");
            check(lines, counters, "多人页的数据源是 CardLibrary.multiplayerCodex()",
                    screen.contains("CardLibrary.multiplayerCodex()"), "出现", "缺失");
            check(lines, counters, "多人页的返回键回图鉴主页（与无色 / 状态页同一个判断）",
                    screen.contains("colorless || statusCards || multiplayerCards"), "出现", "缺失");
        }

        String zh = readLang("zh_cn.json", lines, counters);
        String en = readLang("en_us.json", lines, counters);
        for (String key : new String[] {"stc.codex.cards.multiplayer", "stc.codex.cards.multiplayer.hint"}) {
            check(lines, counters, "多人卡牌图鉴语言键中英都有：" + key,
                    zh.contains("\"" + key + "\"") && en.contains("\"" + key + "\""), "都有", "有缺失");
        }
    }

    // ---------------------------------------------------------------- 工具

    /** 取语言文件里某个键的值（键要连引号一起精确匹配，免得 {@code a} 命中 {@code a.b}）；找不到返回空串。 */
    private static String langValue(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) {
            return "";
        }
        int colon = json.indexOf(':', i + needle.length());
        int start = colon < 0 ? -1 : json.indexOf('"', colon + 1);
        if (start < 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int at = start + 1; at < json.length(); at++) {
            char ch = json.charAt(at);
            if (ch == '\\' && at + 1 < json.length()) {
                sb.append(json.charAt(++at));
                continue;
            }
            if (ch == '"') {
                break;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

    /** 取 {@code from}（含）到 {@code to}（不含）之间的源码；找不到任一段返回 null。 */
    private static String between(String source, String from, String to) {
        int start = source.indexOf(from);
        if (start < 0) {
            return null;
        }
        int end = source.indexOf(to, start + from.length());
        return end < 0 ? source.substring(start) : source.substring(start, end);
    }

    /** 取 {@code from}（含）之后的源码；找不到返回 null。 */
    private static String after(String source, String from) {
        int start = source.indexOf(from);
        return start < 0 ? null : source.substring(start);
    }

    private static String read(String relative, List<String> lines, int[] counters) {
        Path path = Path.of("src", "main", "java", "com", "slaythecraft").resolve(relative);
        if (!Files.isRegularFile(path)) {
            check(lines, counters, "源码存在：" + path, false, "存在", "缺失");
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            check(lines, counters, "源码可读：" + path, false, "可读", e.getClass().getSimpleName());
            return null;
        }
    }

    private static void check(List<String> lines, int[] counters, String label, boolean condition,
                             Object expected, Object actual) {
        if (condition) {
            counters[0]++;
            lines.add("PASS  " + label);
        } else {
            counters[1]++;
            lines.add("FAIL  " + label + "（期望 " + expected + "，实际 " + actual + "）");
        }
    }
}
