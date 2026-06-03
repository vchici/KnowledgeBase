# 生命周期

`init()`（初始化，默认第一次请求时延迟加载）

`service()`（处理请求，根据 HTTP 动词分发给 `doGet`/`doPost`）

`destroy()`（销毁）

# 线程安全

Servlet 绝不是线程安全的！

Tomcat对同一个Servlet类只创建一个单例，当10000个用户并发访问，Tomcat 会从线程池里分配 10,000 个物理线程，**同时并发执行这同一个 Servlet 实例的 `service()` 方法**。

