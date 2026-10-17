# Comparable 与 Comparator 排序速查

> 一句话本质：Comparable 是「我天生会和别人比」（类自己实现 `compareTo`，侵入式，只有一种自然顺序）；Comparator 是「外聘裁判」（`compare(a, b)` 外挂策略，可插拔、可定义任意多种）。lambda 时代排序代码的终点形态：`Comparator.comparing(类::属性).thenComparing(...)`。

## 一、Comparable vs Comparator

| | Comparable | Comparator |
|---|---|---|
| 实现位置 | 排序类**自己**实现 | 独立类 / lambda / 匿名类 |
| 抽象方法 | `int compareTo(T o)` | `int compare(T a, T b)` |
| 语义 | 自然顺序，一种 | 策略排序，可多个 |
| 典型来源 | JDK 类自带（String、Integer…） | 刷题现场按题意临时写 |
| 使用入口 | `Arrays.sort(arr)` 不传比较器 | `sort(x, cmp)` / `new PriorityQueue<>(cmp)` |

> 口诀：`compareTo` 是「我 − 你」，`compare` 是「a − b」；**返回负数排前面，正数排后面，零并列**。

> 方法名辨析（易混三兄弟）：`compareTo(other)` 是 **Comparable 的实例方法**（实现类重写它）；`compare(a, b)` 是 **Comparator 的接口方法**（lambda 实现的就是它）；`Integer.compare(a, b)` 是**包装类的静态工具**（参数是两个 primitive，与 Comparable 无关，专治 primitive 防溢出比较，同理 Long/Double/Character.compare）。决策看**手里的值是 primitive 还是对象**：手里是 `int`（数组元素、局部变量）→ `Integer.compare(x, y)`；手里已是对象（String 或 Integer 实例都是）→ `x.compareTo(y)`。String 没有 `String.compare`，因为字符串不存在 primitive 形态，永远走实例方法。

## 二、Lambda 精简演进链（以「按长度排字符串」为例）

```java
// ① 匿名内部类（JDK 8 之前；面试可能要求手写，证明你懂函数式接口原理）
Arrays.sort(words, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return Integer.compare(a.length(), b.length());
    }
});

// ② Lambda：函数式接口（只有一个抽象方法）才能这样写
Arrays.sort(words, (a, b) -> Integer.compare(a.length(), b.length()));

// ③ 静态工厂 + 方法引用：comparing 接收「key 提取器」，比较逻辑交给 key 的自然顺序
Arrays.sort(words, Comparator.comparing(String::length));

// ④ 多关键字链式：先长度，长度相同再字典序
Arrays.sort(words, Comparator.comparing(String::length)
                             .thenComparing(Comparator.naturalOrder()));
```

> 精简心法（按 key 提取器返回类型决策）：
> ① key 是基本类型 `int/long/double` → `comparingInt/Long/Double`（免装箱，性能优先）；
> ② key 是对象且走它的自然序 → `comparing(k)`；
> ③ key 是对象但不想用自然序 → `comparing(k, cmp)` 两参数版（混升降序专用）；
> ④ 提取不出 key（如二维数组按列比）→ 裸 lambda `(a, b) -> Integer.compare(...)`。
> ①与②结果完全相同，唯一差别是装箱：`comparing(Person::getAge)` 每次比较把 int 装箱成 Integer，O(n log n) 次比较就是 O(n log n) 次装箱；`thenComparing` 家族同规则（Int 版 / 对象版 / 两参数版）。

### 还原练习：`(a, b) -> Integer.compare(a.val, b.val)` 的正式形态

JdkTricks.java `mergeKLists` 里堆的比较器是一行简写，它省掉了四样东西：**接口名、方法名、参数类型、return 与大括号**。从正式形态往回推：

```java
// ① 正式形态：匿名内部类（补全所有被省略的东西）
PriorityQueue<ListNode> pq = new PriorityQueue<>(new Comparator<ListNode>() {
    @Override
    public int compare(ListNode a, ListNode b) {   // 接口名 + 方法名 compare + 参数类型 + return
        return Integer.compare(a.val, b.val);
    }
});

// ② 中间态：lambda 带显式参数类型
PriorityQueue<ListNode> pq = new PriorityQueue<>(
        (ListNode a, ListNode b) -> Integer.compare(a.val, b.val));

// ③ 一行简写：靠「目标类型推断」省掉参数类型
PriorityQueue<ListNode> pq = new PriorityQueue<>(
        (a, b) -> Integer.compare(a.val, b.val));

// ④ 终极简写：能提取 key 时（假设 ListNode 有 getVal()），先赋变量再传入
Comparator<ListNode> cmp = Comparator.comparing(ListNode::getVal);
PriorityQueue<ListNode> pq = new PriorityQueue<>(cmp);
```

为什么 ③ 能编译：`PriorityQueue` 构造器参数是 `Comparator<? super E>`，左侧泛型已把 `E` 推断为 `ListNode`（目标类型已知）→ lambda 参数类型可省；`Comparator` 是函数式接口、只有一个抽象方法 → 方法名和 `return` 可省。**换个没有目标类型的位置（如 `Object o = (a, b) -> ...`）立刻编译不过**。

> 呼应坑 3：`new PriorityQueue<>(Comparator.comparing(l -> l.val))` 实测能编译（左侧变量就是目标类型）；真正会挂的是没有目标类型的场合（赋给 Object、裸语句），兜底写法见坑 3。

## 三、Comparator 静态工厂全家桶

| 方法 | 作用 | 示例 |
|---|---|---|
| `comparing(k)` | 按 key 升序（对象 key，走其 compareTo） | `comparing(Person::getName)` |
| `comparingInt/Long/Double(k)` | 按 key 升序（基本类型，免装箱） | `comparingInt(String::length)` |
| `thenComparing(k)` | 拼次级关键字（同样有 Int 版） | 见下 |
| `reversed()` | **反转整条已拼好的规则** | `comparingInt(String::length).reversed()` |
| `naturalOrder()` / `reverseOrder()` | 自然序 / 反自然序——泛型，任何实现 `Comparable` 的类型都行（Integer 数值序、String 字典序、枚举声明序），不限于字符串 | `new PriorityQueue<>(Comparator.reverseOrder())` |
| `nullsFirst(n)` / `nullsLast(n)` | null 排前/排后 | 工程常见，刷题少用 |

多关键字 + 混合升降序：

```java
// 先年龄升序；同年龄按工资降序；再按姓名升序
Comparator<Person> cmp = Comparator.comparingInt(Person::getAge)
        .thenComparing(Person::getSalary, Comparator.reverseOrder())
        .thenComparing(Person::getName);
```

## 四、手撕题高频写法

```java
// 1. 二维数组按第一列（合并区间 56 / 区间调度）——提取不出 key，lambda 最简
Arrays.sort(arr, (a, b) -> Integer.compare(a[0], b[0]));

// 2. List<int[]> 同理
intervals.sort((a, b) -> Integer.compare(a[0], b[0]));

// 3. 大顶堆
new PriorityQueue<Integer>(Comparator.reverseOrder());

// 4. 堆按第二个元素排序（合并K链表 23 / 任务调度）
new PriorityQueue<int[]>((a, b) -> Integer.compare(a[1], b[1]));

// 5. Stream 中排序
list.stream().sorted(Comparator.comparing(Person::getAge).reversed()).toList();
```

## 五、三个必踩坑

### 坑 1：`a - b` 溢出

大数相减结果溢出为负 → 比较器语义错乱 → 排序结果错乱（JDK 8 的 TimSort 甚至直接抛 `IllegalArgumentException`）。统一写 `Integer.compare(a, b)`。

### 坑 2：`reversed()` 的作用域是「整条已拼好的规则」

```java
// ❌ 想要「年龄升序 + 工资降序」——结果年龄也被反转成降序了
Comparator.comparingInt(Person::getAge)
          .thenComparingInt(Person::getSalary).reversed();

// ✅ reversed 只包住第二段
Comparator.comparingInt(Person::getAge)
          .thenComparing(Comparator.comparingInt(Person::getSalary).reversed());
// 等价写法：thenComparing(key, Comparator.reverseOrder())
```

### 坑 3：隐式 lambda 的推断依赖「目标类型」

JDK 23 实测：只要有可用目标类型（赋值给具体泛型、`sort` 参数、菱形构造器、链式 `thenComparing`），隐式 lambda 都能推断成功。真正会挂的是**没有目标类型**的场合：

```java
// ❌ 挂：赋给 Object —— s 被推断成 Object，Object 没有 length()
Object o = Comparator.comparing(s -> s.length());

// ❌ 挂：裸表达式语句 —— 同理
Comparator.comparing(s -> s.length());

// ✅ 兜底三件套：方法引用 / 显式参数类型 / 显式类型见证
Comparator.comparing(String::length)
Comparator.comparing((String s) -> s.length())
Comparator.<String, Integer>comparing(s -> s.length())
```

> 老版本 javac 对链式 `thenComparing(隐式lambda)` 也会推断失败，现代 JDK 已修复；遇到报错（尤其老项目）按兜底三件套处理即可。

## 六、compareTo 与 equals 不一致的坑（TreeSet/TreeMap）

TreeMap/TreeSet 判「同一个元素」**只看 `compareTo == 0`，不看 equals**：

```java
Set<BigDecimal> set = new TreeSet<>();
set.add(new BigDecimal("1.0"));
set.add(new BigDecimal("1.00"));
set.size();   // 1！compareTo 相等，第二个被拒绝
// 换成 HashSet 是 2（equals 不相等）
```

所以自定义类实现 Comparable 时，应保持「`compareTo == 0` ⟺ `equals`」，否则放进有序集合行为诡异——这也是面试追问「为什么 TreeMap 的 key 不要求重写 equals」的标准答案。

---

关联：[数组与集合互转API](01-数组与集合互转API.md)（坑速查表）、[栈与队列API](02-栈与队列API.md)（堆的比较器选择）；可运行示例见 JdkTricks.java 的 `demoSort()`。
