# SoundRecord

## Looking for English documents?

English documentation is available in [README_EN.md](README_EN.md).

![SoundRecord](https://r2.yume.games/srd/c4e32c63-40d2-40f5-a8d8-09dc0646e5ca.png)

将你喜欢的声音永远保存。

最初的目的是开发一款用于保存Hypixel SkyBlock音乐的软件。

SoundRecord 旨在保存服务器内你喜欢而无法制作出来的音乐。
它由客户端 Fabric 模组和服务端 Paper 插件组成：客户端负责录制游戏内播放的声音并保存为 `.srd` 文件，服务端负责接收、管理和播放这些录音。

在进行录制前，请确保你的网络状态良好，这可能影响最终的录制结果。

## 功能

- 暂停页面添加录制入口。
- 支持全部音效与音乐模式录制。
- 支持 Modern 模式，让录音从第一个非点击音效开始播放。
- 自动排除 UI 点击按钮声音。
- 保存 `.srd` 文件并上传到服务器插件目录。
- 服务端使用 `/record` 播放、停止和查看玩家当前播放状态。

## 构建

```powershell
.\build.bat
```

构建完成后文件位于：

- `build/dist/SoundRecord-Fabric-1.0.0.jar`
- `build/dist/SoundRecord-Paper-1.0.0.jar`

## 安装

- 将 `SoundRecord-Fabric-1.0.0.jar` 放入客户端 `mods` 文件夹。
- 将 `SoundRecord-Paper-1.0.0.jar` 放入服务端 `plugins` 文件夹。
- 服务端录音文件位于 `plugins/SoundRecord/records`。

## 命令

```text
/record
```

## 许可证

此项目基于 GPL v3 许可证开源。

## 已支持的语言

- 简体中文
- 繁体中文（香港特别行政区/台湾）
- 英文
