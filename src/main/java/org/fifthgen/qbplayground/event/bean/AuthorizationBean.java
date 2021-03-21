package org.fifthgen.qbplayground.event.bean;

public class AuthorizationBean {

    private String code;
    private OAuthState state;
    private String realmId;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OAuthState getState() {
        return state;
    }

    public void setState(OAuthState state) {
        this.state = state;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public String toString() {
        return "AuthenticationBean{" +
                "code='" + code + '\'' +
                ", state='" + state + '\'' +
                ", realmId='" + realmId + '\'' +
                '}';
    }
}
