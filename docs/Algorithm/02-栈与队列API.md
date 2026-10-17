# 栈与队列 API 速查

> 一句话本质：写算法不要用 `Stack`（继承 Vector、带锁的遗留类），栈和队列统一用 `Deque`（实现选 `ArrayDeque`），堆用 `PriorityQueue`。

## Deque：栈 / 队列 / 双端队列三合一

```java
// 当栈用：只操作一头
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);       // 压栈 = addFirst
stack.pop();         // 弹栈 = removeFirst，空栈抛异常
stack.peek();        // 看栈顶，空返回 null

// 当队列用：一头进一头出
Queue<Integer> queue = new ArrayDeque<>();
queue.offer(1);      // 入队，失败返回 false（add 抛异常）
queue.poll();        // 出队，空返回 null（remove 抛异常）
queue.peek();        // 看队头，空返回 null

// 双端队列：两头都能操作
Deque<Integer> dq = new ArrayDeque<>();
dq.addFirst(1);  dq.addLast(2);
dq.pollFirst();  dq.pollLast();
```

方法对照表（抛异常 vs 返回特殊值）：

| 操作 | 抛异常版 | 返回特殊值版 |
|------|---------|-------------|
| 插入 | `add(e)` / `addLast(e)` | `offer(e)` / `offerLast(e)` |
| 移除 | `remove()` / `removeFirst()` | `poll()` / `pollFirst()` |
| 检查 | `element()` / `getFirst()` | `peek()` / `peekFirst()` |

> 写题习惯：刷题时用 `push/pop/peek`、`offer/poll/peek` 这套简写，语义清晰；空容器时返回 `null` 的版本（peek/poll）更不容易崩。

## 为什么不用 Stack

| | `Stack` | `ArrayDeque` |
|--|---------|--------------|
| 继承 | Vector，所有方法带 `synchronized` | 无锁 |
| 能力 | 只能当栈 | 栈 / 队列 / 双端通吃 |
| 官方态度 | Javadoc 明确建议改用 Deque | 推荐 |

## PriorityQueue（堆）

```java
// 小顶堆（默认）
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
// 大顶堆
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
// 自定义比较：按数组第二个元素升序
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]));

pq.offer(x);     // 入堆 O(log n)
pq.poll();       // 弹堆顶 O(log n)
pq.peek();       // 看堆顶 O(1)
```

## 题型 → 结构选型

| 题型 | 用什么 | 代表题 |
|------|--------|--------|
| 括号匹配、单调栈 | `ArrayDeque`（push/pop） | LC 20、LC 739 |
| BFS / 层序遍历 | `ArrayDeque`（offer/poll） | LC 102 |
| 滑动窗口最大值 | Deque 存下标（单调队列） | LC 239 |
| Top K、合并 K 个链表 | `PriorityQueue` | LC 215、LC 23 |
| 中位数动态维护 | 大顶堆 + 小顶堆对偶 | LC 295 |
