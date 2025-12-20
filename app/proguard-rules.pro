#
# ████████ ██   ██  █████   █████  ██████  ██████
#    ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
#    ██      ███   ███████ ███████ ██████  ██████
#    ██     ██ ██  ██   ██ ██   ██ ██      ██
#    ██    ██   ██ ██   ██ ██   ██ ██      ██
#
# TXASplit - Proguard Rules
# Build by TXA
# Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
#

# Keep kotlinx.serialization models
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
