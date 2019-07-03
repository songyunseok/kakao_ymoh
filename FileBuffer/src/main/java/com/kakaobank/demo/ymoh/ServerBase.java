package com.kakaobank.demo.ymoh;

import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ServerBase implements Server {

    protected AtomicBoolean running = new AtomicBoolean(false);

    protected AtomicBoolean interrupted = new AtomicBoolean(false);

    protected Lock lock = new ReentrantLock();

    protected Condition cv = lock.newCondition();

    protected String identifier;

    @Override
    public String getId() {
        return this.identifier;
    }

    protected abstract Logger logger();

    @Override
    public void start() {
        if (running.get()) {
            logger().warn(String.format("%s is already running", identifier));
            return;
        }
        Thread t = new Thread(() -> {
            running.set(true);
            logger().info(String.format("Running %s", identifier));
            lock.lock();
            try {
                doStart();
            } finally {
                lock.unlock();
                logger().info(String.format("%s finished", identifier));
                running.set(false);
            }
        });
        t.setName(identifier);
        t.start();
    }

    protected abstract void doStart();

    @Override
    public boolean isRunning() {
        return running.get();
    }

    protected abstract void doStop();

    @Override
    public void stop() {
        doStop();
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

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof ServerBase && getClass() == o.getClass()) {
            ServerBase other = (ServerBase)o;
            return identifier.equals(other.identifier);
        }
        return false;
    }

}
