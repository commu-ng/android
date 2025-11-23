package ng.commu.ui.components

import android.text.Html
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor)
                textSize = 16f
                setLineSpacing(6f, 1.5f)
            }
        },
        update = { textView ->
            val styledHtml = addParagraphSpacing(html)
            val spanned = Html.fromHtml(styledHtml, Html.FROM_HTML_MODE_COMPACT)
            textView.text = spanned
            textView.setTextColor(textColor)
        }
    )
}

private fun addParagraphSpacing(html: String): String {
    // Add CSS styling for paragraph spacing
    return """
        <style>
            p {
                margin-bottom: 1em;
                line-height: 1.6;
            }
        </style>
        $html
    """.trimIndent()
}
