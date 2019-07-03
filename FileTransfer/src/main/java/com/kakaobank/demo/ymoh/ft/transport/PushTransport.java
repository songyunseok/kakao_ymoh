package com.kakaobank.demo.ymoh.ft.transport;

import com.kakaobank.demo.ymoh.DateUtils;
import com.kakaobank.demo.ymoh.FileQueue;
import com.kakaobank.demo.ymoh.FileQueueLocator;
import com.kakaobank.demo.ymoh.SessionUtils;
import com.kakaobank.demo.ymoh.fb.Operation;
import com.kakaobank.demo.ymoh.ft.SocketTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.ByteChannel;
import java.util.Date;

@Component
@Scope("prototype")
public class PushTransport extends SocketTransport {

    private static Logger logger = LoggerFactory.getLogger(PushTransport.class);

    private FileQueue queue;

    @Autowired
    private FileQueueLocator locator;

    private static volatile long instanceCounter = 0;

    public PushTransport() {
        this.identifier = "PUSH_" + Long.toString(++instanceCounter);
    }

    @Override
    public String getMethod() {
        return "PUSH";
    }

    @Override
    protected String getHomePathPropertyName() {
        return ".push.home";
    }

    @Override
    protected void resolveHomeDir() {
        super.resolveHomeDir();
        try {
            queue = locator.locate(homeDir);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean repeat(ByteChannel byteChannel) throws Exception {
        String token = queue.nextToken();
        if (token != null) {
            try {
                File file = queue.consume(token);
                Date date = new Date();

                Operation.PushRequest request = Operation.PushRequest.newBuilder()
                        .setToken(token)
                        .setUser(user)
                        .setFileName(file.getName())
                        .setLength(file.length())
                        .setPath(path)
                        .setDate(DateUtils.formatDate(date))
                        .setTime(DateUtils.formatTime(date))
                        .build();

                writeRequest(byteChannel, "PUSH", request.toByteArray());
                readResponse(byteChannel, token);
                try (InputStream inputStream = new FileInputStream(file)) {
                    long len = SessionUtils.write(byteChannel, inputStream);
                    if (len < 0) {
                        throw new EOFException(String.format("PushTransport '%s' was disconnected", identifier));
                    }
                }
                readResponse(byteChannel, token);

                increaseCount();
                queue.commit(token);
            } catch (Exception e) {
                queue.rollback(token);
                throw e;
            }
            return true;
        }
        return false;
    }

}
