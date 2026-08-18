# 远程仓库的“同步（Sync fork）”按钮不要随便按

当你点击 GitHub/Gitee 等平台上的“同步（Sync fork）”或“丢弃更改并同步”按钮时，平台实际上是在执行一个强行覆盖（Reset）的操作。

- 它会把上游原仓库（Upstream）的最新状态，强行同步到你的 Fork 仓的对应分支上。
    
- 如果你之前推送到 Fork 仓的 Commit 还没有被上游原仓库合并（Merge），那么在同步的那一瞬间，**你的 Commit 就会从 Fork 仓的分支线路上被无情抹去**。

在此之后，`git pull --rebase` 会发生不重放的问题。

## `git pull --rebase` 为什么没有重放？

前提：你本地有提交 B、C（C 已 push 到你的 Fork），点了「Sync fork」后远端 Fork 的 `main` 被强推到上游的 E（A → D → E），C 在远端被抹掉。

此时本地执行 `git pull --rebase`（本质 = `git fetch` + `git rebase`）：

1. **Fetch**：拉取远端最新状态，本地 `origin/main` 从 C 更新为 E。

2. **Rebase 先要确定「哪些本地提交需要重放」**：本应重放本地独有、远端没有的提交（也就是 B、C）。

3. **关键点——fork-point（分叉点）机制**：`git pull --rebase` 默认开启 `--fork-point`。Git 会去翻 `origin/main` 的 reflog，寻找「它最近一次指向过、且是当前本地分支祖先的提交」。因为你之前 push 过 C，reflog 里就躺着 C，于是 Git 把分叉点误判成 C。

4. **重放范围变成 `C..本地分支`**：而你的本地分支尖恰好就是 C，`C..C` 是空集 → Git 认为「没有提交需要重放」，直接把分支指到 E。

5. **结果**：B、C 没有被重放、从分支上消失，本地变成和远程一模一样的 E。

注意：这和 `patch-id` 是两码事。`patch-id` 是「你的改动已被上游以相同内容合并（如 PR 被 squash/cherry-pick）时跳过重复提交」；而同步 Fork 后提交消失，主因是 fork-point 把分叉点误判到了已 push 的 C 上。提交并没有真正丢失，reflog 里还留着（见下一节）。想避免这种情况，可用 `git pull --rebase --no-fork-point` 强制从真正的合并基点重放。

## 但是，Git Reflog可以救场

只要你在本地成功执行过 `git commit`，Git 的引用日志（Reflog）就会记录下你当时那个 Commit 的 SHA-1 哈希值。这是你的后悔药。

执行 `git reflog` 命令，会看到一个类似下面的列表（从新到旧排列）：

```bash
7a2b3c4 HEAD@{0}: pull --rebase: checkout origin/main
1f4e5d6 HEAD@{1}: commit: feat: 完成了某个核心功能  <-- 盯紧这个！这就是你丢失的提交
a1b2c3d HEAD@{2}: checkout: moving from dev to main
```

然后cherry pick即可。