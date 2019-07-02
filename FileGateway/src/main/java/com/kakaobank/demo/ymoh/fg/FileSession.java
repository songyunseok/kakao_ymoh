package com.kakaobank.demo.ymoh.fg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;

import java.nio.channels.SocketChannel;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.kakaobank.demo.ymoh.SessionUtils;

public class FileSession implements Session {

    private static Logger logger = LoggerFactory.getLogger(FileSession.class);

    private SocketChannel socketChannel;

    private String sessionId;

    private AtomicBoolean running = new AtomicBoolean(false);

    private AtomicBoolean interrupted = new AtomicBoolean(false);

    private Lock lock = new ReentrantLock();

    private Condition cv = lock.newCondition();

    private Map<String, SessionOperator> operators = new HashMap<String, SessionOperator>();

    private List<SessionListener> listeners = new ArrayList<>();

    public FileSession(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.sessionId = UUID.randomUUID().toString();
    }

    public String getId() {
        return sessionId;
    }

    @Override
    public void start() {
        if (running.get()) {
            logger.warn("FileSession '{}' server is already running", sessionId);
            return;
        }
        Thread t = new Thread(() -> {
            running.set(true);
            lock.lock();
            try {
                while (true) {
                    if (interrupted.get()) {
                        break;
                    }
                    try {
                        byte[] methodBytes = new byte[SessionUtils.OP_NETHOD_SIZE];
                        int n = SessionUtils.read(socketChannel, methodBytes);
                        if (n < 0) {
                            throw new EOFException(String.format("FileSession '%s' was disconnected", sessionId));
                        }
                        String method = SessionUtils.parseString(methodBytes);
                        logger.debug("FileSession '%s' method = %s", sessionId, method);
                        byte[] lengthBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
                        n = SessionUtils.read(socketChannel, lengthBytes);
                        if (n < 0) {
                            throw new EOFException(String.format("FileSession '%s' was disconnected", sessionId));
                        }
                        int length = SessionUtils.parseInt(lengthBytes);
                        logger.debug("FileSession '%s' length = %d", sessionId, length);
                        SessionCommand command = new SessionCommand(method, length, sessionId);
                        SessionOperator operator = operators.get(command.getMethod());
                        if (operator != null) {
                            operator.operate(command, socketChannel);
                            if (socketChannel.isConnected() == false) {
                                break;
                            }
                        } else {
                            logger.warn(String.format("FileSession '%' received an unsupported command '%s'", sessionId, command));
                        }
                        cv.await(200, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        logger.error(String.format("FileSession '%' was interrupted unexpected", sessionId), e);
                        break;
                    }
                }
            } finally {
                lock.unlock();
                for (SessionListener listener : listeners) {
                    listener.sessionStopping(sessionId);
                }
                try {
                    socketChannel.close();
                } catch (Exception ignore) {
                }
                running.set(false);
            }
        });
        t.setName(sessionId);
        t.start();
    }

    @Override
    public void addOperator(SessionOperator operator) {
        String method = operator.getSupportedMethod();
        operators.put(method, operator);
    }

    @Override
    public void addListener(SessionListener listener) {
        listeners.add(listener);
    }

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
