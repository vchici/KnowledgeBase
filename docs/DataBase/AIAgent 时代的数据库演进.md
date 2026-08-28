> AI Native、向量化、图谱化以及高并发 Agent 写入场景

# 数据库八股知识点应该更新的四大核心方向:

过去面试必问索引、事务

现在面试 Agent 或 AI 基础架构，必问 [向量数据库](./向量数据库.md)（Vector DB）和 知识图谱（Knowledge Graph）

它们是 Agent 解决 Hallucination（幻觉）和实现 RAG（检索增强生成）的基石。

## 向量数据库的核心八股

传统必问： MySQL 索引的底层原理（B+ 树）。

Agent 时代必问： 向量索引的底层原理与近似最近邻搜索（ANN）。

## 知识图谱与 Graph RAG

**传统必问：** 关系型数据库的多表 Join 性能优化。

**Agent 时代必问：** **Graph RAG 如何解决长文本和复杂实体关系的推理？**

# 从“人读写”到“高频 Agent 读写”

传统的数据库设计是基于“人类用户的行为模式”（读多写少，有明显的并发波峰波谷）。但 Agent 是一种 **AI 密集型客户端**，它的行为特征是：超高频的思考链路记录（Thought Trajectory）、长文本上下文存储、以及不知疲倦的 24 小时高并发读写。

## 状态与记忆的“冷热分离”八股

**传统必问：** 传统数据库的冷热数据分离（如将 3 个月前的订单移入 HDFS/ClickHouse）。

**Agent 时代必问：** **Agent 的短期记忆（Short-term Memory）与长期记忆（Long-term Memory）如何设计存储架构？**

## 长文本/JSON 的新型存储优化

传统必问： 为什么不建议在 MySQL 中存文本（TEXT/BLOB）？（会导致行溢出，影响 B+ 树页聚簇效率）。

**Agent 时代必问：** Agent 的反思日志（Reflexion Logs）和复杂的 Tool 交互返回值都是庞大的 JSON，怎么存？