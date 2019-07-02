package com.kakaobank.demo.ymoh.ft.transport;

import com.kakaobank.demo.ymoh.ft.SocketTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

public class PushTransport extends SocketTransport {

    private static Logger logger = LoggerFactory.getLogger(PushTransport.class);

    @Override
    public boolean repeat(SocketChannel socketChannel) throws Exception {
        return false;
    }

}
