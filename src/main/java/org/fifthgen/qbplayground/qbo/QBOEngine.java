package org.fifthgen.qbplayground.qbo;

import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.IEntity;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.query.GenerateQuery;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.Config;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.config.Scope;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.data.UserInfoResponse;
import com.intuit.oauth2.exception.InvalidRequestException;
import com.intuit.oauth2.exception.OAuthException;
import com.intuit.oauth2.exception.OpenIdException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fifthgen.qbplayground.event.bean.AuthorizationBean;
import org.fifthgen.qbplayground.utility.Helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.intuit.ipp.query.GenerateQuery.$;
import static com.intuit.ipp.query.GenerateQuery.select;

public class QBOEngine {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String OPEN_ID_TOKEN = "open_id_token";
    private static final String ACCESS_TOKEN_EXPIRES_IN = "access_token_expires_in";
    private static final String REFRESH_TOKEN_EXPIRES_IN = "refresh_token_expires_in";
    private static final String LAST_TOKEN_REQUEST_AT = "last_token_request_at";

    private static final String TX_FORMAT_JSON = "json";
    private static final String TX_FORMAT_XML = "xml";
    private static final String COMPRESSION_FORMAT_GZIP = "gzip";

    private static final SimpleDateFormat STANDARD_DATE_TIME = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);

    private final Logger log;
    private final Environment environment;

    private final String clientId;
    private final String clientSecret;
    private final String authorizationRedirect;

    private final OAuth2Config oAuth2Config;

    private final String companyId;
    private String accessToken;
    private String openIdToken;
    private String refreshToken;
    private Date lastTokenRequestAt;
    private long accessTokenExpiresIn;
    private long refreshTokenExpiresIn;

    public QBOEngine(String companyId,
                     String clientId,
                     String clientSecret,
                     String authorizationRedirect,
                     String accountingAPIHost,
                     Environment environment) {

        this.log = LogManager.getLogger(getClass());
        this.environment = environment;

        this.companyId = companyId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationRedirect = authorizationRedirect;

        // Set API configuration settings
        Config.setProperty(Config.BASE_URL_QBO, accountingAPIHost);
        Config.setProperty(Config.SERIALIZATION_REQUEST_FORMAT, TX_FORMAT_JSON);
        Config.setProperty(Config.SERIALIZATION_RESPONSE_FORMAT, TX_FORMAT_JSON);
        Config.setProperty(Config.COMPRESSION_REQUEST_FORMAT, COMPRESSION_FORMAT_GZIP);
        Config.setProperty(Config.COMPRESSION_RESPONSE_FORMAT, COMPRESSION_FORMAT_GZIP);

        this.oAuth2Config = new OAuth2Config.OAuth2ConfigBuilder(clientId, clientSecret)
                .callDiscoveryAPI(environment)
                .buildConfig();
    }

    public boolean authenticateClient() {
        boolean authenticated = false;
        readTokens();

        if (this.accessToken != null && !this.accessToken.isEmpty()) {
            if (isAccessTokenExpired()) {
                if (!isRefreshTokenExpired()) {
                    OAuth2PlatformClient oAuth2Client = new OAuth2PlatformClient(oAuth2Config);

                    try {
                        BearerTokenResponse tokenResponse = oAuth2Client.refreshToken(this.refreshToken);
                        saveTokens(tokenResponse);

                        authenticated = true;
                    } catch (OAuthException e) {
                        log.error("Failed to refresh token : " + e.getLocalizedMessage());
                    }
                }
            } else {
                authenticated = true;
            }
        }

        return authenticated;
    }

    public String requestAuthorizationUrl(List<Scope> scopes) {
        this.lastTokenRequestAt = new Date();
        String urlStr = null;

        // Generate CSRF token
        String csrfToken = oAuth2Config.generateCSRFToken();

        // Get the authorization URL
        try {
            urlStr = oAuth2Config.prepareUrl(scopes, authorizationRedirect, csrfToken);
        } catch (InvalidRequestException e) {
            log.error("Invalid request : " + e.getLocalizedMessage());
        }

        return urlStr;
    }

    public void fetchTokens(AuthorizationBean authorizationBean) {
        if (authorizationBean.getRealmId().equals(this.companyId)) {
            // Prepare OAuth2PlatformClient
            OAuth2PlatformClient oAuth2Client = new OAuth2PlatformClient(oAuth2Config);

            // Get the bearer token (OAuth2 tokens)
            try {
                BearerTokenResponse tokenResponse = oAuth2Client
                        .retrieveBearerTokens(authorizationBean.getCode(), authorizationRedirect);

                saveTokens(tokenResponse);
            } catch (OAuthException e) {
                log.error("Cannot fetch the token response : " + e.getLocalizedMessage());
            }
        } else {
            log.error("Invalid realmID returned : " + authorizationBean.getRealmId());
        }
    }

    private void saveTokens(BearerTokenResponse tokenResponse) {
        this.accessToken = tokenResponse.getAccessToken();
        this.openIdToken = tokenResponse.getIdToken();
        this.refreshToken = tokenResponse.getRefreshToken();

        this.accessTokenExpiresIn = tokenResponse.getExpiresIn();
        this.refreshTokenExpiresIn = tokenResponse.getXRefreshTokenExpiresIn();

        Properties properties = Helper.readProperties();
        properties.setProperty(ACCESS_TOKEN, this.accessToken);
        properties.setProperty(REFRESH_TOKEN, this.refreshToken);
        properties.setProperty(OPEN_ID_TOKEN, this.openIdToken);
        properties.setProperty(ACCESS_TOKEN_EXPIRES_IN, Long.toString(this.accessTokenExpiresIn));
        properties.setProperty(REFRESH_TOKEN_EXPIRES_IN, Long.toString(this.refreshTokenExpiresIn));
        properties.setProperty(LAST_TOKEN_REQUEST_AT, STANDARD_DATE_TIME.format(this.lastTokenRequestAt));

        Helper.saveProperties(properties);
    }

    private void readTokens() {
        Properties properties = Helper.readProperties();
        this.accessToken = properties.getProperty(ACCESS_TOKEN);
        this.refreshToken = properties.getProperty(REFRESH_TOKEN);
        this.openIdToken = properties.getProperty(OPEN_ID_TOKEN);

        String accessTokenExpiresIn = properties.getProperty(ACCESS_TOKEN_EXPIRES_IN);
        if (accessTokenExpiresIn != null && !accessTokenExpiresIn.isEmpty()) {
            this.accessTokenExpiresIn = Long.parseLong(accessTokenExpiresIn);
        }

        String refreshTokenExpiresIn = properties.getProperty(REFRESH_TOKEN_EXPIRES_IN);
        if (refreshTokenExpiresIn != null && !refreshTokenExpiresIn.isEmpty()) {
            this.refreshTokenExpiresIn = Long.parseLong(refreshTokenExpiresIn);
        }

        String lastTokenRequestAt = properties.getProperty(LAST_TOKEN_REQUEST_AT);
        if (lastTokenRequestAt != null && !lastTokenRequestAt.isEmpty()) {
            try {
                this.lastTokenRequestAt = STANDARD_DATE_TIME.parse(lastTokenRequestAt);
            } catch (ParseException e) {
                log.error("Failed to parse date string : " + e.getLocalizedMessage());
            }
        }
    }

    private boolean isAccessTokenExpired() {
        long diffInMils = Math.abs(new Date().getTime() - this.lastTokenRequestAt.getTime());
        long diffInSecs = TimeUnit.SECONDS.convert(diffInMils, TimeUnit.MILLISECONDS);

        return diffInSecs >= this.accessTokenExpiresIn;
    }

    private boolean isRefreshTokenExpired() {
        long diffInMils = Math.abs(new Date().getTime() - this.lastTokenRequestAt.getTime());
        long diffInSecs = TimeUnit.SECONDS.convert(diffInMils, TimeUnit.MILLISECONDS);

        return diffInSecs >= this.refreshTokenExpiresIn;
    }

    public UserInfoResponse getUserInfo() {
        UserInfoResponse userInfoResponse = null;

        OAuth2PlatformClient oAuth2Client = new OAuth2PlatformClient(oAuth2Config);

        try {
            userInfoResponse = oAuth2Client.getUserInfo(this.accessToken);
        } catch (OpenIdException e) {
            log.error("Couldn't fetch openID user information : " + e.getLocalizedMessage());
        }

        return userInfoResponse;
    }

    public List<Customer> getCustomers() {
        List<Customer> customers = null;

        OAuth2Authorizer oauth = new OAuth2Authorizer(this.accessToken);

        try {
            Context context = new Context(oauth, ServiceType.QBO, companyId);
            DataService service = new DataService(context);

            Customer customer = GenerateQuery.createQueryEntity(Customer.class);

            String query = select($(customer)).generate();

            QueryResult result = service.executeQuery(query);

            customers = new ArrayList<>();
            for (IEntity entity : result.getEntities()) {
                customers.add((Customer) entity);
            }
        } catch (FMSException e) {
            log.error("Couldn't query customers : " + e.getLocalizedMessage());
        }

        return customers;
    }

    public Customer addCustomer(Customer customer) {
        OAuth2Authorizer oauth = new OAuth2Authorizer(this.accessToken);
        Customer resultCustomer = null;

        try {
            Context context = new Context(oauth, ServiceType.QBO, companyId);
            DataService service = new DataService(context);

            resultCustomer = service.add(customer);
        } catch (FMSException e) {
            log.error("Couldn't create QBO context : " + e.getLocalizedMessage());
        }

        return resultCustomer;
    }
}
