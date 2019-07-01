package com.kakaobank.demo.ymoh.fg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;

import java.nio.channels.SocketChannel;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import com.kakaobank.demo.ymoh.Command;
import com.kakaobank.demo.ymoh.SessionUtils;

public class FileSession implements Session {

    private static Logger logger = LoggerFactory.getLogger(FileSession.class);

    private SocketChannel socketChannel;

    private String sessionId;

    private volatile boolean running;

    private Map<String, SessionOperator> operators = new HashMap<String, SessionOperator>();

    public FileSession(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.sessionId = UUID.randomUUID().toString();
    }

    @Override
    public String start() {
        if (sessionId.isEmpty() == false) {
            new Thread(() -> {
                running = true;
                while (true) {
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
                        Command command = new Command(method, length, sessionId);
                        SessionOperator operator = operators.get(command.getMethod());
                        if (operator != null) {
                            operator.operate(command, socketChannel);
                            if (socketChannel.isConnected() == false) {
                                break;
                            }
                        } else {
                            logger.warn(String.format("FileSession '%' received an unsupported command '%s'", sessionId, command));
                        }
                    } catch (Exception e) {
                        logger.error(String.format("FileSession '%' was interrupted unexpected", sessionId), e);
                        break;
                    }
                }
                running = false;
            }).start();
        }
        return sessionId;
    }

    @Override
    public void addOperator(SessionOperator operator) {
        String method = operator.getSupportedMethod();
        operators.put(method, operator);
    }

    @Override
    public void stop() {

    }

}
