package ng.commu.ui.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ng.commu.data.model.CommunityPostReaction

@Composable
fun ReactionsSummary(
    reactions: List<CommunityPostReaction>,
    currentProfileId: String?,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group reactions by emoji and check if current user reacted
    val groupedReactions = reactions
        .groupBy { it.emoji }
        .map { (emoji, reactionList) ->
            val hasUserReacted = currentProfileId != null &&
                reactionList.any { it.user?.id == currentProfileId }
            Triple(emoji, reactionList.size, hasUserReacted)
        }
        .sortedByDescending { it.second }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedReactions.forEach { (emoji, count, hasUserReacted) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasUserReacted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = { onReactionClick(emoji) }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = if (hasUserReacted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
