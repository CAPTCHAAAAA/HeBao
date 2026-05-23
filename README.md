# HeBao
模拟HENU的H5请假程序

<div align="center">

# HeBao

一个运行于 Android WebView 环境中的 H5 动态编辑与运行时注入框架。

用于在不修改原始网页源码的情况下，对页面中的文本与图片进行运行时编辑、替换与交互控制。

<br>

<img src="https://img.shields.io/badge/version-v1.0.0-2ea44f?style=flat-square">
<img src="https://img.shields.io/badge/platform-Android-3DDC84?style=flat-square">
<img src="https://img.shields.io/badge/runtime-WebView-4c8bf5?style=flat-square">
<img src="https://img.shields.io/badge/status-experimental-orange?style=flat-square">

</div>

---

# 项目简介

HeBao 是一个基于 Android WebView 的运行时网页编辑框架。

其核心目标是在宿主应用中动态接管 H5 页面，实现：

- 页面文本实时编辑
- 页面图片动态替换
- Android 与 JavaScript 双向通信
- DOM 运行时劫持
- 第三方 SDK 沙箱模拟
- 持续编辑模式管理

该项目并不依赖网页源码控制权限。

在 WebView 环境下，仅通过注入 JavaScript Runtime 即可完成页面元素的动态控制。

---

# 功能特性

## 文本编辑模式

支持对网页中的文本节点进行持续编辑。

框架会递归遍历 DOM 树中的所有 TextNode，并自动为可编辑文本生成交互层。

功能包括：

- 动态文本高亮
- 点击事件绑定
- Android 原生回调
- 连续编辑模式
- 页面内实时修改

---

## 图片替换模式

支持对网页中的图片资源进行运行时替换。

进入模式后：

- 页面中的所有图片将被自动标记
- 用户点击图片后触发 Android 回调
- 原生层可选择本地图片并替换页面资源

---

## 双向通信桥

内置 Android ↔ JavaScript Bridge。

支持：

- Java 调用 JavaScript
- JavaScript 调用 Android
- 实时页面控制
- 动态数据回传

示例：

```java
webView.addJavascriptInterface(new WebBridge(), "Android");
```

```javascript
window.Android.showToast("Hello");
```

---

## 第三方 SDK 沙箱

为避免部分 H5 页面因缺失宿主环境而出现白屏或崩溃，框架内置：

- wx SDK Mock
- campus SDK Mock
- 浏览器兼容沙箱

用于模拟微信或校园类 WebView Runtime。

---

## 模式互斥状态机

框架内部维护独立的模式状态锁。

例如：

- 进入文本模式时自动退出图片模式
- 防止事件冲突
- 防止 DOM 重复污染
- 避免交互层叠加

---

# 工作原理

HeBao 的核心结构如下：

```text
Android WebView
        ↓
Injected JavaScript Runtime
        ↓
DOM Traversal Layer
        ↓
Editable Overlay System
        ↓
Android Bridge Communication
```

整体工作流程：

1. WebView 加载网页
2. 注入 HeBao Runtime
3. 遍历 DOM 树
4. 接管文本与图片节点
5. 绑定交互事件
6. 通过 Bridge 与 Android 通信
7. Android 回写修改结果

---

# 核心机制

# DOM 深度遍历

框架通过 DFS（Depth First Search）递归遍历整个 DOM 树。

重点处理：

```javascript
node.nodeType === 3
```

即：

```text
TextNode
```

这样可以：

- 精准定位纯文本
- 不破坏原始布局
- 保持 CSS 结构稳定
- 减少对页面逻辑的影响

---

# 文本节点替换

由于 TextNode 无法直接绑定点击事件，因此框架会：

```text
TextNode
    ↓
Span Wrapper
```

通过生成可点击的 span 元素实现文本交互。

---

# 动态交互层

框架会自动为编辑元素附加：

- 高亮描边
- 点击反馈
- 动态阴影
- 动画过渡
- 悬浮退出按钮

以增强移动端交互体验。

---

# 项目结构

```text
HeBao/
├── js/
│   └── hebao-core.js
│
├── android/
│   ├── MainActivity.kt
│   ├── WebBridge.kt
│   └── WebViewManager.kt
│
├── assets/
│   ├── preview/
│   └── screenshots/
│
├── docs/
│   └── architecture.md
│
├── README.md
└── LICENSE
```

---

# 快速开始

# 1. 引入 Runtime

```html
<script src="hebao-core.js"></script>
```

---

# 2. 初始化文本编辑模式

```javascript
window.HeBaoCore.enableTextEditMode();
```

---

# 3. 初始化图片编辑模式

```javascript
window.HeBaoCore.enableImageEditMode();
```

---

# Android 集成

# 开启 JavaScript

```java
webView.getSettings().setJavaScriptEnabled(true);
```

---

# 注入 Bridge

```java
webView.addJavascriptInterface(new WebBridge(), "Android");
```

---

# 接收文本点击事件

```java
@JavascriptInterface
public void onTextClicked(String id, String text) {
    Log.d("HeBao", id + " => " + text);
}
```

---

# 接收图片点击事件

```java
@JavascriptInterface
public void onImageClicked(int index) {
    Log.d("HeBao", "Image Clicked: " + index);
}
```

---

# 示例

## 启动文本编辑模式

```javascript
window.HeBaoCore.enableTextEditMode();
```

---

## 启动图片替换模式

```javascript
window.HeBaoCore.enableImageEditMode();
```

---

## Android 回写文本

```java
webView.evaluateJavascript(
    "document.querySelector('[data-tid=\"text_1\"]').innerText='New Text'",
    null
);
```

---

# 已知问题

当前版本仍属于实验性实现。

在以下场景中可能存在兼容问题：

- React
- Vue
- SPA 页面
- Shadow DOM
- iframe 页面
- 动态 Virtual DOM

原因在于部分框架会主动重新渲染 DOM。

---

# 后续计划

计划加入：

- MutationObserver 自动重挂
- Overlay 无侵入编辑层
- iframe Runtime 注入
- fetch/xhr Hook   - fetch/xhr钩子
- WebSocket Hook
- Shadow DOM 支持
- CSS 隔离系统
- WeakMap DOM 恢复缓存
- React/Vue 兼容层

---

# 安全说明

由于框架依赖：

```java
addJavascriptInterface   添加JavaScript接口
```

因此在生产环境中应注意：

- 限制注入域名
- 避免暴露危险接口
- 禁止任意代码执行
- 关闭调试模式
- 对 Bridge 接口进行权限控制

否则可能存在：

- WebView 注入攻击
- JavaScript 越权调用
- Runtime 劫持风险

---

# 适用场景

HeBao 适用于：

- H5 调试工具
- WebView 壳应用
- 动态页面编辑器
- 网页运行时控制
- 教学演示工具
- 页面魔改工具
- Hybrid App Runtime   -混合应用运行时

---

# License   #许可证

MIT License   与条款

---

# 声明

本项目仅用于：

- 学习研究
- WebView Runtime 技术探索   - WebView Runtime Technology Exploration
- H5 动态交互实验

请勿将其用于非法用途。

---

<div align="center">   <div align="center">

HeBao Runtime Framework   和宝运行时框架

Android WebView Dynamic Injection SystemAndroid WebView 动态注入系统

</   & lt;div>
