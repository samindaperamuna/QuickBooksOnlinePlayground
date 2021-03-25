package org.fifthgen.qbplayground.qbo;

import com.intuit.ipp.data.Customer;
import com.intuit.oauth2.config.Scope;
import net.jodah.concurrentunit.Waiter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fifthgen.qbplayground.contexthandler.AuthRedirectHandler;
import org.fifthgen.qbplayground.event.AuthorizationEvent;
import org.fifthgen.qbplayground.event.FetchTokensEvent;
import org.fifthgen.qbplayground.server.SimpleHttpServer;
import org.fifthgen.qbplayground.test.Context;
import org.junit.jupiter.api.*;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class QBOEngineTest {

    private QBOEngine engine;
    private List<Scope> scopes;
    private static final int DELAY = 30;
    private static Logger log;

    private static SimpleHttpServer server;
    private static Queue<AuthorizationEvent> authQueue;
    private Waiter waiter;

    @BeforeAll
    static void beforeAll() {
        log = LogManager.getLogger(QBOEngine.class);

        authQueue = new PriorityQueue<>();
        server = Context.getInstance().startServer(new AuthRedirectHandler(authQueue));
    }

    @AfterAll
    static void afterAll() {
        server.stopServer();
    }

    @BeforeEach
    void setUp() {
        engine = Context.getInstance().engine;

        scopes = new ArrayList<>();
        scopes.add(Scope.Accounting);
        scopes.add(Scope.Payments);
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
        authorizeClient(Assertions::assertNotNull);
    }

    @Test
    void authenticateClientTest() throws InterruptedException, TimeoutException {
        waiter = new Waiter();

        authorizeClientHelper(authorizationBean -> {
            engine.fetchTokens(authorizationBean);

            waiter.assertTrue(engine.authenticateClient());
            waiter.resume();
        });

        waiter.await(DELAY, TimeUnit.SECONDS);
    }

    @Test
    void getUserInfoTest() throws TimeoutException, InterruptedException {
        waiter = new Waiter();

        authenticationHelper(() -> {
            waiter.assertNotNull(engine.getUserInfo());
            waiter.resume();
        });

        waiter.await(DELAY, TimeUnit.SECONDS);
    }

    @Test
    void getCustomersTest() throws TimeoutException, InterruptedException {
        waiter = new Waiter();

        authenticationHelper(() -> {
            waiter.assertNotNull(engine.getCustomers());
            waiter.resume();
        });

        waiter.await(DELAY, TimeUnit.SECONDS);
    }

    @Test
    void addCustomerTest() throws TimeoutException, InterruptedException {
        waiter = new Waiter();

        authenticationHelper(() -> {
            Customer c = new Customer();
            c.setDisplayName("Samantha");

            Customer res = engine.addCustomer(c);

            waiter.assertEquals(c.getDisplayName(), res.getDisplayName());
            waiter.resume();
        });

        waiter.await(DELAY, TimeUnit.SECONDS);
    }

    /**
     * Helper method to authenticate the client.
     *
     * @param event {@link FetchTokensEvent} instance to be passed on.
     */
    private void authenticationHelper(FetchTokensEvent event) {
        if (!engine.authenticateClient()) {
            authorizeClient(authorizationBean -> {
                engine.fetchTokens(authorizationBean);
                event.onFetchTokenCompleted();
            });
        } else {
            event.onFetchTokenCompleted();
        }
    }

    /**
     * Helper method to authorize the client.
     *
     * @param event {@link AuthorizationEvent} instance which will be passed on.
     */
    private void authorizeClientHelper(AuthorizationEvent event) {
        if (!engine.authenticateClient()) {
            authorizeClient(event);
        }
    }

    /**
     * Helper method which generates an authorization URL and authorizes the user using the system default browser.
     *
     * @param event An {@link AuthorizationEvent} instance which will be added to the processing queue.
     */
    private void authorizeClient(AuthorizationEvent event) {
        String url = engine.requestAuthorizationUrl(scopes);

        try {
            authQueue.add(event);
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            log.error("Failed to navigate to URL : " + e.getLocalizedMessage());
        }
    }
}