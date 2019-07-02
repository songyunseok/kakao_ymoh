package com.kakaobank.demo.ymoh.fg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class BootstrapShell {

    private static Logger logger = LoggerFactory.getLogger(BootstrapShell.class);

    @Autowired
    private FileGateway gateway;

    @ShellMethod("Administrate FileGateway server")
    public void sys(@ShellOption({"-C", "--command"}) String command) {
        logger.debug("Command: {}", command);
        if (command.equalsIgnoreCase("start")) {
            gateway.start();
        } else if (command.equalsIgnoreCase("stop")) {
            gateway.stop();
        } else if (command.equalsIgnoreCase("status")) {
            if (gateway.isRunning()) {
                System.out.println("OPERATIONAL");
            } else {
                System.out.println("STOPPED");
            }
        }
    }

    @ShellMethod("Shutdown FileGateway bootstrap")
    public void shutdown() {
        logger.debug("Shutdown");
        gateway.stop();
        System.exit(0);
    }

}
