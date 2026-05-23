# 在一台电脑配置多个ssh key

正常情况下，一台电脑存储一个ssh key

```powershell
PS C:\Users\vchic> dir ~/.ssh


    Directory: C:\Users\vchic\.ssh


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
-a----         2026/4/30     21:41            411 id_ed25519
-a----         2026/4/30     21:41            100 id_ed25519.pub
-a----         2026/5/23     18:58            828 known_hosts
-a----         2026/5/23     18:58             92 known_hosts.old
```

一个 ssh key 只能用于一个 github 账号，如果还需要一个 ssh key 用来往另一个账号中的仓推送，这里另一个 github 账号名叫 vchici

```powershell
PS C:\Users\vchic> ssh-keygen -t ed25519 -f $env:USERPROFILE\.ssh\id_ed25519_vchici 

# 一路回车之后...

PS C:\Users\vchic> dir ~/.ssh


    Directory: C:\Users\vchic\.ssh


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
-a----         2026/4/30     21:41            411 id_ed25519
-a----         2026/4/30     21:41            100 id_ed25519.pub
-a----         2026/5/23     19:07            399 id_ed25519_vchici
-a----         2026/5/23     19:07             95 id_ed25519_vchici.pub
-a----         2026/5/23     18:58            828 known_hosts
-a----         2026/5/23     18:58             92 known_hosts.old
```

```powershell
PS C:\Users\vchic> Get-Content $env:USERPROFILE\.ssh\id_ed25519_vchici.pub | Set-Clipboard
```

再将剪切板中的 ssh key 粘贴到 github 中

# 配置 ssh config 文件

```powershell
PS C:\Users\vchic> notepad $env:USERPROFILE\.ssh\config
```

config 文件新增内容：

```txt
# 旧账号 VitaminK2001 (保持默认)
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519

# 新账号 vchici (使用新域名区分)
Host github-vchici
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_vchici
```

配置完成后，检查，注意记事本经常会自作聪明地加上 `.txt` 后缀。如果你的文件其实变成了 `config.txt`，SSH 是完全认不出来的。

如果看到的是 **`config.txt`**：请运行下面这行命令把它改回正确名字：

```powershell
Rename-Item $env:USERPROFILE\.ssh\config.txt config
```

检查结果：

```powershell
PS C:\Users\vchic> dir ~/.ssh


    Directory: C:\Users\vchic\.ssh


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
-a----         2026/5/23     19:08            279 config
-a----         2026/4/30     21:41            411 id_ed25519
-a----         2026/4/30     21:41            100 id_ed25519.pub
-a----         2026/5/23     19:07            399 id_ed25519_vchici
-a----         2026/5/23     19:07             95 id_ed25519_vchici.pub
-a----         2026/5/23     18:58            828 known_hosts
-a----         2026/5/23     18:58             92 known_hosts.old
```

# ssh config原理

## 在 github 复制 ssh 链接时，它默认长这样：

`git@github.com:VitaminK2001/old-repo.git`

SSH 看到 `github.com` $\rightarrow$ 查阅 `config` $\rightarrow$ 掏出**旧钥匙** $\rightarrow$ 以 `VitaminK2001` 的身份完成操作。

## 使用新账号复制 ssh 链接时

需要**手动把里面的 `github.com` 删掉，改成 `github-vchici`**：

原链接：`git@github.com:vchici/new-repo.git` 
修改后：`git@github-vchici:vchici/new-repo.git`

SSH 看到 `github-vchici` $\rightarrow$ 查阅 `config` $\rightarrow$ 噢！其实是要去真实的 `github.com` $\rightarrow$ 悄悄换上**新钥匙**（`id_ed25519_vchici`） $\rightarrow$ 以 `vchici` 的身份成功操作！
