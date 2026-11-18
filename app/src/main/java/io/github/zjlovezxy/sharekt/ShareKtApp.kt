package io.github.zjlovezxy.sharekt

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import io.github.zjlovezxy.sharekt.core.auth.AuthProvider
import io.github.zjlovezxy.sharekt.core.auth.AuthRepository
import io.github.zjlovezxy.sharekt.core.data.RepositoryProvider
import io.github.zjlovezxy.sharekt.core.data.WeiboRepository

class ShareKtApp : Application(), RepositoryProvider, AuthProvider {

    lateinit var container: AppContainer
        private set

    override val weiboRepository: WeiboRepository
        get() = container.weiboRepository

    override val authRepository: AuthRepository
        get() = container.authRepository

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(OkHttpNetworkFetcherFactory()) }
                .crossfade(true)
                .build()
        }
    }
}
