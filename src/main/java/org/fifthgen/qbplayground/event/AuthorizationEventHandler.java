package org.fifthgen.qbplayground.event;

import org.fifthgen.qbplayground.event.bean.AuthorizationBean;

public interface AuthorizationEventHandler {

    void onAuthorizationCompleted(AuthorizationBean authorizationBean);
}
