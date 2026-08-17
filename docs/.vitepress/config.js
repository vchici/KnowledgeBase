export default {
  base: "/KnowledgeBase/", // 二级仓库必须配置
  title: "KnowledgeBase",
  description: "个人技术知识库",
  ignoreDeadLinks: true,
  themeConfig: {
    // 导航栏
    nav: [
      { text: "首页", link: "/" }
    ],
    // 侧边栏，在这里挂载你的各个文档目录
    sidebar: [
      {
        text: "AI-Code-Calibration",
        items: [
          { text: "AI 领域拆解", link: "/AI-Code-Calibration/AI-Region-Breakdown" },
          { text: "通用防坑层", link: "/AI-Code-Calibration/General-Anti-Patterns" },
          { text: "Agent & Tool Calling 防坑", link: "/AI-Code-Calibration/Agent-ToolCalling-Anti-Patterns" },
          { text: "结构化输出防坑", link: "/AI-Code-Calibration/StructuredOutput-Anti-Patterns" }
        ]
      },
      {
        text: "设计模式",
        items: [
          { text: "单例模式", link: "/DesignPattern/单例模式" },
          { text: "工厂方法", link: "/DesignPattern/工厂方法" },
          { text: "抽象工厂", link: "/DesignPattern/抽象工厂" },
          { text: "适配器", link: "/DesignPattern/适配器" },
          { text: "装饰器", link: "/DesignPattern/装饰器" },
          { text: "代理模式", link: "/DesignPattern/代理模式" },
          { text: "模板方法", link: "/DesignPattern/模板方法" },
          { text: "策略接口", link: "/DesignPattern/策略接口" }
        ]
      },
      {
        text: "GC",
        items: [
          { text: "内存泄漏", link: "/GC/内存泄漏" },
          { text: "内部类会隐式持有外部类引用", link: "/GC/内部类会隐式持有外部类引用" },
          { text: "方法区", link: "/GC/方法区" }
        ]
      },
      {
        text: "GitSkill",
        items: [
          { text: "CherryPick", link: "/GitSkill/CherryPick" },
          { text: "Clone", link: "/GitSkill/Clone" },
          { text: "LFS", link: "/GitSkill/LFS" },
          { text: "Log", link: "/GitSkill/Log" },
          { text: "Pull", link: "/GitSkill/Pull" },
          { text: "Push", link: "/GitSkill/Push" },
          { text: "Rebase", link: "/GitSkill/Rebase" },
          { text: "Reset", link: "/GitSkill/Reset" },
          { text: "Restore", link: "/GitSkill/Restore" },
          { text: "Revert", link: "/GitSkill/Revert" },
          { text: "Stash", link: "/GitSkill/Stash" },
          { text: "Switch", link: "/GitSkill/Switch" },
          {
            text: "scenario",
            items: [
              { text: "PushDoubleRepo", link: "/GitSkill/scenario/PushDoubleRepo" },
              { text: "ssh config", link: "/GitSkill/scenario/ssh config" }
            ]
          }
        ]
      },
      {
        text: "Health",
        items: [
          {
            text: "血液循环",
            items: [
              { text: "供血功能", link: "/Health/血液循环/供血功能" },
              { text: "爬楼机", link: "/Health/血液循环/爬楼机" },
              { text: "磷虾胶囊", link: "/Health/血液循环/磷虾胶囊" }
            ]
          }
        ]
      },
      {
        text: "JavaWeb",
        items: [
          { text: "Goroutine", link: "/JavaWeb/Goroutine" },
          { text: "Servlet", link: "/JavaWeb/Servlet" },
          { text: "Spring Boot", link: "/JavaWeb/Spring Boot" },
          { text: "Spring MVC", link: "/JavaWeb/Spring MVC" },
          { text: "Spring通关-1-Java内功", link: "/JavaWeb/Spring通关-1-Java内功" },
          { text: "Spring通关-2-IoC-DI", link: "/JavaWeb/Spring通关-2-IoC-DI" },
          { text: "Spring通关-3-SpringBoot实战", link: "/JavaWeb/Spring通关-3-SpringBoot实战" },
          { text: "Spring通关-4-AOP与数据库", link: "/JavaWeb/Spring通关-4-AOP与数据库" },
          { text: "Tomcat Servlet JSP Spring", link: "/JavaWeb/Tomcat Servlet JSP Spring" },
          { text: "Tomcat", link: "/JavaWeb/Tomcat" }
        ]
      },
      {
        text: "LangChain",
        items: [
          { text: "00-大模型API接口规范", link: "/LangChain/00-大模型API接口规范/00-大模型API接口规范" },
          { text: "01-模型初始化", link: "/LangChain/01-模型初始化/01-模型初始化" },
          { text: "02-模型调用", link: "/LangChain/02-模型调用/02-模型调用" },
          { text: "03-在智能体中使用模型", link: "/LangChain/03-在智能体中使用模型/03-在智能体中使用模型" },
          { text: "04-多模态消息", link: "/LangChain/04-消息格式/04-多模态消息" },
          { text: "04-消息", link: "/LangChain/04-消息格式/04-消息" },
          { text: "05-提示词工程", link: "/LangChain/05-提示词工程/05-提示词工程" },
          { text: "06-工具", link: "/LangChain/06-工具/06-工具" },
          { text: "06-预定义工具", link: "/LangChain/06-工具/06-预定义工具" },
          { text: "07-短期记忆", link: "/LangChain/07-短期记忆/07-短期记忆" },
          { text: "08-记忆管理策略", link: "/LangChain/08-记忆管理策略/08-记忆管理策略" },
          { text: "09-私厨管家", link: "/LangChain/09-私厨管家/09" },
          { text: "Prompt模板", link: "/LangChain/Prompt模板" },
          { text: "RAG检索增强生成", link: "/LangChain/RAG检索增强生成" },
          { text: "工具深入", link: "/LangChain/工具深入" },
          { text: "记忆管理", link: "/LangChain/记忆管理" },
          { text: "输出解析器", link: "/LangChain/输出解析器" },
          { text: "链", link: "/LangChain/链" }
        ]
      },
      {
        text: "Magnet",
        items: [
          { text: "快速下载Magnet", link: "/Magnet/快速下载Magnet" }
        ]
      },
      {
        text: "Thread",
        items: [
          { text: "Lock - synchronized", link: "/Thread/Lock/synchronized" }
        ]
      },
      {
        text: "Tool",
        items: [
          { text: "常用命令行工具", link: "/Tool/Windows/常用命令行工具" },
          { text: "快捷键大全", link: "/Tool/Windows/快捷键大全" },
          { text: "操作无法完成（文件夹已在另一程序打开）", link: "/Tool/Windows/操作无法完成，因为其中的文件夹或文件已在另一程序打开" },
          { text: "文件搜索神器Everything", link: "/Tool/Windows/文件搜索神器Everything" },
          { text: "文件权限问题解决", link: "/Tool/Windows/文件权限问题解决" },
          { text: "端口被占用如何解决", link: "/Tool/Windows/端口被占用如何解决" },
          { text: "系统卡顿排查与优化", link: "/Tool/Windows/系统卡顿排查与优化" },
          { text: "配置环境变量", link: "/Tool/Windows/配置环境变量" }
        ]
      },
      {
        text: "UML",
        items: [
          { text: "时序图", link: "/UML/时序图" },
          { text: "类图", link: "/UML/类图" }
        ]
      },
      {
        text: "VPN",
        items: [
          { text: "config", link: "/VPN/config" },
          { text: "hide", link: "/VPN/hide" },
          { text: "scan", link: "/VPN/scan" }
        ]
      }
    ]
  }
}
