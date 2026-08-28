import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池运行 Demo —— 对应文章《线程池在实际工程场景的应用（Java 后端）》第二章
 * 「核心原理：一次任务提交后发生什么」+「拒绝策略 4 种」。
 *
 * 七个参数全部可自定义（与文章七个参数一一对应）：
 *   1. core          核心线程数
 *   2. max           最大线程数
 *   3. keepAlive     非核心线程空闲存活时间
 *   4. unit          时间单位（SECONDS / MILLISECONDS / ...）
 *   5. queueCapacity 任务队列容量（有界；传 0 则用 SynchronousQueue 不排队）
 *   6. prefix        线程名前缀（threadFactory）
 *   7. handler       拒绝策略：1=Abort 2=CallerRuns 3=Discard 4=DiscardOldest 5=自定义
 *
 * 用法（不带参用默认值，带参可全自定义，taskCount 为额外运行参数）：
 *   java ThreadPoolDemo
 *   java ThreadPoolDemo 2 4 2 SECONDS 3 worker 1 12
 *
 * 默认参数跑出来的效果（任务 sleep 500ms，每 120ms 提交一个）：
 *   task-1/2   → 命中① pool<core，新建 Worker 执行
 *   task-3~5   → 命中② queue 未满，入队等待
 *   队列满时    → 命中③ 扩线程到 max / 命中④ 走拒绝策略
 *   （具体拒绝几个是动态的：Worker 边执行边从队列取任务，队列会短暂腾空；
 *     想稳定看到拒绝，把 queueCapacity 调小、taskCount 调大，如：2 4 1 SECONDS 2 worker 1 20）
 *
 * 细节：CallerRunsPolicy 由提交方（main）内联执行任务，不走 Worker，
 *   因此不会打印 beforeExecute/afterExecute 打点，也不计入 completedTaskCount。
 */
public class ThreadPoolDemo {

    public static void main(String[] args) throws Exception {
        // ---------- 1. 解析七参数（支持命令行自定义，缺省走默认） ----------
        int core = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        int max = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        long keepAlive = args.length > 2 ? Long.parseLong(args[2]) : 2;
        TimeUnit unit = args.length > 3 ? TimeUnit.valueOf(args[3].toUpperCase()) : TimeUnit.SECONDS;
        int queueCapacity = args.length > 4 ? Integer.parseInt(args[4]) : 3;
        String prefix = args.length > 5 ? args[5] : "worker";
        int handlerType = args.length > 6 ? Integer.parseInt(args[6]) : 1;
        int taskCount = args.length > 7 ? Integer.parseInt(args[7]) : 12;

        // ---------- 2. threadFactory：业务化命名，方便 jstack 排查 ----------
        AtomicInteger seq = new AtomicInteger(1);
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, prefix + "-" + seq.getAndIncrement());
            t.setDaemon(false);
            return t;
        };

        // ---------- 3. handler：四种内置 + 一种自定义 ----------
        RejectedExecutionHandler handler;
        switch (handlerType) {
            case 2: handler = new ThreadPoolExecutor.CallerRunsPolicy(); break; // 谁提交谁执行（降级不丢）
            case 3: handler = new ThreadPoolExecutor.DiscardPolicy(); break;    // 静默丢弃
            case 4: handler = new ThreadPoolExecutor.DiscardOldestPolicy(); break; // 丢队头最老任务
            case 5: // 自定义：记录拒绝数 + 告警（工程里这里会接告警/补偿）
                handler = (r, executor) -> System.out.println(
                        "  [自定义拒绝] " + taskName(r) + " 被拒，pool=" + executor.getPoolSize()
                                + " queue=" + executor.getQueue().size() + " → 记录指标并告警/补偿");
                break;
            default: handler = new ThreadPoolExecutor.AbortPolicy(); break;     // 抛异常（默认）
        }

        // ---------- 4. 构造线程池（手动 new，不用 Executors；队列传 0 用 SynchronousQueue） ----------
        BlockingQueue<Runnable> queue = queueCapacity <= 0
                ? new SynchronousQueue<>()                 // 不排队，直接扩线程/拒绝
                : new LinkedBlockingQueue<>(queueCapacity); // 有界队列，防堆积

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                core, max, keepAlive, unit, queue, factory, handler) {
            // 打点：观察哪个线程在执行哪个任务（Worker 复用 + 扩线程一目了然）
            @Override protected void beforeExecute(Thread t, Runnable r) {
                System.out.println("      └─▶ " + t.getName() + " 开始执行 " + taskName(r));
            }
            @Override protected void afterExecute(Runnable r, Throwable t) {
                System.out.println("      └─✓ " + taskName(r) + " 执行完毕");
            }
        };

        System.out.println("==== 参数：core=" + core + " max=" + max
                + " keepAlive=" + keepAlive + " " + unit
                + " queue=" + (queueCapacity <= 0 ? "SynchronousQueue" : queueCapacity)
                + " prefix=" + prefix + " handler=" + handlerName(handlerType) + " ====");
        System.out.println("==== 提交 " + taskCount + " 个任务，每个耗时约 500ms，每 120ms 提交一个 ====\n");

        // ---------- 5. 逐个提交，边提交边打印状态 → 4 步流程可视化 ----------
        int rejected = 0;
        for (int i = 1; i <= taskCount; i++) {
            Runnable task = new SleepTask("task-" + i, 500);
            System.out.printf("[提交 %s] pool=%d active=%d queue=%d | %s%n",
                    taskName(task), pool.getPoolSize(), pool.getActiveCount(),
                    pool.getQueue().size(), flowBranch(pool, core, max, queueCapacity));
            try {
                pool.execute(task);
            } catch (RejectedExecutionException e) {
                rejected++;
                System.out.println("      ✗ " + taskName(task) + " 被拒绝（RejectedExecutionException）");
            }
            Thread.sleep(120); // 放慢节奏便于观察（CallerRuns 时主线程会内联执行，天然限流）
        }

        // ---------- 6. 优雅关闭：不再收新任务 → 等存量跑完 ----------
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("\n==== 完成：执行 " + pool.getCompletedTaskCount() + " 个，被拒绝 " + rejected
                + "，期间最大池大小 " + pool.getLargestPoolSize() + " ====");
        System.out.println("==== 收尾：非核心线程空闲 " + keepAlive + " " + unit
                + " 后会被回收，核心线程常驻 ====");
    }

    /** 提交时刻命中 4 步流程的哪一步（按 pool/queue 现状推断，教学用） */
    private static String flowBranch(ThreadPoolExecutor p, int core, int max, int queueCapacity) {
        if (queueCapacity <= 0)                       // SynchronousQueue：不排队
            return p.getPoolSize() < max ? "命中③ 队列不排队（SynchronousQueue）→ 扩线程执行"
                                         : "命中④ 队列不排队且 pool=max → 走拒绝策略";
        if (p.getPoolSize() < core) return "命中① pool<core → 新建 Worker 执行";
        if (p.getQueue().size() < queueCapacity) return "命中② queue 未满 → 入队等待";
        if (p.getPoolSize() < max) return "命中③ 队列满且 pool<max → 扩线程执行";
        return "命中④ pool=max 且 queue 满 → 走拒绝策略";
    }

    private static String taskName(Runnable r) {
        return r instanceof SleepTask ? ((SleepTask) r).name : r.toString();
    }

    private static String handlerName(int type) {
        switch (type) {
            case 2: return "CallerRunsPolicy";
            case 3: return "DiscardPolicy";
            case 4: return "DiscardOldestPolicy";
            case 5: return "自定义";
            default: return "AbortPolicy";
        }
    }

    /** 模拟耗时任务：只睡不干活，靠线程名观察 Worker 复用 */
    static class SleepTask implements Runnable {
        final String name;
        final long sleepMs;
        SleepTask(String name, long sleepMs) { this.name = name; this.sleepMs = sleepMs; }
        @Override public void run() {
            try { Thread.sleep(sleepMs); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
