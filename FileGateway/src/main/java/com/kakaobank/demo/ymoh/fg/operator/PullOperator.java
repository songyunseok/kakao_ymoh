package com.kakaobank.demo.ymoh.fg.operator;

import com.kakaobank.demo.ymoh.DateUtils;
import com.kakaobank.demo.ymoh.FileQueue;
import com.kakaobank.demo.ymoh.FileQueueLocator;
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.SocketChannel;
import java.util.Date;

/*
 * 1. Receive PullRequest from a FileTransfer (FT)
 *  FT --> 'PULL' (32 bytes) + length of PullRequest (6 bytes)
 *  FT --> PullRequest (n bytes)
 * 2. Send PullResponse filled with file description and length depending whether a file exists to transfer
 *  FT <-- length of PullResponse (6 bytes) + PullResponse (n bytes)
 * 3. (Optional) Send a file stream from a directory depending whether a file exists to transfer
 *  FT <-- file stream (n bytes)
 *  FT --> length of SessionResponse (6 bytes) + SessionResponse (n bytes)
 */
@Component
public class PullOperator implements SessionOperator {

    private static Logger logger = LoggerFactory.getLogger(PullOperator.class);

    @Autowired
    private FileQueueLocator locator;

    @Autowired
    private Environment env;

    @Override
    public String getSupportedMethod() {
        return "PULL";
    }

    @Override
    public void operate(SessionCommand command, SocketChannel socketChannel) throws Exception {
        byte[] requestBytes = new byte[command.getLength()];
        int n = SessionUtils.read(socketChannel, requestBytes);
        if (n < 0) {
            throw new EOFException(String.format("PullOperator '%s' was disconnected", command.getSessionId()));
        }

        File homeDir = new File(System.getProperty("user.dir"));
        File parentDir = null;

        Operation.PullRequest request = Operation.PullRequest.parseFrom(requestBytes);
        String user = request.getUser();
        String path = request.getPath();
        String signature = request.getSignature();
        String token = null;
        FileQueue queue = null;
        String status = "OK";
        String reason = "";
        try {
            File inputFile = null;
            try {
                if (user == null || user.length() == 0) {
                    throw new SessionException("User is mandatory");
                }
                String homePath = env.getProperty(user + ".pull.home");
                if (homePath == null || homePath.length() == 0) {
                    throw new SessionException(String.format("Home path [%s] is not found", homePath));
                }
                parentDir = new File(homeDir, homePath);
                if (parentDir.exists() == false) {
                    if (parentDir.mkdirs() == false) {
                        throw new SessionException(String.format("Parent directory [%s] is not accessible", parentDir.getAbsolutePath()));
                    }
                }
                if (path != null && path.length() > 0) {
                    parentDir = new File(parentDir, path);
                    if (parentDir.mkdirs() == false) {
                        throw new SessionException(String.format("Parent directory [%s] is not accessible", parentDir.getAbsolutePath()));
                    }
                }
                queue = locator.locate(parentDir);
                token = queue.nextToken();
                if (token != null) {
                    inputFile = queue.consume(token);
                } else {
                    token = "NOFILE";
                }
            } catch (Exception ex) {
                status = "FAIL";
                reason = ex.getMessage();
                logger.warn("Failed to list files", ex.getMessage());
            }
            Date date = new Date();
            Operation.PullResponse resp = Operation.PullResponse.newBuilder()
                    .setToken(token)
                    .setFileName(inputFile != null ? inputFile.getName() : "")
                    .setLength(inputFile != null ? inputFile.length() : 0)
                    .setDate(DateUtils.formatDate(date))
                    .setTime(DateUtils.formatTime(date))
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
                } catch (EOFException ex) {
                    status = "";
                    logger.warn("Failed to output a pulled file", ex.getMessage());
                } catch (Exception ex) {
                    status = "FAIL";
                    logger.warn("Failed to output a pulled file", ex.getMessage());
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (Exception ex) {
                        status = "";
                        logger.warn("Failed to close a pulled file", ex.getMessage());
                    }
                    if (status.isEmpty() == false) {
                        try {
                            sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
                            n = SessionUtils.read(socketChannel, sizeBytes);
                            if (n < 0) {
                                throw new EOFException(String.format("PullOperator '%s' was disconnected", command.getSessionId()));
                            }
                            respBytes = new byte[SessionUtils.parseInt(sizeBytes)];
                            Operation.SessionResponse reply = Operation.SessionResponse.parseFrom(respBytes);
                            if (token.equals(reply.getToken())) {
                                status = reply.getStatus();
                                if (status.equals("OK") == false) {
                                    reason = reply.getReason();
                                    throw new SessionException(reason != null && reason.length() > 0 ? reason: "Unknown reason");
                                }
                            } else {
                                throw new SessionException("Token in reply is invalid");
                            }
                        } catch (Exception ex) {
                            status = "";
                            logger.warn("Failed to complete a pulled file", ex.getMessage());
                        } finally {
                            if (status.equals("OK")) {
                                queue.commit(token);
                            } else {
                                queue.rollback(token);
                            }
                        }
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
