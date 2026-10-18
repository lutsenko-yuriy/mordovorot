package view.tui

/** The visible part of the solution screen's step list (GH-126): [offset] is the first step
 *  shown. [update] runs on every repaint; a wheel notch or W/S [scroll]s. */
internal class StepListWindow {

    var offset = 0
        private set

    private var total = 0
    private var visible = 0
    private var followed: Int? = null

    /** Clamps to [total] steps in [visible] rows, and brings [current] into view when it
     *  changed since the last call - so a scroll away from it holds until the next step. Back
     *  to no current step (Restart) is back to the top. */
    fun update(total: Int, visible: Int, current: Int?) {
        this.total = total
        this.visible = visible
        if (current == null && followed != null) offset = 0
        if (current != null && current != followed) {
            if (current < offset) offset = current
            if (current >= offset + visible) offset = current - visible + 1
        }
        followed = current
        clamp()
    }

    fun scroll(delta: Int) {
        offset += delta
        clamp()
    }

    fun reset() {
        offset = 0
        followed = null
    }

    private fun clamp() {
        offset = offset.coerceIn(0, (total - visible).coerceAtLeast(0))
    }
}
