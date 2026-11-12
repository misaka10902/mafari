package com.hyper.browser

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * 文本选择快捷菜单：走系统 [ActionMode]，和网页里长按文字完全同一条路径。
 *
 * WebView 长按选中文字时，Chromium 内部就是 `startActionMode(TYPE_FLOATING)`，
 * 系统（MIUI / 澎湃OS）会用自己的 FloatingToolbar 渲染这条 ActionMode，
 * 于是自动带上「问小爱 / 剪切 / 复制 / 粘贴 / ⋮」的澎湃样式。
 *
 * Compose 的输入框不经过原生 TextView，默认浮条由 Compose 自己绘制，
 * 因此不会被系统接管。这里把 Compose 的 [TextToolbar] 换成 ActionMode 实现，
 * 让搜索框的菜单也交给系统渲染，而不是自绘一套仿制品。
 */
class SystemTextToolbar(private val view: View) : TextToolbar {

    private var actionMode: ActionMode? = null
    private var statusValue = TextToolbarStatus.Hidden

    override val status: TextToolbarStatus
        get() = statusValue

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        actionMode?.finish()
        actionMode = null

        val callback = object : ActionMode.Callback2() {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                var order = 1
                onCutRequested?.let { menu.add(MENU_GROUP, MENU_CUT, order++, android.R.string.cut) }
                onCopyRequested?.let { menu.add(MENU_GROUP, MENU_COPY, order++, android.R.string.copy) }
                onPasteRequested?.let { menu.add(MENU_GROUP, MENU_PASTE, order++, android.R.string.paste) }
                onSelectAllRequested?.let { menu.add(MENU_GROUP, MENU_SELECT_ALL, order++, android.R.string.selectAll) }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val action = when (item.itemId) {
                    MENU_CUT -> onCutRequested
                    MENU_COPY -> onCopyRequested
                    MENU_PASTE -> onPasteRequested
                    MENU_SELECT_ALL -> onSelectAllRequested
                    else -> null
                } ?: return false
                mode.finish()
                action()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                if (actionMode === mode) actionMode = null
                statusValue = TextToolbarStatus.Hidden
            }

            /** 把浮条锚定到选区上（系统据此避让键盘、贴边翻转）。 */
            override fun onGetContentRect(mode: ActionMode?, v: View?, outRect: android.graphics.Rect) {
                outRect.set(
                    rect.left.toInt(),
                    rect.top.toInt(),
                    rect.right.toInt(),
                    rect.bottom.toInt(),
                )
            }
        }

        statusValue = TextToolbarStatus.Shown
        actionMode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
        if (actionMode == null) statusValue = TextToolbarStatus.Hidden
    }

    override fun hide() {
        actionMode?.finish()
        actionMode = null
        statusValue = TextToolbarStatus.Hidden
    }

    private companion object {
        const val MENU_GROUP = 0
        const val MENU_CUT = android.R.id.cut
        const val MENU_COPY = android.R.id.copy
        const val MENU_PASTE = android.R.id.paste
        const val MENU_SELECT_ALL = android.R.id.selectAll
    }
}
