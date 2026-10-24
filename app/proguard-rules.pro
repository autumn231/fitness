# ---------------------------------------------------------------------------
# fitness release R8 规则
# 开启 isMinifyEnabled 后必须保留以下反射/序列化相关的符号，否则数据加载会失败。
# ---------------------------------------------------------------------------

# kotlinx.serialization：@Serializable 数据类及其生成的 serializer
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-keepclassmembers @kotlinx.serialization.Serializable class com.fitness.app.data.model.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.fitness.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.fitness.app.data.model.**$$serializer { *; }
-keepclassmembers class com.fitness.app.data.model.** {
    *** write$Self(...);
}

# Room：实体与 DAO 由生成代码直接引用，保留实体构造/字段访问
-keep class com.fitness.app.data.local.** { *; }
-keepclassmembers class com.fitness.app.data.local.** { *; }
-dontwarn androidx.room.paging.**

# Compose / Coil：保留 Compose 运行时对 lambda 的类名可读性
-keepclassmembers class androidx.compose.runtime.** { *; }

# 关闭对协程调试元数据的告警
-dontwarn kotlinx.coroutines.debug.**

# 保留行号，方便线上崩溃定位
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
