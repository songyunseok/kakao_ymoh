package com.kakaobank.demo.ymoh.fg;

import java.nio.channels.SocketChannel;

public interface SessionOperator {

    String getSupportedMethod();

    long getGoodCount();

    long getBadCount();

    void operate(SessionCommand command, SocketChannel socketChannel) throws Exception;

}
