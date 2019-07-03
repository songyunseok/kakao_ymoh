package com.kakaobank.demo.ymoh.ft;

import com.kakaobank.demo.ymoh.Server;
import com.kakaobank.demo.ymoh.ServerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Scope("singleton")
public class FileTransfer extends ServerBase implements Server {

    private static Logger logger = LoggerFactory.getLogger(FileTransfer.class);

    private Map<String,SocketTransport> transports = new ConcurrentHashMap<>();

    public FileTransfer() {
        this.identifier = "FileTransfer";
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @PostConstruct
    @Override
    public void start() {
        super.start();
    }

    @Override
    protected void doStart() {
        while (true) {
            if (interrupted.get()) {
                break;
            }
            try {
                //Do house-keeping
                cv.await(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                logger.error(String.format("'%s' was interrupted unexpected", identifier), e);
                break;
            }
        }
    }

    public void startTransport(SocketTransport transport) {
        transports.put(transport.getId(), transport);
        transport.start();
    }

    public void stopTransport(String id) {
        SocketTransport transport = transports.remove(id);
        if (transport != null) {
            transport.stop();
        }
    }

    public SocketTransport getTransport(String id) {
        return transports.get(id);
    }

    public List<SocketTransport> getAllTransportByUser(String user) {
        List<SocketTransport> list = new ArrayList<>();
        List<String> ids = new ArrayList<>(transports.keySet());
        for (String id : ids) {
            SocketTransport transport = transports.get(id);
            if (transport != null) {
                if (transport.getUser().equals(user)) {
                    list.add(transport);
                }
             }
        }
        return Collections.unmodifiableList(list);
    }

    @PreDestroy
    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected void doStop() {
        List<String> ids = new ArrayList<>(transports.keySet());
        for (String id : ids) {
            SocketTransport transport = transports.remove(id);
            if (transport != null) {
                transport.stop();
            }
        }
    }

}
