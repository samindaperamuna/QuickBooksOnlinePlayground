package org.fifthgen.qbplayground.event.bean;

public class OAuthState {

    private String securityToken;
    private String url;

    public OAuthState(String securityToken, String url) {
        this.securityToken = securityToken;
        this.url = url;
    }

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "OAuthState{" +
                "securityToken='" + securityToken + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
