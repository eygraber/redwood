package example.web.sunspot

import app.cash.treehouse.protocol.PropertyDiff
import example.sunspot.client.SunspotButton
import example.sunspot.client.SunspotNode
import example.sunspot.client.SunspotText
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement

class HtmlSunspotElement(
  override val value: HTMLElement,
) : SunspotNode<HTMLElement> {
  override fun apply(diff: PropertyDiff) {
    throw UnsupportedOperationException()
  }
}

class HtmlSunspotText(
  override val value: HTMLSpanElement,
) : SunspotText<HTMLSpanElement> {
  override fun text(text: String?) {
    value.textContent = text
  }

  override fun color(color: String) {
    value.style.color = color
  }
}

class HtmlSunspotButton(
  override val value: HTMLButtonElement,
  private val onClick: () -> Unit,
) : SunspotButton<HTMLButtonElement> {
  override fun text(text: String?) {
    value.textContent = text
  }

  override fun enabled(enabled: Boolean) {
    value.disabled = !enabled
  }

  override fun onClick(onClick: Boolean) {
    value.onclick = if (onClick) {
      { this.onClick.invoke() }
    } else {
      null
    }
  }
}
