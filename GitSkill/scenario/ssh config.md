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

GitHub 能够正确识别，关键在于：**Git/GitHub 本身完全不认识 `github-vchici` 这个名字，真正起作用的是你本地系统的 SSH 客户端在中间做了“请求转译”**。

这套机制可以拆解为 **三步协作**：

---

### 第一步：Git 委托给 SSH

当你在终端运行 `git push origin master` 时，Git 看到地址是 `git@github-vchici:...`，它知道这是一个 **SSH 协议** 的地址，于是把**建立连接和身份验证的工作全权交给了本地的 SSH 客户端**。

---

### 第二步：本地 SSH 客户端查阅 `config` 秘籍（最核心）

SSH 拿到目标主机名 `github-vchici` 后，会先去读取你配置的 `~/.ssh/config` 文件：

```txt
Host github-vchici
    HostName github.com             # <--- 真实目的地
    User git                        # <--- 登录用户名
    IdentityFile ~/.ssh/id_ed25519_vchici  # <--- 使用的私钥

```

SSH 看到这段配置后，在后台悄悄完成了**三件事**：

1. **替换真实域名**：把目标 IP 地址定向到真实的 `github.com`（而不是去解析 `github-vchici` 这个根本不存在的域名）。
2. **掏出专属钥匙**：强制使用指定路径下的新私钥 `id_ed25519_vchici`。
3. **发起握手**：用这个专属私钥去和 GitHub 的 22 端口建立连接。

---

### 第三步：GitHub 通过公钥反推身份

现在握手请求到达了 GitHub 服务端：

1. **公钥比对**：GitHub 接收到你发来的密钥握手数据，去数据库里匹配这个私钥对应的**公钥 (`id_ed25519_vchici.pub`)**。
2. **确认身份**：GitHub 发现这个公钥被绑定在 **`vchici`** 这个账号下。
3. **鉴权通过**：GitHub 确认：“原来是 `vchici` 本人来了！而他正好有 `vchici/DailyReport` 仓库的写入权限”，于是成功接收你的 Push。

---

### 总结流程图

$$\text{Git Push} \xrightarrow{\text{指令}} \text{本地 SSH} \xrightarrow[\text{换成专属密钥}]{\text{查找 config，重定向至 github.com}} \text{GitHub 服务端} \xrightarrow[\text{确认 vchici 身份}]{\text{匹配公钥}} \text{允许写入}$$

简单来说：**`github-vchici` 只是你在本地给 SSH 设的一个“代号/马甲”。GitHub 根本不知道马甲的存在，它只认你通过该马甲送过去的“特定密钥”。**
