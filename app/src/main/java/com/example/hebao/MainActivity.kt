package com.example.hebao

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var topBarContainer: RelativeLayout
    private lateinit var titleText: TextView
    private lateinit var urlText: TextView
    private lateinit var closeBtn: TextView
    private lateinit var moreBtn: TextView

    private var isDarkMode = true

    // 🌟 核心变量：把网页以字符串形式放在内存里，任由我们揉捏修改
    private var webContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        topBarContainer = findViewById(R.id.topBarContainer)
        titleText = findViewById(R.id.titleText)
        urlText = findViewById(R.id.urlText)
        closeBtn = findViewById(R.id.closeBtn)
        moreBtn = findViewById(R.id.moreBtn)

        setupWebView()
        applyTheme()

        closeBtn.setOnClickListener { finish() }
        moreBtn.setOnClickListener { showCustomPopupMenu(moreBtn) }

        // 软件启动时，直接加载内置的 assets 网页
        loadAssetHtml()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // 伪装微信浏览器 UA
            userAgentString = "$userAgentString MicroMessenger/8.0.45 NetType/WIFI Language/zh_CN"
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                titleText.text = "请假详情"
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                titleText.text = "请假详情"
            }
        }
    }

    /**
     * 🌟 修改后：从软件内部读取 HTML 资产，加载进内存并渲染
     */
    private fun loadAssetHtml() {
        try {
            // 打开 assets 目录下的 perfect_leave.html
            val inputStream = assets.open("perfect_leave.html")
            // 一次性读成字符串放入内存
            webContent = inputStream.bufferedReader().use { it.readText() }

            // 将内存中的数据丢给 WebView 渲染（MIME 类型改为 text/html）
            webView.loadDataWithBaseURL("file:///android_asset/", webContent, "text/html", "UTF-8", null)
            titleText.text = "请假详情"

        } catch (e: Exception) {
            Toast.makeText(this, "未能找到内置网页，请检查 assets 目录", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 🌟 修改后：交互式修改内存数据，绕过所有安全限制
     */
    private fun triggerEditHtml() {
        if (webContent.isEmpty()) {
            Toast.makeText(this, "网页数据未加载", Toast.LENGTH_SHORT).show()
            return
        }

        // 无论是普通的"病假"，还是部分 mhtml 特有的十六进制"=E7=97=85=E5=81=87"，一并干掉
        var modified = false

        if (webContent.contains("病假")) {
            webContent = webContent.replace("病假", "事假")
            modified = true
        }
        if (webContent.contains("=E7=97=85=E5=81=87")) {
            webContent = webContent.replace("=E7=97=85=E5=81=87", "=E4=BA=8B=E5=81=87") // 对应"事假"的编码
            modified = true
        }

        if (modified) {
            // 数据修改后，直接重新丢给 WebView 渲染（MIME 类型改为 text/html）
            webView.loadDataWithBaseURL("file:///android_asset/", webContent, "text/html", "UTF-8", null)
            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "当前页面没有找到需要修改的文字", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 弹出菜单核心控制（UI 样式完全未动）
     */
    private fun showCustomPopupMenu(anchorView: View) {
        val popupView = layoutInflater.inflate(R.layout.layout_custom_popup, null)

        val widthInDp = 95
        val density = resources.displayMetrics.density
        val widthInPx = (widthInDp * density).toInt()

        val popupWindow = PopupWindow(popupView, widthInPx, WindowManager.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 4f
        popupWindow.isOutsideTouchable = true

        val container = popupView.findViewById<LinearLayout>(R.id.popupContainer)
        val menuOpen = popupView.findViewById<TextView>(R.id.menuOpen)
        val menuEdit = popupView.findViewById<TextView>(R.id.menuEdit)
        val menuTheme = popupView.findViewById<TextView>(R.id.menuTheme)
        val line1 = popupView.findViewById<View>(R.id.line1)
        val line2 = popupView.findViewById<View>(R.id.line2)

        val popBgColor = if (isDarkMode) "#2B2B2B" else "#FFFFFF"
        val popTxtColor = if (isDarkMode) "#DFDFDF" else "#222222"
        val strokeColor = if (isDarkMode) "#444444" else "#E5E5E5"
        val lineColor = if (isDarkMode) "#1AFFFFFF" else "#1A000000"

        val drawable = container.background as GradientDrawable
        drawable.setColor(Color.parseColor(popBgColor))
        drawable.setStroke(1, Color.parseColor(strokeColor))

        val tColor = Color.parseColor(popTxtColor)
        menuOpen.setTextColor(tColor)
        menuEdit.setTextColor(tColor)
        menuTheme.setTextColor(tColor)
        line1.setBackgroundColor(Color.parseColor(lineColor))
        line2.setBackgroundColor(Color.parseColor(lineColor))

        // 因为不再需要外部选文件，这里把原本的"打开文件"改为"恢复原状/重载文件"
        menuOpen.text = "重载文件"
        menuEdit.text = "修改"
        menuTheme.text = if (isDarkMode) "浅色" else "深色"

        // 点击事件绑定新逻辑
        menuOpen.setOnClickListener {
            popupWindow.dismiss()
            loadAssetHtml() // 重新从 assets 读取原始文件，相当于重置
            Toast.makeText(this, "文件已恢复初始状态", Toast.LENGTH_SHORT).show()
        }
        menuEdit.setOnClickListener {
            popupWindow.dismiss()
            triggerEditHtml()
        }
        menuTheme.setOnClickListener {
            isDarkMode = !isDarkMode
            applyTheme()
            popupWindow.dismiss()
        }

        val offsetX = (-82 * density).toInt()
        val offsetY = (4 * density).toInt()
        popupWindow.showAsDropDown(anchorView, offsetX, offsetY)
    }

    /**
     * 基础核心 UI 主题切换
     */
    private fun applyTheme() {
        val bgColorStr = if (isDarkMode) "#191919" else "#EDEDED"
        val txtColorStr = if (isDarkMode) "#DFDFDF" else "#111111"
        val urlColorStr = if (isDarkMode) "#7F7F7F" else "#999999"

        val bgColor = Color.parseColor(bgColorStr)
        val txtColor = Color.parseColor(txtColorStr)
        val urlColor = Color.parseColor(urlColorStr)

        topBarContainer.setBackgroundColor(bgColor)
        titleText.setTextColor(txtColor)
        closeBtn.setTextColor(txtColor)
        urlText.setTextColor(urlColor)
        moreBtn.setTextColor(txtColor)

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = bgColor

        if (!isDarkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            window.decorView.systemUiVisibility = 0
        }
    }
}