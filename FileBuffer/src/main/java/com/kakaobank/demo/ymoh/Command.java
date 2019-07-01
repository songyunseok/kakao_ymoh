package com.kakaobank.demo.ymoh;

public class Command {

    private String sessionId;

    private String method;

    private String parameterString;

    public Command() {
        super();
    }

    public Command(String method, String parameterString) {
        this();
        this.method = method;
        this.parameterString = parameterString;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getParameterString() {
        return parameterString;
    }

    public void setParameterString(String parameterString) {
        this.parameterString = parameterString;
    }

    public String toString() {
        return String.format("%s %s", method, parameterString);
    }

}
