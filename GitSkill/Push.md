# 完整情况

$$\text{格式：}\quad \text{git push [远程仓]} \quad \underbrace{\text{refs/heads/本地分支名}}_{\text{源引用 (Source)}} : \underbrace{\text{refs/heads/远端分支名}}_{\text{目的引用 (Destination)}}$$

如果本地分支名和远端分支名名字不同，只说明远程仓名，防呆保护会拒绝执行，必须显示说明分支名。

# 建立关联

## git push -u <远程仓> <本地分支>:<远端分支>

`-u` 是 `--set-upstream` 的简写。这个命令是一个**复合连招**。

### 经典使用场景

在本地建了一个全新的分支 `feature/login`，写完代码准备第一次推到 GitHub。此时 GitHub 上还没有这个分支呢，你直接运行：

```Bash
# 运行完之后，关系立刻生效
git push -u github feature/login
```

远端有了新分支，本地和远端的追踪关系也顺便建好了。

## git branch --set-upstream-to=<远程仓>/<远端分支> <本地分支>

**纯粹的本地配置命令**。它不跟服务器发生任何代码传输（不涉及 `upload` 或 `download`），它只是修改了你本地的 Git 账本。

### 经典使用场景

你在新电脑上通过 `git fetch github` 拿到了 GitHub 上早就存在的 `main` 分支。而你本地此时刚好有一个重命名好的 `gitcode` 本地分支。你想让本地的 `gitcode` 去追踪 GitHub 的 `main`，但你现在还不想推拉代码，只想先把关系定下来：

```Bash
# 格式：git branch --set-upstream-to=远端名/远端分支 本地分支
git branch --set-upstream-to=github/main gitcode
```

# 建立关联之后

如果没有建立追踪，你每次拉取、推送都必须老老实实写全长命令：

- `git push github gitcode:main`
- `git pull github main`

一旦绑定成功，Git 就会产生记忆。以后你只要在这个分支上，直接输入极其清爽的：

- **`git push`**
- **`git pull`**

Git 会自动在后台帮你补全后面所有的仓库名和分支名。