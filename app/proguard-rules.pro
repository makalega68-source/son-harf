# Keep every app class as written: data classes are decoded by kotlinx.serialization and
# Supabase by name, and no app code is removed or renamed. R8 still removes unused library code.
-keep class com.sonharf.game.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, RuntimeVisibleAnnotations

# kotlinx.serialization (generated serializers and companions).
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class **$$serializer { *; }

# Supabase / Ktor reference optional JVM classes that do not exist on Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
-dontwarn kotlinx.serialization.**
-keep class io.github.jan.supabase.** { *; }
