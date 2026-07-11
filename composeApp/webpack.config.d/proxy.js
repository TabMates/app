;(function(config) {
  if (config.devServer) {
    // The backend has no CORS config, so the browser cannot call it directly
    // from the app origin. Proxy API and websocket traffic through the dev
    // server instead (same-origin). Requires BASE_URL_HTTP_WEB / BASE_URL_WS_WEB
    // in local.properties pointing at the dev server itself (http://localhost:8081).
    //
    // The proxy target follows the (uncommented) BASE_URL_HTTP in local.properties —
    // the same backend the other targets use — so switching between the local backend
    // and dev.tabmates.de is one toggle. This file is concatenated into
    // build/wasm/packages/<module>/webpack.config.js, hence the upward search.
    const fs = require('fs');
    const path = require('path');

    let target = 'http://localhost:8080';
    let dir = __dirname;
    for (let i = 0; i < 6; i++) {
      const candidate = path.join(dir, 'local.properties');
      if (fs.existsSync(candidate)) {
        const match = fs.readFileSync(candidate, 'utf8').match(/^BASE_URL_HTTP=(.+)$/m);
        if (match) target = match[1].trim();
        break;
      }
      dir = path.dirname(dir);
    }
    console.log('[tabmates-proxy] forwarding /api and /ws to ' + target);

    config.devServer.proxy = [
      {
        context: ['/api', '/ws'],
        target: target,
        ws: true,
        // Rewrite the Host header to the target's — required for name-based
        // virtual hosts / TLS SNI on dev.tabmates.de, harmless for localhost.
        changeOrigin: true,
      },
    ]
  }
})(config);
