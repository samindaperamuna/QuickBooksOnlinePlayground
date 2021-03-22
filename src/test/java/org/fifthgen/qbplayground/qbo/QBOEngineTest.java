package org.fifthgen.qbplayground.qbo;

import com.intuit.oauth2.config.Scope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fifthgen.qbplayground.contexthandler.AuthRedirectHandler;
import org.fifthgen.qbplayground.test.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

class QBOEngineTest {

    private QBOEngine engine;
    private List<Scope> scopes;

    private Logger log;

    @BeforeEach
    void setUp() {
        log = LogManager.getLogger(getClass());

        engine = Context.getInstance().engine;

        scopes = new ArrayList<>();
        scopes.add(Scope.Accounting);
        scopes.add(Scope.OpenIdAll);
    }

    @AfterEach
    void tearDown() {
        engine = null;
    }

    @Test
    void requestAuthorizationUrlTest() {
        Assertions.assertNotNull(engine.requestAuthorizationUrl(scopes));
        Assertions.assertFalse(engine.requestAuthorizationUrl(scopes).isEmpty());
    }

    @Test
    void fetchTokensTest() {
        if (!engine.authenticateClient()) {
            String url = engine.requestAuthorizationUrl(scopes);

            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                log.error("Failed to navigate to URL : " + e.getLocalizedMessage());
            }

            Context.getInstance().startServer(new AuthRedirectHandler(authBean -> {
                Assertions.assertNotNull(authBean);

                engine.fetchTokens(authBean);
            }));

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void authenticateClientTest() {
        Assertions.assertTrue(engine.authenticateClient());
    }

    @Test
    void getUserInfoTest() {
        Assertions.assertNotNull(engine.getUserInfo());
    }

    @Test
    void getCustomersTest() {
        Assertions.assertNotNull(engine.getCustomers());
    }
}