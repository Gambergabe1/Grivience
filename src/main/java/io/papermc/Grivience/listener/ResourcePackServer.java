package io.papermc.Grivience.listener;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.papermc.Grivience.GriviencePlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ResourcePackServer {
    private final GriviencePlugin plugin;
    private HttpServer server;
    private ExecutorService executor;
    private File servedFile;

    public ResourcePackServer(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public String serve(File file, String host, int port) throws IOException {
        if (server != null) {
            stop();
        }
        servedFile = file;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/" + file.getName(), new FileHandler());
        executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.start();
        return "http://" + host + ":" + port + "/" + file.getName();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (servedFile == null || !servedFile.exists()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", Files.probeContentType(servedFile.toPath()));
            headers.add("Content-Length", Long.toString(servedFile.length()));
            exchange.sendResponseHeaders(200, servedFile.length());
            try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(servedFile)) {
                fis.transferTo(os);
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to serve resource pack: " + ex.getMessage());
            }
        }
    }
}
