package io.github.zjlovezxy.sharekt.core.data

/** 由 Application 实现；feature 通过 requireActivity().application as RepositoryProvider 取仓库。 */
interface RepositoryProvider {
    val weiboRepository: WeiboRepository
}
