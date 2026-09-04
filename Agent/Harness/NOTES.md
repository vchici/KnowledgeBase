# NOTES

## 教学偏好（来自用户画像 + 首次访谈）

- 中文教学，逐步推进，配代码示例与验证步骤
- 喜欢把新概念锚定在自己已有的项目上（dailyreport 是主要锚点）
- 技术背景：Python 熟练、FastAPI / LLM API / MCP 都用过，不需要从零科普

## 首次访谈结论（2026-09-04）

- 「harness 插件」= 概念 + 插件制作两条线都要，但核心目标是**应用到 dailyreport**
- 扩展方向选定：**TRAE/IDE 集成**（为 dailyreport 配 skill/hook，让 AI 工具更聪明地调用），而非重构 dailyreport 内部架构
- 课程主线因此定为：概念（1 课）→ 插件解剖（1 课）→ 动手为 dailyreport 写 skill（1 课）→ hooks 自动化（1 课）→ 团队架构模式选型（1 课）

## 工作区约定

- 课程文件在 `lessons/`，速查在 `reference/`，共享样式 `assets/course.css`，测验组件 `assets/quiz.js`
- 术语统一见 `GLOSSARY.md`，课程与讲解均以它为准
- dailyreport 项目路径：`/Users/savior/Documents/project/dailyreport`（教学时引用真实代码）

## 待验证

- TRAE CN 中 SKILL.md 的触发行为是否与 Claude Code 一致（第 3 课动手时验证）
