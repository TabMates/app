;(function(config) {
  if (config.devServer) {
    // 8080 is taken by the local backend dev server.
    config.devServer.port = 8081
  }
})(config);
