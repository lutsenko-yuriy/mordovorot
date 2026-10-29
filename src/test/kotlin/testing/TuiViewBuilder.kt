package testing

import analytics.AnalyticsService
import buildTuiView
import view.tui.MouseInput
import view.tui.Terminal
import view.tui.TuiInput
import view.tui.TuiView
import viewmodel.KeyboardLayout
import viewmodel.KeyboardLayoutPreference
import viewmodel.ModeSwitcher
import viewmodel.SessionKeyboardLayout
import viewmodel.ViewModel

/** The one place tests build a [TuiView] (GH-78): the production wiring with test defaults. */
fun testTuiView(
    terminal: Terminal,
    analytics: AnalyticsService = RecordingAnalyticsService(),
    input: TuiInput = MouseInput(),
    modeSwitcher: ModeSwitcher = NoopModeSwitcher(),
    viewModel: ViewModel,
    keyboardLayout: SessionKeyboardLayout = sessionKeyboardLayout(),
): TuiView = buildTuiView(terminal, analytics, input, modeSwitcher, viewModel, keyboardLayout)

/** A [SessionKeyboardLayout] starting at [layout], over an in-memory store. */
fun sessionKeyboardLayout(layout: KeyboardLayout = KeyboardLayout.DEFAULT): SessionKeyboardLayout =
    SessionKeyboardLayout(KeyboardLayoutPreference(FakePreferenceRepository(mutableMapOf("keyboard_layout" to layout.wireName))))
