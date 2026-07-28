import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import de.tabmates.convention.appVersion
import de.tabmates.convention.findPluginId
import de.tabmates.convention.libs
import de.tabmates.convention.pathToPackageName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import java.util.Properties

class BuildKonfigConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("buildkonfig"))
            }

            val localProperties: Properties by lazy {
                gradleLocalProperties(rootDir, rootProject.providers)
            }

            fun requireProperty(key: String): String =
                System.getenv(key)
                    ?: localProperties.getProperty(key)
                    ?: throw IllegalStateException(
                        "Missing \"$key\". Define it as an environment variable " +
                            "or in local.properties.",
                    )

            fun optionalProperty(key: String): String? = System.getenv(key) ?: localProperties.getProperty(key)

            // Shared with androidApp's versionName/versionCode — see Project.appVersion.
            val resolvedAppVersion: String = target.appVersion

            extensions.configure<BuildKonfigExtension> {
                packageName = target.pathToPackageName()
                defaultConfigs {
                    // Nullable so the wasmJs override can null it out (web no longer ships the
                    // api-key; the server allow-lists the browser Origin instead). Native targets
                    // are still populated with the real key at runtime — only the compile-time
                    // type becomes String?, so every consumer must null-guard it.
                    buildConfigField(FieldSpec.Type.STRING, "API_KEY", requireProperty("API_KEY"), nullable = true)
                    // Per-release token for the backend's client-version gate:
                    // base64url(HMAC-SHA256(secret, "<platform>|<version>")). CI mints it and passes
                    // only the result — the secret never reaches Gradle or the artifact, which is
                    // the whole point (unlike API_KEY, which ships verbatim and is extractable).
                    // Optional so PR and local builds work without it.
                    buildConfigField(
                        FieldSpec.Type.STRING,
                        "CLIENT_BUILD_TOKEN",
                        optionalProperty("CLIENT_BUILD_TOKEN"),
                        nullable = true,
                    )
                    buildConfigField(FieldSpec.Type.STRING, "BASE_URL_HTTP", requireProperty("BASE_URL_HTTP"))
                    buildConfigField(FieldSpec.Type.STRING, "BASE_URL_WS", requireProperty("BASE_URL_WS"))
                    // User-facing host for shareable links / deep links (e.g. https://app.tabmates.de),
                    // decoupled from the backend API host above. Required (like BASE_URL_HTTP) so the
                    // deep-link host is always explicit; same value on all targets.
                    buildConfigField(FieldSpec.Type.STRING, "BASE_URL_PUBLIC", requireProperty("BASE_URL_PUBLIC"))
                    buildConfigField(FieldSpec.Type.STRING, "APP_VERSION", resolvedAppVersion)
                    // Cloudflare Turnstile site key (a public identifier). Optional: only the web
                    // auth build renders the widget and reads it; every other target/module leaves
                    // it null. Nullable + optionalProperty so it is never a required build property.
                    buildConfigField(
                        FieldSpec.Type.STRING,
                        "TURNSTILE_SITE_KEY",
                        optionalProperty("TURNSTILE_SITE_KEY"),
                        nullable = true,
                    )
                    // Firebase Web Push certificate key (a public identifier, not a secret —
                    // see features/notifications/README.md). Optional: only the web push
                    // controller reads it; every other target/module leaves it null.
                    buildConfigField(
                        FieldSpec.Type.STRING,
                        "FCM_VAPID_KEY",
                        optionalProperty("FCM_VAPID_KEY"),
                        nullable = true,
                    )
                    buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "false")
                }
                targetConfigs {
                    create("debug") {
                        buildConfigField(FieldSpec.Type.BOOLEAN, "IS_DEBUG", "true")
                    }
                    // Optional per-target base URLs so all targets can run against a local
                    // backend at the same time: the Android emulator reaches the host via
                    // 10.0.2.2, the browser must go same-origin through the dev server proxy
                    // (the backend has no CORS config), and iOS simulator/desktop use the
                    // BASE_URL_HTTP/BASE_URL_WS defaults directly.
                    create("android") {
                        optionalProperty("BASE_URL_HTTP_ANDROID")?.let {
                            buildConfigField(FieldSpec.Type.STRING, "BASE_URL_HTTP", it)
                        }
                        optionalProperty("BASE_URL_WS_ANDROID")?.let {
                            buildConfigField(FieldSpec.Type.STRING, "BASE_URL_WS", it)
                        }
                    }
                    create("wasmJs") {
                        // Web sends no x-api-key / api_key: the server recognizes browser traffic by
                        // its allow-listed Origin. Null here; consumers guard with ?.let { }.
                        buildConfigField(FieldSpec.Type.STRING, "API_KEY", null, nullable = true)
                        // Same reasoning for the build token: a secret-derived value in a browser
                        // bundle is not secret. The server expects web to send none, and rejects any
                        // native platform claim that arrives with a browser Origin.
                        buildConfigField(FieldSpec.Type.STRING, "CLIENT_BUILD_TOKEN", null, nullable = true)
                        optionalProperty("BASE_URL_HTTP_WEB")?.let {
                            buildConfigField(FieldSpec.Type.STRING, "BASE_URL_HTTP", it)
                        }
                        optionalProperty("BASE_URL_WS_WEB")?.let {
                            buildConfigField(FieldSpec.Type.STRING, "BASE_URL_WS", it)
                        }
                    }
                }
            }
        }
    }
}
