package com.kakaobank.demo.ymoh.ft.transport;

import com.kakaobank.demo.ymoh.DateUtils;
import com.kakaobank.demo.ymoh.SessionUtils;
import com.kakaobank.demo.ymoh.fb.Operation;
import com.kakaobank.demo.ymoh.ft.SocketTransport;
import com.kakaobank.demo.ymoh.ft.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.channels.ByteChannel;
import java.util.Date;

@Component
@Scope("prototype")
public class PullTransport extends SocketTransport {

    private static Logger logger = LoggerFactory.getLogger(PullTransport.class);

    private static volatile long instanceCounter = 0;

    public PullTransport() {
        this.identifier = "PULL_" + Long.toString(++instanceCounter);
    }

    @Override
    public String getMethod() {
        return "PULL";
    }

    @Override
    protected String getHomePathPropertyName() {
        return ".pull.home";
    }

    @Override
    public boolean repeat(ByteChannel byteChannel) throws Exception {
        Operation.PullRequest request = Operation.PullRequest.newBuilder()
                .setUser(user)
                .setPath(path)
                .build();
        writeRequest(byteChannel, "PULL", request.toByteArray());

        byte[] sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
        int n = SessionUtils.read(byteChannel, sizeBytes);
        if (n < 0) {
            throw new EOFException(String.format("PullTransport '%s' was disconnected", identifier));
        }
        byte[] respBytes = new byte[SessionUtils.parseInt(sizeBytes)];
        n = SessionUtils.read(byteChannel, respBytes);
        if (n < 0) {
            throw new EOFException(String.format("PullTransport '%s' was disconnected", identifier));
        }
        Operation.PullResponse resp = Operation.PullResponse.parseFrom(respBytes);

        String status = resp.getStatus();
        String reason = resp.getReason();
        if (status.equals("OK") == false) {
            throw new TransportException(reason != null && reason.length() > 0 ? reason: "Unknown reason");
        }

        long length = resp.getLength();
        if (length > 0) {
            String token = resp.getToken();
            String fileName = resp.getFileName();
            File tempFile = null;
            OutputStream outputStream = null;
            try {
                tempFile = File.createTempFile("pull_" + DateUtils.formatDate(new Date()) + "_", ".tmp");
                outputStream = new FileOutputStream(tempFile);
                long len = SessionUtils.read(byteChannel, length, outputStream);
                if (len < 0) {
                    throw new EOFException(String.format("PullTransport '%s' was disconnected", identifier));
                }
                outputStream.flush();
                status = "OK";
                reason = "";
            } catch (EOFException ex) {
                status = "";
                logger.warn("Failed to output a file", ex);
            } catch (Exception ex) {
                status = "FAIL";
                reason = ex.getMessage();
                logger.warn("Failed to output a file", ex);
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (tempFile != null) {
                        if (status.equals("OK")) {
                            File dest = new File(homeDir, fileName);
                            tempFile.renameTo(dest);
                            increaseCount();
                        } else {
                            tempFile.delete();
                        }
                    }
                } catch (Exception ex) {
                    status = "";
                    logger.warn("Failed to close an output file", ex.getMessage());
                }
                if (status.isEmpty() == false) {
                    Operation.SessionResponse reply = Operation.SessionResponse.newBuilder()
                            .setToken(token)
                            .setStatus(status)
                            .setReason(reason)
                            .build();
                    respBytes = reply.toByteArray();
                    writeResponse(byteChannel, respBytes);
                }
            }
            return true;
        }
        return false;
    }

}
