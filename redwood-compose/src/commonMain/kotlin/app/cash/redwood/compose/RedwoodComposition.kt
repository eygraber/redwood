/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.redwood.compose

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Updater
import androidx.compose.runtime.currentComposer
import app.cash.redwood.LayoutModifier
import app.cash.redwood.widget.Widget
import kotlin.DeprecationLevel.ERROR
import kotlinx.coroutines.CoroutineScope

public interface RedwoodComposition {
  public fun setContent(content: @Composable () -> Unit)
  public fun cancel()
}

/**
 * @param scope A [CoroutineScope] whose [coroutineContext][kotlin.coroutines.CoroutineContext]
 * must have a [MonotonicFrameClock] key which is being ticked.
 */
@Suppress("UNUSED_PARAMETER")
@Deprecated("Not implemented yet", level = ERROR)
public fun <T : Any> RedwoodComposition(
  scope: CoroutineScope,
  container: Widget.Children<T>,
  factory: Widget.Factory<T>,
): RedwoodComposition {
  TODO()
}

/**
 * A version of [ComposeNode] which exposes the applier to the [factory] function. Through this
 * we expose the factory type [F] to our factory function so the correct widget can be created.
 *
 * @suppress For generated code usage only.
 */
@Composable
@Suppress("FunctionName") // Hiding from auto-complete.
public inline fun <F : Widget.Factory<*>, W : Widget<*>> _RedwoodComposeNode(
  crossinline factory: (F) -> W,
  update: @DisallowComposableCalls Updater<W>.() -> Unit,
  content: @Composable _RedwoodComposeContent<W>.() -> Unit,
) {
  // NOTE: You MUST keep the implementation of this function (or more specifically, the interaction
  //  with currentComposer) in sync with ComposeNode.
  currentComposer.startNode()

  if (currentComposer.inserting) {
    @Suppress("UNCHECKED_CAST") // Safe so long as you use generated composition function.
    val applier = currentComposer.applier as _RedwoodApplier<F>
    currentComposer.createNode {
      factory(applier.factory)
    }
  } else {
    currentComposer.useNode()
  }

  Updater<W>(currentComposer).update()
  _RedwoodComposeContent.Instance.content()

  currentComposer.endNode()
}

/**
 * @suppress For generated code usage only.
 */
@Suppress("ClassName") // Hiding from auto-complete.
public class _RedwoodComposeContent<out W : Widget<*>> {
  @Composable
  public fun into(
    accessor: (W) -> Widget.Children<*>,
    content: @Composable () -> Unit,
  ) {
    ComposeNode<_ChildrenWidget<*>, Applier<*>>(
      factory = {
        @Suppress("UNCHECKED_CAST")
        _ChildrenWidget(accessor as (Widget<Any>) -> Widget.Children<Any>)
      },
      update = {},
      content = content,
    )
  }

  public companion object {
    public val Instance: _RedwoodComposeContent<Nothing> = _RedwoodComposeContent()
  }
}

/**
 * A synthetic widget which allows the applier to differentiate between multiple groups of children.
 *
 * Compose's tree assumes each node only has single list of children. Or, put another way, even if
 * you apply multiple children Compose treats them as a single list of child nodes. In order to
 * differentiate between these children lists we introduce synthetic nodes. Every real node which
 * supports one or more groups of children will have one or more of these synthetic nodes as its
 * direct descendants. The nodes which are produced by each group of children will then become the
 * descendants of those synthetic nodes.
 *
 * @suppress For generated code usage only.
 */
// TODO Make this type private when the applier moves into this module.
@Suppress("ClassName") // Hiding from auto-complete.
public class _ChildrenWidget<T : Any> private constructor(
  public var accessor: ((Widget<T>) -> Widget.Children<T>)?,
  public var children: Widget.Children<T>?,
) : Widget<T> {
  public constructor(accessor: (Widget<T>) -> Widget.Children<T>) : this(accessor, null)
  public constructor(children: Widget.Children<T>) : this(null, children)

  override val value: Nothing get() = throw AssertionError()
  override var layoutModifiers: LayoutModifier
    get() = throw AssertionError()
    set(_) { throw AssertionError() }
}

/**
 * @suppress For generated code usage only.
 */
@Suppress("ClassName") // Hiding from auto-complete.
public interface _RedwoodApplier<F : Widget.Factory<*>> {
  public val factory: F
}
