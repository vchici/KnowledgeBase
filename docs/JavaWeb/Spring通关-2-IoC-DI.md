> Spring IoC 的核心就一句话：你别 new 了，把创建和管理对象的工作交给容器，你只管用。控制反转是手段，依赖注入是结果，三级缓存是兜底。

# 一、为什么需要IoC：从new到容器管理

## 1.1 传统开发的痛点

```java
public class UserController {
    private UserService userService = new UserServiceImpl();
}

public class UserServiceImpl implements UserService {
    private UserRepository userRepository = new UserRepositoryImpl();
}
```

三个问题：

- **换实现要改源码**：想从 `UserRepositoryImpl` 换成 `JpaUserRepositoryImpl`，得打开 `UserServiceImpl` 改代码
- **测试无法 Mock**：单元测试时没法把 `UserRepository` 替换成 Mock 对象，因为它在源码里写死了 `new`
- **类之间硬耦合**：`UserController` 依赖了 `UserServiceImpl` 这个具体类，而不是 `UserService` 这个接口

## 1.2 IoC的思想

类比：以前你自己去超市买菜（`new`），现在你在外卖平台下单，骑手给你送上门（容器注入）。你只关心"我要菜"，不关心"菜从哪来"。

**控制反转**：对象不再自己创建它的依赖，而是由外部容器创建并注入进来。"控制"从对象内部反转到了外部容器。

## 1.3 第一个Spring程序

Maven 依赖：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>6.1.6</version>
</dependency>
```

```java
public interface UserRepository {
    String findName();
}
```

```java
public class UserRepositoryImpl implements UserRepository {
    @Override
    public String findName() {
        return "张三";
    }
}
```

```java
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getUserName() {
        return userRepository.findName();
    }
}
```

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Configuration
public class AppConfig {

    @Bean
    public UserRepository userRepository() {
        return new UserRepositoryImpl();
    }

    @Bean
    public UserService userService() {
        return new UserService(userRepository());
    }
}
```

```java
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService = context.getBean(UserService.class);
        System.out.println(userService.getUserName());
        context.close();
    }
}
```

> ⚠️ **踩坑**：`AppConfig` 里的 `userService()` 方法调用 `userRepository()` 看起来像调了两次，但 Spring 的 CGLIB 代理保证了 `userRepository()` 只创建一个实例。别把 `@Bean` 方法当普通方法理解。

# 二、Bean的生命周期

## 2.1 完整生命周期流程

```
实例化（反射调构造器）
  → 属性注入（@Autowired）
  → BeanNameAware / BeanFactoryAware
  → BeanPostProcessor.postProcessBeforeInitialization
  → @PostConstruct
  → InitializingBean.afterPropertiesSet
  → @Bean(initMethod)
  → BeanPostProcessor.postProcessAfterInitialization
  → 使用
  → @PreDestroy
  → DisposableBean.destroy
  → @Bean(destroyMethod)
```

## 2.2 生命周期验证代码

```java
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class LifeCycleBean implements BeanNameAware, BeanFactoryAware,
        ApplicationContextAware, InitializingBean, DisposableBean {

    public LifeCycleBean() {
        System.out.println("1. 实例化：构造器");
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("2. BeanNameAware：beanName=" + name);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        System.out.println("3. BeanFactoryAware");
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        System.out.println("4. ApplicationContextAware");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("5. @PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("6. InitializingBean.afterPropertiesSet");
    }

    public void customInit() {
        System.out.println("7. customInitMethod");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("8. @PreDestroy");
    }

    @Override
    public void destroy() {
        System.out.println("9. DisposableBean.destroy");
    }

    public void customDestroy() {
        System.out.println("10. customDestroyMethod");
    }
}
```

```java
@Configuration
public class LifeCycleConfig {

    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
    public LifeCycleBean lifeCycleBean() {
        return new LifeCycleBean();
    }
}
```

运行结果：

```
1. 实例化：构造器
2. BeanNameAware：beanName=lifeCycleBean
3. BeanFactoryAware
4. ApplicationContextAware
5. @PostConstruct
6. InitializingBean.afterPropertiesSet
7. customInitMethod
--- 容器关闭 ---
8. @PreDestroy
9. DisposableBean.destroy
10. customDestroyMethod
```

## 2.3 生命周期的坑

```java
@Component
public class BadExample {
    @Autowired
    private UserRepository userRepository;

    public BadExample() {
        System.out.println(userRepository);
    }
}
```

输出：`null`

**原因**：构造器执行时，属性注入还没发生。生命周期顺序是"先实例化，再属性注入"。

**解决**：用 `@PostConstruct` 或构造器注入。

```java
@Component
public class GoodExample {
    private final UserRepository userRepository;

    public GoodExample(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        System.out.println(userRepository);
    }
}
```

# 三、三种依赖注入方式

## 3.1 字段注入（@Autowired写在字段上）

```java
@Component
public class UserService {
    @Autowired
    private UserRepository userRepository;
}
```

三个缺点：

- **无法声明不可变字段**：`private final` 字段不能在声明时赋值，又没有构造器给它赋值，编译都过不了
- **无法脱离容器测试**：`new UserService()` 之后 `userRepository` 是 `null`，只能用反射注入
- **隐藏依赖关系**：从类的外部看不出它依赖了什么，必须打开源码逐行找 `@Autowired`

> ⚠️ **踩坑**：Spring 官方从 4.0 起就不推荐字段注入，IntelliJ IDEA 会在字段注入处标黄警告 "Field injection is not recommended"。

## 3.2 构造器注入（推荐）

```java
@Component
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

Spring 4.3+ 如果只有一个构造器，`@Autowired` 可以省略：

```java
@Component
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

优点：

- 字段可 `final`，保证不可变性和线程安全
- 依赖关系在构造器参数里显式声明，一眼看清
- 测试时直接 `new UserService(mockRepo)` 即可

> ⚠️ **踩坑**：构造器注入 + 循环依赖 = 直接报错。字段注入可能通过三级缓存绕过，但循环依赖本身就是设计问题，字段注入只是掩盖了问题，不是解决了问题。

## 3.3 Setter注入

```java
@Component
public class UserService {
    private UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

适用场景：可选依赖——没有这个依赖也能工作，设了更好，不设也行。

## 3.4 三种方式对比表

| 对比维度 | 字段注入 | 构造器注入 | Setter注入 |
|---------|---------|-----------|-----------|
| 可否 final | ❌ | ✅ | ❌ |
| 依赖可见性 | 隐藏 | 显式 | 显式 |
| 测试友好度 | 差（需反射） | 好（直接传参） | 好（调用setter） |
| 可选依赖 | 不适合 | 不适合 | ✅ 适合 |
| 循环依赖 | 可能绕过 | 直接报错 | 可能绕过 |
| 官方推荐 | ❌ 不推荐 | ✅ 推荐 | 可选依赖时用 |

# 四、@Autowired的歧义与解决

## 4.1 歧义场景

```java
public interface NotificationService {
    void send(String message);
}
```

```java
@Component
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}
```

```java
@Component
public class SmsNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("SMS: " + message);
    }
}
```

```java
@Component
public class AlertService {
    @Autowired
    private NotificationService notificationService;
}
```

报错信息：

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Field notificationService in com.example.AlertService required a single bean, but 2 were found:
    - emailNotificationService: defined in file [...]
    - smsNotificationService: defined in file [...]

Action:

Consider marking one of the beans as @Primary or using @Qualifier.
```

## 4.2 @Primary

在首选实现类上加 `@Primary`：

```java
@Component
@Primary
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}
```

此时 `@Autowired` 会优先注入标了 `@Primary` 的 Bean。

## 4.3 @Qualifier

在注入点指定 Bean 名字：

```java
@Component
public class AlertService {
    @Autowired
    @Qualifier("smsNotificationService")
    private NotificationService notificationService;
}
```

## 4.4 坑

- **优先级**：`@Primary` 是全局优先，`@Qualifier` 是局部指定。同时存在时 `@Qualifier` 优先——局部指令覆盖全局默认值
- **Bean 名字规则**：默认是类名首字母小写。`EmailNotificationService` → `emailNotificationService`，`SMSNotificationService` → `sMSNotificationService`（注意第二个字母大写时首字母不大写，这是 Spring 遵循 JavaBeans 规范的 Introspector.decapitalize 行为）

# 五、Bean的作用域

## 5.1 singleton（默认）

整个容器只有一个实例。Spring 默认就是 singleton。

> ⚠️ **踩坑**：singleton Bean 里注入 prototype Bean，prototype 不会每次都新建。

```java
@Component
@Scope("prototype")
public class PrototypeBean {
    private String id = UUID.randomUUID().toString();

    public String getId() {
        return id;
    }
}
```

```java
@Component
public class SingletonBean {
    @Autowired
    private PrototypeBean prototypeBean;

    public void printId() {
        System.out.println(prototypeBean.getId());
    }
}
```

调用两次 `singletonBean.printId()`，输出同一个 id——prototype 失效了。

**原因**：singleton Bean 只创建一次，属性注入也只发生一次，所以 `prototypeBean` 永远是同一个对象。

**解决**：用 `ObjectProvider` 或 `@Lookup`。

```java
@Component
public class SingletonBean {
    @Autowired
    private ObjectProvider<PrototypeBean> prototypeBeanProvider;

    public void printId() {
        PrototypeBean bean = prototypeBeanProvider.getObject();
        System.out.println(bean.getId());
    }
}
```

或者：

```java
@Component
public abstract class SingletonBean {
    @Lookup
    public abstract PrototypeBean getPrototypeBean();

    public void printId() {
        System.out.println(getPrototypeBean().getId());
    }
}
```

## 5.2 prototype

每次 `getBean()` 都新建一个实例。

```java
@Component
@Scope("prototype")
public class PrototypeBean {
}
```

## 5.3 Web作用域

| 作用域 | 说明 |
|-------|------|
| `request` | 每个 HTTP 请求一个实例，请求结束销毁 |
| `session` | 每个 HTTP Session 一个实例，Session 过期销毁 |
| `application` | ServletContext 生命周期内一个实例 |

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedBean {
}
```

> ⚠️ **踩坑**：Web 作用域的 Bean 被 singleton Bean 注入时，必须加 `proxyMode = ScopedProxyMode.TARGET_CLASS`，否则启动报错 `Scope 'request' is not active for the current thread`。因为 singleton 在启动时就创建，此时没有 HTTP 请求上下文。

# 六、循环依赖与三级缓存

## 6.1 什么是循环依赖

A 依赖 B，B 依赖 A。

```java
@Component
public class A {
    @Autowired
    private B b;
}
```

```java
@Component
public class B {
    @Autowired
    private A a;
}
```

## 6.2 三级缓存机制

| 缓存 | 名称 | 存什么 |
|------|------|--------|
| 一级 | `singletonObjects` | 完全初始化好的 Bean |
| 二级 | `earlySingletonObjects` | 提前暴露的早期引用（可能是代理对象） |
| 三级 | `singletonFactories` | Bean 的 ObjectFactory，调用它才能拿到早期引用 |

为什么要三级而不是两级？因为要延迟决定"这个 Bean 到底要不要生成代理"。如果只有两级，不管有没有被循环依赖引用，都得提前创建代理，打破了 Spring 的设计原则（代理应该在最后一步 `postProcessAfterInitialization` 创建）。

## 6.3 解决流程（A→B→A）

1. 创建 A，实例化后把 A 的 `ObjectFactory` 放入**三级缓存**
2. A 属性注入时发现需要 B，去创建 B
3. B 实例化后属性注入发现需要 A，从**三级缓存**拿到 A 的 `ObjectFactory`，调用 `getObject()` 得到 A 的早期引用，放入**二级缓存**，移除三级缓存中的 A
4. B 完成初始化，放入**一级缓存**
5. A 属性注入 B 完成，完成初始化，放入**一级缓存**，移除二级缓存中的 A

## 6.4 哪些循环依赖解决不了

**构造器注入的循环依赖**：

```java
@Component
public class A {
    private final B b;

    public A(B b) {
        this.b = b;
    }
}
```

```java
@Component
public class B {
    private final A a;

    public B(A a) {
        this.a = a;
    }
}
```

报错信息：

```
***************************
APPLICATION FAILED TO START
***************************

Description:

The dependencies of some of the beans in the application context form a cycle:

┌─────┐
|  a (field private com.example.B com.example.A.b)
↑     ↓
|  b (field private com.example.A com.example.B.a)
└─────┘

Action:

Relying upon circular references is discouraged and they are prohibited by default. Update your application to remove the dependency cycle.
```

**原因**：构造器注入在实例化阶段就需要对方，此时还没进入三级缓存阶段，Spring 根本拿不到早期引用。

**prototype 的循环依赖也不行**：

```
BeanCurrentlyInCreationException: Error creating bean with name 'a': Requested bean is currently in creation: Is there an unresolvable circular reference?
```

**原因**：Spring 只对 singleton Bean 做三级缓存管理，prototype 每次都新建，没有缓存机制。

# 七、条件装配

## 7.1 @Conditional

```java
public class LinuxCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty("os.name").contains("Linux");
    }
}
```

```java
public class WindowsCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty("os.name").contains("Windows");
    }
}
```

```java
@Configuration
public class ConditionConfig {

    @Bean
    @Conditional(LinuxCondition.class)
    public String linuxCommand() {
        return "ls -la";
    }

    @Bean
    @Conditional(WindowsCondition.class)
    public String windowsCommand() {
        return "dir";
    }
}
```

## 7.2 Spring Boot的常用条件注解

| 注解 | 作用 |
|------|------|
| `@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")` | 配置项为指定值时才注册 |
| `@ConditionalOnClass(DataSource.class)` | classpath 里存在该类时才注册 |
| `@ConditionalOnMissingBean` | 容器里没有该类型 Bean 时才注册 |

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @ConditionalOnProperty(name = "db.type", havingValue = "mysql")
    public DataSource mysqlDataSource() {
        return new MysqlDataSource();
    }

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource defaultDataSource() {
        return new H2DataSource();
    }
}
```

# 八、常见坑汇总

## 坑1：@Autowired注入null

**现象**：字段标注了 `@Autowired`，运行时却是 `null`，抛 `NullPointerException`。

```java
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public void doSomething() {
        userRepository.findAll();
    }
}
```

```java
UserService service = new UserService();
service.doSomething();
```

**报错信息**：

```
java.lang.NullPointerException: Cannot invoke "com.example.UserRepository.findAll()" because "this.userRepository" is null
```

**原因**：`new` 出来的对象不受 Spring 容器管理，`@Autowired` 不会生效。

**解决**：从容器获取 Bean，而不是自己 `new`。

```java
UserService service = context.getBean(UserService.class);
```

## 坑2：Bean覆盖

**现象**：两个同类型的 Bean 用了相同的名字，后注册的覆盖先注册的，行为不符合预期。

```java
@Configuration
public class ConfigA {
    @Bean
    public UserRepository userRepository() {
        return new JdbcUserRepository();
    }
}
```

```java
@Configuration
public class ConfigB {
    @Bean
    public UserRepository userRepository() {
        return new JpaUserRepository();
    }
}
```

Spring Boot 2.1+ 默认禁止 Bean 覆盖，报错信息：

```
***************************
APPLICATION FAILED TO START
***************************

Description:

The bean 'userRepository', defined in class path resource [ConfigB.class], could not be registered. A bean with that name has already been defined in class path resource [ConfigA.class] and overriding is disabled.

Action:

Consider renaming one of the beans or enabling overriding by setting spring.main.allow-bean-definition-overriding=true.
```

**原因**：两个 `@Bean` 方法返回了相同名字的 Bean，后注册的试图覆盖先注册的。

**解决**：给 Bean 取不同名字，或在配置中 `spring.main.allow-bean-definition-overriding=false`（默认就是 false，别改成 true 掩盖问题）。

## 坑3：@Value注入失败

**现象**：`@Value("${db.url}")` 注入失败。

```java
@Component
public class DbConfig {
    @Value("${db.url}")
    private String dbUrl;
}
```

**报错信息**：

```
java.lang.IllegalArgumentException: Could not resolve placeholder 'db.url' in value "${db.url}"
```

**原因**：配置文件或环境变量中没有 `db.url` 这个属性。

**解决**：确保属性存在，或给默认值：

```java
@Value("${db.url:jdbc:mysql://localhost:3306/default}")
private String dbUrl;
```

## 坑4：事件顺序错乱

**现象**：`@PostConstruct` 里调用的 Bean 的方法返回了意外结果。

```java
@Component
public class CacheManager {
    private Map<String, String> cache = new HashMap<>();

    @PostConstruct
    public void init() {
        cache.put("key", "value");
    }

    public String get(String key) {
        return cache.get(key);
    }
}
```

```java
@Component
public class UserService {
    @Autowired
    private CacheManager cacheManager;

    @PostConstruct
    public void init() {
        String val = cacheManager.get("key");
        System.out.println(val);
    }
}
```

输出可能是 `null`。

**原因**：`@PostConstruct` 的执行顺序取决于 Bean 的创建顺序。如果 `UserService` 先于 `CacheManager` 初始化，`CacheManager` 的 `@PostConstruct` 还没执行，缓存还没填充。

**解决**：不要在 `@PostConstruct` 中依赖其他 Bean 的初始化结果。用 `@DependsOn` 强制顺序，或改用 `ApplicationListener<ContextRefreshedEvent>` 在容器完全就绪后执行。

```java
@Component
@DependsOn("cacheManager")
public class UserService {
    @Autowired
    private CacheManager cacheManager;

    @PostConstruct
    public void init() {
        System.out.println(cacheManager.get("key"));
    }
}
```

## 坑5：Profile未激活

**现象**：标了 `@Profile("dev")` 的 Bean 没有被注入，报 `NoSuchBeanDefinitionException`。

```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource dataSource() {
        return new H2DataSource();
    }
}
```

**报错信息**：

```
NoSuchBeanDefinitionException: No qualifying bean of type 'com.example.DataSource' available
```

**原因**：没有激活 `dev` profile，Spring 跳过了这个配置类。

**解决**：启动时激活 profile：

```bash
java -jar app.jar --spring.profiles.active=dev
```

或在 `application.properties` 中：

```properties
spring.profiles.active=dev
```
