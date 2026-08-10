package de.tabmates.features.authentication.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import de.tabmates.core.designsystem.preview.PreviewThemes
import de.tabmates.core.designsystem.text.TabMatesInlineLinkText
import de.tabmates.core.designsystem.theme.TabMatesTheme
import de.tabmates.core.domain.legal.LegalUrls
import org.jetbrains.compose.resources.stringResource
import tabmatesapp.features.authentication.presentation.generated.resources.Res
import tabmatesapp.features.authentication.presentation.generated.resources.privacy_notice_register_prefix
import tabmatesapp.features.authentication.presentation.generated.resources.privacy_notice_suffix
import tabmatesapp.features.authentication.presentation.generated.resources.privacy_policy_link

/**
 * Fine print shown wherever an account is created, so the privacy policy is one tap away at the
 * moment data is actually collected. Informational only — no consent checkbox, because sign-up
 * data is processed to perform the contract, not on the basis of consent.
 *
 * [prefix] is the sentence up to the link; the trailing part comes from `privacy_notice_suffix`,
 * which German needs for the separable verb particle.
 */
@Composable
fun PrivacyNotice(
    prefix: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    TabMatesInlineLinkText(
        modifier = modifier,
        textBeforeLink = prefix,
        linkText = stringResource(Res.string.privacy_policy_link),
        textAfterLink = stringResource(Res.string.privacy_notice_suffix),
        onLinkClick = { uriHandler.openUri(LegalUrls.PRIVACY_POLICY) },
        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewThemes
@Composable
private fun PrivacyNoticePreview() {
    TabMatesTheme {
        Surface {
            PrivacyNotice(prefix = stringResource(Res.string.privacy_notice_register_prefix))
        }
    }
}
