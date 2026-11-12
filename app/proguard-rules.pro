# mafari proguard rules

# WebView 的 JS 桥：方法由网页按字符串名字调用，名字不能混淆
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes *Annotation*

# 应用自身代码：允许 R8 删除从未被引用的类与成员（清理死代码），
# 但保留名字，避免反射 / 崩溃栈失真。
-keep,allowshrinking,allowoptimization class com.hyper.browser.** { *; }

-dontwarn org.jetbrains.compose.**
