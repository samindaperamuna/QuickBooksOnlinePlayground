package org.fifthgen.qbplayground.qbo.apicallback;

import com.intuit.ipp.services.CallbackHandler;
import com.intuit.ipp.services.CallbackMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncCallback implements CallbackHandler {

    @Override
    public void execute(CallbackMessage callbackMessage) {
        Logger log = LogManager.getLogger(getClass());
        log.info(callbackMessage.toString());
    }
}
