## 1. 架构设计

```mermaid
flowchart TB
    subgraph "前端层"
        "VideoPlayer 组件" --> "HTML5 Video API"
        "VideoPlayer 组件" --> "自定义控制栏"
        "VideoPlayer 组件" --> "速率控制面板"
    end
    subgraph "浏览器 API"
        "HTML5 Video API" --> "playbackRate"
        "HTML5 Video API" --> "FileReader / URL.createObjectURL"
    end
```

纯前端方案，无需后端服务。视频文件通过浏览器原生 File API 加载到内存，使用 URL.createObjectURL 创建临时 URL 播放。所有速率控制通过 HTML5 Video 元素的 playbackRate 属性实现。

## 2. 技术说明

- 前端：React 18 + TypeScript + Tailwind CSS
- 状态管理：React useState / useRef（组件级状态即可，无需全局状态）
- 视频播放：HTML5 `<video>` 元素 + 原生 API
- 文件加载：`<input type="file">` + `URL.createObjectURL`
- 图标：lucide-react
- 无需后端、无需数据库、无需外部服务

## 3. 路由定义

| 路由 | 用途 |
|------|------|
| /tools/video-player | 在线视频播放器页面 |

## 4. API 定义

无后端 API。所有功能在浏览器端完成。

核心前端接口：
- `HTMLVideoElement.play()` / `.pause()` — 播放控制
- `HTMLVideoElement.playbackRate` — 速率控制（0.25 ~ 4.0）
- `HTMLVideoElement.currentTime` — 进度跳转
- `HTMLVideoElement.volume` — 音量控制
- `URL.createObjectURL(file: File)` — 本地文件加载

## 5. 组件结构

```
VideoPlayer.tsx — 主组件（约 300 行）
  ├── 文件选择区（拖拽 + 点击）
  ├── 视频画面区（<video> 元素）
  ├── 播放控制栏
  │   ├── 播放/暂停按钮
  │   ├── 进度条（可拖拽 seek）
  │   ├── 时间显示（当前 / 总时长）
  │   ├── 音量控制
  │   └── 全屏按钮
  └── 速率控制面板
      ├── 预设速率按钮组
      ├── 自定义速率滑块
      └── 当前速率显示
```

## 6. 关键技术点

### 6.1 速率控制实现

HTML5 Video 元素原生支持 `playbackRate` 属性，可设置为任意正数。浏览器无需任何插件即可实现 0.25x ~ 16x 的速率调节。百度网盘的限制是服务端/前端人为限制，本地播放器完全不受此约束。

### 6.2 文件加载安全

- 使用 `URL.createObjectURL` 创建临时 Blob URL，文件不离开本机
- 组件卸载时调用 `URL.revokeObjectURL` 释放内存
- 接受格式：`.mp4`（video/mp4）
