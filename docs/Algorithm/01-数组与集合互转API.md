# 数组与集合互转 API 速查

> 一句话本质：`int[]` 和 `List<Integer>` 之间没有一行直达的转换 —— 因为 Java 泛型不支持基本类型，`Arrays.asList(int[])` 会把整个数组当成一个元素（得到 `List<int[]>`），必须经过 Stream 装箱或手动循环。

## int[] → `List<Integer>`

```java
int[] arr = {1, 2, 3};

// 方式一：Stream 装箱（最常用）
List<Integer> list1 = Arrays.stream(arr).boxed().collect(Collectors.toList());

// 方式二：手动循环（无装箱流开销，性能最好）
List<Integer> list2 = new ArrayList<>(arr.length);
for (int x : arr) list2.add(x);

// ❌ 错误示范：得到 List<int[]>，size 为 1
List<int[]> wrong = Arrays.asList(arr);
```

## `List<Integer>` → int[]

```java
List<Integer> list = List.of(1, 2, 3);
int[] arr = list.stream().mapToInt(Integer::intValue).toArray();
```

## 其他常用转换

```java
// Integer[] ↔ List<Integer>
List<Integer> list = Arrays.asList(1, 2, 3);        // 返回定长视图，不能 add/remove
Integer[] boxed = list.toArray(new Integer[0]);     // List → 数组

// int[] ↔ Integer[]
Integer[] b = Arrays.stream(arr).boxed().toArray(Integer[]::new);
int[] p = Arrays.stream(b).mapToInt(Integer::intValue).toArray();

// String 相关
char[] chars = s.toCharArray();               // String → char[]
String s2 = new String(chars);                // char[] → String
String[] parts = s.split(",");                // String → String[]（split 参数是正则）
String joined = String.join(",", parts);      // String[] → String
String rev = new StringBuilder(s).reverse().toString();  // 反转字符串
```

## Arrays 工具类高频方法

```java
Arrays.sort(arr);                           // 基本类型双轴快排
Arrays.sort(arr2D, (a, b) -> a[0] - b[0]);  // 二维数组按第一列排序
Arrays.fill(dp, Integer.MAX_VALUE);         // DP 初始化
Arrays.copyOf(arr, n);                      // 复制前 n 个
Arrays.copyOfRange(arr, from, to);          // 区间复制，含头不含尾
Arrays.binarySearch(sortedArr, key);        // 二分查找，必须先有序，找不到返回 -(插入点)-1
Arrays.toString(arr);                       // 打印数组
```

## 坑速查表

| 坑 | 说明 |
|----|------|
| `Arrays.asList(int[])` | 得到 `List<int[]>` 而不是 `List<Integer>` |
| `Arrays.asList` 返回定长视图 | `add/remove` 抛 `UnsupportedOperationException`；改元素会联动原数组 |
| `println(int[])` | 直接打印是哈希码，必须 `Arrays.toString` |
| `sort` 比较器溢出 | `(a, b) -> a[0] - b[0]` 大数相减会溢出，安全写法 `Integer.compare(a[0], b[0])` |
| `Collections.reverse` 只对 List | 数组反转要么转 List，要么手写对撞双指针 |
