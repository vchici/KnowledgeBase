撤销最近几次 commit 有三种方式，取决于你想保留还是丢弃修改：

---

# 1. `git reset --soft`（保留修改，放回暂存区）

```bash
git reset --soft HEAD~3
```

撤销最近 3 次 commit，但**代码修改保留在暂存区**，可以重新编辑后再提交。

---

# 2. `git reset --mixed`（保留修改，放回工作区）

```bash
git reset --mixed HEAD~3
```

撤销最近 3 次 commit，代码修改**放回工作区**（未暂存状态）。这是默认行为，`--mixed` 可以省略。

---

# 3. `git reset --hard`（彻底丢弃修改 ⚠️）

```bash
git reset --hard HEAD~3
```

撤销最近 3 次 commit，**代码修改全部丢弃**，不可恢复。

---

# 对比

| 命令 | commit 历史 | 代码修改 | 修改状态 |
|------|------------|---------|---------|
| `--soft` | 撤销 | ✅ 保留 | 在暂存区 |
| `--mixed` | 撤销 | ✅ 保留 | 在工作区 |
| `--hard` | 撤销 | ❌ 丢弃 | 无 |

---

# 如果已经 push 过了

`reset` 只改本地历史，如果已经 push 到远程，需要强制推送：

```bash
git push -f
```

但这会影响其他协作者，谨慎使用。

---

# 如果只想撤销某几次（不是最近的）

用 `git revert`，它会创建新的 commit 来"反转"指定 commit 的修改，不改历史：

```bash
git revert commitA commitB
```

这种方式更安全，已经 push 过的仓库推荐用 [[Revert]] 而不是 `reset`。