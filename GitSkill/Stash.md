# 列出所有的 stash 记录

git stash list

输出会类似于：

```powershell
stash@{0}: WIP on main: 6fa7312 Fix login bug
stash@{1}: WIP on feature-x: a1b2c3d Add user profile
```

这里的 `stash@{0}`、`stash@{1}` 就是每一条暂存记录的 **ID**（数字越小代表越新）。

# 查看某条 stash 的具体改动

## 只查看哪些文件被修改了（默认查看最新的一条，即 stash@{0}）

```powershell
git stash show
```

## 查看指定某一条 stash 修改的文件（例如 stash@{1}）

```powershell
git stash show stash@{1}
# 查看详细的代码改动（显示具体的 diff 差异）
# 加上 `-p`（或 `--patch`）参数，就能看到具体哪一行被删了或加了
git stash show -p stash@{1}
```

## 弹出（应用并删除）指定的 `git stash`

```powershell
git stash pop stash@{1}
```

## 丢掉指定的 `git stash`

```powershell
git stash pop stash@{1}
```

# 如果你不确定这段代码放回去之后会不会和当前代码产生严重的冲突，建议使用 `apply` 代替 `pop`

```powershell
git stash apply stash@{1}
```

# rebase之前

```powershell
git stash -u
```

暂存 modified（已修改）和 untracked（未跟踪）的文件

其实不需要暂存 untracked 的文件，rebase是重放commit，如果 modified 文件没有 commit，rebase会罢工。

untracked 文件不参与rebase的重放commit，但极端情况下，不暂存 untracked 文件，可能导致冲突：

远端仓库被别人提交了一个和你 untracked 文件同名的文件，rebase会和本地文件撞车，此时会提示：error: The following untracked working tree files would be overwritten by merge...

# stash 和 add

stash：临时清空工作区，把半成品代码先藏(塞)起来，跨分支可见

add：将代码放入stage area，明确下一次commit的文件，和分支绑定

