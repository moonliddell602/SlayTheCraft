# 卡牌插画（cards/）

把卡牌图片放在这里，**用卡牌 id 的路径命名**，例如：

```
cards/strike.png           → 打击
cards/defend.png           → 格挡
cards/arrow_rain.png       → 箭雨
cards/cake.png             → 蛋糕
cards/curse_cruel.png      → 残忍（诅咒）
```

* 建议尺寸 **64×64 或 128×128**（正方形，会被拉伸到卡面插画位）；
  用**原版《杀戮尖塔》的卡图**也可以（它们是竖向长方形，会按插画位裁切/缩放）。
* **一张一张手动复制太慢**：把图按 id 命名丢进这个目录，然后跑一次
  `powershell -ExecutionPolicy Bypass -File tools\import-card-art.ps1 -Import`
  —— 它会编译一个探针、把 `assets/cards/*.png` 全部复制到
  `src/main/resources/assets/stc/textures/card/`，并报告"哪些卡还没有图 + 哪些文件名不是卡牌 id"。
* 全部 **246 张卡的 id 清单**（照着命名）：[`card-art-checklist.md`](card-art-checklist.md)
  （由 `tools\import-card-art.ps1 -List` 自动生成，改了卡池之后重跑一次即可）。
* 复制完 **重新构建**（`.\gradlew.bat build --offline`）才会进 jar；资源包作者也可以直接
  覆盖纹理路径，不用改代码。
* 同步后落到：`src/main/resources/assets/stc/textures/card/<名字>.png`。

## 会出现在哪里

| 位置 | 说明 |
| --- | --- |
| 战斗内手牌 | 卡面**上半部分**（参照原版《杀戮尖塔》：插画在上、文字在下） |
| 卡牌详情页 | 战斗内点手牌 / 牌堆里的卡进入的页面，顶部大图位 |
| 卡牌图鉴 | v0.5.5.1 起图鉴格子画的是**整张卡面**（与手牌同一份画法），插画位同上 |
| 「拥有卡牌」列表 | v0.5.5.1 起同样是整张卡面 |
| 战斗内牌堆 | 「查看我的牌组 / 抽牌堆 / 弃牌堆 / 消耗堆」格子左侧缩略图 |

## 想用别的路径 / 别的命名空间

那就不放进这里，改用代码接口（我可以帮你写）：

```java
StcApi.registerCardTexture(
        ResourceLocation.fromNamespaceAndPath("stc", "strike"),
        ResourceLocation.fromNamespaceAndPath("mypack", "cards/fancy_strike"));
```

（路径不带 `textures/` 前缀、不带 `.png`。）

## 缺图时

插画位会画成占位框（灰底 + 一个"图"字），**不影响游戏**；
所以你可以先放一两张试效果，其余慢慢补。
