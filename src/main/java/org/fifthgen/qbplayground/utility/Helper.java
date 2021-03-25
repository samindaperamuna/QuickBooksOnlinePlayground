package org.fifthgen.qbplayground.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fifthgen.qbplayground.event.bean.OAuthState;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Helper {

    private static final String APPLICATION_PROPERTIES = "application.properties";

    private static final Logger LOG = LogManager.getLogger(Helper.class);

    /**
     * Read properties file and load properties into the system. To access the read properties
     * use {@code System.getProperty(property_key)}.
     */
    public static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = Helper.class.getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES)) {
            properties.load(inputStream);
        } catch (IOException e) {
            LOG.error("Failed to read properties file : " + e.getLocalizedMessage());
        }

        return properties;
    }

    public static void saveProperties(Properties properties) {
        URL url = Helper.class.getClassLoader().getResource(APPLICATION_PROPERTIES);

        if (url != null) {
            try (OutputStream outputStream = new FileOutputStream(new File(url.toURI()))) {
                properties.store(outputStream, "Written on : " + Calendar.getInstance().getTime());
            } catch (IOException | URISyntaxException e) {
                LOG.error("Failed to save properties file : " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Decode state returned by the authentication response which is URL encoded.
     *
     * @param stateString State string in UTF-8 format and URL encoded.
     * @return {@link OAuthState} object constructed or null if an exception occurred.
     */
    public static OAuthState decodeState(String stateString) {
        OAuthState oAuthState = null;

        try {
            String decodedState = URLDecoder.decode(stateString, "utf-8");
            String[] statePairs = decodedState != null ? decodedState.split("\\&") : new String[0];

            Map<String, String> statesMap = new HashMap<>();

            for (String statePair : statePairs) {
                String[] stateDecoupled = !statePair.isEmpty() ? statePair.split("\\=") : new String[0];

                if (stateDecoupled.length > 1) {
                    statesMap.put(stateDecoupled[0], stateDecoupled[1]);
                } else if (stateDecoupled.length > 0) {
                    statesMap.put("security_token", stateDecoupled[0]);
                }
            }

            oAuthState = new OAuthState(statesMap.get("security_token"), statesMap.get("url"));
        } catch (UnsupportedEncodingException e) {
            LOG.error("Couldn't decode OAuth state : " + e.getLocalizedMessage());
        }

        return oAuthState;
    }
}
