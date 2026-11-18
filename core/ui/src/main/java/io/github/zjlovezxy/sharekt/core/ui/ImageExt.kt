package io.github.zjlovezxy.sharekt.core.ui

import android.widget.ImageView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder

/** Coil 3 的 View 扩展，统一占位色和淡入。 */
fun ImageView.loadUrl(url: String?) {
    if (url.isNullOrBlank()) {
        setBackgroundColor(0x0F000000)
        setImageDrawable(null)
        return
    }
    load(url) {
        crossfade(true)
        placeholder(R.drawable.bg_image_placeholder)
        error(R.drawable.bg_image_placeholder)
    }
}
