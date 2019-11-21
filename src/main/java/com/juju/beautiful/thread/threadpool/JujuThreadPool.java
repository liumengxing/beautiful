package com.juju.beautiful.thread.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程池
 *
 * @author juju_liu
 */
public class JujuThreadPool {
    // region fields
    /**
     * 最小线程数，即核心线程数
     */
    private volatile int minSize;
    /**
     * 最大线程数
     */
    private volatile int maxSize;
    /**
     * 线程需要被回收的时间
     */
    private long keepAliveTime;
    /**
     * 存放线程的阻塞队列
     */
    private BlockingQueue<Runnable> workQueue;
    /**
     * juju的线程池
     */
    private volatile Set<JujuThread> jujuThreadPool;
    /**
     * 线程池关闭标志，使用atomicboolean，保证原子性
     */
    private AtomicBoolean isShutDown = new AtomicBoolean(false);
    /**
     * 线程池中任务总数
     */
    private AtomicInteger taskNum = new AtomicInteger();
    private final static Logger logger = LoggerFactory.getLogger(JujuThreadPool.class);
    private final ReentrantLock lock = new ReentrantLock();
    // endregion

    // region juju线程池基本函数

    /**
     *
     * @param minSize
     * @param maxSize
     * @param keepAliveTime
     * @param workQueue
     */
    public JujuThreadPool(int minSize, int maxSize, long keepAliveTime, BlockingQueue<Runnable> workQueue) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.keepAliveTime = keepAliveTime;
        this.workQueue = workQueue;
        this.jujuThreadPool = new ConcurrentHashSet();
    }

    /**
     *
     * @param callable
     * @param <T>
     * @return
     */
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask futureTask = new FutureTask(callable);
        execute(futureTask);
        return futureTask;
    }

    /**
     *
     * @param runnable
     */
    public void execute(Runnable runnable) {
        if (null == runnable) {
            throw new NullPointerException("runnable is null");
        }
        if (isShutDown.get()) {
            logger.info("shutdown already");
            return;
        }
        taskNum.incrementAndGet();
        if (jujuThreadPool.size() < minSize) {
            addThread(runnable);
            return;
        }

        // 写入队列失败，
        if (!workQueue.offer(runnable)) {
            // 创建新线程执行
            if (jujuThreadPool.size() < maxSize) {
                addThread(runnable);
                return;
            } else {
                logger.error(String.format("above maxsize[{%d}]", maxSize));
                try {
                    workQueue.put(runnable);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * 添加任务，需要加锁
     *
     * @param runnable 任务
     */
    private void addThread(Runnable runnable) {
        JujuThread thread = new JujuThread(runnable, true);
        thread.startTask();
        jujuThreadPool.add(thread);
    }

    /**
     * 获取工作线程数量
     *
     * @return
     */
    public int getWorkerCount() {
        return jujuThreadPool.size();
    }

    /**
     * 立即关闭线程池，会造成任务丢失
     */
    public void shutDownNow() {
        isShutDown.set(true);
        tryClose(false);
    }

    /**
     * 任务执行完毕后关闭线程池
     */
    public void shutdown() {
        isShutDown.set(true);
        tryClose(true);
    }

    /**
     * 关闭线程池
     *
     * @param isTry true 尝试关闭      --> 会等待所有任务执行完毕
     *              false 立即关闭线程池--> 任务有丢失的可能
     */
    private void tryClose(boolean isTry) {
        if (!isTry) {
            closeAllTask();
        } else {
            if (isShutDown.get() && taskNum.get() == 0) {
                closeAllTask();
            }
        }

    }

    /**
     * 关闭所有任务
     */
    private void closeAllTask() {
        logger.info("start close thread");
        for (JujuThread jujuThread : jujuThreadPool) {
            jujuThread.closeTask();
        }
    }
    // endregion

    // region 内部类
    /**
     * 内部存放工作线程容器，通过ConcurrentHashMap实现，并发安全。
     *
     * @param <T>
     */
    private final class ConcurrentHashSet<T> extends AbstractSet<T> {
        private ConcurrentHashMap<T, Object> map = new ConcurrentHashMap<>();
        private final Object PRESENT = new Object();

        private AtomicInteger count = new AtomicInteger();

        @Override
        public Iterator<T> iterator() {
            return map.keySet().iterator();
        }

        @Override
        public boolean add(T t) {
            count.incrementAndGet();
            return map.put(t, PRESENT) == null;
        }

        @Override
        public boolean remove(Object o) {
            count.decrementAndGet();
            return map.remove(o) == PRESENT;
        }

        @Override
        public int size() {
            return count.get();
        }
    }

    /**
     * juju的线程
     */
    private final class JujuThread extends Thread {
        private Runnable task;
        private Thread thread;
        private boolean isNew;

        public JujuThread(Runnable task, boolean isNew) {
            this.task = task;
            this.isNew = isNew;
            thread = this;
        }

        public void startTask() {
            thread.start();
        }

        public void closeTask() {
            thread.interrupt();
        }

        @Override
        public void run() {

        }
    }
    // endregion
}
