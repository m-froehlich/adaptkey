# Keep the InputMethodService and its public entry points referenced from the manifest / XML.
-keep class de.froehlichmedia.adaptkey.AdaptKeyService { *; }
-keep class de.froehlichmedia.adaptkey.settings.SettingsActivity { *; }

# D-482: R8 is enabled for the release build at F-Droid's request, as shrinking + optimisation only.
# Names stay readable: no renaming means JNI/name lookups cannot break and the in-app diagnostic log stays
# legible (this is a free-software app, nothing to hide).
-dontobfuscate

# ONNX Runtime's native library looks Java classes, fields and methods up by name (JNI) and its AAR ships no
# consumer rules of its own, so shrinking must never touch it.
-keep class ai.onnxruntime.** { *; }
