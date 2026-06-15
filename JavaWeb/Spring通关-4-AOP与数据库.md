> Spring AOP的本质是"用代理对象拦截方法调用"，事务的本质是"AOP+数据库连接的commit/rollback"，JPA和MyBatis是两种风格的数据访问层——理解了代理，就理解了Spring一半的魔法。

# 一、AOP：面向切面编程

## 1.1 为什么需要AOP

假设你有一个订单系统，每个Service方法都要加日志、权限校验、事务管理：

```java
@Service
public class OrderService {

    public void createOrder(Order order) {
        log.info("开始执行createOrder");
        checkPermission();
        try {
            orderMapper.insert(order);
            log.info("createOrder执行成功");
        } catch (Exception e) {
            log.error("createOrder执行失败", e);
            throw e;
        }
    }

    public void cancelOrder(Long orderId) {
        log.info("开始执行cancelOrder");
        checkPermission();
        try {
            orderMapper.delete(orderId);
            log.info("cancelOrder执行成功");
        } catch (Exception e) {
            log.error("cancelOrder执行失败", e);
            throw e;
        }
    }

    private void checkPermission() {
        // 权限校验逻辑
    }
}
```

10个方法就要写10遍日志+权限+try-catch，改一个日志格式要改10处。这就是**横切关注点**散落各处的灾难。

类比：切面就像一把刀，**横着切开**所有方法，把日志、权限、事务这些"非业务逻辑"统一植入进去，业务代码只写业务。

## 1.2 AOP核心概念

| 概念             | 类比       | 说明                |
| -------------- | -------- | ----------------- |
| 切面（Aspect）     | 一把刀      | 横切逻辑的模块，如"日志切面"   |
| 切点（Pointcut）   | 刀切的位置    | 用表达式匹配哪些方法要被切入    |
| 通知（Advice）     | 切入后做什么   | 前置/后置/环绕等具体逻辑     |
| 连接点（JoinPoint） | 可以下刀的点   | 方法调用、异常抛出等具体位置    |
| 织入（Weaving）    | 把刀插进去的过程 | 把切面应用到目标对象，生成代理对象 |

## 1.3 五种通知类型

```java
@Aspect
@Component
public class LogAspect {

    @Before("execution(* com.example.service.*.*(..))")
    public void before(JoinPoint jp) {
        System.out.println("【前置】方法: " + jp.getSignature().getName());
    }

    @AfterReturning(pointcut = "execution(* com.example.service.*.*(..))", returning = "result")
    public void afterReturning(JoinPoint jp, Object result) {
        System.out.println("【返回后】方法: " + jp.getSignature().getName() + " 返回: " + result);
    }

    @AfterThrowing(pointcut = "execution(* com.example.service.*.*(..))", throwing = "ex")
    public void afterThrowing(JoinPoint jp, Exception ex) {
        System.out.println("【异常后】方法: " + jp.getSignature().getName() + " 异常: " + ex.getMessage());
    }

    @After("execution(* com.example.service.*.*(..))")
    public void after(JoinPoint jp) {
        System.out.println("【最终】方法: " + jp.getSignature().getName());
    }

    @Around("execution(* com.example.service.*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("【环绕-前】");
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long cost = System.currentTimeMillis() - start;
        System.out.println("【环绕-后】耗时: " + cost + "ms");
        return result;
    }
}
```

执行顺序：Around前 → Before → 目标方法 → AfterReturning/AfterThrowing → After → Around后

> ⚠️ **踩坑**：Around通知必须调用`pjp.proceed()`，否则目标方法不会执行！必须返回`proceed()`的结果，否则调用方拿到的返回值是null。

## 1.4 切点表达式

语法：`execution(修饰符? 返回值 包名.类名.方法名(参数) 异常?)`

```java
@Aspect
@Component
public class PointcutDemo {

    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceLayer() {}

    @Pointcut("execution(* com.example.service..*.*(..))")
    public void serviceLayerAndSub() {}

    @Pointcut("@annotation(com.example.annotation.Log)")
    public void logAnnotated() {}

    @Before("serviceLayer()")
    public void beforeService() {
        System.out.println("拦截service包下的方法");
    }

    @Before("serviceLayerAndSub()")
    public void beforeServiceAndSub() {
        System.out.println("拦截service包及子包下的方法");
    }

    @Before("logAnnotated()")
    public void beforeLogMethod() {
        System.out.println("拦截标注了@Log的方法");
    }
}
```

常用写法速查：

| 表达式 | 含义 |
|--------|------|
| `execution(* com.example.service.*.*(..))` | service包下所有类的所有方法 |
| `execution(* com.example.service..*.*(..))` | service包及子包下所有方法 |
| `execution(* *.service.*.*(..))` | 任意包下的service子包 |
| `execution(public * *(..))` | 所有public方法 |
| `@annotation(com.example.Log)` | 标注了@Log注解的方法 |
| `within(com.example.service.*)` | service包下所有类的所有方法 |

## 1.5 AOP的底层原理

Spring AOP的底层就是**动态代理**。你调用的不是原始对象，而是代理对象，代理对象在调用真实方法前后插入切面逻辑。

### JDK动态代理

基于接口，用`Proxy.newProxyInstance()`生成代理类：

```java
public class JdkProxyDemo {

    interface UserService {
        String getName();
    }

    static class UserServiceImpl implements UserService {
        public String getName() {
            return "张三";
        }
    }

    public static void main(String[] args) {
        UserService target = new UserServiceImpl();
        UserService proxy = (UserService) Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (proxyObj, method, methodArgs) -> {
                System.out.println("【代理前】");
                Object result = method.invoke(target, methodArgs);
                System.out.println("【代理后】");
                return result;
            }
        );
        proxy.getName();
    }
}
```

限制：目标类必须实现接口，否则无法代理。

### CGLIB代理

基于继承，生成目标类的子类来代理：

```java
public class CglibProxyDemo {

    static class OrderService {
        public String getOrder() {
            return "订单001";
        }
    }

    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(OrderService.class);
        enhancer.setCallback((MethodInterceptor) (obj, method, args1, proxy) -> {
            System.out.println("【代理前】");
            Object result = proxy.invokeSuper(obj, args1);
            System.out.println("【代理后】");
            return result;
        });
        OrderService proxy = (OrderService) enhancer.create();
        proxy.getOrder();
    }
}
```

优势：不需要接口，直接代理类。

### Spring的默认策略

| 场景 | Spring默认 | Spring Boot 2.x+ |
|------|-----------|-------------------|
| 目标类实现了接口 | JDK动态代理 | **CGLIB** |
| 目标类没实现接口 | CGLIB | CGLIB |

> ⚠️ **踩坑**：Spring Boot 2.x起默认全部用CGLIB（`spring.aop.proxy-target-class=true`），这意味着即使你有接口，代理对象也是目标类的子类而非接口的实现类。如果你用接口类型接收代理对象没问题，但如果用具体类接收，要注意CGLIB生成的是子类。

## 1.6 AOP自调用陷阱（超重要）

这是AOP最经典的坑，面试必问：

```java
@Service
public class OrderService {

    public void methodA() {
        System.out.println("A执行");
        methodB();
    }

    @Transactional
    public void methodB() {
        System.out.println("B执行");
    }
}
```

外部调用`orderService.methodA()`时，`methodB()`上的`@Transactional`切面**不会生效**！

原因：`methodA()`内部调用`methodB()`等价于`this.methodB()`，这里的`this`是原始对象而非代理对象，直接调用绕过了代理。

### 解决方案

**方案1：注入自身（推荐）**

```java
@Service
public class OrderService {

    @Lazy
    @Autowired
    private OrderService self;

    public void methodA() {
        System.out.println("A执行");
        self.methodB();
    }

    @Transactional
    public void methodB() {
        System.out.println("B执行");
    }
}
```

`@Lazy`解决循环依赖，`self`是代理对象，调用`self.methodB()`走代理。

**方案2：AopContext**

```java
@Service
public class OrderService {

    public void methodA() {
        System.out.println("A执行");
        ((OrderService) AopContext.currentProxy()).methodB();
    }

    @Transactional
    public void methodB() {
        System.out.println("B执行");
    }
}
```

需要配置：`@EnableAspectJAutoProxy(exposeProxy = true)`

**方案3：拆分到不同类**

```java
@Service
public class OrderService {

    @Autowired
    private OrderTxService orderTxService;

    public void methodA() {
        System.out.println("A执行");
        orderTxService.methodB();
    }
}

@Service
public class OrderTxService {

    @Transactional
    public void methodB() {
        System.out.println("B执行");
    }
}
```

> ⚠️ **踩坑**：自调用不仅导致`@Transactional`失效，所有基于AOP的注解（`@Async`、`@Cacheable`、`@Retryable`等）都会失效！这是同一个根因。

# 二、事务管理

## 2.1 @Transactional基础

```java
@Service
public class TransferService {

    @Autowired
    private AccountMapper accountMapper;

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        accountMapper.deduct(fromId, amount);
        accountMapper.add(toId, amount);
    }
}
```

`@Transactional`的本质：Spring AOP在方法开始时获取数据库连接并设为手动提交，方法正常结束则commit，抛出RuntimeException则rollback。

## 2.2 事务传播行为（Propagation）

| 传播行为 | 说明 |
|----------|------|
| REQUIRED | 默认。有事务加入，没有就新建 |
| REQUIRES_NEW | 总是新建事务，挂起当前事务 |
| NESTED | 嵌套事务（savepoint），外层回滚内层也回滚，内层回滚外层不受影响 |
| SUPPORTS | 有事务加入，没有就非事务执行 |
| NOT_SUPPORTED | 总是非事务执行，挂起当前事务 |
| NEVER | 非事务执行，存在事务则抛异常 |
| MANDATORY | 必须在事务中，否则抛异常 |

REQUIRED vs REQUIRES_NEW 实战场景：

```java
@Service
public class OrderService {

    @Autowired
    private LogService logService;

    @Transactional
    public void createOrder(Order order) {
        orderMapper.insert(order);
        logService.saveLog("创建订单: " + order.getId());
    }
}

@Service
public class LogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(String message) {
        logMapper.insert(message);
    }
}
```

如果`createOrder`后续报错回滚，`saveLog`用REQUIRES_NEW会独立提交，日志不会丢失。如果用默认的REQUIRED，日志会和订单一起回滚。

NESTED场景：

```java
@Service
public class BatchService {

    @Transactional
    public void batchProcess(List<Item> items) {
        for (Item item : items) {
            processSingle(item);
        }
    }

    @Transactional(propagation = Propagation.NESTED)
    public void processSingle(Item item) {
        itemMapper.update(item);
    }
}
```

某一条处理失败只回滚该条的savepoint，不影响其他条。外层整体回滚时，所有内层也回滚。

## 2.3 隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 说明 |
|----------|------|-----------|------|------|
| READ_UNCOMMITTED | ✅ | ✅ | ✅ | 能读到未提交的数据 |
| READ_COMMITTED | ❌ | ✅ | ✅ | Oracle默认 |
| REPEATABLE_READ | ❌ | ❌ | ✅ | MySQL默认，通过MVCC+间隙锁基本解决幻读 |
| SERIALIZABLE | ❌ | ❌ | ❌ | 完全串行，性能最差 |

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public User getUser(Long id) {
    return userMapper.selectById(id);
}
```

> ⚠️ **踩坑**：MySQL的REPEATABLE_READ下，快照读（普通SELECT）通过MVCC看到的是事务开始时的快照，当前读（SELECT ... FOR UPDATE）看到的是最新数据。两者混用可能导致逻辑不一致。

## 2.4 @Transactional失效的7种场景（超重要）

### 场景1：方法非public

```java
@Service
public class UserService {

    @Transactional
    void createUser(User user) {
        userMapper.insert(user);
    }
}
```

**现象**：事务不生效，异常时数据不回滚。

**原因**：Spring AOP基于代理，只拦截public方法。

**正确写法**：

```java
@Service
public class UserService {

    @Transactional
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

### 场景2：自调用

```java
@Service
public class UserService {

    public void register(User user) {
        createUser(user);
    }

    @Transactional
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

**现象**：`createUser`的事务不生效，异常不回滚。

**原因**：`register`内部调用`createUser`是`this.createUser()`，绕过代理。

**正确写法**：

```java
@Service
public class UserService {

    @Lazy
    @Autowired
    private UserService self;

    public void register(User user) {
        self.createUser(user);
    }

    @Transactional
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

### 场景3：异常被catch吞掉

```java
@Service
public class UserService {

    @Transactional
    public void createUser(User user) {
        try {
            userMapper.insert(user);
            throw new RuntimeException("出错了");
        } catch (Exception e) {
            log.error("创建用户失败", e);
        }
    }
}
```

**现象**：异常被catch后，Spring感知不到异常，不会回滚，数据已插入。

**正确写法**：

```java
@Service
public class UserService {

    @Transactional
    public void createUser(User user) {
        try {
            userMapper.insert(user);
            throw new RuntimeException("出错了");
        } catch (Exception e) {
            log.error("创建用户失败", e);
            throw e;
        }
    }
}
```

### 场景4：异常类型不对

```java
@Service
public class UserService {

    @Transactional
    public void createUser(User user) throws Exception {
        userMapper.insert(user);
        throw new Exception("checked异常");
    }
}
```

**现象**：checked异常不回滚，数据已插入。Spring默认只回滚`RuntimeException`和`Error`。

**正确写法**：

```java
@Service
public class UserService {

    @Transactional(rollbackFor = Exception.class)
    public void createUser(User user) throws Exception {
        userMapper.insert(user);
        throw new Exception("checked异常");
    }
}
```

### 场景5：数据库引擎不支持

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50)
) ENGINE=MyISAM;
```

**现象**：`@Transactional`标注了，异常也抛了，但数据就是不回滚。

**原因**：MyISAM引擎不支持事务。

**正确写法**：

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50)
) ENGINE=InnoDB;
```

### 场景6：传播行为设错

```java
@Service
public class UserService {

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createUser(User user) {
        userMapper.insert(user);
        throw new RuntimeException("出错了");
    }
}
```

**现象**：NOT_SUPPORTED表示非事务执行，异常后数据不回滚。

**正确写法**：

```java
@Service
public class UserService {

    @Transactional
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

### 场景7：Bean未被Spring管理

```java
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

**现象**：`@Transactional`完全无效，类根本不是Spring Bean，AOP无从代理。

**正确写法**：

```java
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

## 2.5 事务最佳实践

```java
@Service
public class OrderService {

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        orderMapper.insert(order);
    }
}
```

- `@Transactional`加在**Service层**，不要加Controller层——Controller不是业务逻辑层，事务粒度太大
- 只读查询加`readOnly = true`，数据库可以进行优化（如不记录undo log）
- 事务方法**尽量简短**，不要包含RPC调用、发邮件、文件IO等外部操作——事务持有数据库连接，长时间占用导致连接池耗尽
- 永远加`rollbackFor = Exception.class`，把checked异常纳入回滚范围

# 三、Spring Data JPA

## 3.1 JPA是什么

JPA（Java Persistence API）是Java的ORM标准，定义了用注解映射Java对象到数据库表的一套规范。Hibernate是JPA最主流的实现。

Spring Data JPA在JPA之上再封装一层：你只需要写接口，Spring帮你生成实现类。

类比：JPA是"自动挡"，你踩油门就行；MyBatis是"手动挡"，你自己换挡。

## 3.2 实体映射

```java
@Entity
@Table(name = "t_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Column(name = "age")
    private Integer age;

    @Column(name = "email", unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

## 3.3 Repository接口

```java
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    List<User> findByAgeGreaterThan(Integer age);

    List<User> findByUsernameContainingAndAgeBetween(String keyword, int minAge, int maxAge);

    Optional<User> findByEmail(String email);

    long countByStatus(UserStatus status);

    boolean existsByUsername(String username);

    void deleteByUsername(String username);
}
```

Spring Data JPA根据方法名自动推导SQL：

| 关键词 | SQL | 示例 |
|--------|-----|------|
| findBy | SELECT ... WHERE | findByUsername |
| And | AND | findByUsernameAndAge |
| Or | OR | findByUsernameOrEmail |
| Between | BETWEEN | findByAgeBetween |
| LessThan | < | findByAgeLessThan |
| GreaterThan | > | findByAgeGreaterThan |
| Like | LIKE | findByUsernameLike |
| Containing | LIKE %x% | findByUsernameContaining |
| OrderBy | ORDER BY | findByAgeOrderByIdDesc |
| In | IN | findByStatusIn |

## 3.4 @Query自定义查询

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.username = :name")
    User findByName(@Param("name") String name);

    @Query("SELECT u FROM User u WHERE u.age > :minAge AND u.status = :status")
    List<User> findByCondition(@Param("minAge") Integer minAge, @Param("status") UserStatus status);

    @Query(value = "SELECT * FROM t_user WHERE email LIKE %:keyword%", nativeQuery = true)
    List<User> searchByEmail(@Param("keyword") String keyword);

    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);
}
```

> ⚠️ **踩坑**：JPQL里写的是实体名和属性名（`User`、`u.username`），不是表名和列名。搞混了会报`QuerySyntaxException`。

## 3.5 N+1问题（超重要）

这是JPA最臭名昭著的性能陷阱：

```java
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no")
    private String orderNo;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
```

```java
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    System.out.println(order.getItems().size());
}
```

假设查到10个Order，SQL执行过程：
1. `SELECT * FROM t_order` — 1次
2. 对每个Order访问`getItems()`时各触发1次：`SELECT * FROM t_order_item WHERE order_id = ?` — 10次

总共11次SQL！100个Order就是101次！

### 解决方案

**方案1：@EntityGraph**

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items"})
    List<Order> findAllByOrderNoNotNull();
}
```

生成1条SQL：`SELECT * FROM t_order o LEFT JOIN t_order_item i ON o.id = i.order_id`

**方案2：JOIN FETCH**

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o JOIN FETCH o.items")
    List<Order> findAllWithItems();
}
```

**方案3：@BatchSize**

```java
@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
@BatchSize(size = 50)
private List<OrderItem> items;
```

不是1+10，而是1+1：先查10个Order，再用`WHERE order_id IN (id1, id2, ..., id10)`一次查完所有OrderItem。

## 3.6 JPA的坑

### 延迟加载在Session外报错

```java
@Service
public class OrderService {

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id).orElse(null);
    }
}
```

```java
@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id) {
        Order order = orderService.getOrder(id);
        order.getItems().size();
        return order;
    }
}
```

**现象**：`org.hibernate.LazyInitializationException: could not initialize proxy - no Session`

**原因**：`@Transactional`方法结束后Session关闭，Controller层访问延迟加载的`items`时Session已不存在。

**正确写法**：在事务内初始化，或用`@EntityGraph`/`JOIN FETCH`一次性查出。

### equals/hashCode用ID

```java
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return id != null && id.equals(((User) o).id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
```

**现象**：新建User（id=null）时，两个不同User的hashCode都是0，放入HashSet会互相覆盖。

**正确写法**：基于业务字段（如username）实现equals/hashCode，或者用`Objects.equals`比较所有业务字段。

### save() vs saveAndFlush()

```java
userRepository.save(user);
```

`save()`只是把实体交给持久化上下文管理，SQL可能还没发到数据库（缓存在EntityManager中）。

```java
userRepository.saveAndFlush(user);
```

`saveAndFlush()`立即将SQL发送到数据库执行。

**场景**：如果你save之后需要立即查到这条数据（同一方法内），或者需要获取数据库生成的默认值，用`saveAndFlush()`。

# 四、MyBatis

## 4.1 MyBatis是什么

MyBatis是半自动ORM框架：SQL你自己写，参数映射和结果映射框架帮你做。

对比JPA：

| 维度 | JPA | MyBatis |
|------|-----|---------|
| SQL | 框架自动生成 | 自己写 |
| 映射 | 注解自动映射 | 注解或XML映射 |
| 复杂查询 | 难写，要学JPQL | 直接写SQL，灵活 |
| 学习成本 | 高（要理解持久化上下文、脏检查等） | 低（会SQL就能上手） |

类比：JPA是全自动相机，MyBatis是单反相机——前者按快门就行，后者你要自己调参数但效果更可控。

## 4.2 基本使用

```java
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User selectByUsername(String username);

    @Insert("INSERT INTO t_user(username, age, email) VALUES(#{username}, #{age}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE t_user SET username=#{username}, age=#{age}, email=#{email} WHERE id=#{id}")
    int update(User user);

    @Delete("DELETE FROM t_user WHERE id = #{id}")
    int deleteById(Long id);
}
```

## 4.3 XML映射文件

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">

    <resultMap id="userResultMap" type="com.example.entity.User">
        <id property="id" column="id"/>
        <result property="username" column="username"/>
        <result property="age" column="age"/>
        <result property="email" column="email"/>
    </resultMap>

    <select id="selectById" resultMap="userResultMap">
        SELECT id, username, age, email FROM t_user WHERE id = #{id}
    </select>

    <select id="selectAll" resultMap="userResultMap">
        SELECT id, username, age, email FROM t_user
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO t_user(username, age, email)
        VALUES(#{username}, #{age}, #{email})
    </insert>

    <update id="update">
        UPDATE t_user SET username=#{username}, age=#{age}, email=#{email}
        WHERE id=#{id}
    </update>

    <delete id="deleteById">
        DELETE FROM t_user WHERE id = #{id}
    </delete>

</mapper>
```

## 4.4 动态SQL

```xml
<select id="searchUsers" resultMap="userResultMap">
    SELECT id, username, age, email FROM t_user
    <where>
        <if test="username != null and username != ''">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="minAge != null">
            AND age &gt;= #{minAge}
        </if>
        <if test="maxAge != null">
            AND age &lt;= #{maxAge}
        </if>
        <choose>
            <when test="sortField == 'age'">
                ORDER BY age
            </when>
            <when test="sortField == 'username'">
                ORDER BY username
            </when>
            <otherwise>
                ORDER BY id
            </otherwise>
        </choose>
    </where>
</select>

<update id="updateSelective">
    UPDATE t_user
    <set>
        <if test="username != null">username = #{username},</if>
        <if test="age != null">age = #{age},</if>
        <if test="email != null">email = #{email},</if>
    </set>
    WHERE id = #{id}
</update>

<select id="selectByIds" resultMap="userResultMap">
    SELECT id, username, age, email FROM t_user WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

| 标签 | 作用 |
|------|------|
| `<if>` | 条件判断，满足才拼接 |
| `<choose>/<when>/<otherwise>` | 类似switch-case |
| `<where>` | 自动处理前缀AND/OR |
| `<set>` | 自动处理末尾逗号 |
| `<foreach>` | 遍历集合，常用于IN |

## 4.5 #{} vs ${}（超重要）

### #{}：预编译参数

```java
@Select("SELECT * FROM t_user WHERE username = #{username}")
User selectByUsername(String username);
```

实际执行的SQL：`SELECT * FROM t_user WHERE username = ?`，参数通过PreparedStatement的`setString`设置。

### ${}：字符串直接拼接

```java
@Select("SELECT * FROM t_user WHERE username = '${username}'")
User selectByUsername(String username);
```

实际执行的SQL：`SELECT * FROM t_user WHERE username = '张三'`，直接拼进SQL字符串。

### SQL注入场景

```java
@Select("SELECT * FROM t_user WHERE username = '${username}'")
User selectByUsername(String username);
```

如果传入`username = "' OR '1'='1"`，实际SQL变成：

`SELECT * FROM t_user WHERE username = '' OR '1'='1'`

所有数据全部暴露！

### 规则

- **参数值一律用`#{}`**
- 只有表名、列名、ORDER BY等**不能用?占位符**的场景才用`${}`，且必须白名单校验

```java
@Select("SELECT * FROM ${tableName} WHERE id = #{id}")
User selectByTable(@Param("tableName") String tableName, @Param("id") Long id);
```

> ⚠️ **踩坑**：`ORDER BY ${column}`也是常见注入点。如果column来自前端输入，必须校验是否在允许的列名列表内，绝不能直接拼接。

## 4.6 MyBatis的坑

### namespace不一致

```java
@Mapper
public interface UserMapper {
    User selectById(Long id);
}
```

```xml
<mapper namespace="com.example.mapper.WrongMapper">
    <select id="selectById" resultType="com.example.entity.User">
        SELECT * FROM t_user WHERE id = #{id}
    </select>
</mapper>
```

**现象**：`org.apache.ibatis.binding.BindingException: Invalid bound statement (not found): com.example.mapper.UserMapper.selectById`

**原因**：XML的namespace必须和Mapper接口的全限定名完全一致。

**正确写法**：`namespace="com.example.mapper.UserMapper"`

### 字段名和属性名不匹配

数据库列名`user_name`，Java属性名`username`，查出来username是null。

**解决方案1**：开启驼峰映射

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
```

`user_name` → `userName`，但注意`user_name`不会映射到`username`（少了个下划线）。

**解决方案2**：用resultMap

```xml
<resultMap id="userResultMap" type="com.example.entity.User">
    <result property="username" column="user_name"/>
</resultMap>
```

**解决方案3**：SQL中起别名

```sql
SELECT user_name AS username FROM t_user
```

### 一级缓存导致脏读

```java
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public void demo() {
        User user1 = userMapper.selectById(1L);
        otherService.updateUser(1L, "新名字");
        User user2 = userMapper.selectById(1L);
        System.out.println(user2.getUsername());
    }
}
```

**现象**：`user2`还是旧数据，因为同一SqlSession内一级缓存生效，第二次查询直接返回缓存。

**原因**：MyBatis一级缓存是SqlSession级别的，同一SqlSession内相同查询走缓存。如果其他SqlSession修改了数据，当前SqlSession看不到。

**解决方案**：在两次查询之间清空缓存，或者在不同SqlSession中操作。Spring默认每次Mapper调用使用不同SqlSession（除非在`@Transactional`方法内），所以在`@Transactional`方法内要特别注意此问题。

# 五、JPA vs MyBatis选型指南

| 维度 | JPA | MyBatis |
|------|-----|---------|
| 学习曲线 | 陡（持久化上下文、脏检查、N+1等概念多） | 平（会SQL就能上手） |
| 简单CRUD | 极快（Repository接口自动生成） | 需手写SQL或用MyBatis-Plus |
| 复杂查询 | 困难（JPQL/HQL表达能力有限，Criteria API难读） | 灵活（直接写SQL） |
| 数据库迁移 | 容易（抽象了数据库方言） | 需改SQL（不同数据库语法不同） |
| 性能优化 | 需理解N+1、延迟加载等陷阱 | SQL可控，优化直观 |
| 适用团队 | 面向对象思维强，领域驱动设计 | SQL功底扎实，追求SQL可控 |
| 国内采用率 | 较低 | 主流 |

**选型建议**：
- 新项目、业务逻辑复杂、领域模型清晰 → JPA
- 老项目、报表多、SQL复杂、团队SQL经验丰富 → MyBatis
- 想要JPA的开发效率又想要SQL的可控 → MyBatis-Plus

# 六、常见坑汇总

| # | 坑 | 现象 | 根因 |
|---|---|------|------|
| 1 | AOP自调用 | 切面不生效，@Transactional/@Async等失效 | this调用绕过代理对象 |
| 2 | @Transactional加在private方法 | 事务不生效，异常不回滚 | Spring AOP只拦截public方法 |
| 3 | JPA的N+1 | 查10条数据触发11次SQL，性能灾难 | 延迟加载逐条查询关联数据 |
| 4 | MyBatis的${} | SQL注入，数据泄露 | 字符串直接拼接，未预编译 |
| 5 | 事务中做RPC | 事务超时，数据库锁持有时间过长，其他请求阻塞 | 事务持有连接，RPC耗时不可控 |
| 6 | JPA延迟加载在事务外 | `LazyInitializationException: no Session` | Session已关闭，代理无法加载 |
| 7 | 多数据源事务 | @Transactional只管一个数据源，跨库操作无法回滚 | Spring事务管理器绑定单个DataSource |

第5条展开：

```java
@Service
public class OrderService {

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        orderMapper.insert(order);
        paymentClient.pay(order);
        notificationClient.notify(order);
    }
}
```

**现象**：`paymentClient.pay()`耗时3秒，事务持有数据库连接和行锁3秒，高并发下连接池耗尽，其他请求超时。

**正确写法**：先完成事务内操作，事务提交后再做外部调用：

```java
@Service
public class OrderService {

    @Autowired
    private OrderService self;

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        orderMapper.insert(order);
    }

    public void createOrderFull(Order order) {
        self.createOrder(order);
        paymentClient.pay(order);
        notificationClient.notify(order);
    }
}
```

第7条展开：

```java
@Service
public class CrossDbService {

    @Autowired
    private Db1Mapper db1Mapper;

    @Autowired
    private Db2Mapper db2Mapper;

    @Transactional
    public void crossDbOperation() {
        db1Mapper.insert(data1);
        db2Mapper.insert(data2);
    }
}
```

**现象**：`db2Mapper.insert()`失败时，`db1Mapper.insert()`不会回滚，因为`@Transactional`只管理一个DataSource的事务。

**解决方案**：使用分布式事务（Seata等），或改用最终一致性方案（消息队列+本地消息表）。
