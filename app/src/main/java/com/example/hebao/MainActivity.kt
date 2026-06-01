package com.example.hebao

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private var currentTemplateId = DEFAULT_TEMPLATE_ID

    private val releasePageUrl = "https://github.com/CAPTCHAAAAA/HeBao/releases"

    private enum class TemplateSource {
        ASSET,
        IMPORTED
    }

    private data class HtmlTemplate(
        val id: String,
        val title: String,
        val subtitle: String,
        val source: TemplateSource,
        val path: String
    )

    private data class TimeMatch(
        val value: String,
        val start: Int,
        val end: Int
    )

    companion object {
        private const val TEMPLATE_PREFS_NAME = "hebao_template_prefs"
        private const val SELECTED_TEMPLATE_ID_KEY = "selected_template_id"
        private const val HIDDEN_ASSET_TEMPLATE_IDS_KEY = "hidden_asset_template_ids"
        private const val IMPORTED_TEMPLATE_DIR_NAME = "imported_html_templates"
        private const val DEFAULT_TEMPLATE_ID = "asset:perfect_leave_img.html"
    }

    private val uiTypeface: Typeface by lazy {
        Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "图片读取失败", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

                runOnUiThread {
                    if (targetImageIndex >= 0) {
                        webView.evaluateJavascript(
                            "document.getElementsByTagName('img')[$targetImageIndex].src = 'data:$mimeType;base64,$base64';",
                            null
                        )
                        Toast.makeText(this@MainActivity, "图片修改成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "请先选择要替换的图片", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "图片修改失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private val pickHtmlTemplateLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        importHtmlTemplate(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isDarkMode = isSystemInDarkMode()

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

        loadSavedTemplateOrDefault()
    }

    private fun loadSavedTemplateOrDefault() {
        val savedId = getSharedPreferences(TEMPLATE_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SELECTED_TEMPLATE_ID_KEY, DEFAULT_TEMPLATE_ID)
            ?: DEFAULT_TEMPLATE_ID

        val template = getAllHtmlTemplates().firstOrNull { it.id == savedId }
            ?: getAllHtmlTemplates().firstOrNull { it.id == DEFAULT_TEMPLATE_ID }
            ?: getAllHtmlTemplates().firstOrNull()

        if (template != null) {
            loadHtmlTemplate(template, saveSelection = false, showToast = false)
        } else {
            loadCurrentHtmlVersion()
        }
    }

    private fun loadCurrentHtmlVersion() {
        val fileName = if (isImageVersion) {
            "perfect_leave_img.html"
        } else {
            "perfect_leave_noimg.html"
        }

        val fallbackTemplate = HtmlTemplate(
            id = "asset:$fileName",
            title = createTemplateTitle(fileName),
            subtitle = "内置资源",
            source = TemplateSource.ASSET,
            path = fileName
        )

        loadHtmlTemplate(fallbackTemplate, saveSelection = false, showToast = false)
    }

    private fun loadHtmlTemplate(
        template: HtmlTemplate,
        saveSelection: Boolean = true,
        showToast: Boolean = true
    ) {
        targetImageIndex = -1
        currentTemplateId = template.id
        isImageVersion = template.path.endsWith("perfect_leave_img.html", ignoreCase = true)

        if (saveSelection) {
            getSharedPreferences(TEMPLATE_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(SELECTED_TEMPLATE_ID_KEY, template.id)
                .apply()
        }

        when (template.source) {
            TemplateSource.ASSET -> {
                webView.loadUrl("file:///android_asset/${template.path}")
            }

            TemplateSource.IMPORTED -> {
                val file = File(template.path)
                if (file.exists()) {
                    webView.loadUrl(Uri.fromFile(file).toString())
                } else {
                    Toast.makeText(this, "模板文件不存在，已回退默认模板", Toast.LENGTH_SHORT).show()
                    getSharedPreferences(TEMPLATE_PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .remove(SELECTED_TEMPLATE_ID_KEY)
                        .apply()
                    loadCurrentHtmlVersion()
                    return
                }
            }
        }

        if (showToast) {
            Toast.makeText(this, "已切换模板：${template.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAllHtmlTemplates(): List<HtmlTemplate> {
        return getAssetHtmlTemplates() + getImportedHtmlTemplates()
    }

    private fun getAssetHtmlTemplates(includeHidden: Boolean = false): List<HtmlTemplate> {
        val templates = mutableListOf<HtmlTemplate>()
        val hiddenIds = if (includeHidden) emptySet() else getHiddenAssetTemplateIds()

        fun listAssetFolder(folder: String): List<String> {
            return try {
                assets.list(folder)?.toList().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun isAssetFolder(path: String): Boolean {
            return listAssetFolder(path).isNotEmpty()
        }

        fun scanAssetFolder(folder: String) {
            val names = listAssetFolder(folder)

            names.forEach { name ->
                val assetPath = if (folder.isBlank()) name else "$folder/$name"

                when {
                    name.endsWith(".html", ignoreCase = true) || name.endsWith(".htm", ignoreCase = true) -> {
                        templates.add(
                            HtmlTemplate(
                                id = "asset:$assetPath",
                                title = createTemplateTitle(assetPath),
                                subtitle = "内置资源",
                                source = TemplateSource.ASSET,
                                path = assetPath
                            )
                        )
                    }

                    isAssetFolder(assetPath) -> {
                        scanAssetFolder(assetPath)
                    }
                }
            }
        }

        scanAssetFolder("")

        return templates
            .distinctBy { it.id }
            .filter { includeHidden || it.id !in hiddenIds }
            .sortedWith(compareBy<HtmlTemplate> { it.path.count { ch -> ch == '/' } }.thenBy { it.path.lowercase(Locale.ROOT) })
    }

    private fun getImportedHtmlTemplates(): List<HtmlTemplate> {
        val dir = File(filesDir, IMPORTED_TEMPLATE_DIR_NAME)
        if (!dir.exists()) return emptyList()

        return dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".html", ignoreCase = true) || file.name.endsWith(".htm", ignoreCase = true))
        }.orEmpty()
            .sortedByDescending { it.lastModified() }
            .map { file ->
                HtmlTemplate(
                    id = "imported:${file.name}",
                    title = createTemplateTitle(file.name),
                    subtitle = file.absolutePath,
                    source = TemplateSource.IMPORTED,
                    path = file.absolutePath
                )
            }
    }

    private fun importHtmlTemplate(uri: Uri) {
        Thread {
            try {
                val rawName = resolveFileName(uri)
                val safeName = sanitizeTemplateFileName(rawName)
                val targetDir = File(filesDir, IMPORTED_TEMPLATE_DIR_NAME).apply {
                    if (!exists()) mkdirs()
                }
                val targetFile = createUniqueTemplateFile(targetDir, safeName)

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "HTML 文件读取失败", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val template = HtmlTemplate(
                    id = "imported:${targetFile.name}",
                    title = createTemplateTitle(targetFile.name),
                    subtitle = targetFile.absolutePath,
                    source = TemplateSource.IMPORTED,
                    path = targetFile.absolutePath
                )

                runOnUiThread {
                    loadHtmlTemplate(template, saveSelection = true, showToast = false)
                    Toast.makeText(this@MainActivity, "已导入并切换模板：${template.title}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "HTML 模板导入失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun getHiddenAssetTemplateIds(): MutableSet<String> {
        return getSharedPreferences(TEMPLATE_PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(HIDDEN_ASSET_TEMPLATE_IDS_KEY, emptySet())
            .orEmpty()
            .toMutableSet()
    }

    private fun saveHiddenAssetTemplateIds(ids: Set<String>) {
        getSharedPreferences(TEMPLATE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(HIDDEN_ASSET_TEMPLATE_IDS_KEY, ids)
            .apply()
    }

    private fun hideAssetTemplate(template: HtmlTemplate): Boolean {
        if (template.source != TemplateSource.ASSET) return false

        val hiddenIds = getHiddenAssetTemplateIds()
        hiddenIds.add(template.id)
        saveHiddenAssetTemplateIds(hiddenIds)

        if (template.id == currentTemplateId) {
            getSharedPreferences(TEMPLATE_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(SELECTED_TEMPLATE_ID_KEY)
                .apply()

            loadSavedTemplateOrDefault()
        }

        return true
    }

    private fun deleteImportedTemplate(template: HtmlTemplate): Boolean {
        if (template.source != TemplateSource.IMPORTED) return false

        val file = File(template.path)
        val deleted = !file.exists() || file.delete()

        if (deleted && template.id == currentTemplateId) {
            getSharedPreferences(TEMPLATE_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(SELECTED_TEMPLATE_ID_KEY)
                .apply()

            loadSavedTemplateOrDefault()
        }

        return deleted
    }

    private fun deleteOrHideTemplate(template: HtmlTemplate): Boolean {
        return when (template.source) {
            TemplateSource.ASSET -> hideAssetTemplate(template)
            TemplateSource.IMPORTED -> deleteImportedTemplate(template)
        }
    }

    private fun resetHiddenAssetTemplates() {
        saveHiddenAssetTemplateIds(emptySet())
        Toast.makeText(this, "已恢复隐藏的内置模板", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteTemplateConfirm(
        template: HtmlTemplate,
        parentDialog: BottomSheetDialog
    ) {
        val dialog = BottomSheetDialog(this)
        val isAssetTemplate = template.source == TemplateSource.ASSET

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22f), dp(18f), dp(22f), dp(26f))
            background = createSolidSheetDrawable(isDarkMode)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(42f), dp(4f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(22f)
            }
            background = createHandleDrawable(isDarkMode)
        }
        root.addView(handle)

        val title = TextView(this).apply {
            text = if (isAssetTemplate) "隐藏内置模板" else "删除导入模板"
            textSize = 19f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#15171A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(28f)
            ).apply {
                bottomMargin = dp(12f)
            }
        }
        root.addView(title)

        val infoBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createFieldBlockDrawable(isDarkMode)
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16f)
            }
        }

        val nameView = TextView(this).apply {
            text = template.title
            textSize = 14f
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#161A20"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8f)
            }
        }

        val detailView = TextView(this).apply {
            text = if (isAssetTemplate) {
                "重要提醒：内置 HTML 属于 APK 打包资源，运行时无法真正删除。这里的“删除”会把它从模板列表隐藏；文件仍在 assets 中，恢复隐藏后会重新出现。"
            } else {
                "将删除本地导入文件：\n${template.path}"
            }
            textSize = 12f
            typeface = uiTypeface
            setTextColor(Color.parseColor(if (isDarkMode) "#9CA8B7" else "#667085"))
        }

        infoBlock.addView(nameView)
        infoBlock.addView(detailView)
        root.addView(infoBlock)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48f)
            )
        }

        val cancelBtn = TextView(this).apply {
            text = "取消"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.parseColor(if (isDarkMode) "#DCE6F4" else "#334155"))
            background = createMenuCapsuleDrawable(isDarkMode)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
                rightMargin = dp(10f)
            }

            setOnClickListener {
                dialog.dismiss()
            }
        }

        val deleteBtn = TextView(this).apply {
            text = if (isAssetTemplate) "确认隐藏" else "确认删除"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.WHITE)
            background = createModernPrimaryButtonDrawable()
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
            }

            setOnClickListener {
                val success = deleteOrHideTemplate(template)
                dialog.dismiss()
                parentDialog.dismiss()

                if (success) {
                    val toastText = if (isAssetTemplate) {
                        "已隐藏内置模板：${template.title}"
                    } else {
                        "已删除导入模板：${template.title}"
                    }
                    Toast.makeText(this@MainActivity, toastText, Toast.LENGTH_SHORT).show()
                    showTemplateSwitchMenu()
                } else {
                    Toast.makeText(this@MainActivity, "操作失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        buttonRow.addView(cancelBtn)
        buttonRow.addView(deleteBtn)
        root.addView(buttonRow)

        attachCapsulePressEffect(infoBlock, cancelBtn, deleteBtn)

        dialog.setContentView(root)

        dialog.setOnShowListener {
            setupBottomSheet(dialog, root)
        }

        dialog.show()
    }

    private fun resolveFileName(uri: Uri): String {
        var result: String? = null

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    result = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {
        }

        return result?.takeIf { it.isNotBlank() }
            ?: "custom_template_${System.currentTimeMillis()}.html"
    }

    private fun sanitizeTemplateFileName(rawName: String): String {
        val baseName = rawName.substringAfterLast('/').substringAfterLast('\\')
        val fixedName = when {
            baseName.endsWith(".html", ignoreCase = true) || baseName.endsWith(".htm", ignoreCase = true) -> baseName
            else -> "$baseName.html"
        }

        return fixedName.replace(Regex("""[\\/:*?"<>|\s]+"""), "_")
    }

    private fun createUniqueTemplateFile(dir: File, fileName: String): File {
        val dotIndex = fileName.lastIndexOf('.')
        val namePart = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val extPart = if (dotIndex > 0) fileName.substring(dotIndex) else ".html"
        var candidate = File(dir, fileName)
        var index = 1

        while (candidate.exists()) {
            candidate = File(dir, "${namePart}_${index}${extPart}")
            index++
        }

        return candidate
    }

    private fun createTemplateTitle(pathOrName: String): String {
        return pathOrName
            .substringAfterLast('/')
            .takeIf { it.isNotBlank() }
            ?: "未命名模板.html"
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
                        window.wx = {
                            config:function(c){
                                if(c && c.success) c.success();
                            },
                            ready:function(f){
                                if(typeof f === 'function') f();
                            },
                            error:function(f){}
                        };

                        window.campus = {
                            config:function(c){
                                if(c && c.success) c.success();
                            }
                        };
                    })();
                """.trimIndent()

                webView.evaluateJavascript(patchJs, null)
            }
        }
    }

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
            runOnUiThread {
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableTextEditMode() {
        val js = """
            (function(){
                var oldImgBtn = document.getElementById('hebao-exit-img-btn');
                if(oldImgBtn) oldImgBtn.click();

                if(document.getElementById('hebao-exit-btn')) return;

                if(!document.getElementById('hebao-modern-style')) {
                    var style = document.createElement('style');
                    style.id = 'hebao-modern-style';
                    style.innerHTML = `
                        .hebao-editable-text {
                            position: relative !important;
                            display: inline !important;
                            padding: 1px 4px !important;
                            margin: 0 1px !important;
                            border-radius: 999px !important;
                            color: inherit !important;
                            cursor: pointer !important;
                            line-height: inherit !important;
                            vertical-align: baseline !important;
                            background: rgba(92, 125, 170, 0.14) !important;
                            box-shadow:
                                inset 0 0 0 1px rgba(92, 125, 170, 0.30),
                                0 2px 8px rgba(40, 70, 110, 0.08) !important;
                            -webkit-box-decoration-break: clone !important;
                            box-decoration-break: clone !important;
                            transition: background 0.14s ease, box-shadow 0.14s ease, transform 0.14s ease !important;
                        }

                        .hebao-editable-text:active {
                            background: rgba(92, 125, 170, 0.22) !important;
                            box-shadow:
                                inset 0 0 0 1px rgba(92, 125, 170, 0.48),
                                0 0 12px rgba(92, 125, 170, 0.18) !important;
                            transform: scale(0.985) !important;
                        }

                        #hebao-exit-btn {
                            position: fixed;
                            bottom: 40px;
                            left: 50%;
                            transform: translateX(-50%);
                            background: rgba(25, 28, 34, 0.96);
                            color: #F4F7FB;
                            padding: 13px 24px;
                            border-radius: 999px;
                            font-size: 15px;
                            font-weight: 700;
                            box-shadow:
                                inset 0 0 0 1px rgba(255,255,255,0.16),
                                0 14px 34px rgba(0,0,0,0.26);
                            z-index: 999999;
                            cursor: pointer;
                            text-align: center;
                            transition: transform 0.1s;
                            letter-spacing: 1px;
                        }

                        #hebao-exit-btn:active {
                            transform: translateX(-50%) scale(0.96);
                        }
                    `;
                    document.head.appendChild(style);
                }

                var count = 0;

                function shouldIgnoreElement(el) {
                    if(!el || el.nodeType !== 1) return false;

                    var tag = el.tagName;
                    if(['SCRIPT','STYLE','NOSCRIPT','TEXTAREA','INPUT','SELECT','OPTION','SVG','CANVAS'].indexOf(tag) >= 0) return true;
                    if(el.id === 'hebao-exit-btn' || el.id === 'hebao-exit-img-btn') return true;
                    if(el.classList && el.classList.contains('hebao-editable-text')) return true;
                    if(el.closest && el.closest('#hebao-exit-btn, #hebao-exit-img-btn, .hebao-editable-text')) return true;

                    var cs = window.getComputedStyle(el);
                    if(!cs || cs.display === 'none' || cs.visibility === 'hidden' || cs.opacity === '0') return true;

                    return false;
                }

                function isGlueText(text) {
                    var t = String(text || '').trim();
                    if(!t) return true;

                    if(/^[\s至到共：:，,。；;（）()【】\[\]\-—~～]+${'$'}/.test(t)) return true;

                    if(/^(实际休假时间|开始时间|结束时间|申请时间|提交时间|发起时间|当前审批人|申请人|请假原因|请假类型|审批人|导员|辅导员)\s*[：:：]?${'$'}/.test(t)) return true;

                    if(/^(实际休假时间|开始时间|结束时间|申请时间|提交时间|发起时间|当前审批人|申请人|请假原因|请假类型|审批人|导员|辅导员)\s*[：:：]\s*${'$'}/.test(t)) return true;

                    return false;
                }

                function appendRaw(frag, text) {
                    if(text) frag.appendChild(document.createTextNode(text));
                }

                function appendWrapped(frag, text) {
                    if(!text) return;

                    if(isGlueText(text)) {
                        appendRaw(frag, text);
                        return;
                    }

                    var span = document.createElement('span');
                    span.className = 'hebao-editable-text';
                    span.setAttribute('data-tid', 'text_' + count++);
                    span.textContent = text;

                    span.onclick = function(e) {
                        e.preventDefault();
                        e.stopPropagation();

                        var id = this.getAttribute('data-tid');
                        var currentTxt = this.textContent;

                        window.Android.onTextClicked(id, currentTxt);
                    };

                    frag.appendChild(span);
                }

                function splitLabelPrefix(frag, text) {
                    var labelReg = /^(.*?(?:实际休假时间|开始时间|结束时间|申请时间|提交时间|发起时间|当前审批人|申请人|请假原因|请假类型|审批人|导员|辅导员)\s*[：:：]\s*)([\s\S]*)${'$'}/;
                    var m = String(text || '').match(labelReg);

                    if(m && m[1]) {
                        appendRaw(frag, m[1]);
                        return m[2] || '';
                    }

                    return text;
                }

                function wrapTextNode(node) {
                    var txt = node.nodeValue;
                    if(!txt || txt.trim() === '') return null;

                    var tokenReg = /(?:\d{4}[年\/\-.]\d{1,2}[月\/\-.]\d{1,2}日?\s*\d{1,2}:\d{2}(?::\d{2})?|\d{1,2}[月\/\-.]\d{1,2}日?\s*\d{1,2}:\d{2}(?::\d{2})?|\d+\s*天\s*\d+\s*小时\s*\d+\s*分钟|\d+\s*小时\s*\d+\s*分钟|\d+\s*小时|\d+\s*分钟)/g;

                    var frag = document.createDocumentFragment();
                    var last = 0;
                    var matched = false;
                    var m;

                    while((m = tokenReg.exec(txt)) !== null) {
                        matched = true;

                        if(m.index > last) {
                            var before = txt.slice(last, m.index);
                            before = splitLabelPrefix(frag, before);

                            if(before) {
                                if(isGlueText(before)) appendRaw(frag, before);
                                else appendWrapped(frag, before);
                            }
                        }

                        appendWrapped(frag, m[0]);
                        last = m.index + m[0].length;
                    }

                    if(matched) {
                        if(last < txt.length) {
                            var after = txt.slice(last);
                            if(isGlueText(after)) appendRaw(frag, after);
                            else appendWrapped(frag, after);
                        }

                        return frag;
                    }

                    var match = txt.match(/^(\s*)([\s\S]*?\S)(\s*)${'$'}/);
                    if(!match) return null;

                    var leading = match[1] || '';
                    var core = match[2] || '';
                    var trailing = match[3] || '';

                    if(core.trim() === '') return null;

                    if(leading) appendRaw(frag, leading);

                    core = splitLabelPrefix(frag, core);
                    if(core) {
                        if(isGlueText(core)) appendRaw(frag, core);
                        else appendWrapped(frag, core);
                    }

                    if(trailing) appendRaw(frag, trailing);

                    return frag;
                }

                function walk(node) {
                    if(node.nodeType === 3) {
                        return wrapTextNode(node);
                    }

                    if(node.nodeType === 1) {
                        if(shouldIgnoreElement(node)) return null;

                        var childNodes = Array.from(node.childNodes);

                        for(var i = 0; i < childNodes.length; i++) {
                            var child = childNodes[i];
                            var replacement = walk(child);

                            if(replacement) {
                                node.replaceChild(replacement, child);
                            }
                        }
                    }

                    return null;
                }

                walk(document.body);

                if(count > 0) {
                    window.Android.showToast('已进入点选文本编辑');

                    var exitBtn = document.createElement('div');
                    exitBtn.id = 'hebao-exit-btn';
                    exitBtn.innerText = '退出点选编辑';

                    exitBtn.onclick = function() {
                        var allSpans = document.querySelectorAll('.hebao-editable-text');

                        for(var i = 0; i < allSpans.length; i++) {
                            var span = allSpans[i];
                            var text = document.createTextNode(span.textContent);
                            span.parentNode.replaceChild(text, span);
                        }

                        this.remove();
                        window.Android.showToast('已退出模式');
                    };

                    document.body.appendChild(exitBtn);
                } else {
                    window.Android.showToast('当前页面没有可编辑文本');
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun showTextEditDialog(elementId: String, currentText: String) {
        val dialog = BottomSheetDialog(this)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22f), dp(18f), dp(22f), dp(26f))
            background = createSolidSheetDrawable(isDarkMode)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val handleBar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(42f), dp(4f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(22f)
            }
            background = createHandleDrawable(isDarkMode)
        }
        rootLayout.addView(handleBar)

        val titleView = TextView(this).apply {
            text = "编辑文本"
            textSize = 18f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#15171A"))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(26f)
            ).apply {
                bottomMargin = dp(7f)
            }
        }
        rootLayout.addView(titleView)

        val tipView = TextView(this).apply {
            text = "时间文字可用滚轮生成；多个时间会先选择替换对象"
            textSize = 12f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#8D96A3" else "#68707A"))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(18f)
            ).apply {
                bottomMargin = dp(16f)
            }
        }
        rootLayout.addView(tipView)

        val editText = EditText(this).apply {
            setText(currentText)
            setSelection(currentText.length)
            setTextColor(Color.parseColor(if (isDarkMode) "#F4F4F4" else "#171717"))
            setHintTextColor(Color.parseColor(if (isDarkMode) "#8C8C8C" else "#8A8A8A"))
            textSize = 15f
            typeface = uiTypeface
            background = createAcrylicInputDrawable(isDarkMode)
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14f)
            }
        }
        rootLayout.addView(editText)

        val toolRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42f)
            ).apply {
                bottomMargin = dp(16f)
            }
        }

        val timeToolBtn = TextView(this).apply {
            text = "时间滚轮"
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.parseColor(if (isDarkMode) "#DCE6F4" else "#334155"))
            background = createMenuCapsuleDrawable(isDarkMode)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
                rightMargin = dp(10f)
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                showTextTimeTool(editText)
            }
        }

        val clearBtn = TextView(this).apply {
            text = "清空"
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.parseColor(if (isDarkMode) "#DCE6F4" else "#334155"))
            background = createMenuCapsuleDrawable(isDarkMode)

            layoutParams = LinearLayout.LayoutParams(
                dp(72f),
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                editText.setText("")
            }
        }

        toolRow.addView(timeToolBtn)
        toolRow.addView(clearBtn)
        rootLayout.addView(toolRow)

        val saveBtn = TextView(this).apply {
            text = "完 成"
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER
            background = createModernPrimaryButtonDrawable()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48f)
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                val newText = editText.text.toString()

                if (newText.isNotEmpty()) {
                    val safeText = newText
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                        .replace("\r", "")

                    val updateJs = "document.querySelector('[data-tid=\"$elementId\"]').textContent = '$safeText';"
                    webView.evaluateJavascript(updateJs, null)
                    dialog.dismiss()
                }
            }
        }
        rootLayout.addView(saveBtn)

        attachCapsulePressEffect(timeToolBtn, clearBtn, saveBtn)

        dialog.setContentView(rootLayout)

        dialog.setOnShowListener {
            setupBottomSheet(dialog, rootLayout)
        }

        dialog.show()
    }

    private fun showTextTimeTool(targetEditText: EditText) {
        val text = targetEditText.text.toString()
        val matches = findTimeMatches(text)

        when {
            matches.isEmpty() -> {
                val selectedStart = targetEditText.selectionStart.coerceAtLeast(0)
                val selectedEnd = targetEditText.selectionEnd.coerceAtLeast(selectedStart)

                showIosWheelDateTimePicker(
                    template = "MM-dd HH:mm",
                    currentText = text,
                    onPicked = { generated ->
                        val result = if (text.isEmpty()) {
                            generated
                        } else {
                            text.replaceRange(selectedStart, selectedEnd, generated)
                        }

                        targetEditText.setText(result)
                        targetEditText.setSelection((selectedStart + generated.length).coerceAtMost(result.length))
                    }
                )
            }

            matches.size == 1 -> {
                val match = matches.first()
                showIosWheelDateTimePicker(
                    template = match.value,
                    currentText = match.value,
                    onPicked = { generated ->
                        val result = text.replaceRange(match.start, match.end, generated)
                        targetEditText.setText(result)
                        targetEditText.setSelection((match.start + generated.length).coerceAtMost(result.length))
                    }
                )
            }

            else -> {
                showTimeTargetChooser(
                    sourceText = text,
                    matches = matches,
                    onChoose = { chosenIndex, chosenMatch ->
                        showIosWheelDateTimePicker(
                            template = chosenMatch.value,
                            currentText = chosenMatch.value,
                            onPicked = { generated ->
                                val freshText = targetEditText.text.toString()
                                val freshMatches = findTimeMatches(freshText)
                                val freshMatch = freshMatches.getOrNull(chosenIndex) ?: chosenMatch

                                val result = if (freshMatch.end <= freshText.length) {
                                    freshText.replaceRange(freshMatch.start, freshMatch.end, generated)
                                } else {
                                    generated
                                }

                                targetEditText.setText(result)
                                targetEditText.setSelection((freshMatch.start + generated.length).coerceAtMost(result.length))
                            }
                        )
                    }
                )
            }
        }
    }

    private fun showTimeTargetChooser(
        sourceText: String,
        matches: List<TimeMatch>,
        onChoose: (Int, TimeMatch) -> Unit
    ) {
        val dialog = BottomSheetDialog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22f), dp(18f), dp(22f), dp(26f))
            background = createSolidSheetDrawable(isDarkMode)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(42f), dp(4f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(22f)
            }
            background = createHandleDrawable(isDarkMode)
        }
        root.addView(handle)

        val title = TextView(this).apply {
            text = "选择要修改的时间"
            textSize = 18f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#15171A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(26f)
            ).apply {
                bottomMargin = dp(6f)
            }
        }
        root.addView(title)

        val preview = TextView(this).apply {
            text = sourceText
            textSize = 12f
            typeface = uiTypeface
            setTextColor(Color.parseColor(if (isDarkMode) "#8D96A3" else "#68707A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14f)
            }
        }
        root.addView(preview)

        matches.forEachIndexed { index, match ->
            val row = createAdvancedMenuRow(
                title = "时间 ${index + 1}",
                subtitle = match.value
            ).apply {
                setOnClickListener {
                    dialog.dismiss()
                    onChoose(index, match)
                }
            }

            root.addView(row)
            attachCapsulePressEffect(row)
        }

        dialog.setContentView(root)

        dialog.setOnShowListener {
            setupBottomSheet(dialog, root)
        }

        dialog.show()
    }

    private fun findTimeMatches(text: String): List<TimeMatch> {
        val regex = Regex("""(?:\d{4}[年/\-.]\d{1,2}[月/\-.]\d{1,2}日?\s*\d{1,2}:\d{2}(?::\d{2})?|\d{1,2}[月/\-.]\d{1,2}日?\s*\d{1,2}:\d{2}(?::\d{2})?)""")

        return regex.findAll(text).map {
            TimeMatch(
                value = it.value,
                start = it.range.first,
                end = it.range.last + 1
            )
        }.toList()
    }

    private fun showIosWheelDateTimePicker(
        template: String,
        currentText: String,
        onPicked: (String) -> Unit
    ) {
        val calendar = parseCalendarFromText(currentText)
            ?: parseCalendarFromText(template)
            ?: Calendar.getInstance()

        val dialog = BottomSheetDialog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22f), dp(18f), dp(22f), dp(26f))
            background = createSolidSheetDrawable(isDarkMode)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(42f), dp(4f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(18f)
            }
            background = createHandleDrawable(isDarkMode)
        }
        root.addView(handle)

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34f)
            ).apply {
                bottomMargin = dp(12f)
            }
        }

        val title = TextView(this).apply {
            text = "时间滚轮"
            textSize = 18f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#15171A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                rightMargin = dp(12f)
            }
        }

        val preview = TextView(this).apply {
            textSize = 13f
            typeface = uiTypeface
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            includeFontPadding = false
            setSingleLine(true)
            setTextColor(Color.parseColor(if (isDarkMode) "#9CA8B7" else "#667085"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
            }
        }

        topRow.addView(title)
        topRow.addView(preview)
        root.addView(topRow)

        val wheelContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = createWheelContainerDrawable(isDarkMode)
            setPadding(dp(8f), dp(10f), dp(8f), dp(10f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(250f)
            ).apply {
                bottomMargin = dp(16f)
            }
        }

        val dateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                weight = 1f
                bottomMargin = dp(8f)
            }
        }

        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                weight = 1f
            }
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val minYear = currentYear - 20
        val maxYear = currentYear + 20

        val yearPicker = createWheelPicker(
            min = minYear,
            max = maxYear,
            value = calendar.get(Calendar.YEAR),
            formatter = { "${it}年" }
        )

        val monthPicker = createWheelPicker(
            min = 1,
            max = 12,
            value = calendar.get(Calendar.MONTH) + 1,
            formatter = { "${it}月" }
        )

        val dayPicker = createWheelPicker(
            min = 1,
            max = calendar.getActualMaximum(Calendar.DAY_OF_MONTH),
            value = calendar.get(Calendar.DAY_OF_MONTH),
            formatter = { "${it}日" }
        )

        val hourPicker = createWheelPicker(
            min = 0,
            max = 23,
            value = calendar.get(Calendar.HOUR_OF_DAY),
            formatter = { String.format(Locale.CHINA, "%02d时", it) }
        )

        val minutePicker = createWheelPicker(
            min = 0,
            max = 59,
            value = calendar.get(Calendar.MINUTE),
            formatter = { String.format(Locale.CHINA, "%02d分", it) }
        )

        val secondPicker = createWheelPicker(
            min = 0,
            max = 59,
            value = calendar.get(Calendar.SECOND),
            formatter = { String.format(Locale.CHINA, "%02d秒", it) }
        )

        fun selectedCalendar(): Calendar {
            return Calendar.getInstance().apply {
                set(Calendar.YEAR, yearPicker.value)
                set(Calendar.MONTH, monthPicker.value - 1)
                set(Calendar.DAY_OF_MONTH, dayPicker.value)
                set(Calendar.HOUR_OF_DAY, hourPicker.value)
                set(Calendar.MINUTE, minutePicker.value)
                set(Calendar.SECOND, secondPicker.value)
                set(Calendar.MILLISECOND, 0)
            }
        }

        fun updateDayRange() {
            val oldDay = dayPicker.value
            val temp = Calendar.getInstance().apply {
                set(Calendar.YEAR, yearPicker.value)
                set(Calendar.MONTH, monthPicker.value - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }

            val maxDay = temp.getActualMaximum(Calendar.DAY_OF_MONTH)
            val fixedDay = oldDay.coerceIn(1, maxDay)

            dayPicker.displayedValues = null
            dayPicker.minValue = 1
            dayPicker.maxValue = maxDay
            dayPicker.value = fixedDay
            dayPicker.displayedValues = Array(maxDay) { "${it + 1}日" }
        }

        fun generatedText(): String {
            return formatCalendarLikeOriginal(
                original = template,
                calendar = selectedCalendar()
            )
        }

        fun updatePreview() {
            updateDayRange()
            preview.text = generatedText()
        }

        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            updatePreview()
        }

        yearPicker.setOnValueChangedListener(listener)
        monthPicker.setOnValueChangedListener(listener)
        dayPicker.setOnValueChangedListener(listener)
        hourPicker.setOnValueChangedListener(listener)
        minutePicker.setOnValueChangedListener(listener)
        secondPicker.setOnValueChangedListener(listener)

        dateRow.addView(yearPicker)
        dateRow.addView(monthPicker)
        dateRow.addView(dayPicker)

        timeRow.addView(hourPicker)
        timeRow.addView(minutePicker)
        timeRow.addView(secondPicker)

        wheelContainer.addView(dateRow)
        wheelContainer.addView(timeRow)
        root.addView(wheelContainer)

        updatePreview()

        val doneBtn = TextView(this).apply {
            text = "插入时间"
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.WHITE)
            background = createModernPrimaryButtonDrawable()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48f)
            )
            isClickable = true
            isFocusable = true

            setOnClickListener {
                onPicked(generatedText())
                dialog.dismiss()
            }
        }

        attachCapsulePressEffect(doneBtn)
        root.addView(doneBtn)

        dialog.setContentView(root)

        dialog.setOnShowListener {
            setupBottomSheet(dialog, root)
        }

        dialog.show()
    }

    private fun createWheelPicker(
        min: Int,
        max: Int,
        value: Int,
        formatter: (Int) -> String
    ): NumberPicker {
        return NumberPicker(this).apply {
            minValue = min
            maxValue = max
            displayedValues = Array(max - min + 1) { formatter(min + it) }
            this.value = value.coerceIn(min, max)
            wrapSelectorWheel = true
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            setTextColorCompat(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#171A1F"))

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
            }
        }
    }

    private fun NumberPicker.setTextColorCompat(color: Int) {
        try {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is EditText) {
                    child.setTextColor(color)
                    child.textSize = 15f
                    child.typeface = uiTypeface
                    child.gravity = Gravity.CENTER
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun parseCalendarFromText(text: String): Calendar? {
        val raw = text.trim()
        if (raw.isEmpty()) return null

        var normalized = raw
            .replace("年", "-")
            .replace("月", "-")
            .replace("日", " ")
            .replace("/", "-")
            .replace(".", "-")
            .replace(Regex("\\s+"), " ")
            .trim()

        normalized = normalized.replace(
            Regex("^(\\d{4}-\\d{1,2}-\\d{1,2})(\\d{1,2}:\\d{2})"),
            "$1 $2"
        )

        normalized = normalized.replace(
            Regex("^(\\d{1,2}-\\d{1,2})(\\d{1,2}:\\d{2})"),
            "$1 $2"
        )

        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "MM-dd HH:mm:ss",
            "MM-dd HH:mm",
            "yyyy-MM-dd"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.CHINA)
                sdf.isLenient = false
                val date = sdf.parse(normalized) ?: continue
                val cal = Calendar.getInstance()
                cal.time = date

                if (pattern.startsWith("MM")) {
                    cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                }

                return cal
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun formatCalendarLikeOriginal(
        original: String,
        calendar: Calendar
    ): String {
        val raw = original.trim()

        val hasYear = Regex("\\d{4}").containsMatchIn(raw)
        val hasSeconds = Regex("\\d{1,2}:\\d{2}:\\d{2}").containsMatchIn(raw)
        val hasTime = Regex("\\d{1,2}:\\d{2}").containsMatchIn(raw)

        val pattern = when {
            raw.contains("年") && hasSeconds -> "yyyy年MM月dd日 HH:mm:ss"
            raw.contains("年") && hasTime -> "yyyy年MM月dd日 HH:mm"
            raw.contains("年") -> "yyyy年MM月dd日"

            raw.contains("月") && !hasYear && hasSeconds -> "MM月dd日 HH:mm:ss"
            raw.contains("月") && !hasYear && hasTime -> "MM月dd日 HH:mm"
            raw.contains("月") && !hasYear -> "MM月dd日"
            raw.contains("月") && hasSeconds -> "yyyy年MM月dd日 HH:mm:ss"
            raw.contains("月") && hasTime -> "yyyy年MM月dd日 HH:mm"
            raw.contains("月") -> "yyyy年MM月dd日"

            raw.contains("/") && !hasYear && hasSeconds -> "MM/dd HH:mm:ss"
            raw.contains("/") && !hasYear && hasTime -> "MM/dd HH:mm"
            raw.contains("/") && !hasYear -> "MM/dd"
            raw.contains("/") && hasSeconds -> "yyyy/MM/dd HH:mm:ss"
            raw.contains("/") && hasTime -> "yyyy/MM/dd HH:mm"
            raw.contains("/") -> "yyyy/MM/dd"

            raw.contains(".") && !hasYear && hasSeconds -> "MM.dd HH:mm:ss"
            raw.contains(".") && !hasYear && hasTime -> "MM.dd HH:mm"
            raw.contains(".") && !hasYear -> "MM.dd"
            raw.contains(".") && hasSeconds -> "yyyy.MM.dd HH:mm:ss"
            raw.contains(".") && hasTime -> "yyyy.MM.dd HH:mm"
            raw.contains(".") -> "yyyy.MM.dd"

            raw.contains("-") && !hasYear && hasSeconds -> "MM-dd HH:mm:ss"
            raw.contains("-") && !hasYear && hasTime -> "MM-dd HH:mm"
            raw.contains("-") && !hasYear -> "MM-dd"
            raw.contains("-") && hasSeconds -> "yyyy-MM-dd HH:mm:ss"
            raw.contains("-") && hasTime -> "yyyy-MM-dd HH:mm"
            raw.contains("-") -> "yyyy-MM-dd"

            hasSeconds -> "yyyy-MM-dd HH:mm:ss"
            hasTime -> "yyyy-MM-dd HH:mm"

            else -> "MM-dd HH:mm"
        }

        return SimpleDateFormat(pattern, Locale.CHINA).format(calendar.time)
    }

    private fun enableImageEditMode() {
        val js = """
            (function(){
                var oldTextBtn = document.getElementById('hebao-exit-btn');
                if(oldTextBtn) oldTextBtn.click();

                if(document.getElementById('hebao-exit-img-btn')) return;

                if(!document.getElementById('hebao-modern-style-img')) {
                    var style = document.createElement('style');
                    style.id = 'hebao-modern-style-img';
                    style.innerHTML = `
                        .hebao-editable-img {
                            box-shadow: 0 0 0 3px rgba(105, 145, 205, 0.62), 0 10px 24px rgba(60, 90, 130, 0.18) !important;
                            border-radius: 10px !important;
                            cursor: pointer !important;
                            transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1) !important;
                            filter: brightness(0.96) saturate(1.02);
                        }

                        .hebao-editable-img:active {
                            transform: scale(0.96) !important;
                            filter: brightness(0.88);
                        }

                        #hebao-exit-img-btn {
                            position: fixed;
                            bottom: 40px;
                            left: 50%;
                            transform: translateX(-50%);
                            background: rgba(25, 28, 34, 0.96);
                            color: #F4F7FB;
                            padding: 13px 24px;
                            border-radius: 999px;
                            font-size: 15px;
                            font-weight: 700;
                            box-shadow:
                                inset 0 0 0 1px rgba(255,255,255,0.16),
                                0 14px 34px rgba(0,0,0,0.26);
                            z-index: 999999;
                            cursor: pointer;
                            text-align: center;
                            transition: transform 0.1s;
                            letter-spacing: 1px;
                        }

                        #hebao-exit-img-btn:active {
                            transform: translateX(-50%) scale(0.96);
                        }
                    `;
                    document.head.appendChild(style);
                }

                var imgs = document.getElementsByTagName('img');

                if(imgs.length === 0) {
                    window.Android.showToast('当前页面没有图片');
                    return;
                }

                window.Android.showToast('已进入连续换图模式');

                for(var i = 0; i < imgs.length; i++) {
                    imgs[i].classList.add('hebao-editable-img');

                    (function(index){
                        imgs[index].onclick = function(e) {
                            e.preventDefault();
                            e.stopPropagation();
                            window.Android.onImageClicked(index);
                        };
                    })(i);
                }

                var exitBtn = document.createElement('div');
                exitBtn.id = 'hebao-exit-img-btn';
                exitBtn.innerText = '退出换图模式';

                exitBtn.onclick = function() {
                    var allImgs = document.querySelectorAll('.hebao-editable-img');

                    for(var j = 0; j < allImgs.length; j++) {
                        allImgs[j].classList.remove('hebao-editable-img');
                        allImgs[j].onclick = null;
                    }

                    this.remove();
                    window.Android.showToast('已退出模式');
                };

                document.body.appendChild(exitBtn);
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun showAdvancedEditMenu() {
        val dialog = BottomSheetDialog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(16f), dp(20f), dp(24f))
            clipToPadding = false
            background = createSolidSheetDrawable(isDarkMode)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(42f), dp(4f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(20f)
            }
            background = createHandleDrawable(isDarkMode)
        }
        root.addView(handle)

        val title = TextView(this).apply {
            text = "高级编辑"
            textSize = 20f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#F4F6FA" else "#15171A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(30f)
            ).apply {
                bottomMargin = dp(18f)
            }
        }
        root.addView(title)

        val freeTextRow = createAdvancedMenuRow(
            title = "点选文本编辑",
            subtitle = "点击页面文字修改，输入框内可使用时间滚轮"
        ).apply {
            setOnClickListener {
                dialog.dismiss()
                enableTextEditMode()
            }
        }

        val imageRow = createAdvancedMenuRow(
            title = "更改页面图片",
            subtitle = "进入连续换图模式"
        ).apply {
            setOnClickListener {
                dialog.dismiss()
                enableImageEditMode()
            }
        }

        val switchRow = createAdvancedMenuRow(
            title = "切换模板",
            subtitle = "选择内置模板，或导入并保存 HTML 模板"
        ).apply {
            setOnClickListener {
                dialog.dismiss()
                showTemplateSwitchMenu()
            }
        }

        root.addView(freeTextRow)
        root.addView(imageRow)
        root.addView(switchRow)

        attachCapsulePressEffect(freeTextRow, imageRow, switchRow)

        dialog.setContentView(root)

        dialog.setOnShowListener {
            setupBottomSheet(dialog, root)
        }

        dialog.show()
    }

    private fun showTemplateSwitchMenu() {
        val dialog = BottomSheetDialog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(16f), dp(20f), dp(24f))
            clipToPadding = false
            background = createSolidSheetDrawable(isDarkMode)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(42f), dp(4f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(20f)
            }
            background = createHandleDrawable(isDarkMode)
        }
        root.addView(handle)

        val title = TextView(this).apply {
            text = "切换模板"
            textSize = 20f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#F4F6FA" else "#15171A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(30f)
            ).apply {
                bottomMargin = dp(8f)
            }
        }
        root.addView(title)

        val tip = TextView(this).apply {
            text = "删除内置 HTML 仅隐藏；导入模板会删除本地文件"
            textSize = 12f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#9DA8B6" else "#68707A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(18f)
            ).apply {
                bottomMargin = dp(14f)
            }
        }
        root.addView(tip)

        val scrollView = ScrollView(this).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val listRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipToPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val importRow = createAdvancedMenuRow(
            title = "导入 HTML 模板",
            subtitle = "选择 .html 文件，导入后会自动保存并切换"
        ).apply {
            setOnClickListener {
                dialog.dismiss()
                pickHtmlTemplateLauncher.launch(arrayOf("text/html", "text/*", "*/*"))
            }
        }
        listRoot.addView(importRow)
        attachCapsulePressEffect(importRow)

        val hiddenAssetTemplateCount = getHiddenAssetTemplateIds().size
        if (hiddenAssetTemplateCount > 0) {
            val restoreRow = createAdvancedMenuRow(
                title = "恢复隐藏的内置模板",
                subtitle = "已隐藏 $hiddenAssetTemplateCount 个；恢复后会重新出现在列表"
            ).apply {
                setOnClickListener {
                    dialog.dismiss()
                    resetHiddenAssetTemplates()
                    showTemplateSwitchMenu()
                }
            }

            listRoot.addView(restoreRow)
            attachCapsulePressEffect(restoreRow)
        }

        val assetTemplates = getAssetHtmlTemplates()
        if (assetTemplates.isNotEmpty()) {
            listRoot.addView(createTemplateSectionLabel("内置模板"))

            assetTemplates.forEach { template ->
                val row = createTemplateManageRow(
                    template = template,
                    onUse = {
                        dialog.dismiss()
                        loadHtmlTemplate(template)
                    },
                    onDelete = {
                        showDeleteTemplateConfirm(template, dialog)
                    }
                )

                listRoot.addView(row)
            }
        }

        val importedTemplates = getImportedHtmlTemplates()
        if (importedTemplates.isNotEmpty()) {
            listRoot.addView(createTemplateSectionLabel("已导入模板"))

            importedTemplates.forEach { template ->
                val row = createTemplateManageRow(
                    template = template,
                    onUse = {
                        dialog.dismiss()
                        loadHtmlTemplate(template)
                    },
                    onDelete = {
                        showDeleteTemplateConfirm(template, dialog)
                    }
                )

                listRoot.addView(row)
            }
        }

        scrollView.addView(listRoot)
        root.addView(scrollView)

        dialog.setContentView(root)

        dialog.setOnShowListener {
            setupBottomSheet(dialog, root)
        }

        dialog.show()
    }

    private fun createTemplateManageRow(
        template: HtmlTemplate,
        onUse: () -> Unit,
        onDelete: () -> Unit
    ): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipToPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(66f)
            ).apply {
                bottomMargin = dp(10f)
            }
        }

        val selectRow = createAdvancedMenuRow(
            title = if (template.id == currentTemplateId) "✓ ${template.title}" else template.title,
            subtitle = template.subtitle
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
                rightMargin = dp(8f)
            }

            setOnClickListener {
                onUse()
            }
        }

        val deleteBtn = TextView(this).apply {
            text = "删除"
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.parseColor(if (isDarkMode) "#DCE6F4" else "#334155"))
            background = createMenuCapsuleDrawable(isDarkMode)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                dp(62f),
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            setOnClickListener {
                onDelete()
            }
        }

        wrapper.addView(selectRow)
        wrapper.addView(deleteBtn)

        attachCapsulePressEffect(selectRow, deleteBtn)

        return wrapper
    }

    private fun createTemplateSectionLabel(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 12f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            letterSpacing = 0.02f
            setTextColor(Color.parseColor(if (isDarkMode) "#8D96A3" else "#68707A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(18f)
            ).apply {
                topMargin = dp(4f)
                bottomMargin = dp(8f)
            }
        }
    }

    private fun createAdvancedMenuRow(
        title: String,
        subtitle: String
    ): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipToPadding = false
            setPadding(dp(16f), 0, dp(14f), 0)
            background = createMenuCapsuleDrawable(isDarkMode)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(66f)
            ).apply {
                bottomMargin = dp(10f)
            }
        }

        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
            }
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 15f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#171A1F"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(22f)
            )
        }

        val subtitleView = TextView(this).apply {
            text = subtitle
            textSize = 12f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#9DA8B6" else "#68707A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(18f)
            )
        }

        val arrow = TextView(this).apply {
            text = "›"
            textSize = 24f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.parseColor(if (isDarkMode) "#8FA0B6" else "#8A94A3"))
            layoutParams = LinearLayout.LayoutParams(
                dp(26f),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        textBox.addView(titleView)
        textBox.addView(subtitleView)
        row.addView(textBox)
        row.addView(arrow)

        return row
    }

    private fun showCustomPopupMenu(anchorView: View) {
        val popupWidth = dp(174f)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
            background = createPopupPanelDrawable(isDarkMode)
            clipToPadding = false
        }

        val menuOpen = createPopupMenuItem("刷新重置")
        val menuEdit = createPopupMenuItem("高级编辑")
        val menuTheme = createPopupMenuItem(if (isDarkMode) "浅色模式" else "深色模式")
        val releaseItem = createPopupMenuItem("发布页")

        container.addView(menuOpen)
        container.addView(menuEdit)
        container.addView(menuTheme)
        container.addView(releaseItem)

        val popupWindow = PopupWindow(
            container,
            popupWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.isClippingEnabled = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = dp(16f).toFloat()
        }

        attachCapsulePressEffect(menuOpen, menuEdit, menuTheme, releaseItem)

        menuOpen.setOnClickListener {
            popupWindow.dismiss()
            webView.reload()
        }

        menuEdit.setOnClickListener {
            popupWindow.dismiss()
            showAdvancedEditMenu()
        }

        menuTheme.setOnClickListener {
            popupWindow.dismiss()
            isDarkMode = !isDarkMode
            applyTheme()
        }

        releaseItem.setOnClickListener {
            popupWindow.dismiss()
            showReleasePageDialog()
        }

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        val screenWidth = resources.displayMetrics.widthPixels
        val margin = dp(8f)

        var x = location[0] + anchorView.width - popupWidth - dp(4f)

        if (x < margin) {
            x = margin
        }

        if (x + popupWidth > screenWidth - margin) {
            x = screenWidth - popupWidth - margin
        }

        val y = location[1] + anchorView.height + dp(6f)

        popupWindow.showAtLocation(window.decorView, Gravity.NO_GRAVITY, x, y)
        playCapsulePopupEnter(container)
    }

    private fun createPopupMenuItem(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 13f
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            setPadding(dp(16f), 0, dp(16f), 0)
            setTextColor(Color.parseColor(if (isDarkMode) "#EEF1F5" else "#171A1F"))
            letterSpacing = 0.02f
            typeface = uiTypeface
            background = createMenuCapsuleDrawable(isDarkMode)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42f)
            ).apply {
                bottomMargin = dp(7f)
            }
        }
    }

    private fun showReleasePageDialog() {
        val dialog = BottomSheetDialog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22f), dp(18f), dp(22f), dp(26f))
            background = createSolidSheetDrawable(isDarkMode)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(42f), dp(4f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(22f)
            }
            background = createHandleDrawable(isDarkMode)
        }
        root.addView(handle)

        val title = TextView(this).apply {
            text = "发布页"
            textSize = 19f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#15171A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(28f)
            ).apply {
                bottomMargin = dp(14f)
            }
        }
        root.addView(title)

        val urlBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createFieldBlockDrawable(isDarkMode)
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16f)
            }

            setOnClickListener {
                dialog.dismiss()
                openReleasePage()
            }

            setOnLongClickListener {
                copyReleasePageUrl()
                true
            }
        }

        val label = TextView(this).apply {
            text = "点按打开，长按复制"
            textSize = 12f
            typeface = uiTypeface
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.parseColor(if (isDarkMode) "#9CA8B7" else "#667085"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(18f)
            ).apply {
                bottomMargin = dp(8f)
            }
        }

        val value = TextView(this).apply {
            text = releasePageUrl
            textSize = 14f
            typeface = uiTypeface
            setTextColor(Color.parseColor(if (isDarkMode) "#F2F4F8" else "#161A20"))
        }

        urlBlock.addView(label)
        urlBlock.addView(value)
        root.addView(urlBlock)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48f)
            )
        }

        val openBtn = TextView(this).apply {
            text = "打开发布页"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.WHITE)
            background = createModernPrimaryButtonDrawable()
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
                rightMargin = dp(10f)
            }

            setOnClickListener {
                dialog.dismiss()
                openReleasePage()
            }
        }

        val copyBtn = TextView(this).apply {
            text = "复制链接"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = uiTypeface
            includeFontPadding = false
            setTextColor(Color.parseColor(if (isDarkMode) "#DCE6F4" else "#334155"))
            background = createMenuCapsuleDrawable(isDarkMode)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                weight = 1f
            }

            setOnClickListener {
                copyReleasePageUrl()
            }
        }

        buttonRow.addView(openBtn)
        buttonRow.addView(copyBtn)
        root.addView(buttonRow)

        attachCapsulePressEffect(urlBlock, openBtn, copyBtn)

        dialog.setContentView(root)

        dialog.setOnShowListener {
            setupBottomSheet(dialog, root)
        }

        dialog.show()
    }

    private fun openReleasePage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releasePageUrl))
            startActivity(intent)
        } catch (_: Exception) {
            copyReleasePageUrl()
            Toast.makeText(this, "未找到可打开链接的应用，已复制链接", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyReleasePageUrl() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("HeBao 发布页", releasePageUrl)
        )
        Toast.makeText(this, "发布页链接已复制", Toast.LENGTH_SHORT).show()
    }

    private fun applyTheme() {
        val bgColor = if (isDarkMode) {
            Color.parseColor("#191919")
        } else {
            Color.parseColor("#F2F2F2")
        }

        val txtColor = if (isDarkMode) {
            Color.parseColor("#DFDFDF")
        } else {
            Color.parseColor("#111111")
        }

        val subColor = if (isDarkMode) {
            Color.parseColor("#7F7F7F")
        } else {
            Color.parseColor("#8A8A8A")
        }

        topBarContainer.setBackgroundColor(bgColor)
        titleText.setTextColor(txtColor)
        closeBtn.setTextColor(txtColor)
        moreBtn.setTextColor(txtColor)
        urlText.setTextColor(subColor)

        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor

        var flags = 0

        if (!isDarkMode) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        window.decorView.systemUiVisibility = flags
    }

    private fun setupBottomSheet(dialog: BottomSheetDialog, animatedRoot: View) {
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.setBackgroundColor(Color.TRANSPARENT)

        val lp = bottomSheet?.layoutParams
        lp?.width = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet?.layoutParams = lp

        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setDimAmount(if (isDarkMode) 0.18f else 0.10f)
        }

        playSheetEnter(animatedRoot)
    }

    private fun attachCapsulePressEffect(vararg views: View?) {
        views.forEach { view ->
            view ?: return@forEach

            view.isClickable = true
            view.isFocusable = true

            view.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(0.968f)
                            .scaleY(0.968f)
                            .alpha(0.88f)
                            .setDuration(70)
                            .start()
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(135)
                            .start()
                    }
                }

                false
            }
        }
    }

    private fun playCapsulePopupEnter(view: View?) {
        view ?: return

        view.alpha = 0f
        view.scaleX = 0.94f
        view.scaleY = 0.94f
        view.translationY = dp(5f).toFloat()

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(165)
            .start()
    }

    private fun playSheetEnter(view: View?) {
        view ?: return

        view.alpha = 0f
        view.translationY = dp(18f).toFloat()

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(185)
            .start()
    }

    private fun createSolidSheetDrawable(dark: Boolean): Drawable {
        val radius = dp(28f).toFloat()

        return GradientDrawable().apply {
            setColor(
                if (dark) Color.parseColor("#1C1D21")
                else Color.parseColor("#F7F7F5")
            )

            setStroke(
                dp(1f),
                if (dark) Color.parseColor("#343741")
                else Color.WHITE
            )

            cornerRadii = floatArrayOf(
                radius, radius,
                radius, radius,
                0f, 0f,
                0f, 0f
            )
        }
    }

    private fun createPopupPanelDrawable(dark: Boolean): Drawable {
        val radius = dp(26f).toFloat()

        return GradientDrawable().apply {
            setColor(
                if (dark) Color.parseColor("#1C1D21")
                else Color.parseColor("#F7F7F5")
            )

            setStroke(
                dp(1f),
                if (dark) Color.parseColor("#343741")
                else Color.WHITE
            )

            cornerRadius = radius
        }
    }

    private fun createMenuCapsuleDrawable(dark: Boolean): Drawable {
        val radius = dp(20f).toFloat()

        return GradientDrawable().apply {
            setColor(
                if (dark) Color.parseColor("#2A2C33")
                else Color.parseColor("#FFFFFF")
            )

            setStroke(
                dp(1f),
                if (dark) Color.parseColor("#3A3D47")
                else Color.parseColor("#E5E7EB")
            )

            cornerRadius = radius
        }
    }

    private fun createAcrylicPanelDrawable(
        dark: Boolean,
        topCornersOnly: Boolean,
        compact: Boolean
    ): Drawable {
        return createSolidSheetDrawable(dark)
    }

    private fun createMinimalPopupPanelDrawable(dark: Boolean): Drawable {
        return createPopupPanelDrawable(dark)
    }

    private fun createStableCapsuleDrawable(
        dark: Boolean,
        active: Boolean
    ): Drawable {
        return createMenuCapsuleDrawable(dark)
    }

    private fun createAcrylicInputDrawable(dark: Boolean): Drawable {
        val radius = dp(16f).toFloat()

        return GradientDrawable().apply {
            setColor(
                if (dark) Color.parseColor("#25272D")
                else Color.WHITE
            )

            setStroke(
                dp(1f),
                if (dark) Color.parseColor("#3A3D47")
                else Color.parseColor("#E5E7EB")
            )

            cornerRadius = radius
        }
    }

    private fun createFieldBlockDrawable(dark: Boolean): Drawable {
        val radius = dp(18f).toFloat()

        return GradientDrawable().apply {
            setColor(
                if (dark) Color.parseColor("#25272D")
                else Color.WHITE
            )

            setStroke(
                dp(1f),
                if (dark) Color.parseColor("#3A3D47")
                else Color.parseColor("#E5E7EB")
            )

            cornerRadius = radius
        }
    }

    private fun createWheelContainerDrawable(dark: Boolean): Drawable {
        val radius = dp(22f).toFloat()

        return GradientDrawable().apply {
            setColor(
                if (dark) Color.parseColor("#25272D")
                else Color.WHITE
            )

            setStroke(
                dp(1f),
                if (dark) Color.parseColor("#3A3D47")
                else Color.parseColor("#E5E7EB")
            )

            cornerRadius = radius
        }
    }

    private fun createModernPrimaryButtonDrawable(): Drawable {
        val radius = dp(18f).toFloat()

        val base = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#263449"),
                Color.parseColor("#405775"),
                Color.parseColor("#607A9E")
            )
        ).apply {
            cornerRadius = radius
        }

        return RippleDrawable(
            ColorStateList.valueOf(Color.argb(70, 255, 255, 255)),
            base,
            null
        )
    }

    private fun createHandleDrawable(dark: Boolean): Drawable {
        return GradientDrawable().apply {
            setColor(
                if (dark) Color.parseColor("#51545F")
                else Color.parseColor("#D1D5DB")
            )
            cornerRadius = dp(999f).toFloat()
        }
    }

    private fun isSystemInDarkMode(): Boolean {
        return try {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightMode == Configuration.UI_MODE_NIGHT_YES
        } catch (_: Exception) {
            true
        }
    }

    private fun dp(value: Float): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}