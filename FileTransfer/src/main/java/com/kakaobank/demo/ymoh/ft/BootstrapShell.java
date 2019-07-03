package com.kakaobank.demo.ymoh.ft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent("FileTransfer bootstrap")
public class BootstrapShell {

    private static Logger logger = LoggerFactory.getLogger(BootstrapShell.class);

    @Autowired
    private FileTransfer transfer;

    @ShellMethod("Administrate FileTransfer server")
    public void sys(@ShellOption(value = {"-C", "--command"}, help = "(start | stop | status)") String command) {
        logger.debug("Command: {}", command);
        /*if (command.equalsIgnoreCase("start")) {
            gateway.start();
        } else if (command.equalsIgnoreCase("stop")) {
            gateway.stop();
        } else if (command.equalsIgnoreCase("status")) {
            if (gateway.isRunning()) {
                System.out.println("OPERATIONAL");
            } else {
                System.out.println("STOPPED");
            }
        }*/
    }

    @ShellMethod("Shutdown FileTransfer bootstrap")
    public void shutdown() {
        logger.debug("Shutdown");
        transfer.stop();
        System.exit(0);
    }

}
