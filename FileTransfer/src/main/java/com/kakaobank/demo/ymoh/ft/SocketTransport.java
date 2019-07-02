package com.kakaobank.demo.ymoh.ft;

import com.kakaobank.demo.ymoh.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetSocketAddress;
//import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class SocketTransport implements Server {

    private static Logger logger = LoggerFactory.getLogger(SocketTransport.class);

    private String host;

    private int port;

    private String name;

    //private Selector selector;

    //private SocketChannel socketChannel;

    private AtomicBoolean running = new AtomicBoolean(false);

    private AtomicBoolean interrupted = new AtomicBoolean(false);

    private Lock lock = new ReentrantLock();

    private Condition cv = lock.newCondition();

    @Value("${gateway.host:}")
    public void setHost(String host) {
        this.host = host;
    }

    @Value("${gateway.port:32133}")
    public void setPort(int port) {
        this.port = port;
    }

    public void setName(String name) { this.name = name; }

    @Override
    public void start() {
        if (running.get()) {
            logger.warn("{}: SocketTransport is already running", name);
            return;
        }
        Thread t = new Thread(() -> {
            running.set(true);
            logger.info("{}: Running SocketTransport", name);
            //interrupted.set(false);
            SocketChannel socketChannel = null;
            lock.lock();
            try {
                //selector = Selector.open();
                InetSocketAddress addr = new InetSocketAddress(host, port);
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                logger.info("{}: Initiating connection", name);
                socketChannel.connect(addr);
                int timeoutCount = 0;
                while (!socketChannel.finishConnect() && ++timeoutCount < 10) {
                    if (interrupted.get()) {
                        break;
                    }
                    logger.debug("(): Connecting...", name);
                    try {
                        cv.await(1000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
                if (interrupted.get() == false) {
                    if (socketChannel.isConnected()) {
                        //writingTolerancy = (timeoutInSeconds > 1000 ? (int) (timeoutInSeconds / 100) : 10);
                        //socketChannel.register(selector, SelectionKey.OP_READ);
                        while (true) {
                            if (interrupted.get()) {
                                break;
                            }
                            if (repeat(socketChannel) == false) {
                                cv.await(1000, TimeUnit.MILLISECONDS);
                            }
                        }
                    } else {
                        throw new IOException("Unable to connect to FileGateway server");
                    }
                }
                if (interrupted.get()) {
                    throw new IOException("Connection has been interrupted");
                }
            } catch (Exception ex) {

            } finally {
                lock.unlock();
                if (socketChannel != null) {
                    try {
                        socketChannel.close();
                    } catch (Exception ignore) {
                    }
                }
                logger.info("{}: SocketTransport finished", name);
                running.set(false);
            }
        });
        t.setName(name);
        t.start();
    }

    public abstract boolean repeat(SocketChannel socketChannel) throws Exception;

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void stop() {
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

}
