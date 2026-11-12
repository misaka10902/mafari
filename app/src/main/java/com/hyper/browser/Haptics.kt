package com.hyper.browser

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * 一段式中等强度震动。
 *
 * 系统的 LONG_PRESS 触觉在 HyperOS 上会拆成「按下 + 抬起」两段，长按网页时观感很吵；
 * 因此 WebView 自身触觉关掉（isHapticFeedbackEnabled = false），由这里统一补一发单段中等震感。
 */
fun mediumTick(context: Context) {
    runCatching {
        val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createOneShot(22L, 140))
    }
}
