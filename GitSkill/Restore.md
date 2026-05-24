# 丢弃未暂存的修改

`git restore <file>`              → 恢复到暂存区状态（不改暂存区，只撤销工作区修改）

`git restore --source=HEAD <file>`  → 恢复到 HEAD 状态（不改暂存区，只撤销工作区修改）

> ⚠️ 工作区修改一旦丢弃，无法追回

# 取消暂存

`git restore --staged <file>`      → 暂存区修改被丢弃，工作区修改不影响

> 可以重新 git add 恢复，但是之前暂存区保留的修改不一定和你现在 git add 导致的结果一致了

# 丢弃未暂存和暂存的修改

`git restore --staged --worktree <file>` → 恢复到 HEAD 状态（丢弃暂存和工作区的修改）

> 工作区的对比记录，是对比暂存区的，如果暂存区为空，对比 HEAD 版本