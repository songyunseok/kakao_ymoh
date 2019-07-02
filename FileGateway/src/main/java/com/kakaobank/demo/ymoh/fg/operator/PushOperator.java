package com.kakaobank.demo.ymoh.fg.operator;

import com.kakaobank.demo.ymoh.DateUtils;
import com.kakaobank.demo.ymoh.SessionUtils;
import com.kakaobank.demo.ymoh.fb.Operation;
import com.kakaobank.demo.ymoh.fg.SessionCommand;
import com.kakaobank.demo.ymoh.fg.SessionOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.util.Date;

@Component
public class PushOperator implements SessionOperator {

    private static Logger logger = LoggerFactory.getLogger(PushOperator.class);

    @Override
    public String getSupportedMethod() {
        return "PUSH";
    }

    @Override
    public void operate(SessionCommand command, SocketChannel socketChannel) throws Exception {
        byte[] requestBytes = new byte[command.getLength()];
        int n = SessionUtils.read(socketChannel, requestBytes);
        if (n < 0) {
            throw new EOFException(String.format("PushOperator '%s' was disconnected", command.getSessionId()));
        }
        Operation.PushRequest request = Operation.PushRequest.parseFrom(requestBytes);
        String token = request.getToken();
        long length = request.getLength();
        String status = "OK";
        String reason = "";
        try {
            try {
                //TODO validate
            } catch (Exception ex) {
                status = "FAIL";
                reason = ex.getMessage();
                logger.warn("Failed to validate push request", ex.getMessage());
            }
            Operation.SessionResponse resp = Operation.SessionResponse.newBuilder()
                    .setToken(token)
                    .setStatus(status)
                    .setReason(reason)
                    .build();
            byte[] respBytes = resp.toByteArray();
            byte[] sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
            SessionUtils.putInt(sizeBytes, respBytes.length);
            SessionUtils.write(socketChannel, sizeBytes);
            SessionUtils.write(socketChannel, respBytes);
            if (status.equals("OK")) {
                /*byte[] tokenBytes = new byte[SessionUtils.OP_TOKEN_SIZE];
                n = SessionUtils.read(socketChannel, tokenBytes);
                if (n < 0) {
                    throw new EOFException(String.format("PushOperator '%s' was disconnected", command.getSessionId()));
                }
                SessionUtils.parseString(tokenBytes);*/
                File parent = new File(System.getProperty("user.dir"));
                File tempFile = null;
                OutputStream outputStream = null;
                try {
                    tempFile = File.createTempFile("push_" + DateUtils.formatDate(new Date()) + "_", ".tmp");
                    outputStream = new FileOutputStream(tempFile);
                    long len = SessionUtils.read(socketChannel, length, outputStream);
                    if (len < 0) {
                        throw new EOFException(String.format("PushOperator '%s' was disconnected", command.getSessionId()));
                    }
                    outputStream.flush();
                    status = "OK";
                    reason = "";
                } catch (EOFException ex) {
                    status = "";
                    logger.warn("Failed to input a pushed file", ex.getMessage());
                } catch (Exception ex) {
                    status = "FAIL";
                    reason = ex.getMessage();
                    logger.warn("Failed to input a pushed file", ex.getMessage());
                } finally {
                    try {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        if (tempFile != null) {
                            if (status.equals("OK")) {
                                File dest = new File(parent, request.getFileName());
                                tempFile.renameTo(dest);
                            } else {
                                tempFile.delete();
                            }
                        }
                    } catch (Exception ex) {
                        status = "";
                        logger.warn("Failed to close a pushed file", ex.getMessage());
                    }
                    if (status.isEmpty() == false) {
                        resp = Operation.SessionResponse.newBuilder()
                                .setToken(token)
                                .setStatus(status)
                                .setReason(reason)
                                .build();
                        respBytes = resp.toByteArray();
                        sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
                        SessionUtils.putInt(sizeBytes, respBytes.length);
                        SessionUtils.write(socketChannel, sizeBytes);
                        SessionUtils.write(socketChannel, respBytes);
                    }
                }
            }
        } finally {
            if (status.equals("OK") == false) {
                socketChannel.close();
            }
        }
    }

}
