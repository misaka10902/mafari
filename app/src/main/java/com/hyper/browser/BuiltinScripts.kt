package com.hyper.browser

/**
 * 推荐脚本。首次打开 app 时会自动从 userScriptUrl 拉取并安装（每个只自动装一次，之后删除不会再装回来）；
 * 去广告规则不在这里，见「设置 → 高级 → 去广告」。
 */
data class BuiltinScript(
    val name: String,
    val description: String,
    /** 直接是 .user.js 的订阅地址 */
    val userScriptUrl: String = "",
    /** 没有地址时直接加入的脚本内容 */
    val code: String = "",
)

val BUILTIN_SCRIPTS = emptyList<BuiltinScript>()
