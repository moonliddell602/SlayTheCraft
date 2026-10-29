卡面插画位（v0.4.5）
====================

这个目录是**卡牌贴图的约定存放位置**：把图片按下面的命名丢进来就会自动显示，
不需要改本模组的代码，也不需要注册任何东西。

命名规则
--------
    assets/<卡牌 id 的命名空间>/textures/card/<卡牌路径>.png

举例（本体的卡在 stc 命名空间）：
    assets/stc/textures/card/strike.png        ← 【打击】
    assets/stc/textures/card/defend.png        ← 【格挡】
    assets/stc/textures/card/arrow_rain.png    ← 【箭雨】
    assets/mypack/textures/card/my_card.png    ← 整合包自己的卡

尺寸随意（绘制时会拉伸到插画位）：建议 64×64 或 128×128 的正方形。
缺图时插画位会画成占位框（一个灰色的"图"字），不影响游戏。

插画位出现在这些界面（位置参照原版《杀戮尖塔》：插画在卡面**上半部分**）：
    * 战斗内的手牌卡面（上半部分，64×96 的卡面里占 58×26）
    * 卡牌详情页（点手牌/牌堆里的卡进入）
    * 卡牌图鉴的格子（左侧缩略图）
    * 战斗内「查看牌组 / 查看牌堆」的格子（左侧缩略图）

想放到别的路径（而不是上面这个约定），用代码接口：

    StcApi.registerCardTexture(
        ResourceLocation.fromNamespaceAndPath("stc", "strike"),
        ResourceLocation.fromNamespaceAndPath("mypack", "cards/fancy_strike"));

（同样不带 textures/ 前缀、不带 .png 后缀。）
