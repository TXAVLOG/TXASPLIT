/*
████████ ██   ██  █████   █████  ██████  ██████  
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██ 
   ██      ███   ███████ ███████ ██████  ██████  
   ██     ██ ██  ██   ██ ██   ██ ██      ██      
   ██    ██   ██ ██   ██ ██   ██ ██      ██      
                                                 
TXASplit - Gradle Settings
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "TXASplit"
include(":app")
