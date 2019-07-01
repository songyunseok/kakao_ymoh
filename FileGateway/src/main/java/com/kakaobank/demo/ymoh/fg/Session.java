package com.kakaobank.demo.ymoh.fg;

public interface Session {

    int OP_COMMAND_SIZE = 1024;

    int OP_FILE_HEADER_SIZE = 6;

    String start();

    void addOperator(SessionOperator operator);

    void stop();

}
