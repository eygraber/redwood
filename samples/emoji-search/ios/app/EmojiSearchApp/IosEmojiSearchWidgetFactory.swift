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

import Foundation

import UIKit
import shared

class IosEmojiSearchWidgetFactory<T : AnyObject>: WidgetEmojiSearchWidgetFactory {
    let treehouseApp: Redwood_treehouseTreehouseApp<T>
    let imageLoader = RemoteImageLoader()

    var RedwoodLayout: WidgetRedwoodLayoutWidgetFactory =
        Redwood_layout_uiviewUIViewRedwoodLayoutWidgetFactory(viewFactory: UIScrollViewFactory())

    init(treehouseApp: Redwood_treehouseTreehouseApp<T>) {
        self.treehouseApp = treehouseApp
    }

    func LazyColumn() -> WidgetLazyColumn {
        return LazyColumnBinding(treehouseApp: treehouseApp)
    }
    func TextInput() -> WidgetTextInput {
        return TextInputBinding()
    }
    func Text() -> WidgetText {
        return TextBinding()
    }
    func Image() -> WidgetImage {
        return ImageBinding(imageLoader: imageLoader)
    }
}

private class UIScrollViewFactory: Redwood_layout_uiviewRedwoodUIScrollViewFactory {
    func create(delegate: Redwood_layout_uiviewRedwoodUIScrollViewDelegate) -> UIScrollView {
        return DelegateUIScrollView(delegate)
    }
}

private class DelegateUIScrollView : UIScrollView {
    private var layoutDelegate: Redwood_layout_uiviewRedwoodUIScrollViewDelegate

    init(_ delegate: Redwood_layout_uiviewRedwoodUIScrollViewDelegate) {
        self.layoutDelegate = delegate
        super.init(frame: .zero)
    }

    required init?(coder: NSCoder) {
        fatalError("unimplemented")
    }

    override var intrinsicContentSize: CGSize {
        let outputSize = layoutDelegate.intrinsicContentSize
        return CGSize(width: outputSize.width, height: outputSize.height)
    }

    override func sizeThatFits(_ size: CGSize) -> CGSize {
        let inputSize = Redwood_layout_uiviewUnsafeSize(width: size.width, height: size.height)
        let outputSize = layoutDelegate.sizeThatFits(size: inputSize)
        return CGSize(width: outputSize.width, height: outputSize.height)
    }

    override func setNeedsLayout() {
        super.setNeedsLayout()
        layoutDelegate.setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        layoutDelegate.layoutSubviews()
    }
}
