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
package app.cash.redwood.protocol.widget

import app.cash.redwood.protocol.ChildrenDiff
import app.cash.redwood.protocol.ChildrenDiff.Companion.RootChildrenTag
import app.cash.redwood.protocol.Diff
import app.cash.redwood.protocol.Id
import app.cash.redwood.widget.MutableListChildren
import example.redwood.widget.DiffConsumingExampleSchemaWidgetFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProtocolDisplayTest {
  @Test fun insertRootIdThrows() {
    val protocolDisplay = ProtocolDisplay(
      container = MutableListChildren(),
      factory = DiffConsumingExampleSchemaWidgetFactory(EmptyExampleSchemaWidgetFactory()),
      eventSink = ::error,
    )
    val diff = Diff(
      childrenDiffs = listOf(
        ChildrenDiff.Insert(
          id = Id.Root,
          tag = RootChildrenTag,
          childId = Id.Root,
          kind = 4 /* button */,
          index = 0,
        ),
      ),
    )
    val t = assertFailsWith<IllegalArgumentException> {
      protocolDisplay.sendDiff(diff)
    }
    assertEquals("Insert attempted to replace existing widget with ID 0", t.message)
  }

  @Test fun duplicateIdThrows() {
    val protocolDisplay = ProtocolDisplay(
      container = MutableListChildren(),
      factory = DiffConsumingExampleSchemaWidgetFactory(EmptyExampleSchemaWidgetFactory()),
      eventSink = ::error,
    )
    val diff = Diff(
      childrenDiffs = listOf(
        ChildrenDiff.Insert(
          id = Id.Root,
          tag = RootChildrenTag,
          childId = Id(1U),
          kind = 4 /* button */,
          index = 0,
        ),
      ),
    )
    protocolDisplay.sendDiff(diff)
    val t = assertFailsWith<IllegalArgumentException> {
      protocolDisplay.sendDiff(diff)
    }
    assertEquals("Insert attempted to replace existing widget with ID 1", t.message)
  }
}
