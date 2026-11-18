package io.github.zjlovezxy.sharekt.core.ui

import android.content.Context
import androidx.annotation.DimenRes

object Dimens {

    /** 设计稿基准宽度，单位 dp。 */
    const val DESIGN_WIDTH_DP = 360f

    fun px(context: Context, @DimenRes id: Int): Int = context.resources.getDimensionPixelSize(id)
}
