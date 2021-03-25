package org.fifthgen.qbplayground.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleHttpServer {

    private static SimpleHttpServer INSTANCE = null;

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_THREAD_COUNT = 5;

    private final Logger log;
    private final ExecutorService executor;

    private HttpServer server;

    private SimpleHttpServer() {
        this.log = LogManager.getLogger(getClass().getSimpleName());
        this.executor = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);

        try {
            this.server = HttpServer.create();

            log.debug("Server instance created");
        } catch (IOException e) {
            log.error("Failed to create the HTTP server : " + e.getLocalizedMessage());
        }
    }

    public static SimpleHttpServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SimpleHttpServer();
        }

        return INSTANCE;
    }

    /**
     * Starts the HTTP server using the default values (default host : {@value DEFAULT_HOST},
     * default port : {@value DEFAULT_PORT}.
     *
     * @return An indication of the status of the server ( 0 if the server started successfully and else -1).
     */
    public int startServer() {
        return startServer(DEFAULT_HOST, DEFAULT_PORT);
    }

    public int startServer(String context) {
        return startServer(DEFAULT_HOST, DEFAULT_PORT);
    }

    public int startServer(String host, int port) {
        try {
            this.server.bind(new InetSocketAddress(host, port), 0);
            this.server.setExecutor(this.executor);
            this.server.start();

            log.info("Server started on port : " + port);

            return 0;
        } catch (IOException e) {
            log.error("Failed to bind the HTTP server to the specified InetSocket: " + e.getLocalizedMessage());
            return -1;
        }
    }

    /**
     * Terminate the server instance.
     */
    public void stopServer() {
        this.server.stop(1);
    }

    public HttpContext createContext(String context, SimpleHttpHandler contextHandler) {
        return this.server.createContext(context, contextHandler);
    }
}
