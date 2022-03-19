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
@file:Suppress("unused")

package io.github.cfraser.deferred

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * [GradlePlugin] is a [KotlinCompilerPluginSupportPlugin] which applies the
 * *deferred-compiler-plugin* to the Kotlin compiler for the target project.
 */
class GradlePlugin : KotlinCompilerPluginSupportPlugin {

  override fun apply(target: Project) {
    target.dependencies.add("implementation", BuildConfig.API_DEPENDENCY)
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = true

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>) =
      kotlinCompilation.target.project.provider { emptyList<SubpluginOption>() }

  override fun getCompilerPluginId() =
      "${BuildConfig.COMPILER_PLUGIN_GROUP}.${BuildConfig.COMPILER_PLUGIN_NAME}"

  override fun getPluginArtifact() =
      SubpluginArtifact(
          BuildConfig.COMPILER_PLUGIN_GROUP,
          BuildConfig.COMPILER_PLUGIN_NAME,
          BuildConfig.COMPILER_PLUGIN_VERSION)
}
