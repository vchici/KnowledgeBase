> Spring 的每一行代码都在用 Java 的高级特性——不懂接口多态就看不懂注入，不懂反射就看不懂 IoC，不懂异常体系就踩不中事务回滚的坑。这 6 个知识点是 Spring 的"内功心法"。

# 一、面向对象：Spring的基因

## 1.1 接口 vs 抽象类

**类比：接口是插座标准，实现类是电器。** 插座只规定"两孔220V"，不管你插台灯还是插冰箱。接口只定义"能做什么"，不关心"怎么做"。

```java
public interface UserService {
    User findUser(String id);
}

@Service
public class UserServiceImpl implements UserService {
    public User findUser(String id) {
        return userDao.selectById(id);
    }
}

@Service
public class MockUserServiceImpl implements UserService {
    public User findUser(String id) {
        return new User("001", "mock");
    }
}
```

**Spring关联：** `@Autowired` 注入的是接口类型 `UserService`，运行时才决定用哪个实现——这就是多态。你换实现类，Controller 一行代码都不用改。

**坑：** 接口有两个实现类时，`@Autowired` 直接报错：

```
Field userService in com.example.Controller required a single bean, but 2 were found:
    - userServiceImpl
    - mockUserServiceImpl
```

解决方式：用 `@Qualifier("userServiceImpl")` 指定名字，或在一个实现类上加 `@Primary` 标记为默认。

## 1.2 多态的实战意义

```java
public void processUser(UserService userService) {
    User user = userService.findUser("001");
}

processUser(new UserServiceImpl());
processUser(new MockUserServiceImpl());
```

**Spring关联：** Controller 里写 `UserService userService`，绝不写 `UserServiceImpl userService`——这就是解耦。Spring 容器负责在运行时塞入真正的实现对象，你只管面向接口编程。

# 二、集合框架：Spring的容器底座

## 2.1 ArrayList vs LinkedList

- **ArrayList**：底层是数组，随机访问 O(1)，插入删除要挪元素 O(n)
- **LinkedList**：底层是双向链表，插入删除 O(1)（已有节点引用时），随机访问 O(n)

**Spring关联：** Spring 内部大量用 `ArrayList` 存 `BeanDefinition`，因为容器启动阶段是"批量注册、顺序遍历"，几乎不涉及中间插入删除，数组结构最合适。

## 2.2 HashMap（重点）

**底层：** 数组 + 链表 + 红黑树（JDK8+）

**put过程：** hash(key) → 定位桶下标 → 桶为空直接放入 → 桶非空则遍历链表/红黑树 → 链表长度≥8且数组长度≥64时转红黑树 → 元素超过阈值(容量×负载因子0.75)时扩容为2倍

```java
void put(K key, V value) {
    int hash = key.hashCode();
    int index = hash % table.length;
    Node<K,V> node = table[index];
    if (node == null) {
        table[index] = new Node<>(hash, key, value);
    } else {
        while (node.next != null) {
            if (node.key.equals(key)) {
                node.value = value;
                return;
            }
            node = node.next;
        }
        node.next = new Node<>(hash, key, value);
    }
    if (++size > table.length * 0.75f) {
        resize();
    }
}
```

**Spring关联：** Spring 的 `singletonObjects`（一级缓存，存完整的单例Bean）就是 `ConcurrentHashMap<String, Object>`，key是Bean名称，value是Bean实例。

**坑：** HashMap 非线程安全，多线程同时 put 时链表可能形成环，导致 `get()` 死循环，CPU飙到100%。Spring 用 `ConcurrentHashMap` 替代，彻底规避此问题。

## 2.3 ConcurrentHashMap

- **JDK7：** 分段锁（Segment），每个段一把锁，并发度=段数
- **JDK8：** CAS + synchronized，锁粒度细化到桶级别（单个数组元素），并发度=数组长度

**Spring关联：** Spring 的 Bean 缓存（`singletonObjects`、`singletonFactories`、`earlySingletonObjects`）全部用 `ConcurrentHashMap` 或其包装类，保证多线程下容器初始化安全。

# 三、异常体系：Spring事务回滚的命门

## 3.1 Checked vs Unchecked Exception

```java
public void read() throws IOException {
    throw new IOException("checked");
}

public void divide() {
    throw new RuntimeException("unchecked");
}
```

**继承树：**

```
Throwable
├── Error（JVM级错误，如StackOverflowError）
└── Exception
    ├── IOException, SQLException 等（Checked，编译器强制处理）
    └── RuntimeException 及其子类（Unchecked，编译器不强制）
```

## 3.2 Spring事务回滚规则（超重要）

**默认规则：只回滚 `RuntimeException` 及其子类和 `Error`，checked异常不回滚！**

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() throws IOException {
        orderDao.insert(order);
        throw new IOException("磁盘故障");
    }
}
```

**坑：** 上面代码抛出 `IOException` 后，数据库的 insert **不会回滚**！订单数据已经写进去了，但业务逻辑认为失败了——数据不一致。

**现象：** 方法抛出异常后查数据库，发现数据还在，事务像没生效一样。

**解决：**

```java
@Transactional(rollbackFor = Exception.class)
```

显式声明对所有 `Exception` 回滚，这是生产代码的标配写法。

# 四、反射：Spring IoC的引擎

## 4.1 反射是什么

**类比：** 正常调用是"看说明书操作"——你知道类名、方法名，直接 `new` 和 `.` 调用。反射是"拆开机器直接拨齿轮"——运行时才拿到类信息，直接操作内部。

```java
Class<?> clazz = Class.forName("com.example.UserService");
Constructor<?> ctor = clazz.getDeclaredConstructor();
Object obj = ctor.newInstance();
Method method = clazz.getMethod("findUser", String.class);
Object result = method.invoke(obj, "001");
```

逐行解释：

1. `Class.forName()` —— 通过类全限定名拿到类的"图纸"（Class对象）
2. `getDeclaredConstructor()` —— 从图纸上找到构造方法
3. `ctor.newInstance()` —— 用构造方法造出实例（等价于 `new UserService()`）
4. `getMethod("findUser", String.class)` —— 从图纸上找到方法签名
5. `method.invoke(obj, "001")` —— 在实例上调用方法（等价于 `obj.findUser("001")`）

## 4.2 Spring怎么用反射

Spring 的 IoC 容器工作流程：

1. 读取配置 / 扫描 `@Component` 等注解 → 拿到类全限定名
2. `Class.forName(类名)` → 加载 Class 对象
3. `clazz.getDeclaredConstructor()` → 反射创建实例
4. 遍历字段，发现 `@Autowired` → `field.set(obj, dependency)` 反射注入属性

**这就是 IoC 容器的底层原理——你写的 `new` 全被 Spring 用反射替代了。**

## 4.3 反射的坑

- **性能：** 反射比直接调用慢1~2个数量级（Spring 用 `ReflectionUtils` 缓存 Method/Field 对象缓解）
- **破坏封装：** `setAccessible(true)` 可以访问 private 字段
- **坑：** Spring 中 Bean 的私有字段 `@Autowired` 能注入，就是靠反射 `field.setAccessible(true)` 强行突破访问限制。所以你写 `private` 挡不住 Spring，但能挡住你自己——别指望 private 阻止依赖注入

# 五、注解：Spring的指挥棒

## 5.1 注解的本质

**注解本身不做任何事，只是标记，必须有代码去读取它才有意义。**

**类比：** 注解是便利贴，反射是读便利贴的人。你在冰箱上贴"买牛奶"，但没人看就等于没贴。`@Autowired` 本身什么逻辑都没有，是 Spring 的 `AutowiredAnnotationBeanPostProcessor` 用反射读到这个注解后才执行注入。

## 5.2 自定义注解

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MyInject {
}
```

```java
public class SimpleIoC {
    public static void inject(Object obj) throws Exception {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(MyInject.class)) {
                field.setAccessible(true);
                Object dependency = getBean(field.getType());
                field.set(obj, dependency);
            }
        }
    }
}
```

**Spring关联：** `@Autowired` 就是这个原理——注解标记 + 反射读取 + 注入依赖。上面 15 行代码就是一个最简 IoC 容器的核心逻辑。

## 5.3 注解的坑

**`@Retention` 必须是 `RUNTIME` 才能反射读取！**

- `SOURCE`：编译后丢弃（如 `@Override`、`@SuppressWarnings`）
- `CLASS`：编译后保留在字节码中，但 JVM 运行时忽略
- `RUNTIME`：运行时保留，反射可读

**坑：** 自定义注解忘记加 `@Retention(RetentionPolicy.RUNTIME)`，默认是 `CLASS`，反射读不到，`isAnnotationPresent()` 永远返回 false，注入静默失败——不报错但就是不生效，极难排查。

Spring 的 `@Component`、`@Autowired`、`@Transactional` 等全部是 `RUNTIME` 保留。

# 六、泛型：Spring的类型安全网

## 6.1 类型擦除

泛型在编译后擦除为 `Object`，运行时不存在泛型信息。

```java
List<String> a = new ArrayList<>();
List<Integer> b = new ArrayList<>();
System.out.println(a.getClass() == b.getClass());
```

输出：`true`。编译器帮你做类型检查，但字节码层面 `List<String>` 和 `List<Integer>` 是同一个类。

## 6.2 Spring中的泛型注入

Spring 4+ 支持泛型注入：

```java
public interface Repository<T> {}

@Repository
public class UserRepository implements Repository<User> {}

@Repository
public class OrderRepository implements Repository<Order> {}
```

```java
@Service
public class UserService {
    @Autowired
    private Repository<User> userRepo;
}
```

`Repository<User>` 和 `Repository<Order>` 不会冲突，Spring 能正确注入。

**底层原理：** 类型擦除说运行时拿不到泛型信息，但 Spring 用 `ResolvableType` 在类定义时（编译期信息保留在父类/接口的签名中）提取泛型参数，突破了类型擦除的限制。

---

## 总结：6个知识点在Spring中的对应关系

| Java知识点 | Spring中的应用 |
|---|---|
| 接口+多态 | `@Autowired`注入接口，运行时多态切换实现类 |
| 集合框架 | `singletonObjects`用`ConcurrentHashMap`，`BeanDefinition`列表用`ArrayList` |
| 异常体系 | `@Transactional`默认只回滚`RuntimeException`，checked异常需`rollbackFor` |
| 反射 | IoC容器通过`Class.forName()`+反射创建Bean、注入属性 |
| 注解 | `@Component`/`@Autowired`等注解+反射读取=Spring的配置方式 |
| 泛型 | `ResolvableType`突破类型擦除，实现泛型Bean的精确注入 |
