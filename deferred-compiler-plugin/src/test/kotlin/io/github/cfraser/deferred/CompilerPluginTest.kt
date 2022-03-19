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
package io.github.cfraser.deferred

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows

class CompilerPluginTest {

  @Test
  fun testHelloWorld(testInfo: TestInfo) {
    """
    defer { this += "World!" }
    this += "Hello"
    """
        .trimIndent()
        .sourceFile(testInfo)
        .compile()
        .assertDeferred(testInfo, listOf("Hello", "World!"))
  }

  @Test
  fun testReturn(testInfo: TestInfo) {
    """
    defer { this += "After" }
    this += "Before"
    return this
    """
        .trimIndent()
        .sourceFile(testInfo)
        .compile()
        .assertDeferred(testInfo, listOf("Before", "After"))
  }

  // FIXME
  @Test
  fun testInnerBlock(testInfo: TestInfo) {
    """
    (1..10).forEach {
      defer { this += "${"$"}it" }
    }
    this += "0"
    """
        .trimIndent()
        .sourceFile(testInfo)
        .compile()
        .assertDeferred(testInfo, buildList { repeat(11) { this += "$it" } })
  }

  @Test
  fun testExceptionThrown(testInfo: TestInfo) {
    val result =
        SourceFile.kotlin(
                "${testInfo.clazz}.kt",
                """
                package $PACKAGE
            
                class ${testInfo.clazz} {
                  @JvmField
                  val list = mutableListOf<String>()
                  fun add() {
                    defer { list += "After" }
                    list += "Before"
                    error(Unit)
                  }
                }
                """.trimIndent())
            .compile()

    assertEquals(ExitCode.OK, result.exitCode)
    val clazz = result.classLoader.loadClass("$PACKAGE.${testInfo.clazz}")
    val method = assertNotNull(clazz.declaredMethods.find { method -> method.name == "add" })
    val instance = clazz.getDeclaredConstructor().newInstance()
    val exception = assertThrows<InvocationTargetException> { method.invoke(instance) }
    assertTrue { exception.cause is IllegalStateException }
    val field = assertNotNull(clazz.declaredFields.find { field -> field.name == "list" })
    assertEquals(listOf("Before", "After"), field[instance] as? List<*>)
  }

  private companion object {

    const val PACKAGE = "io.github.cfraser.deferred"

    val TestInfo.clazz: String
      get() =
          checkNotNull(testMethod.orElse(null)?.name?.capitalizeAsciiOnly()) {
            "Failed to get method name from test info"
          }

    fun String.sourceFile(testInfo: TestInfo): SourceFile {
      val clazz = testInfo.clazz
      val statements = this
      return SourceFile.kotlin(
          "$clazz.kt",
          """
          package $PACKAGE
      
          class $clazz {
            fun list() = buildList {
              $statements
            }
          }
          """.trimIndent())
    }

    fun SourceFile.compile(): KotlinCompilation.Result =
        KotlinCompilation()
            .also {
              it.sources = listOf(this)
              it.compilerPlugins = listOf(CompilerPlugin())
              it.inheritClassPath = true
              it.messageOutputStream = System.out
            }
            .compile()

    fun KotlinCompilation.Result.assertDeferred(testInfo: TestInfo, expected: List<String>) {
      assertEquals(ExitCode.OK, exitCode)
      val clazz = classLoader.loadClass("$PACKAGE.${testInfo.clazz}")
      val method = assertNotNull(clazz.declaredMethods.find { method -> method.name == "list" })
      val list =
          assertNotNull(method.invoke(clazz.getDeclaredConstructor().newInstance()) as? List<*>)
      assertEquals(expected, list)
    }
  }
}
