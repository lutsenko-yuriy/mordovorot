// GH-110: serve on localhost only. KGP's devServer block has no host, and webpack-dev-server
// otherwise listens on every network interface.
if (config.devServer) {
    config.devServer.host = "localhost";
}
