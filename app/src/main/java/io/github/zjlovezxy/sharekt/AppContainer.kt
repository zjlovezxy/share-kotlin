package io.github.zjlovezxy.sharekt

import android.content.Context
import io.github.zjlovezxy.sharekt.core.auth.AuthRepository
import io.github.zjlovezxy.sharekt.core.auth.AuthStore
import io.github.zjlovezxy.sharekt.core.data.JsonCache
import io.github.zjlovezxy.sharekt.core.data.WeiboRepository
import io.github.zjlovezxy.sharekt.core.network.MapiClient
import io.github.zjlovezxy.sharekt.core.network.PreferencesCookieStorage
import io.github.zjlovezxy.sharekt.core.network.WeiboApi
import io.github.zjlovezxy.sharekt.core.network.WeiboCookieJar
import java.io.File

/**
 * 手写依赖容器。不用 Hilt —— 这个规模的 App 上注解处理器只会拖慢编译。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val authStore: AuthStore = AuthStore(appContext)

    private val cookieJar = WeiboCookieJar(PreferencesCookieStorage(appContext))

    private val mapiClient = MapiClient(
        jar = cookieJar,
        sessionProvider = { authStore.load() },
    )

    private val weiboApi = WeiboApi(mapiClient)

    private val jsonCache = JsonCache(File(appContext.cacheDir, "mapi"))

    val weiboRepository = WeiboRepository(weiboApi, jsonCache)

    val authRepository = AuthRepository(mapiClient, cookieJar, authStore)
}
