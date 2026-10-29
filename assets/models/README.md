# 模型（models/）

| 子文件夹 | 同步到 | 说明 |
| --- | --- | --- |
| `item/` | `src/main/resources/assets/stc/models/item/` | 物品模型（`.json`） |
| `block/` | `src/main/resources/assets/stc/models/block/` | 方块模型（`.json`） |

`.bbmodel`（Blockbench 工程）也可以直接放在这里：
`build.gradle` 已排除 `**/*.bbmodel`，不会被误打进 jar；导出 `.json` 后我来接线。

## 给自绘贴图接线的模板

**物品**（例如给战斗军旗换成自绘图 `assets/stc/textures/item/battle_standard.png`）：

```json
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "stc:item/battle_standard" }
}
```

**方块**（例如尖塔入口用 `assets/stc/textures/block/spire_gate.png`）：

```json
{
  "parent": "minecraft:block/cube_all",
  "textures": { "all": "stc:block/spire_gate" }
}
```

方块的 `blockstates/<方块id>.json` 已经指向 `stc:block/<方块id>`，
所以只要把对应文件放进 `models/block/`，模型和贴图就会一起生效。

## 现在的模型长什么样

| 文件 | 现在 | 换成自绘 |
| --- | --- | --- |
| `models/item/battle_standard.json` | `minecraft:item/generated` + `minecraft:item/skull_banner_pattern` | 把 `layer0` 改成 `stc:item/battle_standard` |
| `models/block/spire_gate.json` | `cube_all` + `minecraft:block/lodestone_top` | 把 `all` 改成 `stc:block/spire_gate` |
| `models/block/card_recycler.json` | `cube_all` + `minecraft:block/blast_furnace_side` | 把 `all` 改成 `stc:block/card_recycler` |
