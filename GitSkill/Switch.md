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

## 如果想要丢弃修改，见[[Restore]]