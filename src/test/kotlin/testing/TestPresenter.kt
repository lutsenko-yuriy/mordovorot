package testing

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import presenter.BasePresenter
import storage.FileSaveRepository
import storage.SaveRepository
import view.View

/**
 * A minimal concrete [BasePresenter] subclass with no extra members, so `BasePresenter*Test`
 * exercises the shared core without binding to either UI's subclass (GH-23).
 */
class TestPresenter(
    view: View,
    board: BoardModel = BoardImpl(),
    saves: SaveRepository = FileSaveRepository(),
    analytics: AnalyticsService = NoopAnalyticsService(),
) : BasePresenter(view, board, saves, analytics)
