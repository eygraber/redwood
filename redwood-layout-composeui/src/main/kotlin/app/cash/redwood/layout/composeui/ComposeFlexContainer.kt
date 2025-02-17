/*
 * Copyright (C) 2022 Square, Inc.
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
package app.cash.redwood.layout.composeui

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import app.cash.redwood.flexcontainer.AlignItems
import app.cash.redwood.flexcontainer.FlexContainer
import app.cash.redwood.flexcontainer.FlexDirection
import app.cash.redwood.flexcontainer.JustifyContent
import app.cash.redwood.flexcontainer.isHorizontal
import app.cash.redwood.layout.api.Overflow
import app.cash.redwood.layout.api.Padding
import app.cash.redwood.widget.Widget
import app.cash.redwood.widget.compose.ComposeWidgetChildren

internal class ComposeFlexContainer(private val direction: FlexDirection) {
  private val container = FlexContainer().apply {
    flexDirection = direction
    roundToInt = true
  }

  private val _children = ComposeWidgetChildren()
  val children: Widget.Children<@Composable () -> Unit> get() = _children

  private var recomposeTick by mutableStateOf(0)
  private var overflow by mutableStateOf(Overflow.Clip)

  private lateinit var context: Context
  private var padding = Padding.Zero

  var modifier: Modifier by mutableStateOf(Modifier)

  fun padding(padding: Padding) {
    this.padding = padding
    recomposeTick++
  }

  fun overflow(overflow: Overflow) {
    this.overflow = overflow
  }

  fun alignItems(alignItems: AlignItems) {
    container.alignItems = alignItems
    recomposeTick++
  }

  fun justifyContent(justifyContent: JustifyContent) {
    container.justifyContent = justifyContent
    recomposeTick++
  }

  val composable: @Composable () -> Unit = {
    Layout(
      content = {
        // Observe this so we can manually trigger recomposition.
        recomposeTick

        // Get this for use in 'measure'.
        context = LocalContext.current

        _children.render()
      },
      modifier = if (overflow == Overflow.Scroll) {
        if (direction.isHorizontal) {
          modifier.horizontalScroll(rememberScrollState())
        } else {
          modifier.verticalScroll(rememberScrollState())
        }
      } else {
        modifier
      },
      measurePolicy = ::measure,
    )
  }

  private fun measure(
    scope: MeasureScope,
    measurables: List<Measurable>,
    constraints: Constraints,
  ): MeasureResult = with(scope) {
    syncItems(measurables)
    container.padding = padding.toSpacing(context)

    val (widthSpec, heightSpec) = constraints.toMeasureSpecs()
    val result = container.measure(widthSpec, heightSpec)
    val (layoutWidth, layoutHeight) = result.containerSize

    return layout(layoutWidth.toInt(), layoutHeight.toInt()) {
      container.layout(result)

      for (item in container.items) {
        (item.measurable as ComposeMeasurable).placeable.place(item.left.toInt(), item.top.toInt())
      }
    }
  }

  private fun syncItems(measurables: List<Measurable>) {
    container.items.clear()
    measurables.forEachIndexed { index, measurable ->
      container.items += measurable.asItem(context, _children.widgets[index].layoutModifiers, direction)
    }
  }
}
