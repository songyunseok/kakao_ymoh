package com.kakaobank.demo.ymoh.ft;

import com.kakaobank.demo.ymoh.ft.transport.PullTransport;
import com.kakaobank.demo.ymoh.ft.transport.PushTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

@ShellComponent("FileTransfer bootstrap")
public class BootstrapShell {

    private static Logger logger = LoggerFactory.getLogger(BootstrapShell.class);

    @Autowired
    private FileTransfer transfer;

    @Autowired
    private ApplicationContext appContext;

    @ShellMethod("Start a file-pushing transport")
    public void startPush(@ShellOption(value = {"-U", "--user"}) String user, @ShellOption(value = {"-P", "--path"}, defaultValue = "") String path) {
        logger.debug("Start push - user: {}, path: {}", user, path);
        PushTransport transport = appContext.getBean(PushTransport.class);
        transport.setUser(user);
        transport.setPath(path);
        transfer.startTransport(transport);
    }

    @ShellMethod("Start a file-pulling transport")
    public void startPull(@ShellOption(value = {"-U", "--user"}) String user, @ShellOption(value = {"-P", "--path"}, defaultValue = "") String path) {
        logger.debug("Start push - user: {}, path: {}", user, path);
        PullTransport transport = appContext.getBean(PullTransport.class);
        transport.setUser(user);
        transport.setPath(path);
        transfer.startTransport(transport);
    }

    @ShellMethod("List file transports by user")
    public void list(@ShellOption(value = {"-U", "--user"}) String user) {
        logger.debug("List - user: {}", user);
        List<SocketTransport> transports = transfer.getAllTransportByUser(user);
        for (SocketTransport transport : transports) {
            System.out.printf("|%-16s|%-12s|%-18s|%n", transport.getId(), transport.getMethod(), transport.isRunning() ? "ACTIVE" : "");
        }
    }

    @ShellMethod("Stop a file transport")
    public void stop(@ShellOption(value = {"-I", "--id"}) String id) {
        logger.debug("List - id: {}", id);
        transfer.stopTransport(id);
    }

    @ShellMethod("Shutdown FileTransfer bootstrap")
    public void shutdown() {
        logger.debug("Shutdown");
        transfer.stop();
        System.exit(0);
    }

}
