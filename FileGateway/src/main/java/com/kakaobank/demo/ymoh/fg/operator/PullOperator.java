package com.kakaobank.demo.ymoh.fg.operator;

import com.kakaobank.demo.ymoh.Command;
import com.kakaobank.demo.ymoh.DateUtils;
import com.kakaobank.demo.ymoh.SessionUtils;
import com.kakaobank.demo.ymoh.fb.Operation;
import com.kakaobank.demo.ymoh.fg.SessionOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileFilter;
import java.io.InputStream;
import java.nio.channels.SocketChannel;
import java.util.Date;

@Component
public class PullOperator implements SessionOperator {

    private static Logger logger = LoggerFactory.getLogger(PullOperator.class);

    @Override
    public String getSupportedMethod() {
        return "PULL";
    }

    @Override
    public void operate(Command command, SocketChannel socketChannel) throws Exception {
        byte[] requestBytes = new byte[command.getLength()];
        int n = SessionUtils.read(socketChannel, requestBytes);
        if (n < 0) {
            throw new EOFException(String.format("PullOperator '%s' was disconnected", command.getSessionId()));
        }
        Operation.PullRequest request = Operation.PullRequest.parseFrom(requestBytes);
        String token = request.getToken();
        String status = "OK";
        String reason = "";
        try {
            File inputFile = null;
            try {
                File parent = new File(System.getProperty("user.dir"));
                File files [] = parent.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && pathname.isHidden() == false;
                    }
                });
                if (files != null && files.length > 0) {
                    inputFile = files[0];
                }
            } catch (Exception ex) {
                status = "FAIL";
                reason = ex.getMessage();
                logger.warn("Failed to validate push request", ex.getMessage());
            }
            Operation.PullResponse resp = Operation.PullResponse.newBuilder()
                    .setToken(token)
                    .setFileName(inputFile != null ? inputFile.getName() : "")
                    .setLength(inputFile != null ? inputFile.length() : 0)

                    .setStatus(status)
                    .setReason(reason)
                    .build();
            byte[] respBytes = resp.toByteArray();
            byte[] sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
            SessionUtils.putInt(sizeBytes, respBytes.length);
            SessionUtils.write(socketChannel, sizeBytes);
            SessionUtils.write(socketChannel, respBytes);
            if (status.equals("OK") && inputFile != null) {
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(inputFile);
                    long len = SessionUtils.write(socketChannel, inputStream);
                    if (len < 0) {
                        throw new EOFException(String.format("PullOperator '%s' was disconnected", command.getSessionId()));
                    }
                    status = "OK";
                    reason = "";
                } catch (Exception ex) {
                    status = "FAIL";
                    reason = ex.getMessage();
                    logger.warn("Failed to write a pushed file", ex.getMessage());
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (inputFile != null) {
                            if (status.equals("OK")) {
                                //File dest = new File(".done", resp.getFileName());
                                //inputFile.renameTo(dest);
                                inputFile.delete();
                            } else {
                                //inputFile.delete();
                            }
                        }
                    } catch (Exception ex) {
                        status = "FAIL";
                        reason = ex.getMessage();
                        logger.warn("Failed to close a pulled file", ex.getMessage());
                    }
                    Operation.SessionResponse reply = Operation.SessionResponse.newBuilder()
                            .setToken(token)
                            .setStatus(status)
                            .setReason(reason)
                            .build();
                    respBytes = reply.toByteArray();
                    sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
                    SessionUtils.putInt(sizeBytes, respBytes.length);
                    SessionUtils.write(socketChannel, sizeBytes);
                    SessionUtils.write(socketChannel, respBytes);
                }
            }
        } finally {
            if (status.equals("OK") == false) {
                socketChannel.close();
            }
        }
    }

}
