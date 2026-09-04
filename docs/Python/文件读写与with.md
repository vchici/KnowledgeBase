读写文件是 Python 高频操作。核心就三件事：`open()` 打开、读/写、关闭。推荐一律用 `with` 让文件自动关闭，避免忘记 `close()` 导致句柄泄漏。

# 1. 基本读写

`open(路径, 模式, encoding=...)` 返回文件对象。常用模式：

| 模式 | 含义 |
| --- | --- |
| `r` | 只读（默认） |
| `w` | 写入，**覆盖**原内容 |
| `a` | 追加，写到末尾 |
| `x` | 新建，文件已存在则报错 |
| `r+` | 读写（不截断） |
| 加 `b` | 二进制模式，如 `rb` / `wb` |

```python-run
# 写文件
with open("/tmp/demo.txt", "w", encoding="utf-8") as f:
    f.write("第一行\n")
    f.write("第二行\n")

# 读文件
with open("/tmp/demo.txt", "r", encoding="utf-8") as f:
    content = f.read()          # 一次性读全部
print(repr(content))
```

# 2. 读取的几种方式

```python-run
# readline：一次读一行
with open("/tmp/demo.txt", encoding="utf-8") as f:
    print("第一行:", repr(f.readline()))

# readlines：读成列表
with open("/tmp/demo.txt", encoding="utf-8") as f:
    print("所有行:", f.readlines())

# 直接遍历文件对象：最省内存（逐行）
with open("/tmp/demo.txt", encoding="utf-8") as f:
    for line in f:
        print("遍历:", repr(line))
```

大数据文件优先用「逐行遍历」，而不是 `read()` 一次性塞进内存。

# 3. with 的作用

`with` 是**上下文管理器**，离开代码块时自动调用 `close()`，即使中途抛异常也会关闭。对比手动写法：

```python-run
# 手动：容易忘 close，异常时还可能漏关
f = open("/tmp/demo.txt", encoding="utf-8")
try:
    data = f.read()
finally:
    f.close()

# with：等价且简洁，推荐
with open("/tmp/demo.txt", encoding="utf-8") as f:
    data = f.read()
print(data)
```

# 4. 追加与新建

```python-run
# a：追加，不覆盖
with open("/tmp/demo.txt", "a", encoding="utf-8") as f:
    f.write("追加的第三行\n")

with open("/tmp/demo.txt", encoding="utf-8") as f:
    print(f.read())
```

```python-run
# x：文件已存在会报错，避免误覆盖
try:
    with open("/tmp/demo.txt", "x", encoding="utf-8") as f:
        f.write("不该执行")
except FileExistsError as e:
    print("文件已存在:", e)
```

# 5. 编码与二进制

文本文件默认按平台编码，中文务必显式 `encoding="utf-8"`。二进制文件（图片、音频等）用 `b` 模式，不写 `encoding`：

```python-run
# 二进制读写字节
with open("/tmp/bytes.bin", "wb") as f:
    f.write(b"hello")

with open("/tmp/bytes.bin", "rb") as f:
    data = f.read()
print(data, type(data).__name__)   # b'hello' bytes
```

# 6. 判断文件是否存在 / 路径处理

用 `os.path` 或更现代的 `pathlib`：

```python-run
from pathlib import Path

p = Path("/tmp/demo.txt")
print("存在:", p.exists())
print("是文件:", p.is_file())
print("文件名:", p.name)
print("后缀:", p.suffix)
print("父目录:", p.parent)

# 逐行读，pathlib 也支持
for line in p.read_text(encoding="utf-8").splitlines():
    print("pathlib 读:", line)
```

# 7. 常见坑

- 忘写 `encoding="utf-8"`，读中文在 Windows 上容易乱码
- 用 `w` 打开已有文件会**清空**原内容，谨慎；不确定就用 `a` 或 `x`
- 读完后文件指针在末尾，重复 `read()` 得到空串，需 `seek(0)` 回到开头
- 遍历文件对象最省内存，别对超大文件 `readlines()`

```python-run
with open("/tmp/demo.txt", encoding="utf-8") as f:
    first = f.read()
    second = f.read()        # 已到末尾，读到空串
    print("第二次读:", repr(second))

    f.seek(0)                # 回到开头
    print("seek 后再读:", repr(f.read()[:5]))
```
