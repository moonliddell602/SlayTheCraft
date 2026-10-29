# 贴图（textures/）

按用途分子文件夹，同步后分别落到 `src/main/resources/assets/stc/textures/` 下同名目录。

| 子文件夹 | 同步到 | 用途 | 现状 |
| --- | --- | --- | --- |
| `items/` | `textures/item/` | 物品贴图 | ⚠️ 需要我改一行模型（现在用原版贴图） |
| `blocks/` | `textures/block/` | 方块贴图 | ⚠️ 需要我改一行模型 |
| `gui/` | `textures/gui/` | 界面面板 / 卡面框 / 按钮 | ⚠️ 需要我加一层可选皮肤 |
| `entity/villager/profession/` | `textures/entity/villager/profession/` | 村民职业皮肤 | ✅ 直接可用（`card_master.png`） |
| `status/` | `textures/status/` | 状态 / 充能球图标 | ⚠️ 需要我加自定义图标表 |

## 命名建议

* 物品：`<物品id>.png`，例如 `battle_standard.png`、`card_binder.png`
* 方块：`<方块id>.png`，例如 `spire_gate.png`、`card_recycler.png`
* 界面：按区域命名，例如 `card_frame.png`、`card_art_slot.png`、`panel_book.png`、`button.png`
* 状态：按状态名命名，例如 `poison.png`（中毒）、`decay.png`（凋零）、`burn.png`（灼烧）、
  `orb_lightning.png`（闪电球）

## 尺寸

* 物品 / 方块：16×16（想更清晰就 32×32，但要和原版观感一致）
* 村民皮肤：64×64
* 界面面板：按像素 1:1 画，另外告诉我它贴在哪块区域（卡面 / 书页面板 / 按钮）

## 现在用的是哪些原版贴图（替换参考）

| 位置 | 当前贴图 |
| --- | --- |
| 战斗军旗 | `minecraft:item/skull_banner_pattern`（旗帜图案） |
| 尖塔入口方块 | `minecraft:block/lodestone_top` |
| 卡牌回收站方块 | `minecraft:block/blast_furnace_side` |
| 状态图标 | `minecraft:textures/mob_effect/*`（中毒 / 凋零 / 抗性提升…）与 `item/fire_charge`（灼烧） |
| 卡牌大师村民 | `textures/entity/villager/profession/card_master.png`（沙漠村民贴图） |
