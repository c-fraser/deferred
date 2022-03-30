import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import java.net.URI
import java.net.URL
import java.util.jar.Attributes
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

if (JavaVersion.current() < JavaVersion.VERSION_1_8)
    throw GradleException("Java 8+ is required for this project")

plugins {
  kotlin("jvm") apply false
  id("org.jetbrains.dokka") apply false
  id("com.adarshr.test-logger") apply false
  id("com.diffplug.spotless")
  id("io.gitlab.arturbosch.detekt")
  id("io.github.gradle-nexus.publish-plugin")
  id("org.jreleaser")
  id("com.github.ben-manes.versions")
}

allprojects {
  group = "io.github.c-fraser"
  version = "0.1.1"

  repositories {
    mavenCentral()
    maven { url = URI("https://oss.sonatype.org/content/repositories/snapshots/") }
  }
}

subprojects project@{
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "io.gitlab.arturbosch.detekt")
  apply(plugin = "com.adarshr.test-logger")

  plugins.withType<JavaLibraryPlugin>() {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
    }
  }

  dependencies {
    val junitVersion: String by rootProject

    "implementation"(kotlin("stdlib"))
    "testImplementation"(kotlin("test"))
    "testImplementation"(kotlin("test-junit5"))
    "testImplementation"("org.junit.jupiter:junit-jupiter:$junitVersion")
  }

  tasks {
    withType<KotlinCompile>().configureEach {
      kotlinOptions {
        jvmTarget = "${JavaVersion.VERSION_1_8}"
        freeCompilerArgs = listOf("-Xjsr305=strict")
      }
    }

    withType<Jar> {
      manifest {
        val moduleName =
            this@project.name.removePrefix("${rootProject.name}-").removeSuffix("-plugin")
        attributes(
            "${Attributes.Name.IMPLEMENTATION_TITLE}" to this@project.name,
            "${Attributes.Name.IMPLEMENTATION_VERSION}" to this@project.version,
            "Automatic-Module-Name" to "io.github.cfraser.deferred.$moduleName")
      }
    }

    withType<Test> { useJUnitPlatform() }

    withType<DokkaTask>().configureEach {
      dokkaSourceSets {
        named("main") {
          moduleName.set(this@project.name)
          platform.set(Platform.jvm)
          jdkVersion.set(JavaVersion.VERSION_1_8.ordinal)
          sourceLink {
            localDirectory.set(this@project.file("src/main/kotlin"))
            remoteUrl.set(
                URL(
                    "https://github.com/c-fraser/deferred/tree/main/${this@project.name}/src/main/kotlin"))
            remoteLineSuffix.set("#L")
          }
        }
      }
    }

    withType<Detekt> {
      jvmTarget = "${JavaVersion.VERSION_1_8}"
      buildUponDefaultConfig = true
      config.setFrom(rootDir.resolve("detekt.yml"))
    }
  }

  plugins.withType<MavenPublishPlugin> {
    configure<PublishingExtension> {
      val dokkaJavadocJar by
          tasks.creating(Jar::class) {
            val dokkaJavadoc by tasks.getting(AbstractDokkaTask::class)
            dependsOn(dokkaJavadoc)
            archiveClassifier.set("javadoc")
            from(dokkaJavadoc.outputDirectory.get())
          }

      val sourcesJar by
          tasks.creating(Jar::class) {
            val sourceSets: SourceSetContainer by this@project
            dependsOn(tasks["classes"])
            archiveClassifier.set("sources")
            from(sourceSets["main"].allSource)
          }

      publications {
        create<MavenPublication>("maven") {
          from(this@project.components["java"])
          artifact(dokkaJavadocJar)
          artifact(sourcesJar)
          pom {
            name.set(this@project.name)
            description.set("${this@project.name}-${this@project.version}")
            url.set("https://github.com/c-fraser/deferred")
            inceptionYear.set("2021")

            issueManagement {
              system.set("GitHub")
              url.set("https://github.com/c-fraser/deferred/issues")
            }

            licenses {
              license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
              }
            }

            developers {
              developer {
                id.set("c-fraser")
                name.set("Chris Fraser")
              }
            }

            scm {
              url.set("https://github.com/c-fraser/deferred")
              connection.set("scm:git:git://github.com/c-fraser/deferred.git")
              developerConnection.set("scm:git:ssh://git@github.com/c-fraser/deferred.git")
            }
          }
        }
      }

      plugins.withType<SigningPlugin>() {
        configure<SigningExtension> {
          publications.withType<MavenPublication>().all mavenPublication@{
            useInMemoryPgpKeys(System.getenv("GPG_SIGNING_KEY"), System.getenv("GPG_PASSWORD"))
            sign(this@mavenPublication)
          }
        }
      }
    }
  }

  configure<TestLoggerExtension>() { showStandardStreams = true }
}

configure<SpotlessExtension> {
  val ktfmtVersion: String by rootProject

  val licenseHeader =
      """
      /*
      Copyright 2022 c-fraser

      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at

          https://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      See the License for the specific language governing permissions and
      limitations under the License.
      */
      """.trimIndent()

  kotlin {
    ktfmt(ktfmtVersion)
    licenseHeader(licenseHeader)
    target(fileTree(rootProject.rootDir) { include("**/src/**/*.kt") })
  }

  kotlinGradle { ktfmt(ktfmtVersion) }
}

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
      username.set(System.getenv("SONATYPE_USERNAME"))
      password.set(System.getenv("SONATYPE_PASSWORD"))
    }
  }
}

jreleaser {
  project {
    website.set("https://github.com/c-fraser/deferred")
    authors.set(listOf("c-fraser"))
    license.set("Apache-2.0")
    extraProperties.put("inceptionYear", "2021")
  }

  release {
    github {
      owner.set("c-fraser")
      overwrite.set(true)
      token.set(System.getenv("GITHUB_TOKEN").orEmpty())
      changelog {
        formatted.set(Active.ALWAYS)
        format.set("- {{commitShortHash}} {{commitTitle}}")
        contributors.enabled.set(false)
        for (status in listOf("added", "changed", "fixed", "removed")) {
          labeler {
            label.set(status)
            title.set(status)
          }
          category {
            title.set(status.capitalize())
            labels.set(listOf(status))
          }
        }
      }
    }
  }
}

tasks {
  val detektAll by
      registering(Detekt::class) {
        jvmTarget = "${JavaVersion.VERSION_1_8}"
        parallel = true
        buildUponDefaultConfig = true
        config.setFrom(rootDir.resolve("detekt.yml"))
        setSource(files(projectDir))
        include("**/*.kt", "**/*.kts")
        exclude("**/build/**", "**/resources/**")
      }

  spotlessApply { finalizedBy(detektAll) }
}
