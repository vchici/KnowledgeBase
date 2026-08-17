# Scenario 修改历史某次 commit 的提交信息

```bash
# 简洁查看提交历史，记下要改的那个 commit 的哈希或它是倒数第几个
git log --oneline
```

```
示例输出
a1b2c3d (HEAD -> main) 修复登录bug
e4f5g6h 添加用户模块      ← 假设要改这个
c7d8e9f 初始化项目
```
 
```bash
# 假设要改的是倒数第2个，即 HEAD~2 往前数
git rebase -i HEAD~2

# 编辑器里把目标行改成
reword e4f5g6h 添加用户模块
pick    a1b2c3d 修复登录bug

# 保存退出后，Git 会逐个停在标记为 reword的 commit 上让你重新编辑提交信息
git rebase --continue
```

# Scenario 修改历史某次 commit 的提交内容

```bash
edit e4f5g6h 添加用户模块
pick a1b2c3d 修复登录bug

git add <修改的文件>
git commit --amend --no-edit   # --no-edit 保留原提交信息，去掉则重新编辑

git rebase --continue
```

[提交信息](#scenario-修改历史某次-commit-的提交信息) 和 [提交内容](#scenario-修改历史某次-commit-的提交内容)的修改不会改变已经commit的顺序和commit的时间点，但是会改变从修改的那条commit开始一直到HEAD的commit hash值