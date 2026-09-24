# 从远程分支创建本地分支

远程分支本地操作不了，得先让本地知道它存在——`git fetch` 把远程的分支信息同步成本地的远程跟踪分支（`origin/*`），然后：

```bash
git switch <branch>                      # 推荐：本地没有该分支时，自动基于 origin/<branch> 创建并建立跟踪
git switch -c <branch> origin/<branch>   # 显式写法
git checkout <branch>                    # 老写法，行为相同
```

`git switch <branch>` 发现本地没有这个分支、但恰好只有一个远程有 `origin/<branch>` 时，等价于 `git switch -c <branch> --track origin/<branch>`，起点都不用自己写。该行为由 `checkout.guess` 控制（默认开启）。如果有多个远程有，则命令执行失败 `fatal: 'weekly/260910' matched multiple (2) remote tracking branches` 。

## 跟踪（upstream）关系

起点是 `origin/*` 时默认自动建立跟踪，之后 `git status` 能显示 ahead/behind，`git pull` / `git push` 不带参数就知道拉谁、推谁。本地名字和远程不同也一样：

```bash
git switch -c feature origin/dev   # 本地 feature 跟踪 origin/dev
```

不想自动跟踪加 `--no-track`；分支已存在但没建跟踪关系，事后补：

```bash
git branch -u origin/<branch> <branch>   # 或 --set-upstream-to
```

查看所有跟踪关系：`git branch -vv`。

# 删除本地分支

删除分支不归 switch/checkout 管，由 `git branch` 负责：

```bash
git branch -d <branch>   # 只删已合并的分支（安全）
git branch -D <branch>   # 强制删，即使未合并
```

- 不能删除当前所在的分支，先[切换](./Switch.md)走再删
- `-d` 删除前会检查这个分支上的提交都能从别处（上游分支或 HEAD 所在分支）沿提交图走到，未合并则拒绝执行，防止丢提交；确认真的不要了才用 `-D`
- 删分支只是删掉一个指针，提交本体还在仓库里。误删了可用 `git reflog` 找到 SHA 后 `git branch <branch> <sha>` 重建，见[Pull](./Pull.md#但是git-reflog可以救场)
- 删除本地分支不影响远程分支，删远程走的是 push，见[Push](./Push.md#删除远程分支)
