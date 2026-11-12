// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

object BuildConfig {
    const val LIBRARY_VERSION = "0.9.4"
    const val LIBRARY_ID = "top.yukonga.miuix.kmp"
    const val APPLICATION_NAME = "Miuix"
    const val APPLICATION_VERSION_NAME = "1.0.9"
    const val APPLICATION_ID = "top.yukonga.miuix.uitest"
    const val APPLICATION_SHARED_ID = "top.yukonga.miuix.shared"
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 0
    const val TARGET_SDK = 37
    const val MIN_SDK = 24
    const val BUILD_TOOLS_VERSION = "37.0.0"
    const val JDK_VERSION = 21
}

fun org.gradle.api.Project.getGitVersionCode(): Int {
    // 源码压缩包不带 .git：没有仓库时直接回退，不要真的去调 git
    // （git 失败会让 configuration cache 判定为构建问题，Gradle 9 会直接失败）
    if (!rootProject.file(".git").exists()) return 1
    return try {
        providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim().toInt()
    } catch (_: Exception) {
        1
    }
}

fun org.gradle.api.Project.getGitHashShort(): String {
    if (!rootProject.file(".git").exists()) return "local"
    return try {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim().ifBlank { "local" }
    } catch (_: Exception) {
        // 源码包可能不含 .git；版本号仅用于本地 substitution，失败时回退。
        "local"
    }
}
