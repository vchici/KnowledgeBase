# 已有知识基线：dailyreport 本身就是一个 mini harness

用户独立开发了 dailyreport（Python / pip 包 / MCP server），其中已包含：LLM 意图解析（text_parser.py）、双路加权检索（retriever.py）、上下文截断（chat_history 仅保留 8 轮，README 明言「避免历史过长稀释注意力」）、按日期持久化记忆（storage.py）、MCP 工具面（12 个工具）。

**Evidence**: 项目源码与 README（/Users/savior/Documents/project/dailyreport），用户在首次访谈中直接以该项目为锚点提出学习目标。

**Implications**:
- 不要教 MCP 基础、LLM 基础、检索基础——用户都会，且动手能力较强
- 概念教学应始终映射到 dailyreport 的真实模块上（这正是第 1 课的设计）
- 用户缺的是：术语体系（harness / skill / hook / 渐进式披露）与插件工程化经验（frontmatter 规范、目录约定、分发方式）
