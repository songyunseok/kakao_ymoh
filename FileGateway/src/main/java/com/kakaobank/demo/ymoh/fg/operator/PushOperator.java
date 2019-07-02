package com.kakaobank.demo.ymoh.fg.operator;

import com.kakaobank.demo.ymoh.DateUtils;
import com.kakaobank.demo.ymoh.SessionUtils;
import com.kakaobank.demo.ymoh.fb.Operation;
import com.kakaobank.demo.ymoh.fg.SessionCommand;
import com.kakaobank.demo.ymoh.fg.SessionException;
import com.kakaobank.demo.ymoh.fg.SessionOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;
import java.util.Date;

/*
 * 1. Receive PushRequest from a FileTransfer (FT)
 *  FT --> 'PUSH' (32 bytes) + length of PushRequest (6 bytes)
 *  FT --> PushRequest (n bytes)
 *  FT <-- length of SessionResponse (6 bytes) + SessionResponse (n bytes)
 * 2. (Optional) Receive a file stream depending whether the previous SessionStatus has OK status
 *  FT --> file stream (n bytes)
 *  FT <-- length of SessionResponse (6 bytes) + SessionResponse (n bytes)
 */
@Component
public class PushOperator implements SessionOperator {

    private static Logger logger = LoggerFactory.getLogger(PushOperator.class);

    @Autowired
    private Environment env;

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

        File userDir = new File(System.getProperty("user.dir"));
        File homeDir = null;

        Operation.PushRequest request = Operation.PushRequest.parseFrom(requestBytes);
        String token = request.getToken();
        String user = request.getUser();
        String fileName = request.getFileName();
        long length = request.getLength();
        String path = request.getPath();
        String checkSum = request.getCheckSum();
        String signature = request.getSignature();
        String status = "OK";
        String reason = "";
        try {
            try {
                if (token == null || token.length() == 0) {
                    throw new SessionException("Token is mandatory");
                }
                if (user == null || user.length() == 0) {
                    throw new SessionException("User is mandatory");
                }
                if (fileName == null || fileName.length() == 0) {
                    throw new SessionException("FileName is mandatory");
                }
                if (length <= 0) {
                    throw new SessionException("Length must be greater than 0");
                }
                String homePath = env.getProperty(user + ".push.home");
                if (homePath == null || homePath.length() == 0) {
                    throw new SessionException(String.format("Home path [%s] is not found", homePath));
                }
                homeDir = new File(userDir, homePath);
                if (homeDir.exists() == false) {
                    if (homeDir.mkdirs() == false) {
                        throw new SessionException(String.format("Parent directory [%s] is not accessible", homeDir.getAbsolutePath()));
                    }
                }
                if (path != null && path.length() > 0) {
                    homeDir = new File(homeDir, path);
                    if (homeDir.mkdirs() == false) {
                        throw new SessionException(String.format("Parent directory [%s] is not accessible", homeDir.getAbsolutePath()));
                    }
                }
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
                                File dest = new File(homeDir, fileName);
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
