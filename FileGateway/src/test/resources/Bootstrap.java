package com.kakaobank.demo.ymoh.fg;

import com.kakaobank.demo.ymoh.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

/*
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
*/
import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

@Component
public class Bootstrap implements Server {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    @Autowired
    private FileGateway gateway;

    private AtomicBoolean running = new AtomicBoolean(false);

    private AtomicBoolean interrupted = new AtomicBoolean(false);

    private Lock lock = new ReentrantLock();

    private Condition cv = lock.newCondition();

    /*private String host;

    private int port;

    @Value("${bootstrap.host}")
    public void setHost(String host) {
        this.host = host;
    }

    @Value("${bootstrap.port}")
    public void setPort(int port) {
        this.port = port;
    }*/

    @Override
    public void start() {
        running.set(false);
        interrupted.set(false);
        Thread t = new Thread(() -> {
            logger.info("Running Bootstrap");
            //ServerSocketChannel serverSocketChannel;
            //Selector selector;
            running.set(true);
            lock.lock();
            try {
                /*selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                ServerSocket serverSocket = serverSocketChannel.socket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind((host == null || host.length() == 0) ? new InetSocketAddress(port) : new InetSocketAddress(host, port));
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);*/

                loop: while (true) {
                    if (interrupted.get()) {
                        break;
                    }
                    cv.await(1000, TimeUnit.MILLISECONDS);

                    /*try {
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
                                        socketChannel.register(selector, SelectionKey.OP_READ);
                                        logger.info("Accepted a client connection from {}", socketChannel.getRemoteAddress().toString());
                                    }
                                } else if (key.isReadable()) {
                                    SocketChannel socketChannel = (SocketChannel)key.channel();
                                    byte[] byteArray = new byte[1];
                                    int len = SessionUtils.read(socketChannel, byteArray);
                                    if (len < 0) {
                                        logger.warn("Client was disconnected");
                                        continue;
                                    }
                                    boolean shutdown = false;
                                    String status = "";
                                    int cmd = SessionUtils.parseInt(byteArray);
                                    if (cmd == 1) {
                                        if (gateway.isRunning() == false) {
                                            gateway.start();
                                            status = "STARTING";
                                        } else {
                                            status = "STARTED";
                                        }
                                    } else if (cmd == 2) {
                                        if (gateway.isRunning()) {
                                            gateway.stop();
                                            status = "STOPPING";
                                        } else {
                                            status = "STOPPED";
                                        }
                                    } else if (cmd == -1) {
                                        if (gateway.isRunning()) {
                                            gateway.stop();
                                        }
                                        status = "SHUTDOWN";
                                        shutdown = true;
                                    } else {
                                        if (gateway.isRunning()) {
                                            status = "RUNNING";
                                        } else {
                                            status = "STOPPED";
                                        }
                                    }
                                    try {
                                        byteArray = new byte[10];
                                        SessionUtils.putString(byteArray, status);
                                        SessionUtils.write(socketChannel, byteArray);
                                    } catch (Throwable ignore) {
                                    }
                                    if (shutdown) {
                                        break loop;
                                    }
                                }
                            }
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        logger.warn("An error occurred while processing a client request", e);
                    }*/
                }
            } catch (Throwable throwable) {
                logger.error("A fatal error occurred while listening", throwable);
            } finally {
                lock.unlock();
                logger.info("Bootstrap finished");
            }
            running.set(false);
        });
        //t.setDaemon(true);
        t.setName("FileGateway-Bootstrap");
        t.start();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void stop() {
        interrupted.set(true);
    }
}
