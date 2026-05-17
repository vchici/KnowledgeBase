# 场景

同一份代码/文件，同时想要提交到两个独立分支

# 解法

如果你已经把文件在 `main`（GitCode）分支上**写好并且 commit 了**。现在想让 `github` 分支也一模一样拥有这些文件：

## 解法一
### 切到 github 分支 

```PowerShell
git switch github
```

(此时文件夹里的新文件确实消失了，别慌)

### 用一条命令把 `main` 分支的那个文件/文件夹强行“拽”过来

```PowerShell
# 格式：git checkout <来源分支> <文件或文件夹路径>
git checkout main src/components/xxx

# 如果是想把整个项目的最新状态都拽过来覆盖：
git checkout main .
```

**结果：** 消失的文件瞬间重新出现在你的文件夹里，并且自动处于“待提交（绿色）”状态。

### 最后正常提交并推送

```PowerShell
git add .
git commit -m "feature: News initial"
git push github github:main
```

## 解法二

## 用 `git stash` “暂存口袋”法

如果你刚刚在 `main` 分支把文件写好了，但**两边都还没运行 `git commit`**。你想让这两个分支分别拥有这笔修改并各自提交：

```PowerShell
git stash
```

git stash 只针对没有暂存的文件

## 切到 `github` 分支，把口袋里的修改拿出来：

```PowerShell
git switch github
# pop 代表放出来的同时，把口袋清空，apply 代表应用但不清空口袋
git stash apply
```

(此时修改来到了 github 分支，你可以正常 add、commit、push 走 GitHub 流程)

## 再切回 `main` 分支，再次把修改拿出来（如果有备份的话）

 `git stash pop` 清空口袋

## 解法三

使用 `cherry-pick`（拣选）法（适合：多笔 Commit 精准复刻）

你在 `main` 分支写好了功能，正常 `git add` 和 `git commit -m "feat: news feature"`。

运行 `git log -n 1 --oneline` 复制这笔提交的哈希值（比如 `c4d5e6f`）。

运行 `git push gitcode main:main` 先把进度推给 GitCode。

切换分支：`git switch github`。

运行：

```PowerShell
git cherry-pick c4d5e6f
```

Git 会把 `main` 分支上的那笔修改完全复制一份到 `github` 分支上，生成一笔全新的、作者是 GitHub 账号的 Commit。你直接 `git push github github:main` 即可。