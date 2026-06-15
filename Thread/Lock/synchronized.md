# 阶段

随着竞争线程数量的提升：

偏向锁（1）[^2] -- 轻量级锁（>1，但无冲突[^1]） -- 重量级锁（存在冲突，自旋超过一定次数）

# 消耗

## 重量级锁阶段

用户态到内核态的切换：

线程挂起时，操作系统要保存当前线程的状态，再把CPU让给别的线程。

锁释放时，操作系统又要经历一次反向切换唤醒它。

## 轻量级锁阶段

CPU自旋消耗：

未抢到锁的线程，CAS自旋，while(true)死循环，占用的CPU资源取决于，持有锁的线程执行的时间。

## 偏向锁阶段

锁对象处于从未加锁状态，线程将自己ID写入锁对象的 `Mark Word` 中

线程再次进入时，只需对比线程ID是否是自己，是则直接放行无消耗

# 锁对象

每一个Java对象，对象头（Mark Word）中有一个插槽。

执行到`synchronized(lock)` 时，会看 `lock` 对象的对象头，

如果里面里面为空，写入线程ID，

当其他线程执行到此处时，看到 `lock` 对象的对象头里是别人的名字，

`lock` 对象就会把它抓住，扔进等待队列（EntryList）

直到名字被擦除，`lock` 才会从队列里“唤醒”其他线程抢门票

## 非公平机制

这里的“唤醒”后的竞争，是非公平的。

从内核态往用户态切换的线程，和处于运行态的新到的线程一同争抢，

大概率新到线程通过一个CAS操作，将对象头里改成自己的名字，

因为切换状态的线程还没来得及执行CAS操作。

好处：

挂起/唤醒线程需要上万个CPU周期，如果搞公平锁，

要等老线程从内核态到运行态再到执行，同时新到的线程要挂起。

## 可重入锁

可重入是指：**同一个线程**已经持有锁的情况下，再次获取同一把锁时，不会被自己阻塞。

### synchronized 的可重入

```java
public class ReentrantSyncDemo {
    public synchronized void methodA() {
        System.out.println("methodA 获取锁");
        methodB();  // 调用 methodB，需要同一把锁
    }

    public synchronized void methodB() {
        System.out.println("methodB 再次获取同一把锁，不会被自己阻塞");
    }

    public static void main(String[] args) {
        new ReentrantSyncDemo().methodA();
    }
}
```

如果 synchronized 不可重入，`methodA` 持有锁后调用 `methodB`，`methodB` 尝试获取同一把锁时会被自己阻塞——**死锁**。

### ReentrantLock 的可重入

```java
ReentrantLock lock = new ReentrantLock();

lock.lock();    // 第1次获取，state 从 0 → 1
lock.lock();    // 第2次获取，state 从 1 → 2（重入）
lock.lock();    // 第3次获取，state 从 2 → 3（重入）

lock.unlock();  // 第1次释放，state 从 3 → 2
lock.unlock();  // 第2次释放，state 从 2 → 1
lock.unlock();  // 第3次释放，state 从 1 → 0，锁真正释放
```

AQS 通过 `state` 计数实现可重入：每次 lock 让 state+1，每次 unlock 让 state-1，state 归零才真正释放锁。

### 底层实现对比

**synchronized（ObjectMonitor）**：Monitor 内部维护 `_recursions` 计数器

```
线程进入 synchronized(lock):
  如果 Monitor._owner == 自己:
    _recursions++     ← 重入，计数器+1
  否则:
    抢锁，_owner = 自己，_recursions = 1

线程退出 synchronized(lock):
  _recursions--
  如果 _recursions == 0:
    释放锁，_owner = null
  否则:
    还没完全退出，继续持有
```

**ReentrantLock（AQS）**：用 `state` 字段充当计数器

```
lock.lock():
  如果当前线程 == exclusiveOwnerThread:
    state++           ← 重入，state+1
  否则:
    CAS 尝试把 state 从 0 改成 1

lock.unlock():
  state--
  如果 state == 0:
    exclusiveOwnerThread = null  ← 锁真正释放
    唤醒后继线程
```

### 可重入的实际意义

最常见的场景是**父类与子类的同步方法调用**：

```java
public class Base {
    public synchronized void doSomething() {
        System.out.println("Base doSomething");
    }
}

public class Child extends Base {
    @Override
    public synchronized void doSomething() {
        System.out.println("Child doSomething");
        super.doSomething();  // 再次获取同一把锁（this 对象的 Monitor）
    }
}

new Child().doSomething();
```

如果没有可重入，`super.doSomething()` 会尝试获取 `this` 的锁，但 `this` 的锁已经被自己持有→死锁。

### 不可重入会怎样？

如果把 ReentrantLock 换成一个不可重入的锁：

```java
// 假设有一个不可重入的锁
NonReentrantLock lock = new NonReentrantLock();

lock.lock();
try {
    lock.lock();    // 死锁！锁被自己持有，又去抢同一把锁，永远等不到
    try {
        // 永远到不了这里
    } finally {
        lock.unlock();
    }
} finally {
    lock.unlock();
}
```

所以可重入的本质是：**锁认的是线程，不是调用次数**。同一个线程可以反复进入，只要最终退出次数等于进入次数，锁就释放。

## Object.wait() / Object.notify()

通过锁对象（Monitor）管理线程的生存状态

`lock.wait()` ：

释放锁，把 Monitor 的`_owner` 擦除，让出通行证；

将线程扔进 Monitor 的 `_WaitSet`（等待池） 里面；

线程进入 `WAITING` 状态，彻底放弃 CPU。

`lock.notifyAll()` ：

把 `_WaitSet`（等待池）里的所有线程全部叫醒；

把它们全部移步到 `_EntryList`（锁池）中；

和外面新来的活线程一起，非公平抢锁大乱斗。

```java
package Lock;

public class AlterPrint {
    private int flag = 1;
    private final Object lock = new Object();
    public void printA() {
        synchronized (lock) {
            while (true) {
                while (flag != 1) {
                    try { lock.wait(); }    
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("a");
                flag = 2;
                lock.notifyAll();
            }
        }
    }
    public void printB() {
        synchronized (lock) {
            while (true) {
                while (flag != 2) {
                    try { lock.wait(); }    
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("b");
                flag = 3;
                lock.notifyAll();
            }
        }
    }
    public void printC() {
        synchronized (lock) {
            while (true) {
                while (flag != 3) {
                    try { lock.wait(); }    
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("c");
                flag = 1;
                lock.notifyAll();
            }
        }
    }
    public static void main(String[] args) {
        AlterPrint p = new AlterPrint();
        Thread a = new Thread(() -> {
            p.printA();
        });
        Thread b = new Thread(() -> {
            p.printB();
        });
        Thread c = new Thread(() -> {
            p.printC();
        });
        a.start();
        b.start();
        c.start();
    }
}
```

## Condition

synchronized 通过同一把锁对象来控制并发

所有阻塞的线程都被扔进同一个等待池中，

当调用 `notifyAll()` 唤醒时，池子里的线程无差别地被全部唤醒

`notify()` 唤醒在底层的实现是，唤醒 `ObjectMonitor` 内部的 `_waitSet` 这个双向循环链表的头节点，但是，线程进入 `_waitSet` 的顺序取决于抢锁失败、执行 `wait()` 的先后顺序，哪个线程先执行 `wait()` 由CPU调度决定，不可控，所以表现出来是随机的。

如果一把锁可以配多个不同的等待房间？

```java
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
```

每个Condition对象底层维护一个单向条件队列

当调用`lock.newCondition()`时，是在AQS内部创建了一个新的单向条件队列

线程执行`notFull.await()`，它会释放当前锁，线程节点会被丢进notFull这个单向条件队列的尾部，线程挂起

执行`notFull.signal()`，AQS把notFull单向队列的头节点移出来，放回AQS的双向同步队列的尾部

### Java 锁的两套底层体系

Java 并发锁有两套完全独立的底层实现：

```
                        Java 锁
                       /       \
                      /         \
              synchronized    j.u.c 显式锁
                  |              |
          ObjectMonitor        AQS
         (JVM C++ 实现)    (Java 层实现)
              |                  |
         EntryList          CLH 变体队列
         WaitSet           Condition 条件队列
```

**体系一：synchronized → ObjectMonitor**

`synchronized` 是 JVM 内置锁，底层由 C++ 实现的 `ObjectMonitor` 支撑。本文档前面讲的锁升级（偏向锁→轻量级锁→重量级锁）、`_EntryList`、`_WaitSet`、`_owner` 都属于这一套体系。`ObjectMonitor` 的等待队列是 C++ 层面的双向循环链表，与 AQS 无关。

**体系二：j.u.c 显式锁 → AQS → CLH 变体队列**

`java.util.concurrent` 包中的显式锁（ReentrantLock、ReentrantReadWriteLock 等）底层都基于 AQS（AbstractQueuedSynchronizer）。AQS 是纯 Java 实现，内部使用 CLH 变体队列管理等待线程。

哪些组件基于 AQS：

| 组件 | AQS 的使用方式 |
|------|-------------|
| ReentrantLock | 独占模式，state=0 未锁定，state=1 锁定（可重入时递增） |
| ReentrantReadWriteLock | 共享模式（读）+ 独占模式（写），state 高 16 位读锁计数，低 16 位写锁计数 |
| Semaphore | 共享模式，state 表示剩余许可数 |
| CountDownLatch | 共享模式，state 表示剩余计数 |
| CyclicBarrier | 内部用 ReentrantLock + Condition，间接使用 AQS |

**两套体系的核心区别**：

| | synchronized (ObjectMonitor) | j.u.c 锁 (AQS) |
|---|---|---|
| 实现语言 | C++（JVM 内部） | Java（JDK 类库） |
| 队列结构 | EntryList 双向循环链表 | CLH 变体双向链表 |
| 条件等待 | WaitSet + notify/notifyAll | Condition 队列 + signal/signalAll |
| 锁类型 | 仅非公平 | 可选公平/非公平 |
| 可中断性 | 不可中断获取 | lockInterruptibly() 可中断 |
| 超时获取 | 不支持 | tryLock(timeout) 支持 |

#### 区别一：条件等待——一个池子 vs 多个房间

synchronized 只有一个 WaitSet，notifyAll 唤醒所有线程，无法精准唤醒：

```java
// synchronized: 只有一个 WaitSet，无法区分等待原因
synchronized (lock) {
    while (!conditionA) {
        lock.wait();       // 等待条件A的线程
    }
    // ...
    lock.notifyAll();      // 唤醒所有人，等待条件B的线程也被白白叫醒
}

synchronized (lock) {
    while (!conditionB) {
        lock.wait();       // 等待条件B的线程，和等待条件A的线程混在同一个池子
    }
    // ...
    lock.notifyAll();      // 同样，所有人都被叫醒
}
```

AQS 的 Condition 可以创建多个独立的等待房间，精准唤醒：

```java
// AQS: 每个Condition是独立的等待房间
ReentrantLock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();  // 房间1: 等"不满"
Condition notEmpty = lock.newCondition();  // 房间2: 等"不空"

// 生产者：缓冲区满时，只去 notFull 房间睡觉
lock.lock();
try {
    while (count == items.length) {
        notFull.await();          // 只在 notFull 房间等
    }
    put(x);
    notEmpty.signal();            // 精准唤醒 notEmpty 房间的消费者
} finally {
    lock.unlock();
}

// 消费者：缓冲区空时，只去 notEmpty 房间睡觉
lock.lock();
try {
    while (count == 0) {
        notEmpty.await();         // 只在 notEmpty 房间等
    }
    take();
    notFull.signal();             // 精准唤醒 notFull 房间的生产者
} finally {
    lock.unlock();
}
```

这就是本文档前面 BoundedBuffer 例子的核心优势：synchronized 的 notifyAll 相当于广播，Condition 的 signal 相当于定向通知。

#### 区别二：可中断获取锁

synchronized 获取锁不可被中断，线程一旦阻塞就只能等：

```java
// synchronized: 死等，无法中断
Thread t = new Thread(() -> {
    synchronized (lock) {
        // 持有锁不释放
    }
});
t.start();

Thread t2 = new Thread(() -> {
    synchronized (lock) {
        // 如果 t 不释放锁，t2 永远卡在这里
        // 即使外部调用 t2.interrupt() 也无法让它从等待锁的状态中退出
    }
});
t2.start();
t2.interrupt();  // 无效！t2 仍然卡在等待锁
```

AQS 的 lockInterruptibly 可以响应中断：

```java
// AQS: 获取锁的过程中可以被中断
Thread t2 = new Thread(() -> {
    try {
        lock.lockInterruptibly();  // 等待锁时如果被中断，抛出 InterruptedException
        try {
            // 临界区
        } finally {
            lock.unlock();
        }
    } catch (InterruptedException e) {
        System.out.println("等锁等太久，被中断了，干别的事去");
        // 可以做降级处理，而不是死等
    }
});
t2.start();
t2.interrupt();  // 有效！t2 会从等待锁的状态中退出
```

#### 区别三：超时获取锁

synchronized 不支持超时，拿不到就死等。AQS 支持 tryLock 带超时：

```java
// synchronized: 拿不到锁就永远等
synchronized (lock) {
    // 没有办法设置"等3秒还拿不到就算了"
}

// AQS: tryLock 带超时
if (lock.tryLock(3, TimeUnit.SECONDS)) {
    try {
        // 3秒内拿到了锁
    } finally {
        lock.unlock();
    }
} else {
    // 3秒没拿到，走降级逻辑
    System.out.println("锁竞争太激烈，先干别的事");
}
```

#### 区别四：公平性选择

synchronized 只有非公平模式。AQS 可以选择公平或非公平：

```java
// synchronized: 只有非公平
// 线程释放锁后，任何线程都可以抢，刚被唤醒的线程大概率抢不过新到的线程

// AQS: 可选公平
ReentrantLock fairLock = new ReentrantLock(true);   // 公平锁，按等待顺序获取
ReentrantLock unfairLock = new ReentrantLock(false); // 非公平锁（默认），允许插队

// 公平锁场景：订单处理系统，先下单的先处理
fairLock.lock();
try {
    processOrder();  // 严格按线程到达顺序处理
} finally {
    fairLock.unlock();
}

// 非公平锁场景：高吞吐量系统，吞吐量优先
unfairLock.lock();
try {
    processRequest(); // 允许插队，减少线程上下文切换
} finally {
    unfairLock.unlock();
}
```

#### 什么时候用 synchronized，什么时候用 AQS 锁？

| 场景 | 选择 | 原因 |
|------|------|------|
| 简单互斥，无需高级功能 | synchronized | 代码简洁，JVM 自动优化（锁升级） |
| 需要精准唤醒特定线程 | AQS + Condition | synchronized 只有一个 WaitSet |
| 需要可中断获取锁 | AQS lockInterruptibly | synchronized 不可中断 |
| 需要超时获取锁 | AQS tryLock | synchronized 不支持超时 |
| 需要公平锁 | AQS 公平模式 | synchronized 只有非公平 |
| 需要读写分离 | ReentrantReadWriteLock | synchronized 不区分读写 |

### CLH 队列与 AQS 的关系

AQS 的双向同步队列不是凭空设计的，而是 **CLH 队列的变体（variant）**。

CLH 是三个人名缩写（Craig, Landin, Hagersten），是一种自旋锁队列算法。

原版 CLH 的核心机制：每个线程自旋检查**前驱节点**的 `locked` 字段

```
单向链表
Head ──► Node ──► Node ──► Node
            ↑
          当前线程盯着前驱的 locked 字段自旋
```

```java
// 原版 CLH 伪代码
Node myNode = new Node(true);       // locked = true，表示我需要锁
Node pred = tail.getAndSet(myNode); // 把自己挂到尾部，拿到前驱
while (pred.locked) { }             // 自旋！CPU 空转！等前驱释放
// 前驱 locked 变成 false → 我拿到锁了
```

释放锁：`myNode.locked = false;` 后继的自旋立刻退出。

AQS 借鉴了 CLH "每个节点只关注前驱" 的思想，但做了三个关键改造：

1. **自旋 → park/unpark**：原版 CLH 是 `while (pred.locked) {}` 自旋空转 CPU；AQS 改为 `LockSupport.park()` 挂起线程让出 CPU，前驱释放时主动 `unpark` 唤醒后继
2. **单向链表 → 双向链表**：原版 CLH 只有 next 指针；AQS 增加 prev 指针，因为 park 后线程可能被中断或超时唤醒，需要从队列中移除自己，双向链表才能 O(1) 断开节点
3. **locked 布尔值 → waitStatus 状态机**：原版 CLH 只有 true/false；AQS 定义了 CANCELLED(1)、SIGNAL(-1)、CONDITION(-2)、PROPAGATE(-3) 等多种状态

| <br /> | 原版 CLH           | AQS 队列                |
| ------ | ---------------- | --------------------- |
| 链表方向   | 单向               | 双向                    |
| 等待方式   | 自旋（CPU 空转）       | park 挂起（让出 CPU）       |
| 唤醒方式   | 前驱改 locked=false | 前驱主动 unpark 后继        |
| 节点状态   | true/false 二值    | 5 种 waitStatus        |
| 取消处理   | 不支持              | CANCELLED 状态 + 双向链表摘除 |

AQS 选择 CLH 变体而非另一种队列算法 MCS 的原因：CLH 更容易实现取消（线程只需检查前驱状态，前驱取消了就跳过它）、更容易实现条件变量（Condition 队列和同步队列之间转移节点，prev 指针天然支持）。

### 线程进出 AQS 队列涉及到内核态和用户态的切换吗？

单纯的**进出队列（将线程封装为 Node 并通过 CAS 链入或移出双向链表）**，这个指针交换的过程完全是在**用户态**通过 CAS 原子指令完成的，非常轻量。

但是，当线程进入队列后，如果拿不到锁，AQS 会调用 `LockSupport.park()` 将线程挂起。这个挂起以及后续被 `unpark()` 唤醒的过程，由于需要操作系统内核来调度线程状态并保存/恢复 CPU 寄存器上下文，因此**会涉及到用户态和内核态的重型切换**。

AQS 的高性能就在于，它在线程真正 `park()` 挂起前，会利用用户态的 CAS 进行有限度的重试抢锁，尽量避免线程真正进入内核态阻塞，从而压榨出了极高的并发吞吐量。

[^1]: 冲突指的是：A还没释放，B就来抢

[^2]: 在多线程竞争激烈的现代互联网高并发场景下，偏向锁往往一启动就失效并升级，但撤销需要等待全局安全点，这个过程非常沉重。
    JDK18之后已经被彻底移出了。

