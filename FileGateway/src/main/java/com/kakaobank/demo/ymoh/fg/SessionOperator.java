package com.kakaobank.demo.ymoh.fg;

import java.nio.channels.SocketChannel;

import com.kakaobank.demo.ymoh.Command;

public interface SessionOperator {

    String getSupportedMethod();

    void operate(Command command, SocketChannel socketChannel) throws Exception;

}
