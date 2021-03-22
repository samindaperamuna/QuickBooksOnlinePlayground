package org.fifthgen.qbplayground.test;

import com.intuit.oauth2.config.Environment;
import org.fifthgen.qbplayground.contexthandler.AuthRedirectHandler;
import org.fifthgen.qbplayground.qbo.QBOEngine;
import org.fifthgen.qbplayground.server.SimpleHttpServer;
import org.fifthgen.qbplayground.utility.Helper;

import java.util.Properties;

public class Context {

    private static final String COMPANY_ID = "companyId";
    private static final String CLIENT_ID = "OAuth2AppClientId";
    private static final String CLIENT_SECRET = "OAuth2AppClientSecret";
    private static final String REDIRECT_URL = "OAuth2AppRedirectUri";
    private static final String ACCOUNTING_API_HOST = "IntuitAccountingAPIHost";

    private static Context CONTEXT;

    public QBOEngine engine;

    private Context() {
        Properties properties = Helper.readProperties();

        String companyId = properties.getProperty(COMPANY_ID);
        String clientId = properties.getProperty(CLIENT_ID);
        String clientSecret = properties.getProperty(CLIENT_SECRET);
        String redirectURL = properties.getProperty(REDIRECT_URL);
        String accountingAPIHost = properties.getProperty(ACCOUNTING_API_HOST);

        this.engine = new QBOEngine(companyId,
                clientId,
                clientSecret,
                redirectURL,
                accountingAPIHost,
                Environment.SANDBOX);
    }

    public static Context getInstance() {
        if (CONTEXT == null) {
            CONTEXT = new Context();
        }

        return CONTEXT;
    }

    public void startServer(AuthRedirectHandler redirectHandler) {
        SimpleHttpServer server = SimpleHttpServer.getInstance();
        server.createContext("/oauth2redirect", redirectHandler);
        server.startServer();
    }
}
