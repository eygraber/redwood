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
package app.cash.redwood.protocol.compose

import app.cash.redwood.protocol.ChildrenDiff
import app.cash.redwood.protocol.Diff
import app.cash.redwood.protocol.DiffSink
import app.cash.redwood.protocol.LayoutModifiers
import app.cash.redwood.protocol.PropertyDiff

/**
 * Aggregate [ChildrenDiff]s and [PropertyDiff]s until [trySend] sends them as a [Diff] to
 * [diffSink]. Each call to [trySend] clears the internal state back to empty.
 *
 * This class is not thread safe.
 */
internal class DiffAppender(private val diffSink: DiffSink) {
  private var childrenDiffs = mutableListOf<ChildrenDiff>()
  private var layoutModifiers = mutableListOf<LayoutModifiers>()
  private var propertyDiffs = mutableListOf<PropertyDiff>()

  fun append(childrenDiff: ChildrenDiff) {
    childrenDiffs += childrenDiff
  }

  fun append(layoutModifiers: LayoutModifiers) {
    this.layoutModifiers += layoutModifiers
  }

  fun append(propertyDiff: PropertyDiff) {
    propertyDiffs += propertyDiff
  }

  /**
   * If there were any calls to [append] since the last call to this function, send them as a [Diff]
   * to [diffSink] and reset the internal lists to be empty. This function is a no-op if there were
   * no calls to [append] since the last invocation.
   */
  fun trySend() {
    val existingChildrenDiffs = childrenDiffs
    val existingLayoutModifierDiffs = layoutModifiers
    val existingPropertyDiffs = propertyDiffs
    if (existingPropertyDiffs.isNotEmpty() || existingLayoutModifierDiffs.isNotEmpty() || existingChildrenDiffs.isNotEmpty()) {
      childrenDiffs = mutableListOf()
      layoutModifiers = mutableListOf()
      propertyDiffs = mutableListOf()

      val diff = Diff(
        childrenDiffs = existingChildrenDiffs,
        layoutModifiers = existingLayoutModifierDiffs,
        propertyDiffs = existingPropertyDiffs,
      )
      diffSink.sendDiff(diff)
    }
  }
}
