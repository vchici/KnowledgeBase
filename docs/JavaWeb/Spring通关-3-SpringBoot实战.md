> Spring Boot 的本质就是：约定大于配置 + 自动装配，让你用最少的代码跑起一个生产级应用。

# 一、Spring Boot是什么：消灭配置地狱

## 1.1 传统Spring的配置地狱

想象一下：你只是想写一个返回"Hello World"的接口，但在传统Spring时代，你需要先填一堆表格才能开工——就像去银行办业务，取个号要填5张表。

一个最简单的Hello World需要的配置文件清单：

| 文件 | 作用 | 大概行数 |
|------|------|---------|
| `web.xml` | 配置DispatcherServlet、ContextLoaderListener | 30+ |
| `spring-mvc.xml` | 配置组件扫描、视图解析器、静态资源处理 | 40+ |
| `spring-context.xml` | 配置数据源、事务管理器、MyBatis等 | 50+ |
| `pom.xml` | 手动声明几十个依赖，还要处理版本冲突 | 100+ |
| `logback.xml` | 日志配置 | 20+ |

加起来200行配置，业务代码0行。这就是"配置地狱"——你花了80%的时间在配环境，只有20%的时间在写业务。

## 1.2 Spring Boot的两大核心

**起步依赖（Starter）**：一个依赖拉齐全家桶。

传统方式你要这样写：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-core</artifactId>
</dependency>
```

Spring Boot只需要：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

一个Starter把上面所有依赖全拉进来，版本号由Spring Boot统一管理，不存在冲突。

**自动配置（Auto-Configuration）**：`@SpringBootApplication`里的`@EnableAutoConfiguration`会根据你引入的依赖自动装配Bean。你加了Starter-Web，它就自动配好DispatcherServlet、内嵌Tomcat、Jackson；你加了Starter-JPA，它就自动配好DataSource、Hibernate。你什么都不用管，开箱即用。

## 1.3 @SpringBootApplication拆解

`@SpringBootApplication`是一个复合注解，等价于下面三个：

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
public class MyApp { }
```

| 注解 | 作用 |
|------|------|
| `@SpringBootConfiguration` | 本质就是`@Configuration`，标记这是一个配置类 |
| `@EnableAutoConfiguration` | 根据classpath里的依赖，自动装配对应的Bean |
| `@ComponentScan` | 扫描当前包及子包下的`@Component`、`@Service`、`@Repository`等 |

一句话总结：**配置类 + 自动装配 + 组件扫描**，三位一体。

---

# 二、第一个REST接口（5分钟实战）

## 2.1 创建项目

打开 [Spring Initializr](https://start.spring.io)，选择：

- Project: Maven
- Language: Java
- Spring Boot: 3.x
- Dependencies: **Spring Web**

点击Generate，下载解压，用IDE打开。

`pom.xml`关键依赖：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

`spring-boot-starter-parent`统一管理了所有依赖的版本号，你不需要写`<version>`。

## 2.2 写第一个接口

**启动类：**

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

**Controller：**

```java
package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
```

启动`DemoApplication`的main方法，浏览器访问 http://localhost:8080/hello ，看到`Hello, Spring Boot!`。

就这么简单——没有web.xml，没有spring-mvc.xml，一个启动类 + 一个Controller，完事。

## 2.3 常见启动失败

**端口8080被占用：**

报错信息：
```
Web server failed to start. Port 8080 was already in use.
```

解决：在`application.properties`中修改端口：

```properties
server.port=8081
```

**依赖下载失败：**

报错信息：
```
Could not transfer artifact org.springframework.boot:spring-boot-starter-web:pom
```

原因：默认从Maven中央仓库下载，国内网络经常超时。配置国内Maven镜像，在`settings.xml`中添加阿里云镜像：

```xml
<mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/central</url>
</mirror>
```

**包结构错误：**

报错信息：
```
NoSuchBeanDefinitionException: No qualifying bean of type 'com.example.demo.controller.HelloController'
```

原因：启动类不在根包下。比如启动类在`com.example.demo`，Controller在`com.example.controller`（少了demo这一层），`@ComponentScan`默认只扫启动类所在包及子包，扫不到。

解决：要么把启动类移到根包，要么手动指定扫描范围：

```java
@SpringBootApplication(scanBasePackages = "com.example")
```

---

# 三、配置文件全解

## 3.1 application.properties vs application.yml

两种格式，功能完全一样，选一个用就行。yml更流行，因为层级缩进更直观。

**properties格式：**

```properties
server.port=8081
app.name=MyApp
app.description=A cool app
```

**yml格式：**

```yaml
server:
  port: 8081
app:
  name: MyApp
  description: A cool app
```

yml的层级缩进就像文件夹结构，一眼就能看出归属关系。**注意：yml的缩进必须用空格，不能用Tab。**

## 3.2 @Value注入配置

```java
@RestController
public class AppController {

    @Value("${app.name}")
    private String appName;

    @GetMapping("/app-name")
    public String getAppName() {
        return appName;
    }
}
```

**坑：** 配置项不存在时启动直接报错：

```
IllegalArgumentException: Could not resolve placeholder 'app.name' in value "${app.name}"
```

解决：给默认值，冒号后面就是默认值：

```java
@Value("${app.name:Unknown}")
private String appName;
```

## 3.3 @ConfigurationProperties（推荐）

`@Value`一个一个注入太累，`@ConfigurationProperties`一把全注入：

```java
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name;
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```

```yaml
app:
  name: MyApp
  description: A cool app
```

使用时直接注入：

```java
@RestController
public class AppController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping("/app-info")
    public String appInfo() {
        return appProperties.getName() + ": " + appProperties.getDescription();
    }
}
```

优点：类型安全（编译期检查）、支持JSR-303校验（`@NotBlank`等）、支持嵌套对象。

**坑：** 必须加`@Component`或用`@EnableConfigurationProperties`注册，否则Bean不创建，注入时`NullPointerException`。

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
}
```

上面这样写，Spring根本不知道这个类的存在。要么加`@Component`，要么在启动类上加：

```java
@EnableConfigurationProperties(AppProperties.class)
```

## 3.4 配置优先级

从高到低：

| 优先级 | 配置源 | 说明 |
|--------|--------|------|
| 1 | 命令行参数 | `--server.port=8081` |
| 2 | 外部配置文件 | jar包同目录下的`application.yml` |
| 3 | jar内配置文件 | `classpath:application.yml` |
| 4 | `@PropertySource` | 代码中指定的配置文件 |

**坑：** 多个配置源有同名key时，高优先级覆盖低优先级。比如你jar内`application.yml`写了`server.port=8080`，启动时命令行加了`--server.port=9090`，最终生效的是9090。这在排查"为什么配置没生效"时非常关键——先查有没有更高优先级的配置源覆盖了你的值。

---

# 四、Profile环境切换

## 4.1 为什么需要Profile

开发用本地数据库，测试用测试库，线上用生产库——三个环境三套配置。总不能每次上线手动改配置文件吧？Profile就是Spring Boot的"换装系统"，一键切换。

## 4.2 使用方式

创建三个配置文件：

`application-dev.yml`：
```yaml
server:
  port: 8080
db:
  url: jdbc:mysql://localhost:3306/dev
```

`application-prod.yml`：
```yaml
server:
  port: 80
db:
  url: jdbc:mysql://prod-server:3306/prod
```

`application.yml`（主配置，指定激活哪个Profile）：
```yaml
spring:
  profiles:
    active: dev
```

启动时也可以命令行指定：

```bash
java -jar app.jar --spring.profiles.active=prod
```

代码中还可以按Profile注册不同的Bean：

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        return new EmbeddedDatabaseBuilder().setType(H2).build();
    }

    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://prod-server:3306/prod");
        return ds;
    }
}
```

## 4.3 坑

**Profile未激活时，`@Profile`注解的Bean不创建。** 如果你忘了在`application.yml`里写`spring.profiles.active=dev`，那`@Profile("dev")`的Bean根本不会被创建，注入时直接`NoSuchBeanDefinitionException`。

**默认Profile是`default`。** 如果你没激活任何Profile，Spring Boot用的是`default`。所以`@Profile("default")`的Bean在没激活Profile时也会生效。

---

# 五、参数绑定全解

## 5.1 @RequestParam

接收URL查询参数：`GET /user?name=Tom&age=20`

```java
@GetMapping("/user")
public String getUser(@RequestParam String name,
                      @RequestParam(required = false, defaultValue = "0") Integer age) {
    return "Name: " + name + ", Age: " + age;
}
```

`required = false`表示参数可选，`defaultValue`给默认值。不加`required = false`时，参数缺失会报400：

```
Required request parameter 'name' is not present
```

## 5.2 @PathVariable

接收URL路径变量：`GET /user/123`

```java
@GetMapping("/user/{id}")
public String getUser(@PathVariable Long id) {
    return "User ID: " + id;
}
```

路径变量是URL的一部分，不是查询参数。`{id}`占位符和`@PathVariable`参数名必须一致，不一致要手动指定：

```java
@GetMapping("/user/{userId}")
public String getUser(@PathVariable("userId") Long id) {
    return "User ID: " + id;
}
```

## 5.3 @RequestBody

接收请求体中的JSON，反序列化为Java对象：`POST /user`

```java
@PostMapping("/user")
public String createUser(@RequestBody UserDTO userDTO) {
    return "Created: " + userDTO.getName();
}
```

```java
public class UserDTO {
    private String name;
    private Integer age;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
```

请求：
```
POST /user
Content-Type: application/json

{"name": "Tom", "age": 20}
```

**坑1：不加`@RequestBody`时参数全为null。** 如果写成：

```java
@PostMapping("/user")
public String createUser(UserDTO userDTO) {
    return "Created: " + userDTO.getName();
}
```

没有`@RequestBody`，Spring会按表单方式绑定参数（`@ModelAttribute`），JSON请求体的数据全绑不上，`name`和`age`都是`null`。

**坑2：JSON字段名和Java字段名不匹配。** 前端传`user_name`，Java里是`userName`，默认匹配不上，结果为`null`。用`@JsonProperty`映射：

```java
public class UserDTO {
    @JsonProperty("user_name")
    private String userName;
}
```

## 5.4 @RequestHeader

获取请求头：

```java
@GetMapping("/header")
public String getHeader(@RequestHeader("Authorization") String token) {
    return "Token: " + token;
}
```

同样支持`required`和`defaultValue`：

```java
@GetMapping("/header")
public String getHeader(@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
    return "Request ID: " + requestId;
}
```

## 5.5 参数校验

引入依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

在DTO上加校验注解：

```java
public class UserDTO {

    @NotBlank(message = "名字不能为空")
    @Size(min = 2, max = 20, message = "名字长度2-20")
    private String name;

    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 150, message = "年龄不能超过150")
    private Integer age;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
```

在Controller中用`@Valid`触发校验：

```java
@PostMapping("/user")
public String createUser(@Valid @RequestBody UserDTO userDTO, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
        return bindingResult.getFieldError().getDefaultMessage();
    }
    return "Created: " + userDTO.getName();
}
```

常用校验注解速查：

| 注解 | 作用 |
|------|------|
| `@NotNull` | 不能为null |
| `@NotBlank` | 不能为null且trim后长度>0（字符串专用） |
| `@NotEmpty` | 不能为null且不能为空（集合、字符串） |
| `@Size` | 长度范围 |
| `@Min` / `@Max` | 数值最小/最大值 |
| `@Email` | 邮箱格式 |
| `@Pattern` | 正则匹配 |

**坑：`@Valid`和`@Validated`的区别。** `@Valid`是JSR-303标准注解，`@Validated`是Spring扩展注解。核心区别：`@Validated`支持**分组校验**，`@Valid`不支持。

分组校验示例：

```java
public interface Create {}
public interface Update {}

public class UserDTO {

    @Null(groups = Create.class, message = "创建时ID必须为空")
    @NotNull(groups = Update.class, message = "更新时ID不能为空")
    private Long id;

    @NotBlank(groups = {Create.class, Update.class}, message = "名字不能为空")
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

```java
@PostMapping("/user")
public String create(@Validated(Create.class) @RequestBody UserDTO userDTO) {
    return "Created";
}

@PutMapping("/user")
public String update(@Validated(Update.class) @RequestBody UserDTO userDTO) {
    return "Updated";
}
```

创建时校验Create分组的规则，更新时校验Update分组的规则。

---

# 六、统一响应封装

## 6.1 为什么需要统一响应

前后端分离项目，前端需要统一的JSON格式来解析响应。如果每个接口返回格式不一样——有的直接返回字符串，有的返回对象，有的报错返回HTML——前端解析会崩溃。

统一响应就像快递包装：不管里面装的是书还是衣服，外面都是标准纸箱，贴着统一的面单。

## 6.2 实现代码

```java
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

Controller使用：

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        UserDTO user = new UserDTO();
        user.setName("Tom");
        user.setAge(20);
        return Result.success(user);
    }

    @PostMapping("/user")
    public Result<String> createUser(@RequestBody UserDTO userDTO) {
        return Result.success("创建成功");
    }
}
```

响应示例：

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "name": "Tom",
        "age": 20
    }
}
```

---

# 七、全局异常处理

## 7.1 @RestControllerAdvice + @ExceptionHandler

没有全局异常处理时，Controller抛异常，Spring默认返回一个白板错误页（Whitelabel Error Page），前端拿到一堆HTML，完全没法解析。全局异常处理就像一个"兜底网"，所有Controller抛出的异常都能统一拦截，转成标准JSON响应。

**自定义业务异常：**

```java
public class BusinessException extends RuntimeException {

    private Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() { return code; }
}
```

**全局异常处理器：**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error("服务器内部错误: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error("系统异常");
    }
}
```

Controller中直接抛异常：

```java
@GetMapping("/user/{id}")
public Result<UserDTO> getUser(@PathVariable Long id) {
    if (id < 0) {
        throw new BusinessException(400, "用户ID不能为负数");
    }
    UserDTO user = new UserDTO();
    user.setName("Tom");
    return Result.success(user);
}
```

## 7.2 坑

**异常处理器只处理Controller层抛出的异常，Filter里的异常捕获不到。** 如果你在Filter中抛异常，`@RestControllerAdvice`根本不会触发，客户端会收到Tomcat默认的错误页面。

解决：在Filter中手动处理异常，或者用`ErrorController`兜底：

```java
@RestController
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    public Result<Void> handleError(HttpServletRequest request) {
        Integer status = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String message = (String) request.getAttribute("javax.servlet.error.message");
        return Result.error(status, message);
    }
}
```

**多个`@ExceptionHandler`的匹配顺序：最具体的异常类型优先。** 比如同时有`handleRuntimeException`和`handleBusinessException`，当抛出`BusinessException`时，会匹配`handleBusinessException`，因为`BusinessException`比`RuntimeException`更具体。如果两个Handler的异常类型是平级的（比如`IllegalArgumentException`和`IllegalStateException`），匹配顺序不确定，要避免这种设计。

---

# 八、常用Starter清单

| Starter | 用途 |
|---------|------|
| `spring-boot-starter-web` | Web开发（内嵌Tomcat + Spring MVC + Jackson） |
| `spring-boot-starter-data-jpa` | JPA数据库操作（Hibernate） |
| `spring-boot-starter-data-redis` | Redis操作（Lettuce客户端） |
| `spring-boot-starter-validation` | 参数校验（Hibernate Validator） |
| `spring-boot-starter-test` | 测试（JUnit 5 + Mockito） |
| `spring-boot-starter-actuator` | 应用监控（健康检查、指标暴露） |

---

# 九、常见坑汇总

### 1. 启动类包位置不对→ComponentScan扫不到Bean→NoSuchBeanDefinitionException

**现象：** 启动报错`NoSuchBeanDefinitionException`，Controller或Service注入失败。

**原因：** `@ComponentScan`默认扫描启动类所在包及其子包。如果启动类在`com.example.demo`，而Controller在`com.example.controller`（不在demo的子包下），就扫不到。

**解决：** 把启动类移到根包下（如`com.example`），或手动指定扫描范围：

```java
@SpringBootApplication(scanBasePackages = "com.example")
```

---

### 2. @RestController和@Controller混用→返回视图名而非JSON

**现象：** 接口返回的不是JSON，而是报错页面或视图名解析错误：`CircularViewPathException`或`javax.servlet.ServletException: Could not resolve view with name 'hello'`。

**原因：** `@Controller`默认返回视图名（由视图解析器渲染HTML），只有在方法上加`@ResponseBody`才会返回JSON。`@RestController`等价于`@Controller + @ResponseBody`，所有方法默认返回JSON。

**解决：** 写REST接口统一用`@RestController`，不要用`@Controller`。

---

### 3. 静态资源被拦截→自定义WebMvcConfigurer放行

**现象：** 放在`static/`目录下的CSS、JS、图片访问404。

**原因：** 自定义了拦截器（`HandlerInterceptor`）但没有排除静态资源路径，拦截器把静态资源请求也拦了。

**解决：** 在注册拦截器时排除静态资源路径：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**");
    }
}
```

---

### 4. 日期格式化→默认返回时间戳，需@JsonFormat或全局配置

**现象：** 接口返回的日期是`1704067200000`这样的时间戳，前端无法直接使用。

**原因：** Jackson默认将`Date`和`LocalDateTime`序列化为时间戳。

**解决：** 字段级别用`@JsonFormat`：

```java
public class UserDTO {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
```

全局配置，在`application.yml`中：

```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
```

---

### 5. 跨域问题→@CrossOrigin或全局CorsConfiguration

**现象：** 前端调用接口报错：`Access to XMLHttpRequest at 'http://localhost:8080/user' from origin 'http://localhost:3000' has been blocked by CORS policy`。

**原因：** 浏览器的同源策略限制了跨域请求，后端没有配置CORS。

**解决：** 方法级别加`@CrossOrigin`：

```java
@CrossOrigin(origins = "http://localhost:3000")
@GetMapping("/user")
public Result<UserDTO> getUser() { ... }
```

全局配置：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }
}
```

---

### 6. 请求参数中文乱码→Spring Boot默认UTF-8，但Tomcat的URI编码需配置

**现象：** GET请求的URL中包含中文参数，后端接收到乱码，如`name=???`。

**原因：** Spring Boot的请求体编码默认UTF-8没问题，但Tomcat对URL的解码编码可能不是UTF-8。

**解决：** 在`application.yml`中配置：

```yaml
server:
  tomcat:
    uri-encoding: UTF-8
```

---

### 7. jar包部署读取不到外部文件→用InputStream替代File

**现象：** 本地IDE运行正常，打成jar包部署后，读取classpath下的文件报错：`FileNotFoundException`或`NoSuchFileException`。

**原因：** jar包内的文件不是操作系统意义上的文件，不能用`new File("classpath:xxx")`的方式读取。IDE运行时class文件在磁盘上，可以当File读；jar包里class文件被打包进jar，不是独立文件。

**解决：** 用`InputStream`读取：

```java
@Value("classpath:template.xlsx")
private Resource templateResource;

public void processTemplate() throws IOException {
    try (InputStream is = templateResource.getInputStream()) {
    }
}
```

不要用：

```java
File file = new File("classpath:template.xlsx");
```

这在jar包环境下永远找不到文件。
