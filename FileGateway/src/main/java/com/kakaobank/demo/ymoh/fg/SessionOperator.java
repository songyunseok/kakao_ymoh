package com.kakaobank.demo.ymoh.fg;

import java.nio.channels.ByteChannel;

public interface SessionOperator {

    String getSupportedMethod();

    long getGoodCount();

    long getBadCount();

    void operate(SessionCommand command, ByteChannel byteChannel) throws Exception;

}
