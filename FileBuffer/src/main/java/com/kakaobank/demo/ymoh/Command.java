package com.kakaobank.demo.ymoh;

public class Command implements java.io.Serializable {

    private String method;

    private int length;

    private String sessionId;

    public Command() {
        super();
    }

    public Command(String method, int length, String sessionId) {
        this();
        this.method = method;
        this.length = length;
        this.sessionId = sessionId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String toString() {
        return String.format("%s %d %s", method, length, sessionId);
    }

}
