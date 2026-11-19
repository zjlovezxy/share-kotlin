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
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar

    private var handled = false

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
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = View.GONE
                if (url != null) tryFinish(url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean = false
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
                    setResult(RESULT_OK)
                    finish()
                }
                .onFailure { throwable ->
                    AppLog.w(TAG, "登录态落地失败", throwable)
                    handled = false
                    progress.visibility = View.GONE
                    Toast.makeText(this@LoginActivity, "登录信息保存失败：${throwable.message}", Toast.LENGTH_LONG)
                        .show()
                }
        }
    }

    private fun tryFinish(url: String) {
        if (handled) return
        val cookie = CookieManager.getInstance().getCookie(url) ?: return
        if (!cookie.contains("${ApiConfig.COOKIE_SUB}=")) return
        handled = true
        progress.visibility = View.VISIBLE
        login(cookie)
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
        private const val LOGIN_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        fun intent(context: Context): Intent = Intent(context, LoginActivity::class.java)

        /** 调试用：直接带着 cookie 打开登录页。 */
        fun intent(context: Context, cookie: String): Intent =
            intent(context).putExtra(EXTRA_COOKIE, cookie)
    }
}
