# 远程仓库的「Sync fork」按钮不要随便按

## 前因：Sync fork 本质是强推覆盖

「Sync fork」按钮有两个选项，性质完全不同：

- **Update branch**：正常的合并——Fork 只落后、没有自己的独有提交时直接**快进**（指针前移，历史保持一条直线）；两边都有独有提交时产生一个 **merge commit** 把两条线接起来。所谓「接起来」是新建一个提交 M，它有**两个父提交**：一个指向你的 C（本地尖），一个指向上游的 E（远端尖），两条开发线在 M 汇合，历史图变成 `A→B→C→M` 和 `A→D→E↗M`，谁也不丢。
- **Discard commits（丢弃更改并同步）**：hard reset 式的强推，把上游最新状态原样覆盖到 Fork 仓的分支上。**你已 push 到 Fork、但还没被上游合并的 Commit，会当场从分支线路上被抹掉**。

## 后果：`git pull --rebase` 也不重放了

设本地有提交 B、C，且已 push 到 Fork（远端 `main` = A→B→C）。点完「丢弃更改并同步」，远端被强推成 A→D→E，C 被抹掉。

本地执行 `git pull --rebase`（本质 = `git fetch` + `git rebase`），按预期应该把 B、C 重放到 E 之上。但实际结果是：**B、C 没有被重放，本地直接变成和远端一模一样的 E**。

## 原理：merge-base 与 fork-point，两种找分叉点的算法

rebase 的第一步是回答：**「哪些提交算你独有、需要重放？」** 即先找分叉点，再重放 `分叉点..本地分支`。找分叉点有两种算法：

- **merge-base（提交图算法）**：从两个分支尖同时往回走，取第一个交汇点。纯看提交图，不需要任何记忆。
- **fork-point（reflog 算法）**：翻 `origin/main` 的 reflog，取它「最近一次指向过、且是本地分支祖先」的提交。看的是历史记忆。

平时两者结果相同，一旦历史被重写就会给出不同答案：

**场景一：上游自己 rebase 重写历史（fork-point 就是为它设计的）**

上游原是 A→X→Y，你的本地从 Y 长出 B、C；上游后来把历史重写成 A→D→E。

**「重写」是怎么发生的？** 提交不可变（SHA 由内容、父提交、提交信息共同决定），rebase/amend 从不修改旧提交，而是**拿旧提交当底稿重演出一串新提交，再把分支指针拨过去**。典型动机：发布前把零碎提交压干净、改提交信息、移除误提交的敏感文件。上游维护者的操作大致是：

```Bash
git rebase -i A      # 把 X、Y 标记为 squash → 重演出新提交 D；之后继续工作产生 E
                     # 此时 main = A→D→E，X、Y 沦为没人引用的旧对象（本体还在仓库里）
git push --force     # 远端 main 还指着 Y，新历史不含 Y → 普通推送非快进被拒，只能强推
```

远端 main 就这样从 Y 被拨到 E。你 fetch 时，本地 `origin/main` 的 reflog 记下这次跳动（…→Y→E）——fork-point 要翻的正是它。

- merge-base 仍是 A → 重放 `A..本地` = X、Y、B、C，把上游已丢弃的 X、Y 也带回来 ❌
- fork-point 查 reflog：origin/main 上次到过 Y → 重放 `Y..本地` = B、C ✔

**场景二：Sync fork 强推（本例）**

本地 A→B→C，你 push 过 C；Sync fork 把 fork 的 main 从 C 强拨到 E（A→D→E）。

- merge-base 是 A → 重放 B、C，这才是正确结果 ✔（但默认没走这条路）
- fork-point 查 reflog：origin/main 上次到过 C → 重放 `C..本地` = 空集，什么都不重放 ❌

**两个场景的区别只在人的意图，不在 Git 的数据。** reflog 里留下的证据完全相同——`origin/main` 都是从旧位置被强拨到新位置，旧位置上的提交都不在新历史里。fork-point 读不到意图，只能赌「强推 = 旧提交作废」：场景一赌赢了（X、Y 的作者=上游自己，确实作废了），场景二赌输了（B、C 的作者=你，从未作废，只是平台替你做了同步）。

> 别和 `patch-id` 混淆：patch-id 是「改动内容已被上游以相同方式合并（PR 被 squash / cherry-pick）时自动跳过重复提交」的正常去重机制；本例提交消失的主因是 fork-point 误判。想避开，用 `git pull --rebase --no-fork-point` 强制从真正的合并基点重放。

好在提交并没有真正丢失——reflog 里还留着，见下一节。

## 但是，Git Reflog可以救场

只要你在本地成功执行过 `git commit`，Git 的引用日志（Reflog）就会记录下你当时那个 Commit 的 SHA-1 哈希值。这是你的后悔药。

执行 `git reflog` 命令，会看到一个类似下面的列表（从新到旧排列）：

```bash
7a2b3c4 HEAD@{0}: pull --rebase: checkout origin/main
1f4e5d6 HEAD@{1}: commit: feat: 完成了某个核心功能  <-- 盯紧这个！这就是你丢失的提交
a1b2c3d HEAD@{2}: checkout: moving from dev to main
```

然后cherry pick即可。

# 「软修改」是什么意思

「软」是 reset 家族的术语（soft reset，软重置），pull 本身并没有 `--soft` 参数。reset 的本质是把分支指针往回挪，三个参数的区别在于**回退波及到哪一层**——止步越早越「软」，详见[Reset](./Reset.md)：

- `--soft`（软）：只挪分支指针，**暂存区、工作区都不碰** → 被撤销提交的改动留在暂存区（相当于只撤销了 `git commit` 这一步）
- `--mixed`（默认）：指针回挪后再重置**暂存区**，工作区不动 → 改动退回工作区（相当于连 `git add` 也一起撤销了）
- `--hard`（硬）：**暂存区、工作区一起对齐目标提交** → 未提交的修改被一并覆盖，彻底丢弃

开头说的 Sync fork「丢弃更改并同步」就是 `--hard` 这一档。

对照 pull 自己的「软硬」：`git fetch` 只更新远程跟踪分支（如 `origin/main`），不碰工作区和本地分支，是最软的同步；`pull` = fetch + 集成（merge/rebase），集成阶段才真正改动工作区。

# pull 的常用参数

pull = fetch + 集成（merge 或 rebase），参数大多是转交给这两步执行的。

## 集成方式（除 --rebase 外最核心）

```Bash
git pull --ff-only   # 只允许快进，有分叉直接报错（最安全的日常拉取）
git pull --no-ff     # 即使能快进也强制产生 merge commit
git pull --squash    # 远端改动压进暂存区，不自动生成 merge commit
```

历史分叉时，新版本 Git 会报错要求明确指定策略，也可以一劳永逸写进配置：

```Bash
git config pull.rebase false   # 分叉时用 merge（默认）
git config pull.rebase true    # 分叉时用 rebase
git config pull.ff only        # 分叉时直接报错
```

## 工作区保护

```Bash
git pull --autostash   # 集成前自动 stash 未提交的本地改动，完成后自动恢复
```

## 其他常用

| 参数 | 作用 |
| --- | --- |
| `--all` | 拉取所有远程仓库 |
| `--tags` | 连同 tag 一起拉取 |
| `--prune` | 顺带删除远端已不存在分支的本地追踪引用 |
| `--allow-unrelated-histories` | 允许合并没有共同祖先的两条历史 |