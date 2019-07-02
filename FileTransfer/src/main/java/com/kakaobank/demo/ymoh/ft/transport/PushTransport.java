package com.kakaobank.demo.ymoh.ft.transport;

import com.kakaobank.demo.ymoh.FileQueue;
import com.kakaobank.demo.ymoh.FileQueueLocator;
import com.kakaobank.demo.ymoh.ft.SocketTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.channels.SocketChannel;

@Component
@Scope("prototype")
public class PushTransport extends SocketTransport {

    private static Logger logger = LoggerFactory.getLogger(PushTransport.class);

    private File dir;

    private FileQueue queue;

    @Autowired
    private FileQueueLocator locator;

    public void setFile(String path) {
        dir = new File(path);
        try {
            if (dir.exists() == false) {
                if (dir.mkdir() == false) {
                    throw new RuntimeException(String.format("Unable to make a directory '%s'", dir.getCanonicalPath()));
                }
            }
            queue = locator.locate(dir);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean repeat(SocketChannel socketChannel) throws Exception {
        String token = queue.nextToken();
        if (token != null) {
            File file = queue.consume(token);
        }
        return false;
    }

}
