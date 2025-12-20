/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - Root Gradle Build
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false

    id("androidx.room") version "2.6.1" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
}

// Version manager tasks (used by tools/version_manager.ps1)

data class TXAVersion(val major: Int, val minor: Int, val patch: Int, val suffix: String)

fun parseTXAVersion(raw: String): TXAVersion {
    val v = raw.trim()
    val parts = v.split("_")
    val suffix = if (parts.size >= 2) parts.drop(1).joinToString("_") else ""
    val nums = parts[0].split(".")
    require(nums.size == 3) { "Invalid version format: $v (expected X.Y.Z_txa)" }
    return TXAVersion(nums[0].toInt(), nums[1].toInt(), nums[2].toInt(), suffix)
}

fun TXAVersion.toRaw(): String {
    val base = "${major}.${minor}.${patch}"
    return if (suffix.isBlank()) base else "${base}_${suffix}"
}

fun TXAVersion.toVersionCode(): Int = major * 10000 + minor * 100 + patch

fun versionFile() = rootProject.file("version.txa")

fun readCurrentVersion(): TXAVersion {
    val file = versionFile()
    val raw = if (file.exists()) file.readText() else "1.0.0_txa"
    return parseTXAVersion(raw)
}

fun writeVersion(v: TXAVersion) {
    versionFile().writeText(v.toRaw())
}

tasks.register("updateVersion") {
    group = "txa"
    description = "Ensure version.txa is applied to Android build"
    doLast {
        val v = readCurrentVersion()
        println("TXASplit version: ${v.toRaw()} (versionCode=${v.toVersionCode()})")
    }
}

tasks.register("incrementVersion") {
    group = "txa"
    description = "Increment version in version.txa (use -Ppatch|-Pminor|-Pmajor)"
    doLast {
        val current = readCurrentVersion()
        val bumpPatch = project.hasProperty("patch")
        val bumpMinor = project.hasProperty("minor")
        val bumpMajor = project.hasProperty("major")

        val bumped = when {
            bumpMajor -> current.copy(major = current.major + 1, minor = 0, patch = 0)
            bumpMinor -> current.copy(minor = current.minor + 1, patch = 0)
            bumpPatch -> current.copy(patch = current.patch + 1)
            else -> throw GradleException("Missing flag: -Ppatch or -Pminor or -Pmajor")
        }

        writeVersion(bumped)
        println("New version: ${bumped.toRaw()} (versionCode=${bumped.toVersionCode()})")
    }
}

tasks.register("downgradeVersion") {
    group = "txa"
    description = "Set version.txa to -PtargetVersion=X.Y.Z_txa"
    doLast {
        val target = (project.findProperty("targetVersion") as String?)
            ?: throw GradleException("Missing -PtargetVersion")
        val v = parseTXAVersion(target)
        writeVersion(v)
        println("Downgraded version: ${v.toRaw()} (versionCode=${v.toVersionCode()})")
    }
}
