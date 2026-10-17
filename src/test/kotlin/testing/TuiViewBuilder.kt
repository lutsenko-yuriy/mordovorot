package testing

import analytics.AnalyticsService
import buildTuiView
import view.tui.MouseInput
import view.tui.Terminal
import view.tui.TuiInput
import view.tui.TuiView
import viewmodel.ModeSwitcher
import viewmodel.ViewModel

/** The one place tests build a [TuiView] (GH-78): the production wiring with test defaults. */
fun testTuiView(
    terminal: Terminal,
    analytics: AnalyticsService = RecordingAnalyticsService(),
    input: TuiInput = MouseInput(),
    modeSwitcher: ModeSwitcher = NoopModeSwitcher(),
    viewModel: ViewModel,
): TuiView = buildTuiView(terminal, analytics, input, modeSwitcher, viewModel)
