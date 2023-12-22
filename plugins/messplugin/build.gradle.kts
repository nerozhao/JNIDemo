buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20")
        classpath("com.android.tools.build:gradle:8.2.0")
    }
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.2.0")
    implementation("com.guardsquare:proguard-gradle:7.4.1")
}

sourceSets {
    main {
        kotlin {
            srcDirs("src/main/kotlin")
        }
    }
}

gradlePlugin {
    plugins {
        create("messplugin") {
            id = "messplugin"
            implementationClass = "com.test.plugin.MessPlugin"
        }
    }
}