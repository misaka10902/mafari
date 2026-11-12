package com.hyper.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 设置 → 隐私与安全 → 指纹保护：独立二级页面（原先是底部 sheet），与脚本 / 去广告 / 下载页同一套布局。 */
@Composable
fun FingerprintScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    blurEnabled: Boolean = true,
) {
    var showFpExempt by remember { mutableStateOf(false) }
    var fpPending by remember { mutableStateOf<Int?>(null) } // 等待用户确认开启的指纹保护项

    val backdrop = rememberLayerBackdrop()
    val glass = rememberGlassActive(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    val topScrolled = rememberTopBarScrolled(scrollBehavior)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PageGlassTopBar(
                title = "增强保护",
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
                    .padding(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                Spacer(Modifier.height(8.dp))
                // ① 隐私报告
                val fpOn = AdBlock.fpMaster
                val fpCount = listOf(AdBlock.fpCanvas, AdBlock.fpWebgl, AdBlock.fpAudio, AdBlock.fpFonts).count { it }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "隐私报告",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (fpOn) "保护中 · $fpCount / 4 项" else "未开启",
                            fontSize = 12.sp,
                            color = if (fpOn) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "${AdBlock.fpReads}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = " 次指纹读取已被处理",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                        )
                    }
                    Text(
                        text = when {
                            AdBlock.fpSince == 0L && !fpOn -> "开启保护后，这里会统计网页读取你设备指纹的次数"
                            AdBlock.fpSince == 0L -> "已开启保护，暂未发现网页读取指纹"
                            fpOn && AdBlock.fpReads == 0L -> "自 ${fpSinceText(AdBlock.fpSince)} 开启保护以来，暂未发现网页读取指纹"
                            fpOn -> "自 ${fpSinceText(AdBlock.fpSince)} 开启保护以来的累计"
                            else -> "保护已关闭 · 数字停留在 ${fpSinceText(AdBlock.fpSince)} 开启的那一轮"
                        },
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        text = "网页在隐藏或关闭时上报，数字仅供参考",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                // ② 基础隐私保护：与指纹保护无关的两项基础开关，放在隐私报告下面
                SmallTitle(text = "基础隐私保护")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    AppSwitchPreference(
                        title = "发送禁止跟踪请求",
                        checked = settings.sendDnt,
                        onCheckedChange = { settings.updateSendDnt(it) },
                    )
                    AppSwitchPreference(
                        title = "禁用 WebRTC",
                        checked = settings.webrtcDisabled,
                        onCheckedChange = { settings.updateWebrtcDisabled(it) },
                    )
                }

                // ③ 指纹保护：总开关 + 展开的四项 + 原「高级」的两项，合并为一组
                SmallTitle(text = "指纹保护")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp),
                ) {
                    AppSwitchPreference(
                        title = "开启指纹保护",
                        checked = fpOn,
                        onCheckedChange = { if (it) fpPending = AdBlock.FP_ALL else AdBlock.setFingerprintMaster(false) },
                    )
                    AnimatedVisibility(
                        visible = fpOn,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            AppSwitchPreference(
                                title = "Canvas 指纹",
                                checked = AdBlock.fpCanvas,
                                onCheckedChange = { if (it) fpPending = AdBlock.FP_CANVAS else AdBlock.updateFingerprint(AdBlock.FP_CANVAS, false) },
                            )
                            AppSwitchPreference(
                                title = "WebGL 指纹",
                                checked = AdBlock.fpWebgl,
                                onCheckedChange = { if (it) fpPending = AdBlock.FP_WEBGL else AdBlock.updateFingerprint(AdBlock.FP_WEBGL, false) },
                            )
                            AppSwitchPreference(
                                title = "Audio 指纹",
                                checked = AdBlock.fpAudio,
                                onCheckedChange = { if (it) fpPending = AdBlock.FP_AUDIO else AdBlock.updateFingerprint(AdBlock.FP_AUDIO, false) },
                            )
                            AppSwitchPreference(
                                title = "Fonts 指纹",
                                checked = AdBlock.fpFonts,
                                onCheckedChange = { if (it) fpPending = AdBlock.FP_FONTS else AdBlock.updateFingerprint(AdBlock.FP_FONTS, false) },
                            )
                        }
                    }
                    AppDropdownPreference(
                        backdrop = if (glass) backdrop else null,
                        title = "未预注册页面",
                        items = listOf("仅记录，不干预", "停止并在保护下重载", "停止加载"),
                        selectedIndex = AdBlock.fpUnprotectedPolicy.coerceIn(0, 2),
                        onSelectedIndexChange = { AdBlock.updateFpUnprotectedPolicy(it) },
                    )
                    ArrowPreference(
                        title = "保护豁免站点",
                        onClick = { showFpExempt = true },
                    )
                }

            }

            HostListDialog(
                show = showFpExempt,
                backdrop = if (glass) backdrop else null,
                title = "保护豁免站点",
                summary = "豁免站点不注入指纹保护，刷新后生效",
                emptyText = "还没有豁免站点",
                hosts = AdBlock.fpExemptHosts,
                onAdd = { AdBlock.exemptFingerprintSite(it) },
                onRemove = { AdBlock.unexemptFingerprintSite(it) },
                onDismiss = { showFpExempt = false },
            )

            val fpAsk = fpPending
            AppGlassDialog(
                show = fpAsk != null,
                title = when (fpAsk) {
                    AdBlock.FP_CANVAS -> "开启 Canvas 指纹保护？"
                    AdBlock.FP_WEBGL -> "开启 WebGL 指纹保护？"
                    AdBlock.FP_AUDIO -> "开启 Audio 指纹保护？"
                    AdBlock.FP_FONTS -> "开启 Fonts 指纹保护？"
                    else -> "开启指纹保护？"
                },
                backdrop = if (glass) backdrop else null,
                onDismissRequest = { fpPending = null },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "开启后可实现更精准的隐私保护，但会影响部分网页的功能，如果遇到问题请停止使用",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppDialogButton(
                            onClick = { fpPending = null },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "取消", style = MiuixTheme.textStyles.button)
                        }
                        AppDialogButton(
                            onClick = {
                                fpAsk?.let {
                                    if (it == AdBlock.FP_ALL) AdBlock.setFingerprintMaster(true) else AdBlock.updateFingerprint(it, true)
                                }
                                fpPending = null
                            },
                            modifier = Modifier.weight(1f),
                            role = top.yukonga.miuix.kmp.glass.GlassDialogButtonRole.Primary,
                        ) {
                            Text(text = "仍要开启", style = MiuixTheme.textStyles.button)
                        }
                    }
                }
            }

        }
    }
}

private fun fpSinceText(time: Long): String =
    java.text.SimpleDateFormat("M月d日 HH:mm", java.util.Locale.getDefault()).format(java.util.Date(time))
