# LinkedHashMap 与 LRU

> 一句话本质：LinkedHashMap = HashMap 的 O(1) 读写 + 内部双向链表记录顺序。刷题最大价值是几行代码造出 LRU 缓存，不用手写链表。

## 为什么刷题用它

1. 继承 HashMap：`get` / `put` / `containsKey` 全部 O(1)，能用 HashMap 的地方都能直接换它
2. 内部多一条双向链表把所有 entry 串起来，遍历按确定顺序输出（HashMap 遍历顺序不可预期）
3. 构造参数 `accessOrder=true`：每次 `get` / `put` 都把该 entry 挪到链表尾，链表头就是最久未访问的——LRU 的核心逻辑一行到位
4. 覆写 `removeEldestEntry`：每次 `put` 后回调，返回 `true` 就自动删掉链表头，淘汰逻辑免手写

对比手写：常规 LRU 写法是 HashMap + 自定义双向链表，要自己维护 head/tail 和 prev/next 指针，几十行；LinkedHashMap 把这些全封装了。

## LRU 模板

对应 [Test.java](Test.java)，可直接提交 LC 146：

```java
class LRU {
    private final int cap;
    private final LinkedHashMap<Integer, Integer> map;
    LRU(int cap) {
        this.cap = cap;
        // 三个参数：初始容量、负载因子、accessOrder（true=按访问排序）
        map = new LinkedHashMap<>(cap, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > cap;   // put 之后超容量就淘汰链表头
            }
        };
    }
    int get(int key) {
        return map.getOrDefault(key, -1);   // get 命中会自动挪到链表尾
    }
    void put(int key, int val) {
        map.put(key, val);
    }
}
```

时间复杂度：get / put 均 O(1)。

## 关键细节

- `accessOrder=true` 时 `get` 也改变顺序；默认 `false` 只按插入顺序排
- `removeEldestEntry` 在每次 `put` 之后调用，参数 eldest 就是即将被淘汰的那个 entry
- 迭代只走链表不扫哈希桶，O(n) 且顺序确定

## 题型 → 结构选型

| 题型 | 用什么 | 代表题 |
|------|--------|--------|
| LRU 缓存 | `LinkedHashMap(accessOrder=true)` | LC 146 |
| 按插入顺序去重、找第一个不重复的字符 | `LinkedHashMap`（默认） | LC 387 |
| 只需要 key 映射、不关心顺序 | 普通 `HashMap`（开销更小） | 通用 |
