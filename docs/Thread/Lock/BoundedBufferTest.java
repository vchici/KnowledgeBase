package Lock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// 1. 你的 BoundedBuffer 类（稍作修改，让 take 方法返回 Object）
class BoundedBuffer {
    private final Lock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition(); 
    private final Condition notEmpty = lock.newCondition(); 

    // 为了方便测试看效果，把容量改小一点（比如 3）
    private final Object[] items = new Object[3]; 
    private int putptr, takeptr, count;

    public void put(Object x) throws InterruptedException {
        lock.lock(); 
        try {
            while (count == items.length) {
                System.out.println(Thread.currentThread().getName() + " [Put] 发现缓冲区满了 ❌，进入 notFull 房间睡觉...");
                notFull.await(); 
            }
            items[putptr] = x;
            if (++putptr == items.length) putptr = 0;
            ++count;
            
            System.out.println(Thread.currentThread().getName() + " 成功放进数据: [" + x + "], 当前库存: " + count);
            
            notEmpty.signal(); 
        } finally {
            lock.unlock(); 
        }
    }

    public Object take() throws InterruptedException { // 修改返回值类型为 Object
        lock.lock();
        try {
            while (count == 0) {
                System.out.println(Thread.currentThread().getName() + " [Take] 发现缓冲区空了 ❌，进入 notEmpty 房间睡觉...");
                notEmpty.await();
            }
            Object x = items[takeptr];
            if (++takeptr == items.length) takeptr = 0;
            --count;
            
            System.out.println(Thread.currentThread().getName() + " 成功取出数据: [" + x + "], 剩余库存: " + count);
            
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }
}

// 2. 测试主类
public class BoundedBufferTest {
    public static void main(String[] args) {
        // 创建一个容量为 3 的有界缓冲区
        BoundedBuffer buffer = new BoundedBuffer();

        // 创建 3 个生产者线程
        for (int i = 1; i <= 3; i++) {
            final int producerId = i;
            new Thread(() -> {
                int itemNumber = 1;
                while (true) {
                    try {
                        // 每一个生产者往里面放自己生产的商品，比如 "商品-1-1"
                        String data = "商品-" + producerId + "-" + itemNumber++;
                        buffer.put(data);
                        // 随机睡一会儿，模拟生产耗时
                        Thread.sleep((long) (Math.random() * 1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "生产者-" + i).start();
        }

        // 创建 2 个消费者线程（故意让消费者比生产者少，或者消费慢一点，更容易触发“满”的场景）
        for (int i = 1; i <= 2; i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        buffer.take();
                        // 故意让消费者睡得久一点，造成供大于求，逼迫缓冲区变满
                        Thread.sleep((long) (Math.random() * 1500));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "消费者-" + i).start();
        }
    }
}