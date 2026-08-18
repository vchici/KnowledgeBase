> 工作区的修改想丢弃，用 `git restore`。工作区修改一旦丢弃，无法追回，因为没有commit

# 用暂存区状态覆盖工作区

`git restore <file>` → 恢复到暂存区状态（暂存区没变，只撤销工作区修改）

# 拿 commit 里的版本覆盖工作区

`git restore --source=HEAD <file>` → 恢复到 HEAD 状态（暂存区没变，只撤销工作区修改）

---

> 暂存区的修改想丢弃，用 `git restore`。

# 丢弃暂存区的修改

`git restore --staged <file>` → 暂存区修改被丢弃，工作区修改不影响

> 可以重新 git add 恢复，但是之前暂存区保留的修改不一定和你现在 git add 导致的结果一致了

# 丢弃暂存区和工作区的修改

`git restore --staged --worktree <file>` → 恢复到 HEAD 状态（丢弃暂存和工作区的修改）

> 工作区的对比记录，是对比暂存区的，如果暂存区为空，对比 HEAD 版本