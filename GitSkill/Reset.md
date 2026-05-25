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

将Head指针指回之前的某个提交，区别是将被撤销的代码修改，分别吐到哪个区域。

| 命令        | commit 历史 | 代码修改 | 本地修改状态                                                       |
| --------- | --------- | ---- | ------------------------------------------------------------ |
| `--soft`  | 撤销        | ✅ 保留 | 之前已经 `commit` 的文件 $\rightarrow$ 变成已暂存（Staged）状态              |
| `--mixed` | 撤销        | ✅ 保留 | 之前已经 `commit` 的文件 $\rightarrow$ 变成未暂存（Unstaged / Modified）状态 |
| `--hard`  | 撤销        | ❌ 丢弃 | 之前已经 `commit` 的文件 $\rightarrow$ **彻底消失**                     |

$$\text{工作区 (Work Dir)} \xrightarrow{\text{git add}} \text{暂存区 (Stage)} \xrightarrow{\text{git commit}} \text{版本库 (History)}$$

## 为什么有些文件回到了工作区会显示未跟踪的状态？

原因在于：**这个文件在被撤销的那个 `commit` 之前，在 Git 历史中到底“存不存在”。**

我们可以把文件分为两种情况来看：

修改已有文件（显示为 `modified`），因为 Git 的历史记录里认得这个文件，所以它知道这个文件被“修改”了，状态会显示为 **`modified`**

全新添加的文件（显示为 `untracked`），在“过去”的那个时间点，**这个文件在 Git 的世界里根本不存在**。

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