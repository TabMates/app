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
    console.log('[tabmates-proxy] forwarding /api and /ws/group to ' + target);

    config.devServer.proxy = [
      {
        // /ws/group, not /ws: webpack-dev-server's own HMR socket is ws://localhost:8081/ws, and a
        // '/ws' context prefix-matches it too — the dev server would then proxy its own live-reload
        // socket to the backend, which answers with "Invalid frame header" and kills HMR.
        context: ['/api', '/ws/group'],
        target: target,
        ws: true,
        // Rewrite the Host header to the target's — required for name-based
        // virtual hosts / TLS SNI on dev.tabmates.de, harmless for localhost.
        changeOrigin: true,
        // Web no longer sends x-api-key; the backend authorizes the browser by its
        // allow-listed Origin instead. But calls to the app go same-origin through
        // this proxy (page + API both on :8081), and browsers omit the Origin header
        // on same-origin simple GETs — so the backend would see no key AND no Origin
        // and 401 at its ApiKeyFilter. Inject the dev-server origin (must match
        // port.js and the backend's cors.allowed-origins) so keyless auth passes.
        // (WS handshakes already carry Origin natively, so this is belt-and-suspenders
        // there — it re-sets the same value.)
        headers: { origin: 'http://localhost:8081' },
      },
    ]
  }
})(config);
