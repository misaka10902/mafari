package com.hyper.browser

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onApplyUserAgent: (String) -> Unit,
    onOpenScripts: () -> Unit,
    onOpenAdBlock: () -> Unit,
    onOpenFingerprint: () -> Unit,
    onOpenCookie: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onPageEnhancementsChange: (Boolean) -> Unit = {},
    blurEnabled: Boolean = true,
) {
    val context = LocalContext.current
    var showUa by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showWallpaper by remember { mutableStateOf(false) }
    var showPermSheet by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var reclaimable by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { reclaimable = withContext(Dispatchers.IO) { CacheCleaner.reclaimableBytes(context) } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                // 系统照片选择器只给一次性读权限：复制到应用私有目录，重启后仍可读取
                val dir = java.io.File(context.filesDir, "wallpaper").apply { mkdirs() }
                val target = java.io.File(dir, "wallpaper_${System.currentTimeMillis()}.img")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                // 删除上一张，避免文件越攒越多
                settings.wallpaper.takeIf { it.isNotBlank() }?.let { old ->
                    runCatching {
                        val path = android.net.Uri.parse(old).path ?: return@runCatching
                        java.io.File(path).takeIf { it.exists() }?.delete()
                    }
                }
                settings.updateWallpaper(android.net.Uri.fromFile(target).toString())
            }.onFailure {
                Toast.makeText(context, "壁纸读取失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val backdrop = rememberLayerBackdrop()
    val glass = rememberGlassActive(blurEnabled)
    val popupVisuals = GlassPopupDefaults.visuals()
    val scrollBehavior = MiuixScrollBehavior()
    val topScrolled = rememberTopBarScrolled(scrollBehavior)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageGlassTopBar(
                title = "设置",
                backdrop = backdrop,
                glass = glass,
                scrollBehavior = scrollBehavior,
                onBack = onBack,
                backButton = {
                    LiquidGlassBackButton(
                        surfaceVisible = topScrolled,
                        onClick = onBack,
                    )
                },
            )
        },
    ) { innerPadding ->
        val topInset = innerPadding.calculateTopPadding()
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .background(MiuixTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(top = topInset)
                    // 沉浸：内容可滚到手势小白条下方，仅在末尾留出小白条高度
                    .padding(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                SmallTitle(text = "外观")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    SettingsRow(
                        title = "主页壁纸",
                        summary = if (settings.wallpaper.isBlank()) "未设置" else "已设置",
                        onClick = { showWallpaper = true },
                    )
                    AppSwitchPreference(
                        title = "高级材质",
                        checked = settings.blurMode != BLUR_OFF,
                        onCheckedChange = { settings.updateBlurMode(if (it) BLUR_MIUIX else BLUR_OFF) },
                    )
                }

                SmallTitle(text = "浏览")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    AppDropdownPreference(
                        backdrop = if (glass) backdrop else null,
                        title = "外部链接打开方式",
                        items = listOf("应用内打开", "自定义标签页", "其他浏览器"),
                        selectedIndex = settings.linkOpenMode.coerceIn(0, 2),
                        onSelectedIndexChange = { settings.updateLinkOpenMode(it) },
                    )
                    SettingsRow(
                        title = "搜索引擎",
                        summary = SEARCH_ENGINES.getOrNull(settings.searchIndex)?.name ?: "自定义",
                        onClick = { showSearch = true },
                    )
                    SettingsRow(
                        title = "书签与历史",
                        onClick = onOpenBookmarks,
                    )
                    SettingsRow(
                        title = "UA 标识管理",
                        summary = uaSummary(settings.userAgent),
                        onClick = { showUa = true },
                    )
                }

                SmallTitle(text = "隐私与安全")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    // Cookie 管理：v2.0.0 新增，含接受策略 / 启动时清理 / 手动清除
                    ArrowPreference(
                        title = "Cookie 管理",
                        onClick = onOpenCookie,
                    )
                    SettingsRow(
                        title = "网站权限管理",
                        onClick = { showPermSheet = true },
                    )
                    SettingsRow(
                        title = "增强保护",
                        onClick = onOpenFingerprint,
                    )
                }

                SmallTitle(text = "高级")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    AppSwitchPreference(
                        title = "网页增强",
                        checked = settings.pageEnhancements,
                        onCheckedChange = {
                            settings.updatePageEnhancements(it)
                            onPageEnhancementsChange(it)
                            AdBlock.updateEnabled(it)
                        },
                    )
                    SettingsRow(
                        title = "广告拦截",
                        onClick = onOpenAdBlock,
                    )
                    SettingsRow(
                        title = "脚本管理",
                        onClick = onOpenScripts,
                    )
                    SettingsRow(
                        title = "代理",
                        onClick = { showProxyDialog = true },
                    )
                    SettingsRow(
                        title = "清理缓存",
                        summary = "当前可清理 " +
                            android.text.format.Formatter.formatFileSize(context, reclaimable),
                        onClick = {
                            scope.launch {
                                // WebView 网页缓存要在主线程清；其余文件在 IO 线程删
                                val clearWeb: () -> Unit = {
                                    runCatching { WebView(context.applicationContext).apply { clearCache(true); destroy() } }
                                }
                                val freed = withContext(Dispatchers.IO) {
                                    CacheCleaner.cleanNow(context) { runBlocking(Dispatchers.Main) { clearWeb() } }
                                }
                                reclaimable = withContext(Dispatchers.IO) { CacheCleaner.reclaimableBytes(context) }
                                Toast.makeText(
                                    context,
                                    if (freed > 0) "已清理 " + android.text.format.Formatter.formatFileSize(context, freed) else "没有可清理的缓存",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }

            }

            // UA 选择放在设置页自己的 Scaffold 里：浏览主界面的覆盖层会被二级页面盖住
            UaDialog(
                show = showUa,
                initial = settings.userAgent,
                // 采样设置页自己的采样层（不采 browserBackdrop，主页可能含壁纸）；关闭模糊时回退不透明色
                backdrop = if (glass) backdrop else null,
                onDismiss = { showUa = false },
                onConfirm = {
                    settings.updateUserAgent(it)
                    onApplyUserAgent(it)
                    showUa = false
                },
            )

            SearchSheet(
                show = showSearch,
                current = settings.searchIndex,
                custom = settings.customSearch,
                backdrop = if (glass) backdrop else null,
                onDismiss = { showSearch = false },
                onPick = {
                    settings.updateSearch(it)
                    showSearch = false
                },
                onCustom = {
                    settings.updateCustomSearch(it)
                    showSearch = false
                },
            )

            AppGlassDialog(
                show = showWallpaper,
                title = "主页壁纸",
                backdrop = if (glass) backdrop else null,
                onDismissRequest = { showWallpaper = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    WallpaperDialogRow(
                        title = "选择壁纸",
                        onClick = {
                            showWallpaper = false
                            // 系统照片选择器（澎湃OS 会直接调起相册选图），而不是文件管理器
                            picker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                    )
                    WallpaperDialogRow(
                        title = "清除壁纸",
                        titleColor = MiuixTheme.colorScheme.error,
                        onClick = {
                            showWallpaper = false
                            settings.updateWallpaper("")
                        },
                    )
                    WallpaperDialogRow(
                        title = "模糊壁纸",
                        onClick = { settings.updateWallpaperBlur(!settings.wallpaperBlur) },
                    ) {
                        AppSwitch(
                            checked = settings.wallpaperBlur,
                            onCheckedChange = { settings.updateWallpaperBlur(it) },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    AppDialogButton(
                        onClick = { showWallpaper = false },
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        minHeight = 44.dp,
                    ) {
                        Text(text = "关闭", fontSize = 16.sp, style = MiuixTheme.textStyles.button)
                    }
                }
            }

            // 代理：官方 ProxyController，HTTP / SOCKS5
            var proxyOn by remember(showProxyDialog) { mutableStateOf(settings.proxyEnabled) }
            var proxyType by remember(showProxyDialog) { mutableStateOf(settings.proxyType) }
            var proxyHost by remember(showProxyDialog) { mutableStateOf(settings.proxyHost) }
            var proxyPort by remember(showProxyDialog) {
                mutableStateOf(if (settings.proxyPort > 0) settings.proxyPort.toString() else "")
            }
            var proxyAuth by remember(showProxyDialog) { mutableStateOf(settings.proxyAuth) }
            var proxyUser by remember(showProxyDialog) { mutableStateOf(settings.proxyUser) }
            var proxyPass by remember(showProxyDialog) { mutableStateOf(settings.proxyPass) }
            // SOCKS5 官方 API 不支持账密：认证开关置灰；代理关闭时仍可先配好认证
            val authSwitchEnabled = true
            AppGlassDialog(
                show = showProxyDialog,
                title = "代理",
                backdrop = if (glass) backdrop else null,
                onDismissRequest = { showProxyDialog = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    AppSwitchPreference(
                        title = "启用代理",
                        checked = proxyOn,
                        onCheckedChange = { proxyOn = it },
                    )
                    AppDropdownPreference(
                        backdrop = if (glass) backdrop else null,
                        title = "类型",
                        items = listOf("HTTP", "SOCKS5"),
                        selectedIndex = proxyType.coerceIn(0, 1),
                        onSelectedIndexChange = { proxyType = it },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = proxyHost,
                            onValueChange = { proxyHost = it },
                            modifier = Modifier.weight(2f),
                            label = "主机",
                            singleLine = true,
                        )
                        TextField(
                            value = proxyPort,
                            onValueChange = { proxyPort = it.filter { c -> c.isDigit() }.take(5) },
                            modifier = Modifier.weight(1f),
                            label = "端口",
                            singleLine = true,
                        )
                    }
                    AppSwitchPreference(
                        title = "认证",
                        checked = proxyAuth,
                        onCheckedChange = { proxyAuth = it },
                        enabled = authSwitchEnabled,
                    )
                    if (proxyAuth && authSwitchEnabled) {
                        TextField(
                            value = proxyUser,
                            onValueChange = { proxyUser = it },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            label = "用户名",
                            singleLine = true,
                        )
                        TextField(
                            value = proxyPass,
                            onValueChange = { proxyPass = it },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            label = "密码",
                            singleLine = true,
                        )
                    }
                    if (proxyAuth && proxyType == 1) {
                        Text(
                            text = "SOCKS5 认证由应用内本机中转（127.0.0.1 随机端口）完成，WebView 自身不支持 SOCKS5 认证。",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    WebViewProxy.lastError?.let { err ->
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppDialogButton(
                            onClick = { showProxyDialog = false },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "取消", style = MiuixTheme.textStyles.button)
                        }
                        AppDialogButton(
                            onClick = {
                                val port = proxyPort.toIntOrNull() ?: 0
                                settings.updateProxy(
                                    enabled = proxyOn,
                                    type = proxyType,
                                    host = proxyHost,
                                    port = port,
                                    // 关闭代理时也保存认证配置，便于下次直接启用
                                    auth = proxyAuth,
                                    user = proxyUser,
                                    pass = proxyPass,
                                )
                                showProxyDialog = false
                                Toast.makeText(
                                    context,
                                    if (proxyOn && proxyHost.isNotBlank() && port in 1..65535) {
                                        "代理已${if (proxyType == 1) "（SOCKS5）" else "（HTTP）"}生效"
                                    } else {
                                        "代理已关闭${if (proxyAuth) "（认证配置已保存）" else ""}"
                                    },
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            role = top.yukonga.miuix.kmp.glass.GlassDialogButtonRole.Primary,
                        ) {
                            Text(text = "保存", style = MiuixTheme.textStyles.button)
                        }
                    }
                }
            }

            AppBottomSheet(
                show = showPermSheet,
                title = "网站权限管理",
                backdrop = if (glass) backdrop else null,
                onDismissRequest = { showPermSheet = false },
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
                    val permOptions = listOf("每次使用时询问", "总是允许", "总是拒绝")
                    listOf(
                        Triple("摄像头", settings.cameraPermission, settings::updateCameraPermission),
                        Triple("麦克风", settings.micPermission, settings::updateMicPermission),
                        Triple("定位", settings.locationPermission, settings::updateLocationPermission),
                        Triple("网站通知", settings.notificationPermission, settings::updateNotificationPermission),
                        Triple("剪贴板", settings.clipboardPermission, settings::updateClipboardPermission),
                        Triple("声音", settings.mediaAutoplay, settings::updateMediaAutoplay),
                        Triple("网页重定向", settings.redirectPermission, settings::updateRedirectPermission),
                    ).forEach { (label, current, update) ->
                        AppDropdownPreference(
                            title = label,
                            items = permOptions,
                            selectedIndex = current.coerceIn(0, 2),
                            onSelectedIndexChange = { update(it) },
                            backdrop = if (glass) backdrop else null,
                            visuals = popupVisuals,
                            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 壁纸弹窗里的一行：标题统一 16sp、说明统一 12sp，右侧可放开关。 */
@Composable
private fun WallpaperDialogRow(
    title: String,
    onClick: () -> Unit,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    summary: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = titleColor)
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun SettingsRow(title: String, onClick: () -> Unit, summary: String? = null) {
    ArrowPreference(
        title = title,
        summary = summary,
        onClick = onClick,
    )
}

private fun uaSummary(ua: String): String = when {
    ua.isBlank() -> "跟随系统"
    ua == DESKTOP_UA -> "桌面 Chrome"
    else -> ua.take(48) + if (ua.length > 48) "…" else ""
}

@Composable
private fun SearchSheet(
    show: Boolean,
    current: Int,
    custom: String,
    backdrop: top.yukonga.miuix.kmp.blur.Backdrop?,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
    onCustom: (String) -> Unit,
) {
    var text by remember(show) { mutableStateOf(custom) }
    AppBottomSheet(
        show = show,
        title = "搜索引擎",
        backdrop = backdrop,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            SEARCH_ENGINES.forEachIndexed { index, engine ->
                val engineInteraction = remember { MutableInteractionSource() }
                val enginePressed by engineInteraction.collectIsPressedAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (enginePressed) MiuixTheme.colorScheme.onBackground.copy(alpha = 0.09f)
                            else Color.Transparent
                        )
                        .clickable(interactionSource = engineInteraction, indication = null) { onPick(index) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = engine.name,
                        fontSize = 16.sp,
                        color = if (index == current) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (index == current) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = MiuixIcons.Basic.Check,
                            contentDescription = "已选中",
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = "自定义（用 %s 代表关键词）",
                singleLine = true,
            )
            Spacer(Modifier.height(14.dp))
            AppTextButton(
                text = "使用自定义",
                onClick = { onCustom(text) },
                modifier = Modifier.fillMaxWidth(),
                primary = true,
            )
        }
    }
}
