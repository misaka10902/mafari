package com.hyper.browser

import android.content.Context
import android.graphics.Rect
import android.text.Layout
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.HorizontalScrollView

/**
 * 单行输入宿主：HorizontalScrollView 里放一个「按内容自然宽度布局」的原生 EditText。
 *
 * 之前的做法是把 EditText 直接放进 AndroidView 并压成输入框宽度，交给 TextView 自己横向滚动。
 * 实测在部分机型上不工作：文字变长后布局宽度不跟着走，光标和文字都不移动，点按定位还会偏。
 *
 * 这里换一种可控的做法——EditText 多宽就多宽（单行、不折行、不自己滚动），
 * 横向滚动全部交给外层 ScrollView，并在每次文本/选区变化后按光标位置精确滚动。
 * 因为文本完整布局且坐标与像素一一对应，点按落光标也是准的。
 *
 * 滚动时机放在 onPreDraw（每帧 measure/layout 之后、draw 之前），而不是 post {}：
 * post 排在消息队列里，往往在下一帧布局之前就跑了，此时 EditText 的 Layout 还是
 * 上一次的（宽度更短），按它算出来的光标位置天然落后于新文本；等布局更新完，
 * 滚动值又不会重算，于是每敲一个字都慢一拍，累积起来就是「光标卡住一秒再跳」。
 * onPreDraw 里拿到的必定是当前文本的 Layout，同一帧内完成滚动，视觉上零延迟。
 */
internal class ScrollingEditTextHost(context: Context) {

    private val density = context.resources.displayMetrics.density
    private val defaultMarginPx = (10f * density).toInt()

    /** 待校正的滚动（选区/文本变化时置位，绘制前消费）。 */
    private var followPending = false
    private var followMarginPx = defaultMarginPx
    /** 置位时标脏的那个 View。用传入的实例而不是 editText 字段： */
    /** EditText 父类构造期间就会回调 onSelectionChanged，那时字段还没赋值。 */
    private var pendingView: View? = null

    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (followPending) {
            followPending = false
            val ok = runCatching { applyFollowCursor() }.getOrDefault(true)
            if (!ok) {
                // 这一帧还没准备好（尚未布局 / 视口宽度为 0），补一次标脏，下帧再来
                requestFollowCursor()
            }
        }
        true
    }

    val editText: EditText = object : EditText(context) {
        override fun onSelectionChanged(selStart: Int, selEnd: Int) {
            super.onSelectionChanged(selStart, selEnd)
            // 敲字、点按落光标、全选、拖手柄都会走到这里：
            // 只置位不立刻计算，真正的位置校正留给布局完成后的 onPreDraw。
            // 传 this：父类构造期间外部 editText 字段尚为 null，直接用它必崩。
            markFollowPending(this)
        }

        override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
            if (focused) markFollowPending(this)
        }
    }.apply {
        // 视觉由调用方按 miuix 输入框还原：去掉原生下划线 / 内边距 / 默认色
        background = null
        setPadding(0, 0, 0, 0)
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        isSingleLine = true
        // 关键：不要自己横向滚动，宽度由文字撑开，滚动交给外层
        setHorizontallyScrolling(false)
        isSaveEnabled = false
        // 单行不断词：文本变长时省掉换行 / 连字符计算，长 URL 输入更跟手
        breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
        hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
    }

    val scrollView: HorizontalScrollView = object : HorizontalScrollView(context) {
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            // 在 attach 之后再挂，避免 ViewTreeObserver 在 merge 时丢掉监听
            runCatching { viewTreeObserver.addOnPreDrawListener(preDrawListener) }
        }

        override fun onDetachedFromWindow() {
            runCatching { viewTreeObserver.removeOnPreDrawListener(preDrawListener) }
            super.onDetachedFromWindow()
        }
    }.apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        // 文本比视口短时把 EditText 撑满整条，点空白处也能聚焦
        isFillViewport = true
        addView(
            editText,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    /**
     * 请求在下一帧绘制前把光标带回可视区，[marginPx] 是与右边缘保持的余量。
     * 只是置位 + 标脏，不做任何测量，可以在 onTextChanged 里逐字调用。
     */
    fun followCursor(marginPx: Int = defaultMarginPx) {
        followMarginPx = marginPx
        markFollowPending(editText)
    }

    private fun markFollowPending(view: View) {
        pendingView = view
        if (followPending) return
        followPending = true
        // 标脏即可触发下一帧 traversals，onPreDraw 会把位置校正到当前文本
        view.invalidate()
    }

    private fun requestFollowCursor() {
        followPending = true
        pendingView?.invalidate()
    }

    private fun applyFollowCursor(): Boolean {
        val et = editText
        val viewport = scrollView.width
        if (viewport <= 0 || !et.isLaidOut || et.width <= 0) return false
        val text = et.text ?: return true
        val offset = et.selectionEnd.coerceIn(0, text.length)
        // 直接用 Paint 量「光标之前那一段」的像素宽度，而不是 Layout.getPrimaryHorizontal：
        // Layout 的水平坐标受当前行宽限制，文本超出时会被裁到行尾，算出来的滚动值偏小，
        // 表现出来就是「光标冲出框外好几个字才追一下」。Paint 量的是真实排版宽度，
        // 与 TextView 绘制时用的字体度量完全一致，不受可用宽度影响。
        val caret = et.paint.measureText(text, 0, offset).toInt() + et.paddingLeft
        val margin = followMarginPx.coerceIn(0, viewport / 3)
        val current = scrollView.scrollX
        val maxScroll = (et.width - viewport).coerceAtLeast(0)
        val target = when {
            caret > current + viewport - margin -> caret - viewport + margin
            caret < current + margin -> caret - margin
            else -> current
        }.coerceIn(0, maxScroll)
        if (target != current) scrollView.scrollTo(target, 0)
        return true
    }
}
