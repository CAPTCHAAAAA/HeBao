<div align="center">

# 🧧 HeBao

### 一个专注于本地 H5 模板预览、交互调试与 WebView 外壳还原的 Android 实验项目

<p>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Core-WebView-4285F4?style=for-the-badge&logo=googlechrome&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-BottomSheet-111827?style=for-the-badge" />
</p>

<p>
  <b>让 H5 模板像原生页面一样运行。</b><br/>
  轻量、沉浸、可编辑、可导入、可切换。
</p>

</div>

---

## ✨ 项目简介

**HeBao** 是一个 Android WebView 外壳实验项目，用于在本地加载、预览和调试 HTML 模板页面。

它通过接近真实移动端浏览器体验的 WebView 容器，配合本地模板资源、JS Bridge、可视化编辑工具和模板切换能力，让 HTML 页面可以在 App 内获得更自然的移动端展示效果。

> 本项目适用于 H5 页面还原、移动端 WebView 适配测试、离线模板预览、UI 学习研究和个人实验。

---

## ⚠️ 使用声明

本项目仅用于：

* H5 页面离线预览
* WebView 容器学习
* 前端页面适配测试
* 本地模板管理实验
* UI 交互与 Android 原生控件学习

**请勿将本项目用于伪造证明、冒充官方页面、绕过学校/单位管理、欺骗他人、规避审核或任何违反法律法规、校纪校规、平台规则的行为。**

开发者不对任何滥用行为负责。

---

## 🚀 功能亮点

### 🧩 本地 HTML 模板加载

支持从 `assets` 中加载本地 HTML 模板，让页面无需网络也能在 WebView 中运行。

* 自动加载本地资源
* 支持多模板切换
* 支持 HTML / HTM 文件
* 支持资源索引式模板管理

---

### 🎛️ 模板切换系统

内置「切换模板」入口，可以在多个模板之间快速切换。

* 自动扫描 assets 中的 HTML 模板
* 导入外部 HTML 文件
* 保存导入模板
* 记录当前选择
* 支持隐藏内置模板
* 支持删除导入模板

> 内置模板来自 APK 打包资源，运行时无法真正删除；所谓“删除”本质是隐藏。导入模板则会从本地存储中真实删除。

---

### 🖊️ 点选文本编辑

进入点选文本模式后，页面中的可编辑文本会被高亮标记。

点击文字即可唤起底部编辑面板，对当前文本进行修改。

* 自动识别文本节点
* 保留页面原始布局
* 点击即改
* 修改后实时反映到页面
* 支持退出编辑模式恢复普通页面状态

---

### 🕒 时间滚轮工具

针对页面中的时间内容，提供类 iOS 风格滚轮选择器。

适合在模板调试时快速生成不同日期时间格式。

支持识别：

* `yyyy-MM-dd HH:mm`
* `yyyy/MM/dd HH:mm`
* `yyyy年MM月dd日 HH:mm`
* `MM-dd HH:mm`
* 带秒时间格式

---

### 🖼️ 图片替换模式

进入图片编辑模式后，页面中的图片会被标记为可替换对象。

点击图片即可从系统文件选择器中选取新图片，并以 Base64 Data URL 的形式注入到页面。

* 连续换图
* 点击目标图片替换
* 支持常见图片格式
* 无需修改 HTML 源文件

---

### 🌗 深色 / 浅色主题

App 外壳支持深色和浅色模式切换。

* 跟随系统初始状态
* 可在菜单中手动切换
* 顶栏、弹窗、胶囊按钮同步换肤

---

### 🪟 类原生底部菜单

项目大量使用 BottomSheet 风格交互，搭配胶囊按钮、圆角面板和轻量动画。

整体 UI 更接近现代移动端 App，而不是传统调试工具。

---

## 🧱 技术栈

| 模块    | 技术                                 |
| ----- | ---------------------------------- |
| 主语言   | Kotlin                             |
| 平台    | Android                            |
| 页面容器  | WebView                            |
| 页面通信  | JavascriptInterface                |
| UI 弹窗 | Material BottomSheetDialog         |
| 模板资源  | assets / app private files         |
| 图片导入  | ActivityResultContracts.GetContent |
| 本地持久化 | SharedPreferences / internal files |

---

## 📂 推荐目录结构

```text
app/
├─ src/
│  └─ main/
│     ├─ java/
│     │  └─ com/example/hebao/
│     │     └─ MainActivity.kt
│     ├─ assets/
│     │  ├─ template_a.html
│     │  ├─ template_b.html
│     │  └─ templates/
│     │     └─ more_template.html
│     └─ res/
│        └─ layout/
│           └─ activity_main.xml
```

导入的 HTML 文件会保存在 App 私有目录中：

```text
filesDir/imported_html_templates/
```

---

## 🧪 适用场景

* 本地 H5 页面预览
* WebView 适配测试
* HTML 模板快速调试
* 移动端 UI 还原研究
* Android 与 JS 通信实验
* 离线页面展示工具开发

---

## 🛠️ 构建方式

### 1. 克隆项目

```bash
git clone https://github.com/CAPTCHAAAAA/HeBao.git
```

### 2. 使用 Android Studio 打开

选择项目根目录，等待 Gradle Sync 完成。

### 3. 放入 HTML 模板

将 HTML 文件放入：

```text
app/src/main/assets/
```

或：

```text
app/src/main/assets/templates/
```

### 4. 构建运行

点击 Android Studio 的 Run 按钮，或使用：

```bash
./gradlew assembleDebug
```

---

## 🧭 使用说明

### 打开菜单

点击右上角更多按钮，可看到：

* 刷新重置
* 高级编辑
* 深色 / 浅色模式
* 发布页

### 高级编辑

高级编辑中包含：

* 点选文本编辑
* 更改页面图片
* 切换模板

### 切换模板

模板列表会自动展示：

* assets 内置 HTML
* 用户导入 HTML

点击模板即可切换当前 WebView 页面。

---

## 📌 模板管理说明

### 内置模板

内置模板位于：

```text
app/src/main/assets/
```

特点：

* 随 APK 一起打包
* 可被自动扫描
* 可从列表中隐藏
* 无法在 App 运行时真正删除

### 导入模板

导入模板来自用户手动选择的 HTML 文件。

特点：

* 保存到 App 私有目录
* 可在模板菜单中再次选择
* 可真实删除
* 删除后不可恢复，除非重新导入

---

## 🧠 设计理念

HeBao 的目标不是做一个复杂的大而全浏览器，而是做一个轻量、稳定、专注的 H5 模板容器。

它更像是一个：

> “披着原生 App 外壳的 HTML 实验台。”

少一点干扰，多一点沉浸。
少一点配置，多一点即开即用。
让页面预览、模板切换、文本修改和图片替换都尽可能自然。

---

## 🗺️ Roadmap   ## ️路线图

* [x] 本地 assets HTML 加载
* [x] WebView 基础容器
* [x] JS Bridge 通信
* [x] 点选文本编辑
* [x] 图片替换模式
* [x] 时间滚轮工具
* [x] 深色 / 浅色模式
* [x] 模板切换
* [x] 外部 HTML 导入
* [x] 导入模板删除
* [x] 内置模板隐藏
* [ ] 模板分组
* [ ] 模板重命名
* [ ] 模板预览缩略图
* [ ] 配置导入 / 导出
* [ ] 更完整的模板元数据系统

---

## 🤝 贡献

欢迎提交 Issue 或 Pull Request。Welcome to submit an issue or pull request.

你可以参与：

* UI 优化
* 模板管理增强
* WebView 兼容性改进
* 代码结构整理
* 文档完善

---

## 📄 License

本项目仅供学习、研究和合法场景下的本地页面预览使用。

请在遵守所在地法律法规、学校/单位规定及平台规则的前提下使用。

---

<div align="center">   <div align   对齐="center"   "center">

### Made with Kotlin & WebView

<b>HeBao</b> · Local H5 Preview Shell

</div>   < / div>
::: ​​
