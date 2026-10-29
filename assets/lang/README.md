# 语言文件（lang/）

放**翻译 / 文案补充**。两种给法都行：

1. **给我文案**（最省事）：直接写「键 → 文案」，或按下面的 JSON 放进来，我合并进去；
2. 直接改 `src/main/resources/assets/stc/lang/zh_cn.json` / `en_us.json`（注意那是打包目录）。

```json
{
  "stc.my_feature.title": "我的新界面",
  "stc.my_feature.hint": "点一下就好"
}
```

## 为什么这里不自动复制

`assets/stc/lang/*.json` 现在各有 **240 多个键**，自动复制会把已有文案整份覆盖掉。
所以 `tools/import-assets.ps1` 只**提示**、不复制，由我按语言逐键合并（缺键补、同键覆盖）。

## 现有语言文件在哪

| 文件 | 用途 |
| --- | --- |
| `src/main/resources/assets/stc/lang/zh_cn.json` | 简体中文（主语言） |
| `src/main/resources/assets/stc/lang/en_us.json` | 英文（键与中文一一对应） |

加新键时的约定：跟现有风格一致即可（`stc.<模块>.<名字>`），
`en_us.json` 也要有同名的键，否则英文客户端会显示原始键名。
