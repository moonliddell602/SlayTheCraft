# 字体与语言（fonts/ · lang/）

## fonts/

中文字体文件（`.ttf` / `.otf`）放这里，例如 `assets/fonts/ChinesePixel.ttf`。
打包后会落到 `assets/stc/font/`。

> 现状：界面用的是**原版字体**（`TextStyle` 统一走原版 `Font`，默认不描边 —— 配置 `uiTextShadow`）。
> 要换成像素中文字体需要我加一层 `font` 定义 + 在 `TextStyle` 里切过去，缺字体时自动回退原版，
> 所以可以放心先放进来。

用法举例（我会做的事）：在 `assets/stc/font/` 下建一个字体定义 JSON，把中文码位段指到你的 `.ttf`，
然后在界面里用 `Style` 指定它。

## lang/

翻译 / 文案补充。给我「键 → 文案」即可，我合并进
`assets/stc/lang/zh_cn.json` 与 `en_us.json`（键名可以沿用现有的 `stc.*` 前缀）。

也可以直接把一份 JSON 放在这里（形如 `{"stc.xxx": "文案"}`），我按语言合并。
