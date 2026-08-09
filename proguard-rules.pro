# CFHC release R8/ProGuard rules.
# Applied on top of the AGP default (proguard-android-optimize.txt) when the
# release buildType runs minifyEnabled true. Keep rules are intentionally
# minimal — the engine uses plain-text saves (no reflection/serialization) and
# AndroidResourceProvider.getIdentifier() is safe under R8.

# --- GraphView 4.2.2 (com.jjoe64:graphview) — only third-party runtime dep ---
# Library uses reflection to instantiate series classes; keep the whole package.
-keep class com.jjoe64.graphview.** { *; }
-dontwarn com.jjoe64.graphview.**

# --- Enum safety: valueOf/values() are accessed reflectively by Java itself ---
# Used widely by the engine (AudioEvent, Personality, Practicefocus, etc.).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- JNI safety (no native code today; guards future System.loadLibrary usage) ---
-keepclasseswithmembernames class * {
    native <methods>;
}
