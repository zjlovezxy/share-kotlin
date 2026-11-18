package io.github.zjlovezxy.sharekt.core.ui

/**
 * 跨 feature 跳转只走这个接口：feature 之间零依赖，
 * 宿主（app）实现它并持有 NavController。
 */
interface Navigator {
    fun openStatus(statusId: String)
    fun openSettings()
    fun openLogin()
}
