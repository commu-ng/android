package ng.commu.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ng.commu.viewmodel.AppMode

@Composable
fun ModeToggleButton(
    currentMode: AppMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Icon(
            imageVector = when (currentMode) {
                AppMode.APP -> Icons.Default.Settings
                AppMode.CONSOLE -> Icons.Default.People
            },
            contentDescription = when (currentMode) {
                AppMode.APP -> "Switch to Console Mode"
                AppMode.CONSOLE -> "Switch to App Mode"
            }
        )
    }
}
