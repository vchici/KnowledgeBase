import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

/**
 * Java 手撕题库函数速查（可运行版）
 *
 * 配套笔记：01-数组与集合互转API.md、02-栈与队列API.md
 * 编译运行：javac -encoding UTF-8 JdkTricks.java && java JdkTricks
 *
 * 覆盖清单（按对解题速度的决定性程度分三档）：
 *   第一档（用错 = 多写 30~50 行还容易出 Bug）
 *     1. LinkedHashMap(accessOrder=true) 重写 removeEldestEntry → LRU 缓存 (146)
 *     2. PriorityQueue 小顶堆/大顶堆/双堆                       → 合并K链表(23) / 中位数(295)
 *     3. ArrayDeque 当栈/队列（别用 Stack / LinkedList）        → 20/102/200
 *     4. HashMap.computeIfAbsent / getOrDefault                 → 异位词分组(49)
 *   第二档（减少样板代码）
 *     5. Arrays.sort + Comparator（Integer.compare 防溢出）
 *     6. Character.isLetterOrDigit / toLowerCase                → 验证回文串(125)
 *     7. StringBuilder.reverse
 *     8. TreeMap.floorKey / ceilingKey                          → 有序容器找前驱后继
 *   第三档（冷门但用出来就是加分项）
 *     9. Integer.bitCount 与手写 n&(n-1)                        → 位1的个数(191)
 *    10. BitSet                                                 → N皇后位运算标记
 *    11. Collections.shuffle / swap（只认 List，不认 int[]）
 */
public class JdkTricks {

    public static void main(String[] args) {
        demoLRU();              // 第一档 1
        demoPriorityQueue();    // 第一档 2
        demoArrayDeque();       // 第一档 3
        demoComputeIfAbsent();  // 第一档 4
        demoSort();             // 第二档 5
        demoCharacter();        // 第二档 6
        demoStringBuilder();    // 第二档 7
        demoTreeMap();          // 第二档 8
        demoBitCount();         // 第三档 9
        demoBitSet();           // 第三档 10
        demoCollections();      // 第三档 11
    }

    // ==================== 第一档 1：LinkedHashMap 一把梭 LRU（146） ====================

    /**
     * 手写双向链表 + HashMap 至少 40 行（Node(key,value,prev,next) + 虚拟头尾 +
     * remove/addLast 指针翻转），LinkedHashMap 十几行搞定。
     *
     * 精妙点：accessOrder=true 让 get()/put() 都把该 key 挪到链表尾部（最近使用），
     * 头部即最久未使用；只需重写 removeEldestEntry，put 后自动回调，返回 true 就淘汰头部。
     * HashMap 做不到（无序），TreeMap 做不到（按 key 排序而非访问序）。
     * 面试追问：大概率会让你再手写一遍双向链表版——两手都要会。
     */
    static class LRUCache {
        private final int cap;
        private final LinkedHashMap<Integer, Integer> map;

        LRUCache(int capacity) {
            this.cap = capacity;
            // accessOrder 决定「谁算最老」
            this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                    return size() > cap;   // 超容量自动淘汰最久未访问的头节点
                }
            };
        }

        int get(int key) { return map.getOrDefault(key, -1); }
        void put(int key, int value) { map.put(key, value); }
    }

    static void demoLRU() {
        System.out.println("== 1. LinkedHashMap(accessOrder=true) 实现 LRU ==");
        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.get(1);        // 访问 1 → 1 变为最新
        cache.put(3, 3);     // 容量超限，淘汰最久未访问的 2
        System.out.println("get(1) = " + cache.get(1) + "（期望 1）");
        System.out.println("get(2) = " + cache.get(2) + "（期望 -1，已被淘汰）");
        System.out.println();
    }

    // ==================== 第一档 2：PriorityQueue（堆） ====================

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
    }

    /** 合并 K 个升序链表（23）：小顶堆维护 K 个当前头节点，poll 最小、offer 其 next。O(N log K)。 */
    static ListNode mergeKLists(ListNode[] lists) {
        PriorityQueue<ListNode> pq = new PriorityQueue<>(
                (a, b) -> Integer.compare(a.val, b.val));   // 小顶堆；用 compare 防减法溢出
        for (ListNode h : lists) if (h != null) pq.offer(h);

        ListNode dummy = new ListNode(0), tail = dummy;
        while (!pq.isEmpty()) {
            ListNode n = pq.poll();
            tail.next = n;
            tail = n;
            if (n.next != null) pq.offer(n.next);
        }
        return dummy.next;
    }

    /**
     * 数据流中位数（295）：双堆。maxHeap 存小的一半，minHeap 存大的一半，
     * 保证 maxHeap 顶 <= minHeap 顶、maxHeap 数量持平或多 1。
     */
    static class MedianFinder {
        private final PriorityQueue<Integer> minHeap = new PriorityQueue<>();                           // 大的一半
        private final PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder()); // 小的一半

        void addNum(int num) {
            maxHeap.offer(num);
            minHeap.offer(maxHeap.poll());           // 先保证两堆顶有序
            if (maxHeap.size() < minHeap.size()) {   // 再平衡：让 maxHeap 持平或多 1
                maxHeap.offer(minHeap.poll());
            }
        }

        double findMedian() {
            return maxHeap.size() > minHeap.size()
                    ? maxHeap.peek()
                    : (maxHeap.peek() + minHeap.peek()) / 2.0;
        }
    }

    static void demoPriorityQueue() {
        System.out.println("== 2. PriorityQueue：合并K链表 + 双堆中位数 ==");
        ListNode l1 = buildList(1, 4, 5), l2 = buildList(1, 3, 4), l3 = buildList(2, 6);
        System.out.println("合并K链表: " + printList(mergeKLists(new ListNode[]{l1, l2, l3}))
                + "（期望 1->1->2->3->4->4->5->6）");

        MedianFinder mf = new MedianFinder();
        for (int x : new int[]{1, 2, 3, 4, 5}) mf.addNum(x);
        System.out.println("数据流 1~5 的中位数 = " + mf.findMedian() + "（期望 3.0）");
        // 坑1：默认小顶堆，大顶堆必须 reverseOrder 或 (a,b)->b-a，写反中位数直接错
        // 坑2：remove(Object) 是 O(n)（先线性查找）；要 O(log n) 删任意元素换 TreeSet/TreeMap
        // 坑3：传 comparator 必须同时传初始容量 new PriorityQueue<>(11, cmp)，否则匹配不到重载
        System.out.println();
    }

    static ListNode buildList(int... vals) {
        ListNode dummy = new ListNode(0), t = dummy;
        for (int v : vals) { t.next = new ListNode(v); t = t.next; }
        return dummy.next;
    }

    static String printList(ListNode head) {
        StringBuilder sb = new StringBuilder();
        for (ListNode cur = head; cur != null; cur = cur.next) {
            sb.append(cur.val);
            if (cur.next != null) sb.append("->");
        }
        return sb.toString();
    }

    // ==================== 第一档 3：ArrayDeque —— 栈/队列统一用它（官方推荐） ====================

    static void demoArrayDeque() {
        System.out.println("== 3. ArrayDeque：一个类既当栈又当队列 ==");
        // ❌ Stack：继承 Vector，方法全 synchronized；❌ LinkedList：链表内存不连续，缓存不友好
        // ✅ ArrayDeque：循环数组，无锁、连续内存
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1); stack.push(2); stack.push(3);    // push 插到队头
        System.out.println("栈 pop = " + stack.pop() + "（LIFO，期望 3）");

        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(1); queue.offer(2); queue.offer(3); // offer 插到队尾
        System.out.println("队列 poll = " + queue.poll() + "（FIFO，期望 1）");
        // 坑1：push 是插队头（栈），offer 是插队尾（队列），别混用
        // 坑2：ArrayDeque 不允许 null——层序遍历别拿 null 当层分隔符，会 NPE
        System.out.println();
    }

    // ==================== 第一档 4：computeIfAbsent / getOrDefault ====================

    static void demoComputeIfAbsent() {
        System.out.println("== 4. HashMap.computeIfAbsent：字母异位词分组（49） ==");
        String[] strs = {"eat", "tea", "tan", "ate", "nat", "bat"};
        Map<String, List<String>> groups = new HashMap<>();
        for (String s : strs) {
            char[] arr = s.toCharArray();
            Arrays.sort(arr);   // 排序后的串作分组 key
            groups.computeIfAbsent(new String(arr), k -> new ArrayList<>()).add(s);
            // 等价于三行：if (!map.containsKey(k)) map.put(k, new ArrayList<>()); map.get(k).add(s);
        }
        System.out.println("分组 = " + groups.values() + "（按 aet/ant/abt 分成三组，顺序可能不同）");
        // 坑：mappingFunction 返回 null 则不会建立映射
        // 同理 getOrDefault 在两数之和/最长连续序列里维护计数也是一行顶三行
        System.out.println();
    }

    // ==================== 第二档 5：Arrays.sort + Comparator ====================

    static void demoSort() {
        System.out.println("== 5. Arrays.sort + Comparator ==");
        int[][] intervals = {{5, 7}, {1, 3}, {2, 4}};
        Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));   // 合并区间(56)：按左端点
        System.out.println("按左端点排序: " + Arrays.deepToString(intervals));

        String[] words = {"bb", "a", "ccc"};
        Arrays.sort(words, Comparator.comparingInt(String::length)       // 多关键字：先长度
                .thenComparing(Comparator.naturalOrder()));              // 再字典序
        System.out.println("先长度后字典序: " + Arrays.toString(words));
        // 坑1：(a, b) -> a[0] - b[0] 大数相减会溢出，统一用 Integer.compare
        // 坑2：基本类型是双轴快排（不稳定），对象是 TimSort（稳定）——多关键字排序靠稳定性
        // 坑3：Arrays.asList(int[]) 得到 List<int[]> 不是 List<Integer>，装箱用 Arrays.stream(nums).boxed()
        System.out.println();
    }

    // ==================== 第二档 6：Character 工具类 ====================

    /** 验证回文串（125）：双指针 + Character 工具，避开自己写 ASCII 判断漏 Unicode 的坑。 */
    static boolean isPalindrome(String s) {
        int l = 0, r = s.length() - 1;
        while (l < r) {
            while (l < r && !Character.isLetterOrDigit(s.charAt(l))) l++;
            while (l < r && !Character.isLetterOrDigit(s.charAt(r))) r--;
            if (Character.toLowerCase(s.charAt(l)) != Character.toLowerCase(s.charAt(r))) return false;
            l++; r--;
        }
        return true;
    }

    static void demoCharacter() {
        System.out.println("== 6. Character.isLetterOrDigit / toLowerCase：验证回文串（125） ==");
        String s = "A man, a plan, a canal: Panama";
        System.out.println("是回文 = " + isPalindrome(s) + "（期望 true）");
        // isLetterOrDigit 走 Unicode 判断；自己写 'a'<=c<='z' 只认 ASCII，遇到全角/中文就错
        System.out.println();
    }

    // ==================== 第二档 7：StringBuilder.reverse ====================

    static void demoStringBuilder() {
        System.out.println("== 7. StringBuilder.reverse ==");
        String s = "hello";
        System.out.println("reverse(hello) = " + new StringBuilder(s).reverse().toString());
        // 坑：String 本身没有 reverse()，s.reverse() 直接编译错，必须经 StringBuilder 中转
        System.out.println();
    }

    // ==================== 第二档 8：TreeMap / TreeSet —— 有序 + O(log n) 前驱后继 ====================

    static void demoTreeMap() {
        System.out.println("== 8. TreeMap.floorKey / ceilingKey ==");
        TreeMap<Integer, String> tm = new TreeMap<>();
        tm.put(1, "a"); tm.put(3, "c"); tm.put(5, "e");
        System.out.println("floorKey(4)   = " + tm.floorKey(4) + "（<=4 的最大 key，期望 3）");
        System.out.println("ceilingKey(4) = " + tm.ceilingKey(4) + "（>=4 的最小 key，期望 5）");
        System.out.println("firstKey/lastKey = " + tm.firstKey() + "/" + tm.lastKey());
        // 场景：滑动窗口中位数、"小于 x 的最大值"——堆删任意元素 O(n)，TreeMap 全程 O(log n)
        System.out.println();
    }

    // ==================== 第三档 9：Integer.bitCount 与手写 n&(n-1) ====================

    static void demoBitCount() {
        System.out.println("== 9. 位 1 的个数（191） ==");
        int n = 0b1011;
        System.out.println("bitCount(11) = " + Integer.bitCount(n) + "（期望 3）");
        int cnt = 0;
        for (int x = n; x != 0; x &= x - 1) cnt++;   // n&(n-1) 消掉最低位的 1
        System.out.println("手写 n&(n-1) 计数 = " + cnt);
        // 回答策略：先说"可以直接用 Integer.bitCount"，再补"手写我用 n&=n-1"——既懂轮子又懂原理
        System.out.println();
    }

    // ==================== 第三档 10：BitSet —— N 皇后位运算标记 ====================

    static void demoBitSet() {
        System.out.println("== 10. BitSet 代替 boolean[] ==");
        int n = 4;
        BitSet col = new BitSet(n), diag1 = new BitSet(2 * n), diag2 = new BitSet(2 * n);
        int r = 0, c = 1;
        boolean attacked = col.get(c) || diag1.get(r + c) || diag2.get(r - c + n);
        col.set(c); diag1.set(r + c); diag2.set(r - c + n);
        System.out.println("放皇后(0,1) 前被攻击? " + attacked + "（期望 false），标记后 col = " + col);
        // 列 + 两方向对角线三条标记用一个 BitSet 更紧凑，O(1) 检查/标记/回溯 clear
        System.out.println();
    }

    // ==================== 第三档 11：Collections.shuffle / swap ====================

    static void demoCollections() {
        System.out.println("== 11. Collections.shuffle / swap（快速选择随机化） ==");
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9));
        Collections.shuffle(list);   // 随机打乱，避免快速排序退化到最坏 O(n²)
        Collections.swap(list, 0, list.size() - 1);
        System.out.println("shuffle+swap 后 = " + list + "（结果随机，仅演示调用）");
        // 坑：Collections 工具只认 List——Collections.shuffle(int[]) 编译不过
        //     int[] 想打乱要么手写 Fisher-Yates，要么先 boxed 成 List
        System.out.println();
    }
}
