# Mission: Harness 插件 — 从概念到 dailyreport 集成

## Why
把 dailyreport 从「一个被动的 MCP 工具」升级为 AI 编程工具生态里的「一等公民」：通过 harness 插件（skill / hook）让 TRAE 等工具更聪明地调用它——自动记录工作、自动生成日报。为此先建立 Agent Harness 的概念地基，再掌握插件的制作方法。

## Success looks like
- 能用自己的话讲清 Model vs Harness，以及为什么同一个模型在不同工具里表现差 16 分
- 能独立编写带 YAML frontmatter 的 SKILL.md，并验证它被 AI 工具正确触发
- 为 dailyreport 完成至少一个 TRAE 集成件（如：会话开始自动查看今日状态 / 完成任务后自动记录）
- 能看懂 revfactory/harness 的产出物（`.claude/agents/` + `.claude/skills/`），并判断哪种团队架构适合自己

## Constraints
- 教学语言：中文；偏好「逐步推进 + 代码示例 + 验证步骤」
- dailyreport 是 Python 项目，已发布为 pip 包，自带 MCP server（12 个工具）
- 主力环境是 TRAE CN，Claude Code 生态知识需注意可迁移性

## Out of scope
- 多智能体框架（LangGraph 等）的深入开发
- Claude Code 源码级逆向工程
- 模型训练 / 微调
