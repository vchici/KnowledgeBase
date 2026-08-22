# 概念

项目管家

# 作用

## 依赖管理

Maven 给每个 Jar 包定义了一个唯一坐标 (groupId, artifactId, version)

在 `pom.xml` 中声明需要的依库 Maven 自动从远程仓库下载，如果 A 依赖 B，B 依赖 C，Maven 会自动把 B 和 C 都下载下来，无需人工干预

## 生命周期

Maven 将项目的构建过程抽象为三个固定的生命周期：clean（清理）、default（构建）、site（生成文档）

常用的命令对应着不同的阶段：
`mvn compile`：编译主代码
`mvn test`：执行单元测试
`mvn package`：打包（生成 Jar 或 War 包）
`mvn install`：将打包好的文件安装到本地仓库，供其他项目依赖

## POM (Project Object Model)

项目**说明书**

不仅包含依赖列表，还定义项目基本信息，插件配置，模块关系等

## 趋势

虽然 Maven 是行业标准，但目前 Gradle​ 正在成为新的趋势

Maven：XML 配置冗长，但稳定、规范，学习曲线平滑，适合传统企业级项目

Gradle：基于 Groovy/Kotlin DSL，语法简洁灵活，构建速度更快，是 Android 开发的官方标配，也是 Spring 等现代开源项目的首选