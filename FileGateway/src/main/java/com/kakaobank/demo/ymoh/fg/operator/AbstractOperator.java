package com.kakaobank.demo.ymoh.fg.operator;

import com.kakaobank.demo.ymoh.SessionUtils;
import com.kakaobank.demo.ymoh.fb.Operation;
import com.kakaobank.demo.ymoh.fg.SessionException;
import com.kakaobank.demo.ymoh.fg.SessionOperator;

import java.io.EOFException;
import java.nio.channels.SocketChannel;

abstract class AbstractOperator implements SessionOperator {

    private volatile long goodCount = 0;

    private volatile long badCount = 0;

    @Override
    public long getGoodCount() {
        return goodCount;
    }

    @Override
    public long getBadCount() {
        return badCount;
    }

    protected void increaseGoodCount() { goodCount++; }

    protected void increaseBadCount() { badCount++; }

    protected void readResponse(SocketChannel socketChannel, String identifier, String token) throws Exception {
        byte[] sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
        int n = SessionUtils.read(socketChannel, sizeBytes);
        if (n < 0) {
            throw new EOFException(String.format("Operator '%s' was disconnected", identifier));
        }
        byte[] respBytes = new byte[SessionUtils.parseInt(sizeBytes)];
        n = SessionUtils.read(socketChannel, respBytes);
        if (n < 0) {
            throw new EOFException(String.format("Operator '%s' was disconnected", identifier));
        }
        Operation.SessionResponse reply = Operation.SessionResponse.parseFrom(respBytes);
        if (token.equals(reply.getToken())) {
            String status = reply.getStatus();
            if (status.equals("OK") == false) {
                String reason = reply.getReason();
                throw new SessionException(reason != null && reason.length() > 0 ? reason: "Unknown reason");
            }
        } else {
            throw new SessionException("Token in reply is invalid");
        }
    }

    protected void writeResponse(SocketChannel socketChannel, byte[] respBytes) throws Exception {
        byte[] sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
        SessionUtils.putInt(sizeBytes, respBytes.length);
        SessionUtils.write(socketChannel, sizeBytes);
        SessionUtils.write(socketChannel, respBytes);
    }

}
