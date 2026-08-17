Spring Boot 不是新框架，它没有取代 Spring MVC。它是为了消灭传统 Spring 繁琐的 XML 配置（配置地狱）而诞生的。它的两大核心支柱是：**起步依赖（Starter）** 和 **自动配置（Auto-Configuration）**。

**起步依赖（自动打包）：** 以前要配几十个版本的 Jar 包，现在一个 `spring-boot-starter-web` 把 Tomcat、Spring MVC、JSON 转换器的依赖全家桶自动拉齐。

**自动配置（自动组装）：** 启动类上的 `@SpringBootApplication` 注解内部包含了 `@EnableAutoConfiguration`。项目启动时，Spring Boot 会去扫描类路径下的组件（通过 `META-INF/spring.factories` 机制）。它一旦发现当前环境里有 Web 相关类，就会**自动在内存中把 `DispatcherServlet` 注册给 Tomcat，并反向把内置（Embedded）的 Tomcat 引擎启动起来**。

