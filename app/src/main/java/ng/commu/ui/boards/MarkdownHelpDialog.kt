package ng.commu.ui.boards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ng.commu.R

@Composable
fun MarkdownHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.markdown_guide_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Text Formatting
                HelpSection(title = stringResource(R.string.markdown_text_formatting)) {
                    MarkdownExample("**bold**", stringResource(R.string.markdown_bold))
                    MarkdownExample("*italic*", stringResource(R.string.markdown_italic))
                    MarkdownExample("~~strikethrough~~", stringResource(R.string.markdown_strikethrough))
                    MarkdownExample("`code`", stringResource(R.string.markdown_inline_code))
                }

                // Headings
                HelpSection(title = stringResource(R.string.markdown_headings)) {
                    MarkdownExample("# Heading 1", "")
                    MarkdownExample("## Heading 2", "")
                    MarkdownExample("### Heading 3", "")
                }

                // Links
                HelpSection(title = stringResource(R.string.markdown_links)) {
                    MarkdownExample("[link text](https://example.com)", stringResource(R.string.markdown_links_description))
                    Text(
                        text = stringResource(R.string.markdown_links_auto),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Images
                HelpSection(title = stringResource(R.string.markdown_images)) {
                    MarkdownExample("![alt text](image-url)", stringResource(R.string.markdown_images_description))
                    Text(
                        text = stringResource(R.string.markdown_images_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Lists
                HelpSection(title = stringResource(R.string.markdown_lists)) {
                    Text(
                        text = stringResource(R.string.markdown_unordered),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CodeBlock(
                        """- Item 1
- Item 2
- Item 3"""
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.markdown_ordered),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    CodeBlock(
                        """1. First item
2. Second item
3. Third item"""
                    )
                }

                // Blockquotes
                HelpSection(title = stringResource(R.string.markdown_blockquotes)) {
                    CodeBlock("> This is a quote")
                }

                // Code Blocks
                HelpSection(title = stringResource(R.string.markdown_code_blocks)) {
                    CodeBlock(
                        """```
code block
with multiple lines
```"""
                    )
                }

                // Line Breaks
                HelpSection(title = stringResource(R.string.markdown_line_breaks)) {
                    Text(
                        text = stringResource(R.string.markdown_line_breaks_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CodeBlock(
                        """First paragraph.

Second paragraph."""
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
            }
        }
    )
}

@Composable
private fun HelpSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

@Composable
private fun MarkdownExample(
    syntax: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = syntax,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }

        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
    }
}
