package ng.commu.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class AppMode {
    APP,      // Community member experience (posts, messages, notifications)
    CONSOLE   // Community management (boards, communities, account settings)
}

@HiltViewModel
class AppModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "app_mode"
        private const val KEY_MODE = "current_mode"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentMode = MutableStateFlow(loadSavedMode())
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    private fun loadSavedMode(): AppMode {
        val savedMode = prefs.getString(KEY_MODE, AppMode.APP.name)
        return try {
            AppMode.valueOf(savedMode ?: AppMode.APP.name)
        } catch (e: IllegalArgumentException) {
            AppMode.APP
        }
    }

    fun switchMode(mode: AppMode) {
        _currentMode.value = mode
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun toggleMode() {
        val newMode = if (_currentMode.value == AppMode.APP) AppMode.CONSOLE else AppMode.APP
        switchMode(newMode)
    }
}
