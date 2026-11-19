package io.github.zjlovezxy.sharekt.core.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.zjlovezxy.sharekt.core.common.AppLog
import io.github.zjlovezxy.sharekt.core.network.ApiConfig
import kotlinx.coroutines.launch

/**
 * 用 WebView 走微博 H5 登录页，拿 SUB/SUBP cookie。
 * 不做 SSO —— 那需要官方 app key，重写版拿不到（也不该冒用）。
 *
 * 关键坑：SUB 是 HttpOnly，`CookieManager.getCookie()` 拿不到它；
 * 但 WebView 发出去的请求头里是有的，所以靠 shouldInterceptRequest 抓 `Cookie` 头。
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar

    private var handled = false

    /** host -> 该 host 请求头里的 Cookie（只留带 SUB 的）。 */
    private val captured = HashMap<String, String>()

    private val authRepository: AuthRepository?
        get() = (application as? AuthProvider)?.authRepository

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)

        val root = FrameLayout(this)
        webView = WebView(this)
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        progress = ProgressBar(this)
        root.addView(
            progress,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )
        setContentView(root)

        title = getString(R.string.sharekt_login_title)

        // 调试/自动化入口：am start ... --es cookie "SUB=xxx; SUBP=yyy" 直接注入登录态，
        // 不用在设备上手动输入账号密码（云真机验证省事，也不泄露密码）。
        val injected = intent?.getStringExtra(EXTRA_COOKIE)
        if (!injected.isNullOrBlank()) {
            handled = true
            progress.visibility = View.VISIBLE
            login(injected)
            return
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = LOGIN_USER_AGENT
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = View.GONE
                maybeLogin()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean = false

            /** WebView 的请求头里带 Cookie（含 HttpOnly 的 SUB），这是唯一可靠来源。 */
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val req = request ?: return null
                val host = req.url?.host ?: return null
                if (!host.endsWith("weibo.cn") && !host.endsWith("weibo.com")) return null
                val cookie = req.requestHeaders?.get("Cookie") ?: return null
                if (cookie.contains("${ApiConfig.COOKIE_SUB}=")) {
                    synchronized(captured) { captured[host] = cookie }
                }
                return null
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        })

        webView.loadUrl(ApiConfig.LOGIN_URL)
    }

    /** 只认 .weibo.cn 这一套 cookie：.weibo.com 是另一个会话，h5 接口不认。 */
    private fun pickCookie(): String? {
        synchronized(captured) {
            captured.entries
                .firstOrNull { it.key == "m.weibo.cn" || it.key == "weibo.cn" || it.key.endsWith(".weibo.cn") }
                ?.let { return it.value }
        }
        // 兜底：普通 cookie 能从 CookieManager 补，HttpOnly 的 SUB 通常补不到。
        for (url in FALLBACK_URLS) {
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrEmpty() && cookie.contains("${ApiConfig.COOKIE_SUB}=")) return cookie
        }
        return null
    }

    private fun maybeLogin() {
        if (handled) return
        val cookie = pickCookie() ?: return
        handled = true
        progress.visibility = View.VISIBLE
        login(cookie)
    }

    private fun login(cookie: String) {
        val repository = authRepository ?: run {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        lifecycleScope.launch {
            runCatching { repository.loginWithCookie(cookie) }
                .onSuccess { session ->
                    AppLog.d(TAG, "登录成功 uid=${session.uid}")
                    CookieManager.getInstance().flush()
                    setResult(RESULT_OK)
                    finish()
                }
                .onFailure { throwable ->
                    AppLog.w(TAG, "登录态落地失败", throwable)
                    handled = false
                    progress.visibility = View.GONE
                    Toast.makeText(this@LoginActivity, "登录未生效：${throwable.message}", Toast.LENGTH_LONG)
                        .show()
                }
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LoginActivity"
        const val EXTRA_COOKIE = "cookie"
        private val FALLBACK_URLS = listOf("https://m.weibo.cn/", "https://weibo.cn/")
        private const val LOGIN_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        fun intent(context: Context): Intent = Intent(context, LoginActivity::class.java)

        /** 调试用：直接带着 cookie 打开登录页。 */
        fun intent(context: Context, cookie: String): Intent =
            intent(context).putExtra(EXTRA_COOKIE, cookie)
    }
}
