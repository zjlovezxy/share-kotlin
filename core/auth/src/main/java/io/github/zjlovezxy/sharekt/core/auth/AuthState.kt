package io.github.zjlovezxy.sharekt.core.auth

/** 当前账号态；多账号时按 uid 隔离 CookieJar，禁止共用。 */
object AuthState {
    @Volatile
    var uid: String? = null
        private set

    val isLoggedIn: Boolean get() = uid != null

    fun login(uid: String) { this.uid = uid }

    fun logout() { uid = null }
}
