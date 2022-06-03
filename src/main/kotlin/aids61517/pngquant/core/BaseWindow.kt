package aids61517.pngquant.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope

abstract class BaseWindow {
  fun show() {
    Application.openWindow(this)
  }

  fun close() {
    Application.closeWindow(this)
  }

  @Composable
  fun startWindow(scope: ApplicationScope) {
    scope.run {
      setupWindow()
    }
  }

  @Composable abstract fun ApplicationScope.setupWindow()
}