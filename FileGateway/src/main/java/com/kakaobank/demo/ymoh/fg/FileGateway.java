package com.kakaobank.demo.ymoh.fg;

import com.kakaobank.demo.ymoh.Server;
import com.kakaobank.demo.ymoh.ServerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import tlschannel.ServerTlsChannel;
import tlschannel.TlsChannel;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Scope("singleton")
public class FileGateway extends ServerBase implements Server, SessionListener {

    private static final Logger logger = LoggerFactory.getLogger(FileGateway.class);

    private String host;

    private int port;

    private Map<String,Session> sessions = new ConcurrentHashMap<>();

    @Autowired
    private SSLContext sslContext;

    @Autowired
    private List<SessionOperator> operators;

    public FileGateway() {
        this.identifier = "FileGateway";
    }

    @Value("${gateway.host:}")
    public void setHost(String host) {
        this.host = host;
    }

    @Value("${gateway.port:32133}")
    public void setPort(int port) {
        this.port = port;
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
        Selector selector = null;
        ServerSocketChannel serverSocketChannel = null;
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind((host == null || host.length() == 0) ? new InetSocketAddress(port) : new InetSocketAddress(host, port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            loop: while (true) {
                if (interrupted.get()) {
                    break;
                }
                try {
                    int n = selector.selectNow();
                    if (n > 0) {
                        Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                        while (iter.hasNext()) {
                            SelectionKey key = iter.next();
                            iter.remove();
                            if (!key.isValid()) {
                                continue;
                            }
                            if (key.isAcceptable()) {
                                ServerSocketChannel server = (ServerSocketChannel)key.channel();
                                SocketChannel socketChannel = server.accept();
                                if (socketChannel != null) {
                                    socketChannel.configureBlocking(false);
                                    //socketChannel.register(selector, SelectionKey.OP_READ);
                                    logger.info("Accepted a client connection from {}", socketChannel.getRemoteAddress().toString());

                                    String address = socketChannel.getRemoteAddress().toString();

                                    TlsChannel tlsChannel = ServerTlsChannel.newBuilder(socketChannel, sslContext).build();

                                    FileSession session = new FileSession(tlsChannel, address);
                                    sessions.put(session.getId(), session);
                                    session.addListener(this);
                                    for (SessionOperator operator : operators) {
                                        session.addOperator(operator);
                                    }
                                    session.start();
                                }
                            }
                        }
                    }
                    cv.await(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    logger.warn("An error occurred while processing a client request", e);
                }
            }
        } catch (Throwable throwable) {
            logger.error("A fatal error occurred while listening", throwable);
        } finally {
            if (selector != null) {
                try {
                    selector.close();
                } catch (Exception ignore) {
                }
            }
            if (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    @PreDestroy
    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected void doStop() {
        List<String> ids = new ArrayList<>(sessions.keySet());
        for (String id : ids) {
            Session session = sessions.remove(id);
            session.stop();
        }
    }

    @Override
    public void sessionStopping(String id) {
        Session session = sessions.remove(id);
        if (session != null) {
            logger.info("Remove a client session '{}'", id);
        }
    }

    public List<SessionOperator> getAllOperators() {
        return Collections.unmodifiableList(operators);
    }

    public List<Session> getAllSessions() {
        List<Session> list = new ArrayList<>();
        List<String> ids = new ArrayList<>(sessions.keySet());
        for (String id : ids) {
            Session session = sessions.get(id);
            if (session != null) {
                list.add(session);
            }
        }
        return Collections.unmodifiableList(list);
    }

}
