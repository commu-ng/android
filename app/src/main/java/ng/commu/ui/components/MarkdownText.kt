package ng.commu.ui.components

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                textSize = 16f
                setLineSpacing(6f, 1.5f)
                this.maxLines = maxLines
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        },
        update = { textView ->
            val markwon = Markwon.builder(textView.context)
                .usePlugin(LinkifyPlugin.create())
                .build()

            markwon.setMarkdown(textView, markdown)
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            textView.maxLines = maxLines
        }
    )
}
