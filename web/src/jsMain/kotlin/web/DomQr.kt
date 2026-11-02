package web

import kotlinx.browser.document
import org.w3c.dom.Element
import web.a11y.hiddenFromReaders
import web.qr.QrCode
import web.qr.QrSvg

private const val SVG_NS = "http://www.w3.org/2000/svg"

/** [code] as an inline SVG (GH-243): dark modules in one path, on the light ground, with the quiet zone. Hidden from
 *  readers: the link field beside it says the same. */
internal fun qrElement(code: QrCode): Element {
    val svg = document.createElementNS(SVG_NS, "svg").apply {
        setAttribute("class", "qr")
        setAttribute("viewBox", QrSvg.viewBox(code))
        setAttribute("shape-rendering", "crispEdges")
    }
    val background = document.createElementNS(SVG_NS, "rect").apply {
        setAttribute("width", QrSvg.extent(code).toString())
        setAttribute("height", QrSvg.extent(code).toString())
        setAttribute("class", "qr-light")
    }
    val modules = document.createElementNS(SVG_NS, "path").apply {
        setAttribute("d", QrSvg.path(code))
        setAttribute("class", "qr-dark")
    }
    svg.append(background, modules)
    return hiddenFromReaders(svg)
}
