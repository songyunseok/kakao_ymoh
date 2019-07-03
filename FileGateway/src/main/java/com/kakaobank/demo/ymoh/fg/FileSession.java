package com.kakaobank.demo.ymoh.fg;

import com.kakaobank.demo.ymoh.ServerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;

import java.nio.channels.SocketChannel;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.kakaobank.demo.ymoh.SessionUtils;

public class FileSession extends ServerBase implements Session {

    private static Logger logger = LoggerFactory.getLogger(FileSession.class);

    private SocketChannel socketChannel;

    private Map<String, SessionOperator> operators = new HashMap<String, SessionOperator>();

    private List<SessionListener> listeners = new ArrayList<>();

    private static volatile long instanceCounter = 0;

    private String address;

    public FileSession(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        //this.sessionId = UUID.randomUUID().toString();
        this.identifier = "FS_" + Long.toString(++instanceCounter);

    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    protected void doStart() {
        try {
            address = socketChannel.getRemoteAddress().toString();
            while (true) {
                if (interrupted.get()) {
                    break;
                }
                try {
                    byte[] methodBytes = new byte[SessionUtils.OP_NETHOD_SIZE];
                    int n = SessionUtils.read(socketChannel, methodBytes);
                    if (n < 0) {
                        throw new EOFException(String.format("FileSession '%s' was disconnected", identifier));
                    }
                    String method = SessionUtils.parseString(methodBytes);
                    logger.debug("FileSession '{}' method = '{}", identifier, method);
                    byte[] lengthBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
                    n = SessionUtils.read(socketChannel, lengthBytes);
                    if (n < 0) {
                        throw new EOFException(String.format("FileSession '%s' was disconnected", identifier));
                    }
                    int length = SessionUtils.parseInt(lengthBytes);
                    logger.debug("FileSession '{}' length = {}", identifier, length);
                    SessionCommand command = new SessionCommand(method, length, identifier);
                    SessionOperator operator = operators.get(command.getMethod());
                    if (operator != null) {
                        operator.operate(command, socketChannel);
                        if (socketChannel.isConnected() == false) {
                            break;
                        }
                    } else {
                        logger.warn(String.format("FileSession '%s' received an unsupported command '%s'", identifier, command));
                    }
                    cv.await(200, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    logger.error(String.format("FileSession '%s' was interrupted unexpected", identifier), e);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(String.format("FileSession '%s' was NOT started", identifier), e);
        } finally {
            for (SessionListener listener : listeners) {
                listener.sessionStopping(identifier);
            }
            try {
                socketChannel.close();
            } catch (Exception ignore) {
            }
        }
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
    protected void doStop() {
    }

}
