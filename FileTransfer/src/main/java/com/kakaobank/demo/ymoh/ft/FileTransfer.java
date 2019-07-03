package com.kakaobank.demo.ymoh.ft;

import com.kakaobank.demo.ymoh.Server;
import com.kakaobank.demo.ymoh.ServerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Scope("singleton")
public class FileTransfer extends ServerBase implements Server {

    private static Logger logger = LoggerFactory.getLogger(FileTransfer.class);

    public FileTransfer() {
        this.identifier = "FileTransfer";
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    protected void doStart() {
        while (true) {
            if (interrupted.get()) {
                break;
            }
            try {

                cv.await(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error(String.format("'%s' was interrupted unexpected", identifier), e);
                break;
            }
        }
    }

    @Override
    protected void doStop() {
    }

}
