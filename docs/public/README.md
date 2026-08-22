# public（VitePress 静态资源目录）

> 目录名 `public` 是 VitePress/Vite 的**固定约定**，不能改名。

## 约定内容

`public/` 是 VitePress 专门存放静态资源的目录。构建时，它里面的所有文件会被**原样复制**到站点根目录（`dist/` 根），不经过任何处理（不压缩、不哈希、不改名）。

URL 对应关系（目录内容直接映射到站点根，不含 `public` 前缀）：

| `docs/public/` 下的文件 | 构建后位置 | 线上 URL |
| --- | --- | --- |
| `pyodide/pyodide.js` | `dist/pyodide/pyodide.js` | `https://<你的站点>/KnowledgeBase/pyodide/pyodide.js` |
| `logo.png` | `dist/logo.png` | `https://<你的站点>/KnowledgeBase/logo.png` |

## 为什么不能改名

这是 **Vite 的默认配置 `publicDir: 'public'`**，VitePress 基于 Vite 直接沿用该默认值，**没有提供修改目录名的配置入口**。如果把文件夹改名为其他名字（如 `assets`、`static`），文件不会再被复制到站点根目录，网页将无法加载这些静态资源（例如 Python 在线运行功能会失效）。

## 目录内容

- `pyodide/` — 网页「Python 在线运行」所需的运行时（Pyodide / WebAssembly，约 14MB）。
  读者在页面上点击「运行」时，由浏览器从**本站同源**加载这套运行时来执行 Python 代码，
  不依赖任何外部 CDN，国内外网络均可稳定使用。
  文件来自官方 npm 包 `pyodide@0.26.4`，可按需升级或替换。

## 注意事项

- 不要删除 `pyodide/`，否则页面上的 Python 运行功能会失效。
- 相关组件逻辑见 `docs/.vitepress/theme/PythonRunner.vue`。
- 笔记中使用 ` ```python-run ` 代码块即可自动获得"可编辑 + 在线运行"能力。
