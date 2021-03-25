package org.fifthgen.qbplayground.event;

import org.fifthgen.qbplayground.event.bean.AuthorizationBean;

public interface AuthorizationEvent {

    void onAuthorizationCompleted(AuthorizationBean authorizationBean);
}
