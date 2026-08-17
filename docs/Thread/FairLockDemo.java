import java.util.concurrent.locks.ReentrantLock;

public class FairLockDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("===== 非公平锁 =====");
        testLock(new ReentrantLock(false));

        System.out.println("\n===== 公平锁 =====");
        testLock(new ReentrantLock(true));
    }

    static void testLock(ReentrantLock lock) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 3; i++) {
                lock.lock();
                try {
                    System.out.println(Thread.currentThread().getName() + " 获得锁 第" + (i + 1) + "次");
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread t1 = new Thread(task, "线程A");
        Thread t2 = new Thread(task, "线程B");
        Thread t3 = new Thread(task, "线程C");

        t1.start(); t2.start(); t3.start();
        t1.join(); t2.join(); t3.join();
    }
}