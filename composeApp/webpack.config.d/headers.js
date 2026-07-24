;(function(config) {
  if (config.devServer) {
    config.devServer.headers = [
        { key: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
        // credentialless (not require-corp): Cloudflare Turnstile's iframe fails under require-corp.
        // Still yields crossOriginIsolated, so OPFS/SQLite keeps working. See docs/WEB_DEPLOYMENT.md.
        { key: 'Cross-Origin-Embedder-Policy', value: 'credentialless' }
    ]
  }
})(config);