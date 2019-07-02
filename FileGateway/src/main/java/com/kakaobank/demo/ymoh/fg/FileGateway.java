package com.kakaobank.demo.ymoh.fg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class FileGateway implements SessionListener {

    private static final Logger logger = LoggerFactory.getLogger(FileGateway.class);

    private AtomicBoolean running = new AtomicBoolean(false);

    private AtomicBoolean interrupted = new AtomicBoolean(false);

    private Lock lock = new ReentrantLock();

    private Condition cv = lock.newCondition();

    private String host;

    private int port;

    private Map<String,Session> sessions = new ConcurrentHashMap<>();

    @Autowired
    private List<SessionOperator> operators;

    @Value("${gateway.host:}")
    public void setHost(String host) {
        this.host = host;
    }

    @Value("${gateway.port:32133}")
    public void setPort(int port) {
        this.port = port;
    }

    @PostConstruct
    public void start() {
        if (running.get()) {
            logger.warn("FileGateway server is already running");
            return;
        }
        Thread t = new Thread(() -> {
            running.set(true);
            logger.info("Running FileGateway");
            ServerSocketChannel serverSocketChannel = null;
            Selector selector = null;
            lock.lock();
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
                                        FileSession session = new FileSession(socketChannel);
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
                lock.unlock();
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
                logger.info("FileGateway finished");
                running.set(false);
            }
        });
        //t.setDaemon(true);
        t.setName("FileGateway");
        t.start();
    }

    public boolean isRunning() {
        return running.get();
    }

    @PreDestroy
    public void stop() {
        List<String> ids = new ArrayList<>(sessions.keySet());
        for (String id : ids) {
            Session session = sessions.remove(id);
            session.stop();
        }
        if (running.get()) {
            lock.lock();
            try {
                interrupted.set(true);
                cv.signalAll();
            } finally {
                lock.unlock();
            }
            while (running.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                    break;
                }
            }
            interrupted.set(false);
        }
    }

    @Override
    public void sessionStopping(String id) {
        Session session = sessions.remove(id);
        if (session != null) {
            logger.info("Remove a client session '{}'", id);
        }
    }

}
