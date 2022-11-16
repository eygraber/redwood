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
package app.cash.zipline.samples.emojisearch.views

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.redwood.LayoutModifier
import app.cash.redwood.treehouse.TreehouseApp
import app.cash.redwood.treehouse.TreehouseWidgetView
import example.schema.widget.LazyColumn
import example.values.LazyListIntervalContent

class ViewLazyColumn<T : Any>(
  private val treehouseApp: TreehouseApp<T>,
  override val value: FrameLayout,
) : LazyColumn<View> {
  override var layoutModifiers: LayoutModifier = LayoutModifier

  private val recyclerView = RecyclerView(value.context).apply {
    layoutManager = LinearLayoutManager(value.context)
  }

  init {
    value.addView(
      recyclerView,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT,
    )
  }

  private class ViewHolder(val view: LinearLayout) : RecyclerView.ViewHolder(view)

  override fun intervals(intervals: List<LazyListIntervalContent>) {
    value.updateLayoutParams<ViewGroup.LayoutParams> {
      width = ViewGroup.LayoutParams.MATCH_PARENT
      height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    recyclerView.adapter = object : RecyclerView.Adapter<ViewHolder>() {
      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
          LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
          },
        )

      override fun getItemCount() = intervals.size

      override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        for (index in 0 until intervals[position].count) {
          holder.view.addView(
            TreehouseWidgetView(value.context, treehouseApp).apply {
              setContent {
                intervals[position].itemProvider.get(index)
              }
            },
            ViewGroup.LayoutParams.WRAP_CONTENT,
            200,
          )
        }
      }
    }
  }
}
