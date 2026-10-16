package testing

import analytics.AnalyticsService
import view.tui.MouseInput
import view.tui.Terminal
import view.tui.TuiInput
import view.tui.TuiView
import viewmodel.ModeSwitcher
import viewmodel.NoopModeSwitcher
import viewmodel.ViewModel

/** The one place tests build a [TuiView] (GH-78), with test defaults. */
fun testTuiView(
    terminal: Terminal,
    analytics: AnalyticsService = RecordingAnalyticsService(),
    input: TuiInput = MouseInput(),
    modeSwitcher: ModeSwitcher = NoopModeSwitcher(),
    viewModel: ViewModel,
): TuiView = TuiView.create(terminal, analytics, input, modeSwitcher, viewModel)
