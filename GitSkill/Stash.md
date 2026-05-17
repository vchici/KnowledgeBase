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
## 如果你不确定这段代码放回去之后会不会和当前代码产生严重的冲突，建议使用 `apply` 代替 `pop`

```powershell
git stash apply stash@{1}
```

