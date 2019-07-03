package com.kakaobank.demo.ymoh.fg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Collections;
import java.util.List;

@ShellComponent("FileGateway bootstrap")
public class BootstrapShell {

    private static Logger logger = LoggerFactory.getLogger(BootstrapShell.class);

    @Autowired
    private FileGateway gateway;

    @ShellMethod("Administrate FileGateway server")
    public void system(@ShellOption(value = {"-C", "--command"}, help = "(start | stop | status)") String command) {
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

    @ShellMethod("List file sessions or operations")
    public void list(@ShellOption(value = {"-C", "--command"}, help = "(sessions | operations)") String command) {
        logger.debug("List {}", command);
        if (command.equalsIgnoreCase("sessions")) {
            List<Session> sessions = gateway.getAllSessions();
            System.out.printf("%s%n", String.join("", Collections.nCopies(72, "-")));
            System.out.printf("|%-16s|%-34s|%-18s|%n", " ID", " ADDRESS", " STATUS");
            System.out.printf("%s%n", String.join("", Collections.nCopies(72, "-")));
            for (Session session : sessions) {
                System.out.printf("|%-16s|%-34s|%-18s|%n", session.getId(), session.getAddress(), session.isRunning() ? "ACTIVE" : "");
                System.out.printf("%s%n", String.join("", Collections.nCopies(72, "-")));
            }
        } else if (command.equalsIgnoreCase("operations")) {
            List<SessionOperator> operators = gateway.getAllOperators();
            System.out.printf("%s%n", String.join("", Collections.nCopies(44, "-")));
            System.out.printf("|%-16s|%-12s|%-12s|%n", " METHOD", " GOOD", " BAD");
            System.out.printf("%s%n", String.join("", Collections.nCopies(44, "-")));
            for (SessionOperator operator : operators) {
                System.out.printf("|%-16s|%12d|%12d|%n", operator.getSupportedMethod(), operator.getGoodCount(), operator.getBadCount());
                System.out.printf("%s%n", String.join("", Collections.nCopies(44, "-")));
            }
        }
    }

    @ShellMethod("Shutdown FileGateway bootstrap")
    public void shutdown() {
        logger.debug("Shutdown bootstrap");
        gateway.stop();
        System.exit(0);
    }

}
