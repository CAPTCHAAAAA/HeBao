package com.example.hebao

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var topBarContainer: RelativeLayout
    private lateinit var titleText: TextView
    private lateinit var urlText: TextView
    private lateinit var closeBtn: TextView
    private lateinit var moreBtn: TextView

    private var isDarkMode = true
    private var isImageVersion = true
    private var targetImageIndex = -1

    // ================= 图片选取与替换 =================
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Thread {
                try {
                    val inputStream = contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val mimeType = contentResolver.getType(it) ?: "image/jpeg"
                    runOnUiThread {
                        webView.evaluateJavascript("document.getElementsByTagName('img')[$targetImageIndex].src = 'data:$mimeType;base64,$base64';", null)
                        Toast.makeText(this@MainActivity, "图片修改成功", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }.start()
        }
    }

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

        loadCurrentHtmlVersion()
    }

    private fun loadCurrentHtmlVersion() {
        val fileName = if (isImageVersion) "perfect_leave_img.html" else "perfect_leave_noimg.html"
        webView.loadUrl("file:///android_asset/$fileName")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val patchJs = """
                    (function(){
                        window.wx = {config:function(c){if(c && c.success) c.success()},ready:function(f){f()},error:function(f){}};
                        window.campus = {config:function(c){if(c && c.success) c.success()}};
                    })();
                """.trimIndent()
                webView.evaluateJavascript(patchJs, null)
            }
        }
    }

    // ================= 安卓与 JS 通信桥梁 =================
    inner class WebAppInterface {
        @JavascriptInterface
        fun onImageClicked(index: Int) {
            targetImageIndex = index
            pickImageLauncher.launch("image/*")
        }

        @JavascriptInterface
        fun onTextClicked(elementId: String, currentText: String) {
            runOnUiThread {
                showTextEditDialog(elementId, currentText)
            }
        }

        @JavascriptInterface
        fun showToast(msg: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
        }
    }

    // ================= 核心：原子级点触改字（现代扁平风） =================
    private fun enableTextEditMode() {
        val js = """
            (function(){
                if(!document.getElementById('hebao-modern-style')) {
                    var style = document.createElement('style');
                    style.id = 'hebao-modern-style';
                    style.innerHTML = `
                        .hebao-editable-text {
                            background-color: rgba(0, 122, 255, 0.12) !important;
                            border-radius: 6px !important;
                            box-shadow: 0 0 0 2px rgba(0, 122, 255, 0.15) !important;
                            cursor: pointer !important;
                            transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1) !important;
                            display: inline;
                            padding: 1px 2px;
                        }
                        .hebao-editable-text:active {
                            background-color: rgba(0, 122, 255, 0.25) !important;
                            box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.4) !important;
                            transform: scale(0.97) !important;
                        }
                    `;
                    document.head.appendChild(style);
                }

                var count = 0;
                function walk(node) {
                    if (node.nodeType === 3) {
                        var txt = node.nodeValue;
                        if (txt.trim() !== '') {
                            var span = document.createElement('span');
                            span.className = 'hebao-editable-text';
                            span.setAttribute('data-tid', 'text_' + count++);
                            span.textContent = txt;
                            
                            span.onclick = function(e) {
                                e.preventDefault();
                                e.stopPropagation();
                                var id = this.getAttribute('data-tid');
                                var currentTxt = this.textContent;
                                
                                var allSpans = document.querySelectorAll('.hebao-editable-text');
                                for(var i=0; i<allSpans.length; i++) {
                                    allSpans[i].classList.remove('hebao-editable-text');
                                    allSpans[i].onclick = null;
                                }
                                window.Android.onTextClicked(id, currentTxt);
                            };
                            return span;
                        }
                    } else if (node.nodeType === 1 && !['SCRIPT','STYLE','NOSCRIPT'].includes(node.tagName)) {
                        if (node.className && typeof node.className === 'string' && node.className.includes('hebao-editable-text')) return;
                        var childNodes = Array.from(node.childNodes);
                        for (var i = 0; i < childNodes.length; i++) {
                            var child = childNodes[i];
                            var replacement = walk(child);
                            if (replacement) node.replaceChild(replacement, child);
                        }
                    }
                    return null;
                }
                
                walk(document.body);
                if(count > 0) window.Android.showToast('请点触需要修改的文字');
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // 🌟 四级编辑面板：极致扁平化文本编辑框
    private fun showTextEditDialog(elementId: String, currentText: String) {
        val dialog = BottomSheetDialog(this)

        val panelColor = if (isDarkMode) "#222222" else "#FFFFFF"
        val inputColor = if (isDarkMode) "#2A2A2A" else "#F2F2F7"
        val textColor = if (isDarkMode) "#EEEEEE" else "#1C1C1E"
        val hintColor = if (isDarkMode) "#777777" else "#999999"

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 30, 60, 60)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(panelColor))
                cornerRadii = floatArrayOf(48f, 48f, 48f, 48f, 0f, 0f, 0f, 0f)
            }
        }

        val handleBar = View(this).apply {
            val lp = LinearLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (4 * resources.displayMetrics.density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (24 * resources.displayMetrics.density).toInt()
            }
            layoutParams = lp
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isDarkMode) "#444444" else "#D1D1D1"))
                cornerRadius = 10f
            }
        }
        rootLayout.addView(handleBar)

        val titleView = TextView(this).apply {
            text = "请注意文本格式"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor(if (isDarkMode) "#FF453A" else "#FF3B30"))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }
            layoutParams = lp
        }
        rootLayout.addView(titleView)

        val editText = EditText(this).apply {
            setText(currentText)
            setSelection(currentText.length)
            setTextColor(Color.parseColor(textColor))
            setHintTextColor(Color.parseColor(hintColor))
            textSize = 15f
            background = GradientDrawable().apply {
                setColor(Color.parseColor(inputColor))
                cornerRadius = (12 * resources.displayMetrics.density)
            }
            setPadding(
                (16 * resources.displayMetrics.density).toInt(),
                (14 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (14 * resources.displayMetrics.density).toInt()
            )
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (24 * resources.displayMetrics.density).toInt()
            }
            layoutParams = lp
        }
        rootLayout.addView(editText)

        val saveBtn = TextView(this).apply {
            text = "完 成"
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#007AFF"))
                cornerRadius = (12 * resources.displayMetrics.density)
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (48 * resources.displayMetrics.density).toInt())
            layoutParams = lp
            isClickable = true

            setOnClickListener {
                val newText = editText.text.toString()
                if (newText.isNotEmpty()) {
                    val safeText = newText.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                    val updateJs = "document.querySelector('[data-tid=\"$elementId\"]').textContent = '$safeText';"
                    webView.evaluateJavascript(updateJs, null)
                    dialog.dismiss()
                }
            }
        }
        rootLayout.addView(saveBtn)

        dialog.setContentView(rootLayout)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }

    // ================= 核心：现代感点触换图 =================
    private fun enableImageEditMode() {
        val js = """
            (function(){
                if(!document.getElementById('hebao-modern-style-img')) {
                    var style = document.createElement('style');
                    style.id = 'hebao-modern-style-img';
                    style.innerHTML = `
                        .hebao-editable-img {
                            box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.7), 0 8px 16px rgba(0, 122, 255, 0.2) !important;
                            border-radius: 8px !important;
                            cursor: pointer !important;
                            transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1) !important;
                            filter: brightness(0.95);
                        }
                        .hebao-editable-img:active {
                            transform: scale(0.95) !important;
                            filter: brightness(0.85);
                        }
                    `;
                    document.head.appendChild(style);
                }
                var imgs = document.getElementsByTagName('img');
                if(imgs.length === 0) { window.Android.showToast('当前页面没有图片'); return; }
                
                window.Android.showToast('请点击需要替换的图片');
                for(var i = 0; i < imgs.length; i++) {
                    imgs[i].classList.add('hebao-editable-img');
                    (function(index){
                        imgs[index].onclick = function(e) {
                            e.preventDefault();
                            e.stopPropagation();
                            for(var j=0; j<imgs.length; j++) {
                                imgs[j].classList.remove('hebao-editable-img');
                                imgs[j].onclick=null;
                            }
                            window.Android.onImageClicked(index);
                        };
                    })(i);
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // 🌟 三级高级菜单：动态智能 Toast 反馈
    private fun showAdvancedEditMenu() {
        val dialog = BottomSheetDialog(this)

        val bgColor = if (isDarkMode) "#222222" else "#FFFFFF"
        val txtColor = if (isDarkMode) "#EEEEEE" else "#1C1C1E"

        // 根布局
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 30, 0, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadii = floatArrayOf(48f, 48f, 48f, 48f, 0f, 0f, 0f, 0f)
            }
        }

        // 顶部小胶囊指示器
        val handleBar = View(this).apply {
            val lp = LinearLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (4 * resources.displayMetrics.density).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }
            layoutParams = lp
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isDarkMode) "#444444" else "#D1D1D1"))
                cornerRadius = 10f
            }
        }
        rootLayout.addView(handleBar)

        fun createMenuRow(iconStr: String, titleStr: String, onClick: () -> Unit): LinearLayout {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val pad = (18 * resources.displayMetrics.density).toInt()
                setPadding(pad + 20, pad, pad, pad)
                isClickable = true
                isFocusable = true
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { onClick() }
            }

            val iconView = TextView(this).apply {
                text = iconStr
                textSize = 20f
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = (16 * resources.displayMetrics.density).toInt()
                }
                layoutParams = lp
            }
            row.addView(iconView)

            val titleView = TextView(this).apply {
                text = titleStr
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor(txtColor))
            }
            row.addView(titleView)

            return row
        }

        rootLayout.addView(createMenuRow("📝", "修改文本") {
            dialog.dismiss()
            enableTextEditMode()
        })
        rootLayout.addView(createMenuRow("🖼️", "更改图片") {
            dialog.dismiss()
            enableImageEditMode()
        })

        // 🌟 核心修改点：动态提示当前切换到了哪个版本
        rootLayout.addView(createMenuRow("🔄", "切换版本") {
            dialog.dismiss()
            isImageVersion = !isImageVersion
            loadCurrentHtmlVersion()

            val toastMsg = if (isImageVersion) "已切换至有图版本" else "已切换至无图版本"
            Toast.makeText(this@MainActivity, toastMsg, Toast.LENGTH_SHORT).show()
        })

        dialog.setContentView(rootLayout)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
        }
        dialog.show()
    }

    // ================= 原有完美的二级菜单 UI 逻辑 =================
    private fun showCustomPopupMenu(anchorView: View) {
        val popupView = layoutInflater.inflate(R.layout.layout_custom_popup, null)
        val density = resources.displayMetrics.density
        val popupWindow = PopupWindow(popupView, (95 * density).toInt(), WindowManager.LayoutParams.WRAP_CONTENT, true)
        popupWindow.elevation = 4f
        popupWindow.isOutsideTouchable = true

        val container = popupView.findViewById<LinearLayout>(R.id.popupContainer)
        val menuOpen = popupView.findViewById<TextView>(R.id.menuOpen)
        val menuEdit = popupView.findViewById<TextView>(R.id.menuEdit)
        val menuTheme = popupView.findViewById<TextView>(R.id.menuTheme)

        val popBgColor = if (isDarkMode) "#2B2B2B" else "#FFFFFF"
        val popTxtColor = if (isDarkMode) "#DFDFDF" else "#222222"
        (container.background as GradientDrawable).setColor(Color.parseColor(popBgColor))

        val tColor = Color.parseColor(popTxtColor)
        menuOpen.setTextColor(tColor); menuEdit.setTextColor(tColor); menuTheme.setTextColor(tColor)

        menuOpen.text = "刷新重置"
        menuEdit.text = "高级编辑"
        menuTheme.text = if (isDarkMode) "浅色模式" else "深色模式"

        menuOpen.setOnClickListener { popupWindow.dismiss(); webView.reload() }
        menuEdit.setOnClickListener {
            popupWindow.dismiss()
            showAdvancedEditMenu()
        }
        menuTheme.setOnClickListener {
            isDarkMode = !isDarkMode
            applyTheme()
            popupWindow.dismiss()
        }
        popupWindow.showAsDropDown(anchorView, (-82 * density).toInt(), (4 * density).toInt())
    }

    private fun applyTheme() {
        val bgColor = Color.parseColor(if (isDarkMode) "#191919" else "#EDEDED")
        val txtColor = Color.parseColor(if (isDarkMode) "#DFDFDF" else "#111111")
        topBarContainer.setBackgroundColor(bgColor)
        titleText.setTextColor(txtColor)
        closeBtn.setTextColor(txtColor)
        moreBtn.setTextColor(txtColor)
        urlText.setTextColor(Color.parseColor(if (isDarkMode) "#7F7F7F" else "#999999"))
        window.statusBarColor = bgColor
        window.decorView.systemUiVisibility = if (!isDarkMode) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
    }
}