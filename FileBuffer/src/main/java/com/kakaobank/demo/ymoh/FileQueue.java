package com.kakaobank.demo.ymoh;

import java.io.File;
import java.io.FileFilter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileQueue {

    private Lock lock = new ReentrantLock();

    private List<String> tokens = new ArrayList<>();

    private Map<String, File> files = new HashMap<>();

    private Map<String, Long> pending = new HashMap<>();

    private File directory;

    private FileFilter fileFilter;

    public FileQueue(File directory) {
        this.directory = directory;
        this.fileFilter = pathname -> {
            if (pathname.isFile() && pathname.isHidden() == false) {
                return true;
            }
            return false;
        };
    }

    public FileQueue(File directory, FileFilter fileFilter) {
        this.directory = directory;
        this.fileFilter = fileFilter;
    }

    public String nextToken() throws Exception {
        lock.lock();
        try  {
            long tid = Thread.currentThread().getId();
            if (tokens.size() == 0) {
                File[] files = directory.listFiles(fileFilter);
                if (files != null && files.length > 0) {
                    String token = null;
                    for (File file : files) {
                        token = UUID.randomUUID().toString();
                        tokens.add(token);
                        this.files.put(token, file);
                        //this.pending.put(token, tid);
                    }
                }
            }
            if (tokens.size() > 0) {
                int i = 0;
                while (i < tokens.size()) {
                    String token = tokens.get(i);
                    Long pendingTid = pending.get(token);
                    if (pendingTid == null || pendingTid.longValue() == tid) {
                        pending.put(token, tid);
                        return token;
                    } else {
                        int index = -1;
                        ThreadMXBean tmbean = ManagementFactory.getThreadMXBean();
                        long[] tids = tmbean.getAllThreadIds();
                        for (int t = 0; t < tids.length; t++) {
                            if (tids[t] == pendingTid.longValue()) {
                                index = t;
                                break;
                            }
                        }
                        if (index == -1) {
                            pending.remove(token);
                            pending.put(token, tid);
                            return token;
                        }
                    }
                    i++;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public File consume(String token) {
        lock.lock();
        try {
            return files.get(token);
        } finally {
            lock.unlock();
        }
    }

    public void commit(String token) {
        lock.lock();
        try {
            pending.remove(token);
            files.remove(token);
            int index = -1;
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i).equals(token)) {
                    index = i;
                    break;
                }
            }
            if (index > 0) {
                tokens.remove(index);
            }
        } finally {
            lock.unlock();
        }
    }

    public void rollback(String token) {
        lock.lock();
        try {
            pending.remove(token);
        } finally {
            lock.unlock();
        }
    }

}
