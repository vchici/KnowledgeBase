# 阶段

线程竞争越激烈,锁就越"重":

只有 1 个线程 → 偏向锁
多个线程但没冲突 → 轻量级锁
有冲突、自旋到一定次数 → 重量级锁

# 消耗

## 偏向锁阶段

锁从没被加过锁。第一个线程把自己的 ID 写进锁对象的 `Mark Word`。

之后同一个线程再来,只要比对自己的 ID 是不是它,是就直接进,几乎零开销。

## 轻量级锁阶段

有 CPU 自旋的消耗。

没抢到锁的线程不会立刻挂起,而是用 CAS 自旋 —— 相当于 `while(true)` 空转死等。持锁线程跑多久,它就要空转多久,白白烧 CPU。

## 重量级锁阶段

有用户态到内核态的切换开销。

线程抢不到锁就挂起:操作系统要保存这个线程的状态,把 CPU 让给别人。

锁释放时,操作系统再反向切换一次,把它唤醒。

# 锁对象

每一个 Java 对象,对象头(Mark Word)里都有一个插槽,用来记“当前谁持锁”。

执行到 `synchronized(lock)` 时,先看 `lock` 的对象头:

- 插槽为空 → 写入当前线程 ID,拿到锁。
- 插槽里是别人的 ID → 当前线程被抓起来,扔进等待队列(EntryList)。

等持有者释放锁、把名字擦掉,`lock` 再从队列里“唤醒”其他线程,大家重新抢门票。

## 非公平机制

锁释放后“唤醒”的竞争是非公平的。

被唤醒的老线程,还得从内核态切回用户态;而新到的线程本来就处于运行态,两者一起抢锁。

结果大概率是新到的线程一个 CAS 就把对象头改成自己的名字——因为老线程还在切换状态,没来得及执行 CAS。

为什么这样反而更好:挂起/唤醒一次线程要花上万个 CPU 周期。如果搞公平锁,就得等老线程从内核态切回用户态、真正跑起来,而新到的线程只能先挂起,白白浪费时间。

## 可重入锁

可重入是指: **同一个线程**已经持有锁的情况下,再次获取同一把锁时,不会被自己阻塞

### synchronized 的可重入

```java
public class ReentrantSyncDemo {
    public synchronized void methodA() {
        System.out.println("methodA 获取锁");
        methodB();  // 调用 methodB,需要同一把锁
    }

    public synchronized void methodB() {
        System.out.println("methodB 再次获取同一把锁,不会被自己阻塞");
    }

    public static void main(String[] args) {
        new ReentrantSyncDemo().methodA();
    }
}
```

如果 synchronized 不可重入,`methodA` 持有锁后调用 `methodB`, `methodB` 尝试获取同一把锁时会被自己阻塞 —— **死锁**

### ReentrantLock 的可重入

```java
ReentrantLock lock = new ReentrantLock();

lock.lock();    // 第1次获取,state 从 0 → 1
lock.lock();    // 第2次获取,state 从 1 → 2(重入)
lock.lock();    // 第3次获取,state 从 2 → 3(重入)

lock.unlock();  // 第1次释放,state 从 3 → 2
lock.unlock();  // 第2次释放,state 从 2 → 1
lock.unlock();  // 第3次释放,state 从 1 → 0,锁真正释放
```

[AQS](#java-锁的两套底层体系) 通过 `state` 计数实现可重入: 每次 lock 让 state+1, 每次 unlock 让 state-1, state 归零才真正释放锁

### synchronized 和 ReentrantLock 底层实现对比

**synchronized(ObjectMonitor)**:Monitor 里用 `_recursions` 计数、`_owner` 记持有者。

```
进入 synchronized(lock):
  如果 _owner == 当前线程:   // 锁已经是自己的
    _recursions++           // 重入,计数 +1
  否则:                     // 锁还没人拿
    抢到锁,_owner = 自己,_recursions = 1

退出 synchronized(lock):
  _recursions--
  如果 _recursions == 0:    // 重入层数全部退完
    _owner = null           // 才真正释放锁
  否则:                     // 还套着外层
    继续持有
```

**ReentrantLock(AQS)**:用 `state` 计数、`exclusiveOwnerThread` 记持有者。

```
lock.lock():
  如果 exclusiveOwnerThread == 当前线程:  // 锁已经是自己的
    state++                               // 重入,直接 +1,不需要 CAS
  否则:                                   // 锁还没人拿,可能有别的线程同时在抢
    CAS 把 state 从 0 改成 1,持有者记为自己 // 必须用 CAS 保证只有一个线程能抢成功

lock.unlock():
  state--
  如果 state == 0:                        // 重入层数全部退完
    exclusiveOwnerThread = null           // 才真正释放锁
    唤醒队列里的后继线程
```

> 为什么重入时直接 `state++`,第一次抢锁却要用 CAS?
>
> - **重入**:持有者就是当前线程自己,别的线程不可能同时改这个 state,普通自增就够了。
> - **第一次抢**:此刻可能有多个线程同时发现 state == 0,都想把它改成 1。普通的"读—判断—写"在并发下会让多个线程都以为自己抢到了;CAS 把"判断 state 是不是 0 + 改成 1"合成一个不可分割的原子操作,只有一个线程能成功,其他线程抢锁失败,进入队列等待。

### 什么时候需要可重入锁

#### 方法之间的嵌套调用

```java
public void TraverseAndProcess(Node node) {
    lock.lock(); // 递归的每一步都会触发加锁
    try {
        // 处理当前节点
        process(node);
        
        // 递归调用
        for (Node child : node.getChildren()) {
            TraverseAndProcess(child); 
        }
    } finally {
        lock.unlock();
    }
}
```

**父类与子类的同步方法调用**:

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
        super.doSomething();  // 再次获取同一把锁(this 对象的 Monitor)
    }
}

new Child().doSomething();
```

如果没有可重入,`super.doSomething()` 会尝试获取 `this` 的锁, 但 `this` 的锁已经被自己持有 → 死锁

保证整个转账过程的**原子性** 

如果把 ReentrantLock 换成 `StampedLock`(JDK 里真实存在的不可重入锁):

```java
StampedLock lock = new StampedLock();

long stamp = lock.writeLock();   // 第一次拿到写锁
try {
    lock.writeLock();            // 死锁！自己持有写锁,又去抢同一把写锁,永远等不到
    // 永远到不了这里
} finally {
    lock.unlockWrite(stamp);     // 只有这一层能释放
}
```

这就是可重入锁的本质:锁只认**是谁在持有**(线程),不认**加了几次**(次数)。所以同一个线程可以反复进入、反复加锁,不会自己把自己卡死;唯一的要求是——进几次就得退几次,进出次数相等,锁才算真正释放。

**不可重入锁正好相反**:锁里没有计数,也不看是谁持有,只记"被占没被占"。一旦被占,谁来都拦——哪怕来抢的就是持锁线程自己。所以同一个线程第二次加锁,就是自己等自己 → 死锁,这正是上面 StampedLock 卡死的原因。

## Object.wait() / Object.notify()

synchronized 只能管"排队":一次只放一个线程进临界区。

但有些场景光排队没用,还得"等条件"。比如生产者-消费者:缓冲区满了,生产者就算抢到锁也没活干;它要是抱着锁干等,消费者拿不到锁进不来,数据永远取不走,整个流程就卡死了。

wait/notify 就是用在这种时候:

- 条件不满足 → 主动释放锁去睡觉,把锁让给别人
- 条件被别人满足了 → 叫醒睡觉的线程,重新排队抢锁

一句话:**synchronized 管互斥,wait/notify 管协作**(生产者-消费者、交替打印都是典型场景)。

通过锁对象(Monitor)管理线程的生存状态

### wait()

释放锁, 把 Monitor 的 `_owner` 擦除,让出通行证

将线程扔进 Monitor 的 `_WaitSet` (等待池) 里面

线程进入 `WAITING` 状态,彻底放弃 CPU

### notifyAll()

把 `_WaitSet` (等待池) 里的所有线程全部叫醒

把它们全部移步到 `_EntryList` (锁池) 中

和外面新来的活线程一起, 非公平抢锁大乱斗

```java
public class AlterPrint {
    private int flag = 1;                      // 轮到谁: 1→a, 2→b, 3→c
    private final Object lock = new Object();  // 三个线程共用一把锁

    // 三个线程跑同一个方法,只是参数不同
    public void print(String word, int myTurn, int nextTurn) {
        synchronized (lock) {
            while (true) {
                while (flag != myTurn) {       // 还没轮到自己
                    try { lock.wait(); }       // 放锁睡觉,把机会让给别人
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(word);      // 轮到自己,打印
                flag = nextTurn;               // 把回合传给下一个人
                lock.notifyAll();              // 叫醒所有睡觉的线程
            }
        }
    }

    public static void main(String[] args) {
        AlterPrint p = new AlterPrint();
        new Thread(() -> p.print("a", 1, 2)).start();
        new Thread(() -> p.print("b", 2, 3)).start();
        new Thread(() -> p.print("c", 3, 1)).start();
    }
}
```

整个例子就三样东西:

- **一把锁 `lock`**: 保证同一时刻只有一个线程在动
- **一个 `flag`**: 记录现在轮到谁
- **wait/notifyAll**: 没轮到的睡觉,轮到的干活,干完传棒

一轮的流程: 抢到锁 → flag 不是自己 → `wait()` 放锁睡觉 → 轮到自己的线程打印、把 flag 传给下一个、`notifyAll()` → 所有人被叫醒,只有 flag 对的那个能通过 while 检查,其余看一眼条件不对,回去接着睡。

运行效果: a b c a b c ... 交替打印。

两个容易忽略的细节(正是这个例子想教的):

- **用 `notifyAll` 不用 `notify`**: `notify` 只随机叫醒一个,万一把 flag 不对的线程叫醒,真正该干活的还在睡,流程就永远卡住;`notifyAll` 全叫醒,不对的人自己会回去睡,不会出错
- **用 `while` 不用 `if`**: 线程醒来时 flag 可能已经被别人改了(还可能被虚假唤醒),必须重新检查条件,不满足就接着睡

## Condition

`synchronized` 通过同一把锁对象来控制并发, 所有阻塞的线程都被扔进同一个等待池中,当调用 `notifyAll()` 唤醒时, 池子里的线程无差别地被全部唤醒.

`notify()` 唤醒在底层的实现是, 唤醒 `ObjectMonitor` 内部的 `_waitSet` 这个双向循环链表的头节点, 但是, 线程进入 `_waitSet` 的顺序取决于抢锁失败、执行 `wait()` 的先后顺序, 哪个线程先执行 `wait()` 由CPU调度决定, 不可控, 所以表现出来是随机的.

**如果一把锁可以配多个不同的等待房间？**

```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BoundedBuffer {
    private final Lock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();  // 房间1: 满 → 生产者在这睡
    private final Condition notEmpty = lock.newCondition();  // 房间2: 空 → 消费者在这睡

    private final Object[] items = new Object[3];  // 容量 3,方便看"满"的效果
    private int putptr, takeptr, count;            // 写指针、读指针、当前库存

    public void put(Object x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {        // 满了,没地方放
                System.out.println(Thread.currentThread().getName() + " 满了,去 notFull 房间睡觉");
                notFull.await();                   // 放锁,进 notFull 房间睡
            }
            items[putptr] = x;                     // 放入数据,写指针环形前进
            if (++putptr == items.length) putptr = 0;
            ++count;
            System.out.println(Thread.currentThread().getName() + " 放入 [" + x + "],库存 " + count);
            notEmpty.signal();                     // 放好了,叫醒 notEmpty 房间的消费者
        } finally {
            lock.unlock();                         // 不管哪条路出来,都放锁
        }
    }

    public Object take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {                   // 空了,没东西拿
                System.out.println(Thread.currentThread().getName() + " 空了,去 notEmpty 房间睡觉");
                notEmpty.await();                  // 放锁,进 notEmpty 房间睡
            }
            Object x = items[takeptr];             // 取出数据,读指针环形前进
            if (++takeptr == items.length) takeptr = 0;
            --count;
            System.out.println(Thread.currentThread().getName() + " 取出 [" + x + "],库存 " + count);
            notFull.signal();                      // 拿走了,叫醒 notFull 房间的生产者
            return x;
        } finally {
            lock.unlock();
        }
    }
}

public class BoundedBufferTest {
    public static void main(String[] args) {
        BoundedBuffer buffer = new BoundedBuffer();

        // 3 个生产者,生产快(200ms 一个);2 个消费者,消费慢(800ms 一个)
        // 供大于求 → 缓冲区被压满 → 生产者成批进 notFull 房间睡觉
        for (int i = 1; i <= 3; i++) {
            new Thread(() -> {
                try {
                    for (int n = 1; n <= 100; n++) {
                        buffer.put("商品-" + n);
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) { e.printStackTrace(); }
            }, "生产者-" + i).start();
        }

        for (int i = 1; i <= 2; i++) {
            new Thread(() -> {
                try {
                    for (int n = 1; n <= 150; n++) {
                        buffer.take();
                        Thread.sleep(800);
                    }
                } catch (InterruptedException e) { e.printStackTrace(); }
            }, "消费者-" + i).start();
        }
    }
}
```

跟 AlterPrint 的"一个池子"不同,这里一把锁配了**两个等待房间**:

- **notFull 房间**: 生产者发现"满了"进去睡
- **notEmpty 房间**: 消费者发现"空了"进去睡

以生产者为例走一遍流程: 抢到锁 → 发现满了 → `notFull.await()` 放锁进房睡 → 消费者取走一个,调 `notFull.signal()` → 睡着的生产者被搬回抢锁队列尾部 → 重新抢锁、重新检查,不满了就放货。

运行后很快就能看到: 消费慢(800ms)、生产快(200ms),生产者成批"去 notFull 房间睡觉",消费者每取走一个数据就把他们叫醒。

**两个房间才是核心优势**: `notEmpty.signal()` 只叫醒 notEmpty 房间里的人,notFull 房间的生产者不会被无辜吵醒再白跑一趟 —— 这就是"一个池子 vs 多个房间"的差别。

两个和 wait/notify 一模一样的老规矩:

- 判断条件用 `while` 不用 `if`: 醒来后条件可能又被别人改了,必须重新检查
- `await()/signal()` 必须在持有锁时调用: `await` 内部会先帮你放锁再睡,醒来后还要重新抢锁才能返回

每个Condition对象底层维护一个单向条件队列

当调用`lock.newCondition()`时, 是在AQS内部创建了一个新的单向条件队列

线程执行`notFull.await()`, 它会释放当前锁, 线程节点会被丢进notFull这个单向条件队列的尾部, 线程挂起

执行`notFull.signal()`, AQS把notFull单向队列的头节点移出来, 放回AQS的双向同步队列的尾部

### Java 锁的两套底层体系

Java 并发锁有两套完全独立的底层实现:

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

`synchronized` 是 JVM 内置锁，底层由 C++ 实现的 `ObjectMonitor` 支撑。

前面讲的锁升级(偏向锁 → 轻量级锁 → 重量级锁)、`_EntryList`(锁池)、`_WaitSet`(等待池)、`_owner`(持锁线程)，全都在这一套体系里。

它的等待队列是 C++ 层面的双向循环链表，跟 AQS 没有任何关系。

**体系二：j.u.c 显式锁 → AQS → CLH 变体队列**

`java.util.concurrent` 包里的显式锁(ReentrantLock、ReentrantReadWriteLock 等)，底层全都基于 AQS(AbstractQueuedSynchronizer)。

AQS 是纯 Java 实现的，内部用 CLH 变体队列管理等待线程。

不同组件只是"用 AQS 的方式"不一样：

| 组件 | 用 AQS 的方式 |
|------|-------------|
| ReentrantLock | 独占模式，state=0 未锁定，state=1 锁定(可重入时递增) |
| ReentrantReadWriteLock | 共享模式(读) + 独占模式(写)，state 高 16 位读锁计数、低 16 位写锁计数 |
| Semaphore | 共享模式，state 表示剩余许可数 |
| CountDownLatch | 共享模式，state 表示剩余计数 |
| CyclicBarrier | 内部用 ReentrantLock + Condition，间接使用 AQS |

**两套体系的核心区别**：

| 维度 | synchronized (ObjectMonitor) | j.u.c 锁 (AQS) |
|---|---|---|
| 实现语言 | C++(JVM 内部) | Java(JDK 类库) |
| 队列结构 | EntryList 双向循环链表 | CLH 变体双向链表 |
| 条件等待 | WaitSet + notify/notifyAll | Condition 队列 + signal/signalAll |
| 锁类型 | 仅非公平 | 可选公平/非公平 |
| 可中断性 | 不可中断获取 | lockInterruptibly() 可中断 |
| 超时获取 | 不支持 | tryLock(timeout) 支持 |

#### 区别一:条件等待——一个池子 vs 多个房间

synchronized 一把锁只有一个 WaitSet，等不同条件的线程全挤在一起，`notifyAll()` 只能"广播"，把所有人都叫醒：

```java
// synchronized：等"A"和等"B"的人混在同一个池子，分不开
synchronized (lock) {
    while (!conditionA) {
        lock.wait();     // 等 A 的人
    }
    // ...
    lock.notifyAll();    // 广播：等 B 的人也一起被无辜叫醒
}

synchronized (lock) {
    while (!conditionB) {
        lock.wait();     // 等 B 的人，和等 A 的挤在同一个池子
    }
    // ...
    lock.notifyAll();    // 广播：等 A 的人也被无辜叫醒
}
```

AQS 的 Condition 一把锁能开多个"房间"，`signal()` 是"定向通知"，只叫醒该房间的人：

```java
// AQS：每个 Condition 是一个独立房间
ReentrantLock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();  // 房间1：等"不满"的生产者
Condition notEmpty = lock.newCondition();  // 房间2：等"不空"的消费者

// 生产者：满了 → 只进 notFull 睡；放入后 → 只叫醒 notEmpty
lock.lock();
try {
    while (count == items.length) notFull.await();
    put(x);
    notEmpty.signal();
} finally { lock.unlock(); }

// 消费者：空了 → 只进 notEmpty 睡；取出后 → 只叫醒 notFull
lock.lock();
try {
    while (count == 0) notEmpty.await();
    take();
    notFull.signal();
} finally { lock.unlock(); }
```

一句话总结：synchronized 的 `notifyAll` 是广播，Condition 的 `signal` 是定向通知。

#### 区别二:可中断获取锁

synchronized 获取锁不可被中断,线程一旦阻塞就只能等:

```java
// synchronized: 死等,无法中断
Thread t = new Thread(() -> {
    synchronized (lock) {
        // 持有锁不释放
    }
});
t.start();

Thread t2 = new Thread(() -> {
    synchronized (lock) {
        // 如果 t 不释放锁,t2 永远卡在这里
        // 即使外部调用 t2.interrupt() 也无法让它从等待锁的状态中退出
    }
});
t2.start();
t2.interrupt();  // 无效！t2 仍然卡在等待锁
```

AQS 的 lockInterruptibly 可以响应中断:

```java
// AQS: 获取锁的过程中可以被中断
Thread t2 = new Thread(() -> {
    try {
        lock.lockInterruptibly();  // 等待锁时如果被中断,抛出 InterruptedException
        try {
            // 临界区
        } finally {
            lock.unlock();
        }
    } catch (InterruptedException e) {
        System.out.println("等锁等太久,被中断了,干别的事去");
        // 可以做降级处理,而不是死等
    }
});
t2.start();
t2.interrupt();  // 有效！t2 会从等待锁的状态中退出
```

#### 区别三:超时获取锁

synchronized 不支持超时，拿不到锁就一直死等；AQS 则可以用 `tryLock` 设置超时：

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
    // 3秒没拿到,走降级逻辑
    System.out.println("锁竞争太激烈,先干别的事");
}
```

#### 区别四:公平性选择

synchronized 只有非公平模式, AQS 可以选择公平或非公平:

```java
// synchronized: 只有非公平
// 线程释放锁后,任何线程都可以抢,刚被唤醒的线程大概率抢不过新到的线程

// AQS: 可选公平
ReentrantLock fairLock = new ReentrantLock(true);   // 公平锁,按等待顺序获取
ReentrantLock unfairLock = new ReentrantLock(false); // 非公平锁(默认),允许插队

// 公平锁场景:订单处理系统,先下单的先处理
fairLock.lock();
try {
    processOrder();  // 严格按线程到达顺序处理
} finally {
    fairLock.unlock();
}

// 非公平锁场景:高吞吐量系统,吞吐量优先
unfairLock.lock();
try {
    processRequest(); // 允许插队,减少线程上下文切换
} finally {
    unfairLock.unlock();
}
```

#### 什么时候用 synchronized,什么时候用 AQS 锁？

| 场景 | 选择 | 原因 |
|------|------|------|
| 简单互斥,无需高级功能 | synchronized | 代码简洁,JVM 自动优化(锁升级) |
| 需要精准唤醒特定线程 | AQS + Condition | synchronized 只有一个 WaitSet |
| 需要可中断获取锁 | AQS lockInterruptibly | synchronized 不可中断 |
| 需要超时获取锁 | AQS tryLock | synchronized 不支持超时 |
| 需要公平锁 | AQS 公平模式 | synchronized 只有非公平 |
| 需要读写分离 | ReentrantReadWriteLock | synchronized 不区分读写 |

### CLH 队列与 AQS 的关系

AQS 的双向同步队列不是凭空设计的,而是 **CLH 队列的变体(variant)**

CLH 是三个人名缩写(Craig, Landin, Hagersten),是一种自旋锁队列算法

原版 CLH 的核心机制:每个线程自旋检查**前驱节点**的 `locked` 字段

```
单向链表
Head ──► Node ──► Node ──► Node
            ↑
          当前线程盯着前驱的 locked 字段自旋
```

```java
// 原版 CLH 伪代码
Node myNode = new Node(true);       // locked = true,表示我需要锁
Node pred = tail.getAndSet(myNode); // 把自己挂到尾部,拿到前驱
while (pred.locked) { }             // 自旋！CPU 空转！等前驱释放
// 前驱 locked 变成 false → 我拿到锁了
```

释放锁:`myNode.locked = false;` 后继的自旋立刻退出

AQS 借鉴了 CLH "每个节点只关注前驱" 的思想,但做了三个关键改造:

1. **自旋 → park/unpark**:原版 CLH 是 `while (pred.locked) {}` 自旋空转 CPU；AQS 改为 `LockSupport.park()` 挂起线程让出 CPU,前驱释放时主动 `unpark` 唤醒后继
2. **单向链表 → 双向链表**:原版 CLH 只有 next 指针；AQS 增加 prev 指针,因为 park 后线程可能被中断或超时唤醒,需要从队列中移除自己,双向链表才能 O(1) 断开节点
3. **locked 布尔值 → waitStatus 状态机**:原版 CLH 只有 true/false；AQS 定义了 CANCELLED(1)、SIGNAL(-1)、CONDITION(-2)、PROPAGATE(-3) 等多种状态

| <br /> | 原版 CLH           | AQS 队列                |
| ------ | ---------------- | --------------------- |
| 链表方向   | 单向               | 双向                    |
| 等待方式   | 自旋(CPU 空转)       | park 挂起(让出 CPU)       |
| 唤醒方式   | 前驱改 locked=false | 前驱主动 unpark 后继        |
| 节点状态   | true/false 二值    | 5 种 waitStatus        |
| 取消处理   | 不支持              | CANCELLED 状态 + 双向链表摘除 |

AQS 选择 CLH 变体而非另一种队列算法 MCS 的原因:CLH 更容易实现取消(线程只需检查前驱状态,前驱取消了就跳过它)、更容易实现条件变量(Condition 队列和同步队列之间转移节点,prev 指针天然支持)

### 线程进出 AQS 队列涉及到内核态和用户态的切换吗？

单纯的**进出队列(将线程封装为 Node 并通过 CAS 链入或移出双向链表)**,这个指针交换的过程完全是在**用户态**通过 CAS 原子指令完成的,非常轻量

但是,当线程进入队列后,如果拿不到锁,AQS 会调用 `LockSupport.park()` 将线程挂起这个挂起以及后续被 `unpark()` 唤醒的过程,由于需要操作系统内核来调度线程状态并保存/恢复 CPU 寄存器上下文,因此**会涉及到用户态和内核态的重型切换**

AQS 的高性能就在于,它在线程真正 `park()` 挂起前,会利用用户态的 CAS 进行有限度的重试抢锁,尽量避免线程真正进入内核态阻塞,从而压榨出了极高的并发吞吐量

> 注1：冲突指的是 A 还没释放，B 就来抢
>
> 注2：在多线程竞争激烈的现代互联网高并发场景下，偏向锁往往一启动就失效并升级，但撤销需要等待全局安全点，这个过程非常沉重。JDK18 之后已经被彻底移出了

