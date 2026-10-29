# 界面贴图（textures/gui/）

收纳包系列界面（主页、卡牌/遗物/事件统计、图鉴、卡牌详情、选择页、确认页、篝火页…）
**共用一套画法**，所以这里放一张图就能全局换掉。

## 收纳包界面皮肤（`gui/binder/`）—— v0.4.6.5 起的正式接口

| 文件 | 作用 | 画法 |
| --- | --- | --- |
| `binder/background.png` | 书页背景 | **整图拉伸**到面板（默认 268×300；放了它就不再画程序化羊皮纸） |
| `binder/button.png` | 按钮（正常） | **九宫格**：四角各 3 像素固定、边与中心拉伸 |
| `binder/button_hover.png` | 按钮（悬停） | 同上；缺文件时用正常态 |
| `binder/button_disabled.png` | 按钮（禁用） | 同上；缺文件时用正常态 |
| `binder/avatar_<角色>.png` | 角色头像 | 等比压进页眉 22×22 方框（`avatar_ironclad` / `_silent` / `_defect` / `_pillager`） |
| `binder/avatar.png` | 通用头像 | 上面那张找不到时用它 |

按钮九宫格示意（数字是九块，3 像素边框不缩放）：

```
┌───┬─────────┬───┐
│ 1 │    2    │ 3 │
├───┼─────────┼───┤
│ 4 │    5    │ 6 │
├───┼─────────┼───┤
│ 7 │    8    │ 9 │
└───┴─────────┴───┘
```

常用按钮尺寸：52×14（返回/小型）、96×14、118×14、140×14。
贴图尺寸随意（绘制时会读原图实际像素尺寸），**只要四角各留 3 像素**。

也可以用代码指定（优先级高于文件，`null` 表示撤销）：

```java
StcApi.registerBinderBackground(id);
StcApi.registerBinderButtons(normal, hovered, disabled);
StcApi.registerBinderAvatar(characterId, id);
StcApi.registerBinderDefaultAvatar(id);
```

## 其它界面贴图

| 子文件夹 | 用途 | 现状 |
| --- | --- | --- |
| `hud/` | 药水栏、充能球、奖励角标等 HUD | ⚠️ 现在全是程序化绘制，需要我接线才能换成贴图 |
| `screen/` | 战斗界面（手牌、敌人栏、牌堆按钮） | ⚠️ 同上 |
| `codex/` | 图鉴专用装饰（除了上面的共用书页） | ⚠️ 同上 |

想换这些界面就说一声，我按同一套"约定路径 + 缺图退回程序化"的方式加接口。

## 已接线的其它贴图位置

| 位置 | 目录 | 说明 |
| --- | --- | --- |
| 卡牌插画 | `textures/card/` | 按卡牌 id 命名（`strike.png`），见该目录里的 README |
| 村民职业皮肤 | `textures/entity/villager/profession/` | `card_master.png` |
| 物品 / 方块 | `textures/item/`、`textures/block/` | ⚠️ 需要我把模型 `layer0`/`all` 指过去 |
