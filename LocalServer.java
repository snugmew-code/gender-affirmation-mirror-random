import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class LocalServer {
    private static final Path ROOT = Paths.get("").toAbsolutePath().normalize();

    public static void main(String[] args) throws IOException {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", LocalServer::serve);
        server.start();
        System.out.println("Serving files from: " + ROOT);
        System.out.println("Open http://127.0.0.1:" + port + "/ in your browser");
        System.out.println("Press Ctrl+C to stop the server.");
    }

    private static void serve(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            String uriPath = exchange.getRequestURI().getPath();
            Path file = ROOT.resolve(uriPath.replaceFirst("^/+", "")).normalize();
            if (!file.startsWith(ROOT)) {
                sendError(exchange, 403, "Forbidden");
                return;
            }
            if (Files.isDirectory(file)) file = file.resolve("index.html");
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                sendError(exchange, 404, "Not found");
                return;
            }

            byte[] bytes = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", mime(file));
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            if (method.equals("HEAD")) {
                exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, bytes.length);
                try (java.io.OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
        } catch (Exception e) {
            System.err.println("Request error: " + e.getMessage());
            try { sendError(exchange, 500, "Internal server error"); } catch (IOException ignored) { }
        } finally {
            exchange.close();
        }
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (java.io.OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String mime(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".txt")) return "text/plain; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".avif")) return "image/avif";
        if (name.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
