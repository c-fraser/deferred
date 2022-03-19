pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }

  val kotlinVersion: String by settings
  val spotlessVersion: String by settings
  val detektVersion: String by settings
  val nexusPublishVersion: String by settings
  val jreleaserVersion: String by settings
  val versionsVersion: String by settings
  val dokkaVersion: String by settings
  val testLoggerVersion: String by settings
  val buildConfigVersion: String by settings

  plugins {
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("com.diffplug.spotless") version spotlessVersion
    id("io.gitlab.arturbosch.detekt") version detektVersion
    id("io.github.gradle-nexus.publish-plugin") version nexusPublishVersion
    id("org.jreleaser") version jreleaserVersion
    id("com.github.ben-manes.versions") version versionsVersion
    id("org.jetbrains.dokka") version dokkaVersion
    id("com.adarshr.test-logger") version testLoggerVersion
    id("com.github.gmazzo.buildconfig") version buildConfigVersion
  }
}

rootProject.name = "deferred"

include("deferred-api", "deferred-compiler-plugin", "deferred-gradle-plugin")
