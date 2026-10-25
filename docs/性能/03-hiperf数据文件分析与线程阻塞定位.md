# hiperf 数据文件分析与线程阻塞定位

> **本质**:hiperf 采的是「CPU 在跑什么」,不是「线程卡在哪」。`.data` 是二进制要 dump 成文本才能看;阻塞线程的用户栈是空的,突破口在**内核栈**。

## 一、`.data` 文件怎么看

- Profiler 录出的 `.data` 是**二进制格式**,记事本打开必然乱码,不是文件坏了。
- `hiperf.exe` 是**设备端工具**,Windows 上通常没有——把文件推回设备 dump 再拉回来:

```bash
hdc file send D:\trace\perf.data /data/local/tmp/        # 电脑 → 设备
hdc shell
hiperf dump -i /data/local/tmp/perf.data -o /data/local/tmp/perf.dump
exit
hdc file recv /data/local/tmp/perf.dump D:\trace\        # 设备 → 电脑
```

- **新版 hdc 命令变化**:`pull` → `file recv`(设备→电脑),`push` → `file send`(电脑→设备),参数顺序都是「远端在前还是本地在前看方向」:recv 是 `remote local`,send 是 `local remote`。
- dump 常用参数:`--head` 只看文件头元信息、`-d` 只看数据段、`-f` 额外函数信息。
- DevEco Studio `File → Open` 直接选 `.data` 也能可视化打开,不想用命令行就走这条。

## 二、dump 文件里的关键概念

### unwind callstack 与 callchain

- **unwind callstack = 动作**:栈回溯,从采样点的 PC 一层层往上还原「谁调了谁」。
- **callchain = 结果**:回溯出来的函数列表,`#0` 是叶子函数(最热的那层)。
- 用途:某函数在栈底出现次数最多 = 热点函数;所有采样的 callchain 按前缀合并 = 火焰图。

### 两种栈回溯方式

| 录制参数 | 原理 | 优缺点 |
| --- | --- | --- |
| `-s fp` | 顺帧指针(Frame Pointer)链往上找 | 快;编译没开 `-fno-omit-frame-pointer` 栈会断 |
| `-s dwarf` | 按 DWARF 调试信息表逐帧解析 | 慢但完整,依赖 debug 符号 |

### 每条采样记录长什么样

```
Sample: 1024 cycles
  event: hw-cpu-cycles
  pid: 1234
  tid: 5678              ← 线程 ID
  comm: HiAceRender      ← 线程名
  ip: 0x7f3a2b1c
  user stack: size 0 dyn_size 0
  kernel stack:
    #0  futex_wait
    #1  sys_futex
```

按线程搜:`findstr "tid: 5678" perf.dump`。

## 三、`user stack: size 0 dyn_size 0` 判读

含义:**这次采样没回溯出用户态调用链**。三种原因按概率排:

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 内核栈有内容、用户栈空 | 采样点落在内核态(系统调用、中断) | 正常现象,不用管 |
| 某进程大量 size 0 | fp 模式栈展开失败(没编帧指针) | 改 `-s dwarf` 重录 |
| swapper/kworker 等内核线程 | 本来就没有用户栈 | 正常 |

偶尔几条为 0、大部分正常 = 采样抖动,忽略。

## 四、线程阻塞为什么 hiperf 抓不到

- 线程阻塞 = **不在 CPU 上跑**;采样器只采「正在跑的」,所以阻塞线程要么采不到、要么用户栈全空——这是工具原理,不是配置问题。
- 对口工具分工:

| 要查什么 | 用什么 |
| --- | --- |
| CPU 热点、函数耗时 | hiperf(火焰图) |
| 线程阻塞、锁等待 | `hidumper --threadstack` 当场抓完整栈 |
| 阻塞时间线、何时被唤醒/切走 | hitrace 录调度事件 |

```bash
hdc shell hidumper --threads <pid>      # 找到阻塞线程的 tid
hdc shell hidumper --threadstack <tid>  # 当场抓该线程完整栈(用户态+内核态)

hitrace -b 4096 -t 10 sched freq idle am   # 录调度轨迹
hitrace --trace_dump > trace.txt           # 导出文本
```

## 五、从 dump 定位阻塞的实操路径

1. 按 tid 搜出该线程全部采样记录。
2. 阻塞态的典型长相:**user stack 空 + kernel stack 是 `futex_wait`** → 卡在锁等待。
3. **内核栈是突破口**:用户栈没了,内核栈还能看出卡在哪类系统调用——`futex_wait` = 锁,`poll`/`select` = IO 等待。
4. 补用户态函数两条路:翻同 tid 其他非阻塞采样的 callchain(看它平时怎么被调起);或复现阻塞时用 `hidumper --threadstack` 当场抓。
5. 想确认「这个线程阻塞了主线程」:hiperf dump 是离散采样快照、**没有时间轴**,判断不了线程间的先后影响关系——用 hitrace 看两个线程在同一时间窗的调度状态(谁在跑、谁在等、等了多久)。(对话原文到此截断,此条为补全方向)

## 一句话总结

**热点看 hiperf,阻塞抓 hidumper,因果看 hitrace**;dump 里 user stack 为空先看内核栈,`futex_wait` 就是锁等待。

## 互链

- 上屏流程与掉帧根因:[01-图片从代码到屏幕的全流程](01-图片从代码到屏幕的全流程.md)
- trace 可视化分析:[冷启动根因与指标搭配](../SmartPerfetto/冷启动根因与指标搭配.md)
