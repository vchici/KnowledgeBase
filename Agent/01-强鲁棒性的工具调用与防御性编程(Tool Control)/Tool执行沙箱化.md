**Tool 执行沙箱化 (Sandboxing)**：

- **学习重点**：学习如何使用 `Docker SDK`、`E2B` 或 Linux 的 `cgroups` 为 Agent 提供隔离的执行环境。
    
- **核心痛点**：如果允许 Agent 自主执行 Python 代码、SQL 语句或 Shell 命令，一旦遭遇 Prompt 注入攻击，黑客可以直接清空生产数据库或控制服务器。