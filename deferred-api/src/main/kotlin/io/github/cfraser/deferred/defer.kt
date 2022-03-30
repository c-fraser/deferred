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

/**
 * Defer the invocation of the [function] until the surrounding *block* exits.
 *
 * The [function] is invoked after either of the following occurs.
 *
 * * A return statement is executed.
 * * The end of the *block body* is reached.
 * * The corresponding thread/coroutine throws an exception.
 *
 * *Deferred functions* are invoked immediately before the containing *block* exits, in the reverse
 * order they were deferred.
 *
 * Any exceptions thrown by the deferred [function] are ignored.
 */
fun defer(function: () -> Unit) {
  runCatching(function)
}
