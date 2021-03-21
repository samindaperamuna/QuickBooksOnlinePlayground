package org.fifthgen.qbplayground.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.util.HashMap;
import java.util.Map;

public abstract class SimpleHttpHandler implements HttpHandler {

    private HttpExchange exchange;

    @Override
    public void handle(HttpExchange httpExchange) {
        this.exchange = httpExchange;

        if ("GET".equals(httpExchange.getRequestMethod())) {
            handleGetRequest(httpExchange);
        } else if ("POST".equals(httpExchange.getRequestMethod())) {
            handlePostRequest(httpExchange);
        }
    }

    protected Map<String, String> getRequestParams() {
        Map<String, String> paramMap = new HashMap<>();

        if (exchange != null) {
            String[] splitURL = exchange.getRequestURI().toString().split("\\?");
            String queryString = splitURL.length > 1 ? splitURL[1] : "";

            String[] params = !queryString.isEmpty() ? queryString.split("\\&") : new String[0];

            for (String param : params) {
                String[] splitParam = param.split("=");

                if (splitParam.length > 1) {
                    paramMap.put(splitParam[0], splitParam[1]);
                }
            }
        }

        return paramMap;
    }

    protected abstract void handlePostRequest(HttpExchange httpExchange);

    protected abstract void handleGetRequest(HttpExchange httpExchange);
}
