package com.kakaobank.demo.ymoh.fg.operator;

import com.kakaobank.demo.ymoh.Command;
import com.kakaobank.demo.ymoh.SessionUtils;
import com.kakaobank.demo.ymoh.fb.Operation;
import com.kakaobank.demo.ymoh.fg.SessionOperator;

import java.io.EOFException;
import java.nio.channels.SocketChannel;

public class PushOperator implements SessionOperator {


    @Override
    public String getSupportedMethod() {
        return "PUSH";
    }

    @Override
    public void operate(Command command, SocketChannel socketChannel) throws Exception {
        byte[] sizeBytes = new byte[6];
        int n = SessionUtils.read(socketChannel, sizeBytes);
        if (n < 0) {
            throw new EOFException(String.format("FileSession '%s' was disconnected", command.getSessionId()));
        }
        int size = Integer.parseInt(new String(sizeBytes, "UTF-8"));
        byte[] headerBytes = new byte[size];
        n = SessionUtils.read(socketChannel, headerBytes);
        if (n < 0) {
            throw new EOFException(String.format("FileSession '%s' was disconnected", command.getSessionId()));
        }
        Operation.FileHeader fileHeader = Operation.FileHeader.parseFrom(headerBytes);
        fileHeader.getLength();
    }

}
