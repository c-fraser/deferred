plugins {
  `java-gradle-plugin`
  `maven-publish`
  signing
  id("com.github.gmazzo.buildconfig")
}

dependencies { implementation(kotlin("gradle-plugin-api")) }

gradlePlugin {
  plugins {
    create("deferred") {
      id = "io.github.cfraser.deferred"
      displayName = "Deferred Gradle Plugin"
      description = "Kotlin Compiler Plugin to enable defer statements"
      implementationClass = "io.github.cfraser.deferred.GradlePlugin"
    }
  }
}

buildConfig {
  packageName("${rootProject.group}.${rootProject.name}".replace("-", ""))

  fun configField(name: String, value: String) {
    buildConfigField(checkNotNull(String::class.simpleName), name, value)
  }

  configField(
      "API_DEPENDENCY",
      """"${rootProject.group}:${project(":deferred-api").name}:${rootProject.version}"""")
  configField("COMPILER_PLUGIN_GROUP", """"${rootProject.group}"""")
  configField("COMPILER_PLUGIN_NAME", """"${project(":deferred-compiler-plugin").name}"""")
  configField("COMPILER_PLUGIN_VERSION", """"${rootProject.version}"""")
}
