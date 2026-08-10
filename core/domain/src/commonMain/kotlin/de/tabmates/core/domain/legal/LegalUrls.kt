package de.tabmates.core.domain.legal

/**
 * Public legal pages on the marketing site. Deliberately not derived from
 * `BuildKonfig.BASE_URL_PUBLIC`: that points at the app host (app.tabmates.de) and, via the
 * environment switcher, can point at a custom backend — the privacy policy must always resolve to
 * the production site regardless of which server the app talks to.
 */
object LegalUrls {
    const val PRIVACY_POLICY = "https://tabmates.de/privacy/"
}
