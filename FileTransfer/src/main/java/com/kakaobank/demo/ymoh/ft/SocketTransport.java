package com.kakaobank.demo.ymoh.ft;

import com.kakaobank.demo.ymoh.Server;
import com.kakaobank.demo.ymoh.ServerBase;
import com.kakaobank.demo.ymoh.SessionUtils;
import com.kakaobank.demo.ymoh.fb.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.io.EOFException;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public abstract class SocketTransport extends ServerBase implements Server {

    private static Logger logger = LoggerFactory.getLogger(SocketTransport.class);

    private String host;

    private int port;

    //private Selector selector;

    //private SocketChannel socketChannel;

    protected File homeDir;

    protected String user;

    protected String path;

    @Autowired
    private Environment env;

    //public void setIdentifier(String identifier) { this.identifier = identifier; }

    @Value("${gateway.host:localhost}")
    public void setHost(String host) {
        this.host = host;
    }

    @Value("${gateway.port:32133}")
    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() { return this.user; }

    public void setUser(String user) { this.user = user; }

    public String getPath() { return this.path; }

    public void setPath(String path) { this.path = path; }

    public abstract String getMethod();

    protected abstract String getHomePathPropertyName();

    protected void resolveHomeDir() {
        File userDir = new File(System.getProperty("user.dir"));
        String homePath = env.getProperty(user + getHomePathPropertyName());
        if (homePath == null || homePath.length() == 0) {
            throw new RuntimeException(String.format("Home path [%s] is not found", homePath));
        }
        homeDir = new File(userDir, homePath);
        if (homeDir.exists() == false) {
            if (homeDir.mkdirs() == false) {
                throw new RuntimeException(String.format("Parent directory [%s] is not accessible", homeDir.getAbsolutePath()));
            }
        }
        if (path != null && path.length() > 0) {
            homeDir = new File(homeDir, path);
            if (homeDir.mkdirs() == false) {
                throw new RuntimeException(String.format("Parent directory [%s] is not accessible", homeDir.getAbsolutePath()));
            }
        }
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    protected void doStart() {
        resolveHomeDir();
        SocketChannel socketChannel = null;
        try {
            //selector = Selector.open();
            InetSocketAddress addr = new InetSocketAddress(host, port);
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            logger.info("{}: Initiating connection", identifier);
            socketChannel.connect(addr);
            int timeoutCount = 0;
            while (!socketChannel.finishConnect() && ++timeoutCount < 10) {
                if (interrupted.get()) {
                    break;
                }
                logger.debug("(): Connecting...", identifier);
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
                    throw new TransportException("Unable to connect to FileGateway server");
                }
            }
            if (interrupted.get()) {
                throw new TransportException("Connection has been interrupted");
            }
        } catch (Exception ex) {
            logger.error(String.format("%s: SocketTransport failed", identifier), ex);
        } finally {
            if (socketChannel != null) {
                try {
                    socketChannel.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public abstract boolean repeat(SocketChannel socketChannel) throws Exception;

    protected void writeRequest(SocketChannel socketChannel, String method, byte[] reqBytes) throws Exception {
        byte[] methodBytes = new byte[SessionUtils.OP_NETHOD_SIZE];
        SessionUtils.putString(methodBytes, method);
        SessionUtils.write(socketChannel, methodBytes);
        byte[] lengthBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
        SessionUtils.putInt(lengthBytes, reqBytes.length);
        SessionUtils.write(socketChannel, lengthBytes);
        SessionUtils.write(socketChannel, reqBytes);
    }

    protected void readResponse(SocketChannel socketChannel, String token) throws Exception {
        byte[] sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
        int n = SessionUtils.read(socketChannel, sizeBytes);
        if (n < 0) {
            throw new EOFException(String.format("SocketTransport '%s' was disconnected", identifier));
        }
        byte[] respBytes = new byte[SessionUtils.parseInt(sizeBytes)];
        n = SessionUtils.read(socketChannel, respBytes);
        if (n < 0) {
            throw new EOFException(String.format("SocketTransport '%s' was disconnected", identifier));
        }
        Operation.SessionResponse reply = Operation.SessionResponse.parseFrom(respBytes);
        if (token.equals(reply.getToken())) {
            String status = reply.getStatus();
            if (status.equals("OK") == false) {
                String reason = reply.getReason();
                throw new TransportException(reason != null && reason.length() > 0 ? reason: "Unknown reason");
            }
        } else {
            throw new TransportException("Token in reply is invalid");
        }
    }

    protected void writeResponse(SocketChannel socketChannel, byte[] respBytes) throws Exception {
        byte[] sizeBytes = new byte[SessionUtils.OP_LENGTH_SIZE];
        SessionUtils.putInt(sizeBytes, respBytes.length);
        SessionUtils.write(socketChannel, sizeBytes);
        SessionUtils.write(socketChannel, respBytes);
    }

    @Override
    protected void doStop() {
    }

}
