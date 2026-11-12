package com.hyper.browser

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.WorldClock
import top.yukonga.miuix.kmp.icon.glass.Translate
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.glass.Share
import top.yukonga.miuix.kmp.icon.glass.Settings
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.icon.glass.Refresh
import top.yukonga.miuix.kmp.icon.glass.Link
import top.yukonga.miuix.kmp.icon.glass.Hide
import top.yukonga.miuix.kmp.icon.glass.Unstar
import top.yukonga.miuix.kmp.icon.glass.FileDownloads
import top.yukonga.miuix.kmp.icon.glass.ScreenMirroring
import top.yukonga.miuix.kmp.icon.glass.Close2
import top.yukonga.miuix.kmp.icon.glass.Close

/** 首页展开菜单的功能项。声明顺序即全新安装时的默认布局。 */
enum class MenuAction(val id: String, val label: String, val icon: ImageVector) {
    Home("home", "主页", MiuixIcons.Home),
    History("history", "历史", MiuixIcons.Glass.WorldClock),
    Downloads("downloads", "下载", MiuixIcons.Glass.FileDownloads),
    Favorite("favorite", "收藏", MiuixIcons.Glass.Unstar),
    Incognito("incognito", "无痕浏览", MiuixIcons.Glass.Hide),
    Desktop("desktop", "桌面版", MiuixIcons.Glass.ScreenMirroring),
    Share("share", "分享", MiuixIcons.Glass.Share),
    AddToDesktop("send_to_desktop", "添加到桌面", AppIcons.AddToDesktop),
    Translate("translate", "翻译", MiuixIcons.Glass.Translate),
    External("external", "外部打开", MiuixIcons.Glass.Link),
    ScriptMenu("script_menu", "脚本菜单", AppIcons.Script),
    CloseTab("close", "关闭标签", MiuixIcons.Glass.Close),
    Find("find", "页内查找", MiuixIcons.Glass.Search),
    Refresh("refresh", "刷新", MiuixIcons.Glass.Refresh),
    Settings("settings", "设置", MiuixIcons.Glass.Settings),
    Quit("quit", "关闭浏览器", MiuixIcons.Glass.Close2),
}

/** 更多面板的图标顺序与隐藏项，可拖动排序 / 拖到下方隐藏并持久化 */
class MenuOrderStore(context: Context) {

    private val prefs = context.getSharedPreferences("mafari_menu", Context.MODE_PRIVATE)

    /** 菜单里显示的功能（有序） */
    val order = mutableStateListOf<MenuAction>()

    /** 已隐藏的功能 */
    val hidden = mutableStateListOf<MenuAction>()

    init {
        fun read(key: String): List<MenuAction> =
            prefs.getString(key, null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()
                .mapNotNull { id -> MenuAction.entries.firstOrNull { it.id == id } }
        if (prefs.getInt(KEY_VERSION, 0) < 3) {
            // 一次性迁移：清掉旧布局（含已移除的功能），按新默认顺序重建
            order.addAll(MenuAction.entries)
            prefs.edit()
                .putString(KEY, order.joinToString(",") { it.id })
                .putString(KEY_HIDDEN, "")
                .putInt(KEY_VERSION, 3)
                .apply()
        } else {
            hidden.addAll(read(KEY_HIDDEN).filter { it != MenuAction.Settings && it != MenuAction.Quit }.distinct())
            order.addAll(read(KEY).filter { it !in hidden }.distinct())
            MenuAction.entries.forEach { if (it !in order && it !in hidden) order.add(it) }
        }
    }

    /** 把 [action] 放到显示区 / 隐藏区的第 [toIndex] 位；「设置」与「关闭浏览器」不允许隐藏 */
    fun place(action: MenuAction, toHidden: Boolean, toIndex: Int) {
        if (toHidden && (action == MenuAction.Settings || action == MenuAction.Quit)) return
        val target = if (toHidden) hidden else order
        order.remove(action)
        hidden.remove(action)
        target.add(toIndex.coerceIn(0, target.size), action)
        persist()
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY, order.joinToString(",") { it.id })
            .putString(KEY_HIDDEN, hidden.joinToString(",") { it.id })
            .apply()
    }

    private companion object {
        const val KEY = "order"
        const val KEY_HIDDEN = "hidden"
        const val KEY_VERSION = "layout_version"
    }
}
