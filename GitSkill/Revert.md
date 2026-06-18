撤销**指定的某次 commit**，用 `git revert`：

```bash
git revert <commit-hash>
```

这会创建一个**新的 commit**，内容就是"反转"指定 commit 的修改，不会改变历史。

---

# 例子

```
A → B → C → D → E
        ↑
     要撤销这个
```

```bash
git revert C
```

结果：

```
A → B → C → D → E → C'
                      ↑
               反转C的修改（新commit）
```

---

# 撤销多个指定 commit

```bash
git revert commitA commitB commitC
```

---

# 撤销连续的多个 commit

```bash
git revert commitA..commitD
```

左开右闭，不包含 `commitA`，包含 `commitD`。

---

# 不想自动生成 revert commit（想手动编辑后再提交）

```bash
git revert -n <commit-hash>
```

修改会放到暂存区，你自己决定什么时候 commit。

---

# `revert` vs `reset` 的区别

|             | `git revert`   | `git reset`      |
| ----------- | -------------- | ---------------- |
| 改变历史        | ❌ 不改，新增 commit | ✅ 改，删除 commit    |
| 已 push 的仓库  | ✅ 安全           | ⚠️ 需要 force push |
| 撤销指定 commit | ✅ 可以           | ❌ 只能从最新往前数       |
| 协作安全        | ✅ 不影响他人        | ❌ 影响他人           |
