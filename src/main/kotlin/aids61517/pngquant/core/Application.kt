package aids61517.pngquant.core

import androidx.compose.runtime.mutableStateListOf

object Application {
  val windows = mutableStateListOf<BaseWindow>()

  fun openWindow(window: BaseWindow) {
    windows.add(window)
  }

  fun closeWindow(window: BaseWindow) {
    windows.remove(window)
  }
}