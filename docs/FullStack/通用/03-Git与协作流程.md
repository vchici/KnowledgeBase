# Git 与协作流程

> Git 是团队协作的基本功，直接决定工作效率与代码安全。

## 核心概念

- 工作区 → 暂存区（index）→ 本地仓库 → 远程仓库
- 关键命令：`add`、`commit`、`push`、`pull`、`fetch`、`merge`、`rebase`、`stash`
- 关联笔记：知识库 [GitSkill](../../GitSkill/) 目录有每个命令的实操详解

## 常用操作速查

| 场景 | 命令 |
|------|------|
| 查看状态/差异 | `git status`、`git diff` |
| 撤销工作区修改 | `git restore <file>` |
| 撤销已暂存 | `git restore --staged <file>` |
| 回退提交（保留历史） | `git revert <commit>` |
| 回退提交（重写历史） | `git reset --hard <commit>` |
| 保存临时改动 | `git stash` / `git stash pop` |
| 摘取指定提交 | `git cherry-pick <commit>` |
| 提交信息规范 | `feat:` / `fix:` / `docs:` / `refactor:` |

> 关联笔记：[Restore](../../GitSkill/Restore.md)、[Reset](../../GitSkill/Reset.md)、[Revert](../../GitSkill/Revert.md)、[Stash](../../GitSkill/Stash.md)、[CherryPick](../../GitSkill/CherryPick.md)

## 分支策略

### 主流模式

```
main ──────── 稳定发布分支，只接受合并
  └── develop ── 集成开发分支
        └── feature/xxx ── 功能分支（从 develop 拉出）
        └── fix/xxx ── 缺陷修复分支
        └── release/x.x ── 发布准备分支
```

- **功能分支**：每次改动开分支，禁止直接在主干提交
- **PR/MR + Code Review**：合并前必须审查
- **保护分支**：main 禁止直接推送，只能通过 MR 合并

### merge vs rebase

| | merge | rebase |
|---|---|---|
| 历史 | 保留分叉，有合并节点 | 线性化，无合并节点 |
| 优点 | 保留真实协作历史 | 历史整洁 |
| 适用 | 公共分支合并 | 本地提交整理、功能分支跟上主线 |

> 注意：rebase 会改写提交，**不要对已推送到共享仓库的分支使用**。

## 团队协作规范

1. **提交信息规范**：Conventional Commits（`feat(模块): 描述`）
2. **小步提交**：一次提交只做一件事，便于 review 和 revert
3. **冲突处理**：先 pull（rebase）再 push；冲突按模块协商，不随意丢弃他人代码
4. **Code Review 要点**：
   - 逻辑正确性与边界情况
   - 安全（注入、越权、敏感信息）
   - 性能（SQL、循环、大对象）
   - 可维护性（命名、职责、测试）
5. **代码规范自动化**：ESLint/Prettier + husky + lint-staged，提交前自动检查

## 常见面试题

1. merge 和 rebase 的区别？什么场景用哪个？
2. 如何解决冲突？
3. revert 和 reset 的区别？
4. 一个分支开发到一半，如何切到另一个分支改 bug？（stash）
5. 团队分支策略如何设计？
