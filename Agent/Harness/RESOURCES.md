# Harness 学习资源

> 均已核实（2026-09-04）。标注用途，按需取用。

## Knowledge

### 概念根基

- [文章：Harness as a Service — Viv Trivedy](https://www.vtrivedy.com/posts/claude-code-sdk-haas-harness-as-a-service)
  「Coding Agent = AI Model + Harness」公式的原始出处。用 for：追溯概念的源头表述。
- [文章：Harness Engineering — Ben Zanghi](https://www.benzanghi.com/blog/harness-engineering)
  把 harness 工程总结为四杠杆（指令文件 / 工具与 MCP / 上下文管理 / 子代理防火墙），含 Mitchell Hashimoto 的经典引语。用 for：建立系统性心智模型。
- [文章：The Harness Effect — Daniel Vaughan](https://codex.danielvaughan.com/2026/04/19/the-harness-effect-same-model-different-tool-different-score/)
  同一模型（Claude Opus）在 Cursor 与 Claude Code 中 Terminal-Bench 2.0 相差 16 分的 benchmark 证据。用 for：用数据说服自己和别人「harness 值得投入」。
- [文章：Agent Harness 深度解析（CSDN）](https://blog.csdn.net/qq_38590739/article/details/160920227)
  中文长文，五大件（工具分发 / 权限门控 / 上下文管理 / 记忆 / 编排）拆解详尽。用 for：中文语境下快速复习。

### 插件制作（官方一手资料）

- [官方文档：Claude Code Skills](https://docs.anthropic.com/en/docs/claude-code/skills)
  SKILL.md 的 YAML frontmatter 规范、存放位置（个人 / 项目 / 插件三级）、动态上下文注入（`` !`cmd` ``）、渐进披露机制。已逐条核实。用 for：写 skill 时的最终依据。
- [规范：Agent Skills 开放标准](https://agentskills.io/)
  SKILL.md 是跨工具的开放标准（不绑定 Claude Code）。用 for：理解为什么这套知识可迁移到 TRAE 等其他工具。

### 案例研究

- [仓库：revfactory/harness](https://github.com/revfactory/harness)
  「团队架构工厂」插件本体。一句话生成 agent 团队 + 技能；6 阶段工作流、6 种团队架构模式。用 for：读它的 skills/harness/SKILL.md 学「元技能」怎么写。
- [仓库：revfactory/claude-code-harness](https://github.com/revfactory/claude-code-harness)
  harness 的 A/B 实验 repo：15 个任务，均分 49.5 → 79.3（+60%），胜率 15/15。`.claude/` 四件套结构（CLAUDE.md / skills / agents / commands）的原始出处。用 for：引用数据、看标准目录结构。
- [文章：revfactory/harness 中文解读（掘金）](https://juejin.cn/post/7646235454803345423)
  6 阶段工作流与 6 种架构模式的中文梳理。用 for：快速预览该插件能干什么。
- [仓库：Alchemishty/agent-harness](https://github.com/Alchemishty/agent-harness)
  另一个 harness 插件：含 hooks / enforcement / skills，主打自愈式验证门禁。用 for：对比两种 harness 插件的设计取向。
- [文章：从 Harness 角度对 Claude Code 源码深度解读（51CTO）](https://www.51cto.com/article/839733.html)
  基于泄露源码的 512K 行 TypeScript 分析：30+ 目录 → harness 子系统映射。用 for：想看「工业级 harness 内部长什么样」时读。

## Wisdom (Communities)

- [r/ClaudeCode](https://www.reddit.com/r/ClaudeCode/)
  Claude Code 用户社区，skill / 插件实践讨论活跃。用 for：晒自己的 dailyreport skill 求点评、踩坑求助。
- [revfactory/harness 的 Issues / Discussions](https://github.com/revfactory/harness/issues)
  插件作者直接答疑。用 for：插件使用问题的一手反馈渠道。

## Gaps

- TRAE CN 的 skill/hook 机制与 Claude Code 的差异点：尚无系统性文档，需在动手课时边试边记（将沉淀到 [[NOTES.md]]）。
