package com.juju.beautiful.thread.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author juju_liu
 */
public class JujuThreadPool {
    private final static Logger logger = LoggerFactory.getLogger(JujuThreadPool.class);

    private final ReentrantLock lock = new ReentrantLock();
    private volatile int minSize;
    private volatile int maxSize;


    private final class Worker extends Thread{
        private Runnable task;
        private Thread thread;
        private boolean isNew;

        public Worker(Runnable task,boolean isNew){
            this.task = task;
            this.isNew = isNew;
            thread = this;
        }

        public void startTask(){
            thread.start();
        }

        public void closeTask(){
            thread.interrupt();
        }

        @Override
        public void run() {

        }
    }
}
