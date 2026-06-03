在没有 Spring Boot 之前，每个接口都要手写 Servlet 类并在 `web.xml` 里配 `<servlet-mapping>`（导致类爆炸和配置地狱）。Spring MVC 采用**前端控制器模式**统一收口。

用户请求直达 Tomcat 端口，Tomcat 将其统一分发给 Spring MVC 的核心——**`DispatcherServlet`**。

`DispatcherServlet` 调用 **`HandlerMapping`（处理器映射器）**，根据请求的 URL（比如 `/goods/list`）去匹配，找到能处理它的那个 Controller 方法。

`DispatcherServlet` 拿到对应的执行链后，调用 **`HandlerAdapter`（处理器适配器）** 来统一负责参数拦截、数据绑定（将 HTTP 传参转为 Java 对象）。

最终适配器执行我们写的 **Controller 业务方法**。

（如果是现代前后端分离项目）Controller 返回的 Java 对象，通过 **`HttpMessageConverter`**（如 Jackson）全自动转换为 **JSON 字符串**，顺着 Response 吐回前端。

