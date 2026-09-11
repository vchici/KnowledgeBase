# Function Calling 与 MCP

> **本质**：Function Calling 让模型从「会说」到「会做」——把工具的 JSON Schema 放进上下文，模型**输出结构化的调用意图**，**真正的执行永远在应用层代码**。MCP 则解决工具接入的标准化：把 M 个模型 × N 个工具的私有适配变成 M + N。

## 核心考点

### 1. Function Calling 完整流程（必须能逐步讲）

```
1. 应用层定义 tools：[{name, description, parameters(JSON Schema)}]
2. 随请求发给模型 → 模型判断需不需要调工具
3. 需要 → 返回结构化 tool_call（函数名 + JSON 参数，不是执行结果！）
4. 应用层校验参数 → 真正执行（调 API / 查库 / 跑代码）
5. 把执行结果以 role=tool 消息回填 → 再次请求模型
6. 模型基于结果继续：再调用（循环）或生成最终回答
```

- **关键认知**：模型只是「指出该调什么工具、传什么参数」，**执行、权限、安全全在应用层**——这是所有安全设计的根基。

### 2. 工具设计规范（工程细节，面试加分项）

- **description 写给模型看**：说清「什么时候该用、什么时候不该用」、参数含义与格式，模型就是靠这些文本做决策。
- 参数**少而精**，能用枚举就不用自由文本；命名语义化（`search_order` 而非 `f1`）。
- **返回结果 token 友好**：截断、分页、只返回必要字段——工具结果直接进上下文，一个大 JSON 就撑爆窗口。
- 错误信息也是上下文：返回「可读的错误 + 怎么修正」模型才能自愈重试。

### 3. 协议细节速记

- `tool_choice`：auto（默认）/ required（必须调）/ none / 指定函数。
- **并行调用**：一次返回多个 tool_call，可并发执行（注意有依赖时要串行）。
- 多工具选择：工具太多（几十个）时模型会挑花眼 → **检索式工具选择**（按语义先选 top-k 工具再注入）。

### 4. MCP：Model Context Protocol（2025 必考热点）

- **是什么**：Anthropic 推的开放协议，统一「模型 ↔ 工具/数据」的连接方式。工具方实现一次 MCP Server，所有支持 MCP 的客户端（Claude、IDE、自建 Agent）都能用。
- **解决什么**：以前 M 个应用接 N 个工具要写 M×N 个适配器，MCP 收敛成 **M + N**（USB-C 类比）。
- **架构三角色**：**Host**（宿主应用）持 **Client**（1:1 连接），连接 **Server**（提供能力），底层 JSON-RPC（stdio / HTTP+SSE 传输）。
- **三类能力**：
  - **Tools**：模型可调用的动作（类似 FC）；
  - **Resources**：应用可读的数据源（文件、DB 记录）；
  - **Prompts**：预置的提示词模板。
- **MCP vs Function Calling 的关系（高频辨析）**：FC 是**模型的能力**（输出调用意图），MCP 是**工具接入的传输与发现协议**，两者互补不互斥——MCP Server 的 tools 最终仍以 FC 的形式暴露给模型。

### 5. 工程坑清单（实战谈资）

工具结果撑爆上下文（要分页/摘要）；循环调用无终止条件（要有 max steps）；错误被静默吞掉（模型拿着空结果继续幻觉）；执行无超时；危险操作（删库、转账）无人工确认。

## 面试高频速答

### 模型怎么知道有哪些工具、什么时候该调？

<details>
<summary>答案</summary>
工具的 name/description/schema 全部序列化进 system/context，模型在生成时把「调工具」当作一种特殊输出（结构化 JSON）。什么时候调完全由模型基于 description 语义判断——所以工具描述的质量直接决定调用准确率。
</details>

### MCP 解决什么问题？

<details>
<summary>答案</summary>
工具生态的碎片化。每个应用×每个工具都要私有集成，MCP 把它标准化成协议：工具方实现一次 Server，任何支持 MCP 的 Host 即插即用，从 M×N 降到 M+N。同时提供了能力发现（list tools）、资源、模板的统一抽象。
</details>

### 工具返回结果太长怎么办？

<details>
<summary>答案</summary>
设计层面：工具只返回必要字段、支持分页、提供「统计摘要」类参数；执行层面：应用层对结果做截断/摘要（可用小模型压缩）再回填；必要时把大结果落到外部存储，只回填引用，让模型按需再取。
</details>

## 互链

- 结构化输出的原理层：[11-Prompt工程](11-Prompt工程.md)
- 工具循环的放大版：[13-Agent架构](13-Agent架构.md)
- 工具调用反模式实战：[Agent-ToolCalling-Anti-Patterns](../AI-Code-Calibration/Agent-ToolCalling-Anti-Patterns.md)
