package com.kakaobank.demo.ymoh.fg;

public interface Session {

    String start();

    void addOperator(SessionOperator operator);

    void stop();

}
