package io.github.zjlovezxy.sharekt.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

/**
 * 微博九宫格：1 张按 4:3 展示，2/4 张两列，其余三列。
 * 自己算是因为 ConstraintLayout 做不出"数量决定列数"的网格。
 */
class NineGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {

    var gap: Int = 0
    var singleAspect: Float = 0.75f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val count = childCount
        if (count == 0) {
            setMeasuredDimension(width, 0)
            return
        }
        val cols = columns(count)
        val cellSize = if (cols == 1) (width * singleAspect).toInt() else (width - gap * (cols - 1)) / cols
        for (index in 0 until count) {
            val child = getChildAt(index)
            val cellWidth = if (cols == 1) width else cellSize
            child.measure(
                MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(cellSize, MeasureSpec.EXACTLY),
            )
        }
        val rows = (count + cols - 1) / cols
        setMeasuredDimension(width, rows * cellSize + (rows - 1) * gap)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        if (count == 0) return
        val width = r - l
        val cols = columns(count)
        val cellSize = if (cols == 1) (width * singleAspect).toInt() else (width - gap * (cols - 1)) / cols
        for (index in 0 until count) {
            val row = index / cols
            val col = index % cols
            val left = col * (cellSize + gap)
            val top = row * (cellSize + gap)
            val cellWidth = if (cols == 1) width else cellSize
            getChildAt(index).layout(left, top, left + cellWidth, top + cellSize)
        }
    }

    private fun columns(count: Int): Int = when {
        count == 1 -> 1
        count == 2 || count == 4 -> 2
        else -> 3
    }
}
