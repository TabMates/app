package de.tabmates.core.designsystem.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

@Composable
fun TabMatesInlineLinkText(
    textBeforeLink: String,
    linkText: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
    textAfterLink: String = "",
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
) {
    val text =
        buildAnnotatedString {
            append(textBeforeLink)
            withLink(
                LinkAnnotation.Clickable(
                    tag = "inline_link",
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                    linkInteractionListener = { onLinkClick() },
                ),
            ) {
                append(linkText)
            }
            append(textAfterLink)
        }

    Text(
        text = text,
        modifier = modifier,
        style = textStyle.copy(color = textColor),
    )
}
