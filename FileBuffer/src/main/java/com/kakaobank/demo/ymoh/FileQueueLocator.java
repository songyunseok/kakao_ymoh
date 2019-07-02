package com.kakaobank.demo.ymoh;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class FileQueueLocator {

    private Lock lock = new ReentrantLock();

    private Map<String,FileQueue> queues = new ConcurrentHashMap<>();

    public FileQueue locate(File directory) throws Exception {
        String path = directory.getCanonicalPath();
        FileQueue queue = queues.get(path);
        if (queue == null) {
            lock.lock();
            try {
                queue = new FileQueue(directory);
                queues.put(path, queue);
            } finally {
                lock.unlock();
            }
        }
        return queue;
    }

}
