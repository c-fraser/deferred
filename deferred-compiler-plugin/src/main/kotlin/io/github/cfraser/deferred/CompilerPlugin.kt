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

import com.google.auto.service.AutoService
import io.github.cfraser.deferred.CompilerPlugin.Generator
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName

/**
 * [CompilerPlugin] is a [ComponentRegistrar] that registers the [Generator] to which transforms the
 * compiler *IR* to enable *defer statements* in Kotlin.
 */
@AutoService(ComponentRegistrar::class)
internal class CompilerPlugin : ComponentRegistrar {

  /** Register the [Generator]. */
  override fun registerProjectComponents(
      project: MockProject,
      configuration: CompilerConfiguration,
  ) {
    IrGenerationExtension.registerExtension(project, Generator)
  }

  /**
   * [Generator] is an [IrGenerationExtension] that generates transformed *IR* using the
   * [Transformer].
   */
  private object Generator : IrGenerationExtension {

    /** Transform each of the [IrModuleFragment.files] using [Transformer]. */
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
      moduleFragment.files.forEach { file -> Transformer(pluginContext).visitFile(file) }
    }
  }

  /**
   * [Transformer] is an [IrElementTransformerVoidWithContext] that transforms [IrBlockBody] to
   * *defer* any *deferrable* statements.
   *
   * @property context the [IrPluginContext] to use to build the transformed [IrBlockBody]
   */
  private class Transformer(private val context: IrPluginContext) :
      IrElementTransformerVoidWithContext() {

    /** Visit the [IrBlockBody] and *defer* any *deferrable* statements. */
    override fun visitBlockBody(body: IrBlockBody): IrBody {
      val visited by lazy { super.visitBlockBody(body) }
      val scope = currentScope?.scope ?: return visited
      val (deferrable, statements) =
          body.statements.partition { it.isDeferrable() }.takeUnless { it.first.isEmpty() }
              ?: return visited
      val builder =
          DeclarationIrBuilder(context, scope.scopeOwnerSymbol, body.startOffset, body.endOffset)
      val returnType =
          statements.find { it is IrReturn }.let { it as? IrReturn }?.type
              ?: context.irBuiltIns.unitType
      return builder.defer(statements, deferrable, returnType)
    }

    private companion object {

      /**
       * Return whether the [IrStatement] is *deferrable*.
       *
       * The only [IrStatement] that is *deferrable* is a `io.github.cfraser.deferred.defer`
       * function call.
       */
      fun IrStatement.isDeferrable(): Boolean =
          this is IrCall && symbol.owner.kotlinFqName == FqName("io.github.cfraser.deferred.defer")

      /**
       * Build an [IrBlockBody] that executes the [IrStatement] instances in a *try-finally*
       * expression.
       *
       * The [statements] are added *as-is* to the *try* block, while the [deferrable] statements
       * are added in reverse order (LIFO) to the *finally* block.
       */
      fun DeclarationIrBuilder.defer(
          statements: List<IrStatement>,
          deferrable: List<IrStatement>,
          returnType: IrType
      ): IrBlockBody = irBlockBody {
        +IrTryImpl(
            startOffset,
            endOffset,
            returnType,
            irBlock { statements.forEach { +it } },
            emptyList(),
            irBlock { (deferrable.size - 1 downTo 0).forEach { +deferrable[it] } })
      }
    }
  }
}
