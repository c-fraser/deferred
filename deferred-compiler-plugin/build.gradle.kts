plugins {
  `java-library`
  `maven-publish`
  signing
  kotlin("kapt")
}

dependencies {
  val autoServiceVersion: String by rootProject

  compileOnly(kotlin("compiler-embeddable"))
  compileOnly("com.google.auto.service:auto-service-annotations:$autoServiceVersion")
  kapt("com.google.auto.service:auto-service:$autoServiceVersion")

  val kotlinCompilerTestingVersion: String by rootProject

  testImplementation(kotlin("compiler-embeddable"))
  testImplementation(project(":deferred-api"))
  testImplementation(
      "com.github.tschuchortdev:kotlin-compile-testing:$kotlinCompilerTestingVersion")
}
