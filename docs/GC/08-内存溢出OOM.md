# 内存溢出 OOM

> 内存泄漏的终点往往是 OOM：程序申请内存时空间不足。按区域分堆/栈/方法区/直接内存四类。

## 泄漏 vs 溢出
- 泄漏（leak）：对象该回收却没回收，内存被白白占用 → 是"因"。
- 溢出（OOM）：可用内存耗尽，申请失败 → 是"果"。

## 四类 OOM

### 1. 堆溢出 `Java heap space`
- 原因：对象创建过多且存活。
- 排查：调大 `-Xms`/`-Xmx`，或用 MAT/JProfiler 找大对象和泄漏点。

### 2. 栈溢出
- `StackOverflowError`：递归过深。
- `OutOfMemoryError`：线程数过多，无法新建线程栈。

### 3. 方法区溢出 `Metaspace`
- 原因：动态类加载过多（CGLIB、反射、动态代理）。
- 排查：`-XX:MaxMetaspaceSize` 限制 + 检查类加载器泄漏。

### 4. 直接内存溢出 `Direct buffer memory`
- 原因：NIO 直接缓冲区过多。
- 排查：`-XX:MaxDirectMemorySize`，检查 DirectByteBuffer 是否未释放。

## 排查思路
1. 先看异常类型 → 定位是哪块内存。
2. dump 堆（`-XX:+HeapDumpOnOutOfMemoryError`）→ 用 MAT 分析。
3. 结合监控（GC 日志、`jstat`、`jmap`）看对象增长趋势。
4. 有增长不回落 → 泄漏（看 [07-内存泄漏](07-内存泄漏.md)）；有回落但峰值太高 → 需要调参/优化。
