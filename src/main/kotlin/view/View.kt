package view

/**
 * Created by yurich on 08.12.16.
 */
interface View {
    /** [notice], when given, is shown on the first screen - e.g. why a mode switch didn't
     *  happen (GH-79): stderr would be hidden behind the full-screen TUI. */
    suspend fun play(notice: String? = null)
}
