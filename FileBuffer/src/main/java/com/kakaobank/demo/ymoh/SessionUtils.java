package com.kakaobank.demo.ymoh;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SessionUtils {

    public static int read(SocketChannel socketChannel, byte[] byteArray) throws Exception {
        int arrayLength = byteArray.length;
        ByteBuffer buffer = ByteBuffer.allocate(arrayLength);
        int size = 0;
        while (size < arrayLength) {
            int n = socketChannel.read(buffer);
            if (n < 0) {
                return n;
            } else {
                size += n;
            }
        }
        buffer.flip();
        buffer.get(byteArray);
        return size;
    }

    public static Command parseCommand(String headerString) throws Exception {
        String method = null;
        String parameterString = null;
        int sepPosition = headerString.indexOf(" ");
        if (sepPosition > -1) {
            method = headerString.substring(0, sepPosition);
            parameterString = headerString.substring(sepPosition+1);
        } else {
            method = headerString;
            parameterString = "";
        }
        return new Command(method, parameterString);
    }

}
