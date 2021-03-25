package org.fifthgen.qbplayground.contexthandler;

import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fifthgen.qbplayground.event.AuthorizationEvent;
import org.fifthgen.qbplayground.event.bean.AuthorizationBean;
import org.fifthgen.qbplayground.server.SimpleHttpHandler;
import org.fifthgen.qbplayground.utility.Helper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Queue;

public class AuthRedirectHandler extends SimpleHttpHandler {

    private final Logger log = LogManager.getLogger(SimpleHttpHandler.class);

    private final Queue<AuthorizationEvent> authQueue;

    public AuthRedirectHandler(Queue<AuthorizationEvent> fetchTokenQueue) {
        this.authQueue = fetchTokenQueue;
    }

    public void addEvent(AuthorizationEvent event) {
    }

    @Override
    protected void handlePostRequest(HttpExchange httpExchange) {
    }

    @Override
    protected void handleGetRequest(HttpExchange httpExchange) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpExchange.getResponseBody()))) {
            StringBuilder respStr = new StringBuilder();
            respStr.append("Authentication successful!");
            respStr.append(System.lineSeparator());
            respStr.append("Auth code : ").append(getRequestParams().get("code"));

            // Process the authorize queue
            AuthorizationEvent authEvtHandler;
            while ((authEvtHandler = authQueue.remove()) != null) {
                AuthorizationBean authBean = new AuthorizationBean();
                authBean.setCode(getRequestParams().get("code"));
                authBean.setState(Helper.decodeState(getRequestParams().get("state")));
                authBean.setRealmId(getRequestParams().get("realmId"));

                authEvtHandler.onAuthorizationCompleted(authBean);
            }

            httpExchange.sendResponseHeaders(200, respStr.toString().length());

            writer.write(respStr.toString());
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to authenticate client : " + e.getLocalizedMessage());
        }
    }
}
