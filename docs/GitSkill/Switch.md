# 切换分支

`git switch <branch>` = `git checkout <branch>`

`git switch -c <new>` = `git checkout -b <new>`

两者功能相同，但是，最好不要用 checkout 切换分支。

# `git checkout <file>` 会丢弃工作区的修改，将文件恢复到暂存区或 HEAD 的状态：

## 文件已修改，未暂存

`git checkout <file> ` → 恢复到 HEAD 的状态（丢弃修改）

## 文件已修改，已暂存

`git checkout <file>`  → 恢复到暂存区的状态（丢弃工作区修改，保留已暂存的）

这就是为什么 switch 更安全：

```bash
git checkout main      # 切换到 main 分支 ✓
git checkout main.txt  # 误操作：丢弃 main.txt 的修改 ✗
git switch main        # 切换到 main 分支 ✓
git switch main.txt    # 报错，不会执行任何操作 ✓
```

## 如果想要丢弃修改，见[Restore](./Restore.md)

# 删除分支

删除分支不是 switch/checkout 的职责，由 `git branch` 负责：

```bash
git branch -d <branch>   # 删除已合并的分支（安全）
git branch -D <branch>   # 强制删除，即使未合并
```

`-d` 删除前会检查该分支是否已合并，未合并则拒绝执行，防止丢提交；确认真的不要了才用 `-D`。

易混淆：`git switch -d <commit>` 不是删除，而是进入分离 HEAD 状态——检出某个提交但不挂分支。

删除**远程**分支走的是 push 操作，见[Push](./Push.md#删除远程分支)