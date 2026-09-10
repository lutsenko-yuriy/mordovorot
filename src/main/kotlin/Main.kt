import presenter.PresenterImpl
import view.ViewImpl

/**
 * Created by yurich on 02.12.16.
 */
fun main() {
    val view = ViewImpl.create { view -> PresenterImpl(view) }
    view.play()
}
