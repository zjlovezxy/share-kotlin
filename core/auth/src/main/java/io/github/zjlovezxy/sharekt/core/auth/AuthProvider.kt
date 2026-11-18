package io.github.zjlovezxy.sharekt.core.auth

/** 由 Application 实现，登录页据此拿到仓库，避免全局单例。 */
interface AuthProvider {
    val authRepository: AuthRepository
}
