# HeBao
模拟HENU的H5请假程序
<h1>🚀 v1.0.0 功能更新日志 (Changelog)</   & lt;h1>

<details open>
  <summary><b>点击展开 / 收起 v1.0.0 核心功能变更描述</b></summary>
  <br>

  <blockquote>   < blockquote>
    <h3>📝 持续文本编辑模式 <code>[Feature]</code></h3>
    <ul>   < ul>
      <li><b>底层实现：</b>采用 <code>DOM</code> 树递归遍历算法，精确提取底层纯文本节点（<code>Node Type 3</code>），解决多层级框架嵌套下的文字点击盲区。</li>
      <li><b>交互反馈：</b>开启后，可编辑文字区域自动覆盖 <code>rgba(0, 122, 255, 0.12)</code> 蓝色微光，支持高频连续点触唤醒弹窗。点击页面底部挂载的 <code>#hebao-exit-btn</code> 悬浮按钮即可一键恢复网页原始干净状态。</li>
    </ul>   < / ul>
  </blockquote>   < / blockquote>

  <blockquote>   < blockquote>
    <h3>🖼️ 持续图片替换模式 <code>[Feature]</code></h3>
    <ul>   < ul>
      <li><b>底层实现：</b>基于 <code>JavascriptInterface</code> 建立原生图片通信桥梁，绑定 <code>GetContent</code> 结果回调。</li>
      <li><b>交互反馈：</b>开启后，网页所有 <code>&lt;img&gt;</code> 标签覆盖高亮框，点触任意图片直接调用系统原生相册，选图后自动在子线程压入 <code>Base64</code> 流，并由主线程静默更新对应 DOM 的 <code>src</code> 属性。支持连续换图，点击底部的 <code>#hebao-exit-img-btn</code> 悬浮按钮即可退出。</li>
    </ul>
  </blockquote>

  <blockquote>
    <h3>🔄 双版本离线页面切换 <code>[Optimization]</code></h3>
    <ul>
      <li>支持在运行期无缝切换内置的 <code>perfect_leave_img.html</code>（有图版）与 <code>perfect_leave_noimg.html</code>（无图版），并伴随“已切换至有/无图版本”的精准状态回显提示。</li>
    </ul>
  </blockquote>

  <blockquote>
    <h3>💎 原生扁平化交互卡片 <code>[UI/UX]</code></h3>
    <ul>
      <li>彻底移除旧版 <code>AlertDialog</code> 弹窗。高级菜单与改字面板改由纯 <code>Kotlin</code> 代码动态绘制，提供 <b>24dp 顶部圆角底栏卡片（<code>BottomSheetDialog</code>）</b>。</li>
      <li>输入框采用内凹式灰色扁平底色块，移除原生下划线；加入系统级 <code>selectableItemBackground</code> 水波纹按压反馈，文字修改面板顶部标有红色的“请注意文本格式”作为安全警示。</li>
    </ul>
  </blockquote>

  <blockquote>   < blockquote>
    <h3>⚡ 稳定性及内存优化 <code>[BugFix]</code></h3>
    <ul>   < ul>
      <li><b>Fix OOM：</b>抛弃旧版将图片全部硬编码进 Base64 导致单文件体积过大触发的 OOM（内存溢出）闪退问题，改为 HTML 与静态资源相对路径分离存储。</li>
      <li><b>Environment Patch：</b>在 WebView 加载结束（<code>onPageFinished</code>）时，自动注入针对 <code>window.wx</code>（微信）和 <code>window.campus</code>（今日校园 SDK）的底层核心运行环境补丁，消除前端环境格式缺失引发的白屏与加载死锁。</li>
    </ul>   < / ul>
  </blockquote>   < / blockquote>

  <blockquote>   < blockquote>
    <h3>🎨 主题与视觉规范 <code>[Design]</code></h3>
    <ul>   < ul>
      <li><b>应用图标：</b>采用严格的 <code>True Flat</code>（纯扁平）矢量几何规范，无渐变与黑圈阴影。使用纯原生 XML <code>Vector</code> 路径，将纯白的“图片底片”与“三条文本横线”符号利落交叠，衬托在 <code>#FF3B30</code> 现代标准全红底色中，任意高分屏缩放不失真。</li>
      <li><b>暗黑模式：</b>界面顶栏、气泡菜单以及底部卡片完美适配沉浸式深色主题（<code>#191919</code> / <code>#222222</code>）与浅色主题，状态栏图标随主题自动进行轻量与暗色避让切换。</li>
    </ul>   < / ul>
  </blockquote>   < / blockquote>

</   & lt;details>   < / details>   & lt;
