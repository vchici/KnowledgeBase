# Skill设计思想：渐进式披露

# 引入这一中间层，本质是为了解决“大语言模型（LLM）的机器特性”与“Android 复杂的底层开发环境”之间的断层。

中间层通常由 **Android CLI（命令行界面）、Android Skills（结构化技能规范）和实时知识库**，工具和这个三件套进行交互

Android CLI：命令集

# 中间层具体包含了什么

## 统一接口层：Android CLI 命令行工具

原本的 Android 命令行工具（如 `sdkmanager`, `avdmanager`, `adb`, `gradlew`）极其零散且参数复杂，大模型在使用时极易拼错参数或搞混依赖。 Android CLI 将这些全部收拢，转译成了**对 Agent 极其友好的高内聚命令**：

- **`android create`**：一键从官方模板脚手架（如 `empty-activity-agp-9`）生成新项目。
- **`android emulator`**：不再需要复杂的 AVD 参数，直接提供 `medium_phone` 或 `large_desktop` 模板让 Agent 自动启动和管理虚拟设备。
- **`android run`**：自动编译、部署并拉起指定的 Activity 页面。
- **`android layout` & `android screenshot`**：Agent 的“眼睛”。`android layout` **以机器最爱读的 JSON 格式**直接返回当前屏幕的 UI 布局树，比起用大模型去视觉解析图片（Screenshot）能暴省 70% 的 Token，速度提升 3 倍。

## 动态大脑层：Android 专用知识库 (Android Knowledge Base)

专门针对大模型的“外挂 RAG（检索增强生成）”实时文档库。

- **包含内容：** Android 官方最新的 API 变更、最佳实践（Best Practices）、Firebase 与 Kotlin 的最新演进。
- **作用：** 解决大模型知识滞后（Cut-off）的问题。当 Agent 想要调用某个 API 时，中间层工具会自动利用它来进行“上下文注入（Grounding）”，确保 Agent 写出的代码绝对符合当前最新的标准，杜绝大模型瞎编（幻觉）过时 API。

## 任务蓝图层：Android Skills (模块化技能集)

这是整个中间层中最具创新性的部分。它通过遵循 **Agent Skills 开放标准**，把一个个复杂的复杂大任务（如：升级 AGP 9、老项目迁移到 Compose），打包成了一套包含指令指导、模板、甚至配套脚本的“资产包”。

# 中间层的具体作用是什么
## 降低 Token 消耗，把图形界面转化为“机器友好型”接口

### 人类读的日志变成机器读的JSON

传统的命令行里面是冗长的，没有结构化的信息，

Agent必须用Token一行行阅读，靠语意理解猜测出错原因，昂贵且容易漏掉关键报错。

AndroidCLI的改造，让运行结果无论成功失败，都会输出json结构化的数据，

结构化的键值对是大模型的底层母语，无需复杂文本清洗，就可以定位Bug。

### 屏蔽底层工具链的碎屑化参数

原生命令比较细碎，对于每一步操作都有单独的小指令。

Android CLI 将碎屑化的底层命令封装成了高内聚的“原子化意图”，一条简短指令干一连串的活。

Agent不需要记忆成百上千个底层工具的 flag。

### “可机器解析”的 UI 树：暴省 Token 的功臣

`android layout` 命令会返回 JSON 格式的 UI 树，Skill 在处理界面相关的任务时（比如：自动化 UI 测试、无障碍适配检查），JSON 的价值被放大了无数倍：

- 如果给大模型看**手机截图**：多模态大模型需要消耗海量的 Token，而且很难精准定位某个组件的绝对坐标（`X, Y` 轴）。
- 如果给大模型看 **JSON 布局树**：

``` JSON
{
  "component": "Button",
  "id": "btn_submit",
  "clickable": true,
  "bounds": [100, 200, 300, 250]
}
```

这种纯文本的、具有树状嵌套关系的 JSON 代码是它的“最爱”。

它能瞬间在一大堆节点中定位到那个没有写 `contentDescription`（无障碍标签）的 ImageView，并精准实施修复。

## 统一“知识标准”，解决大模型时效性滞后与“幻觉”

Android 生态的技术迭代极其迅速（例如从 Jetpack 升级到 Navigation 3，或者从 XML 转向 Compose 架构），而底层大模型的训练数据往往存在截止日期（Cut-off time）。
- **痛点：** 哪怕是性能最强的 LLM，也经常因为不知道最新的 Android API 规范而写出过时的、无法编译的代码（即“胡说八道”）。
- **中间层的作用：** 三件套中集成了**内置的实时动态知识库**。它作为一个中间“外挂大脑”，实时同步 Android、Firebase 和 Kotlin 的最新文档。这样一来，无论底座大模型的训练数据有多陈旧，只要通过中间层检索，AI Agent 永远能拿到最新的推荐模式（Best Practices）来编写代码。

## 解耦应用层与模型层，打造“即插即用”的开放代理生态

**中间层的作用：** 通过构建统一的中间层，上层的 AI 代理与下层的系统环境实现了解耦。这意味着这个中间层不仅兼容 Google Gemini，还同样支持第三方代理（如 Claude Code、Codex 等）。任何一家的 Agent 只要接入 Android 的标准中间层，都能无缝、安全地调度 Android 的开发工具链。

## 流程化与原子化：将复杂开发拆解为“Markdown 技能书”

**中间层的作用：** 三件套中引入了 **Android Skills (`SKILL.md`)**。这是一套基于 Markdown 的、模块化的结构化指令集。中间层把各种复杂的开发任务（如：适配全面屏、升级 AGP 版本）拆解成规范的、可自动触发的“原子化技能”。当 Agent 收到需求时，中间层会自动匹配并触发对应的技能书，规范 Agent 的每一步操作，从而保证其生成的代码始终符合 Android 官方的工程规范。

