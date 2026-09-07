import { readdirSync, readFileSync, statSync, writeFileSync, mkdirSync } from 'node:fs'
import { join, relative, basename, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
// vitepress-plugin-mermaid 是纯 ESM 包，vitepress 以 CJS 方式加载 config.js 时 require 会失败，
// 所以直接相对路径引入其 ESM 入口，由 esbuild 打进 config bundle（仅 node 侧，客户端仍是正常的 ESM 包解析）
import { withMermaid } from '../../node_modules/vitepress-plugin-mermaid/dist/vitepress-plugin-mermaid.es.mjs'

// config.js 位于 docs/.vitepress/ 下，docsDir 即上级的 docs 目录
const docsDir = join(dirname(fileURLToPath(import.meta.url)), '..')

// 与 VitePress 一致的标题锚点（slug）生成逻辑
const rControl = /[\u0000-\u001f]/g
const rSpecial = /[\s~`!@#$%^&*()\-_+=[\]{}|\\;:"'“”‘’<>,.?/]+/g
const rCombining = /[\u0300-\u036F]/g
function slugify(str) {
  return str
    .normalize('NFKD')
    .replace(rCombining, '')
    .replace(rControl, '')
    .replace(rSpecial, '-')
    .replace(/-{2,}/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/^(\d)/, '_$1')
    .toLowerCase()
}

// 去掉行内 markdown 标记，得到标题纯文本
function stripInline(text) {
  return text
    .replace(/`([^`]*)`/g, '$1')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .trim()
}

// 解析一篇笔记的所有标题，按标题层级（#/##/###…）嵌套成树
function parseSections(content) {
  const headings = []
  let inFence = false
  let fenceChar = ''
  let fenceLen = 0
  for (const line of content.split('\n')) {
    // 围栏代码块（``` 或 ~~~，允许最多 3 个前导空格）内的 # 注释不算标题
    const fence = line.match(/^ {0,3}(```+|~~~+)/)
    if (fence) {
      const char = fence[1][0]
      const len = fence[1].length
      if (!inFence) {
        inFence = true
        fenceChar = char
        fenceLen = len
      } else if (char === fenceChar && len >= fenceLen && /^ {0,3}(```+|~~~+)[ \t]*$/.test(line)) {
        inFence = false
      }
      continue
    }
    if (inFence) continue
    const m = line.match(/^(#{1,6})\s+(.+?)\s*$/)
    if (m) {
      const plain = stripInline(m[2])
      headings.push({ level: m[1].length, text: plain, anchor: slugify(plain), items: [] })
    }
  }
  const root = { level: 0, items: [] }
  const stack = [root]
  for (const h of headings) {
    while (stack.length > 1 && stack[stack.length - 1].level >= h.level) stack.pop()
    stack[stack.length - 1].items.push(h)
    stack.push(h)
  }
  return root.items
}

// 把章节树转成侧边栏子项（拼上文章链接 + #锚点）；有子级的一律可折叠
function toSidebarItems(nodes, articleLink) {
  return nodes.map((n) => {
    const item = { text: n.text, link: `${articleLink}#${n.anchor}` }
    if (n.items.length) {
      item.collapsed = false
      item.items = toSidebarItems(n.items, articleLink)
    }
    return item
  })
}

// 递归扫描 docs 目录，生成可折叠的侧边栏结构
function buildSidebar(dir) {
  const entries = readdirSync(dir).sort((a, b) => a.localeCompare(b, 'zh-CN'))
  const items = []
  for (const name of entries) {
    if (name.startsWith('.')) continue
    const full = join(dir, name)
    const stat = statSync(full)
    if (stat.isDirectory()) {
      const children = buildSidebar(full)
      if (children.length) items.push({ text: name, collapsed: true, items: children })
    } else if (name.endsWith('.md') && !META_PAGES.has(name)) {
      const sections = parseSections(readFileSync(full, 'utf-8'))
      const articleLink = '/' + relative(docsDir, full).replace(/\\/g, '/').replace(/\.md$/, '')
      const text = basename(name, '.md')
      if (sections.length) {
        items.push({ text, link: articleLink, collapsed: true, items: toSidebarItems(sections, articleLink) })
      } else {
        items.push({ text, link: articleLink })
      }
    }
  }
  return items
}

// ---------- 知识图谱数据生成 ----------
// 元页面：导航用途的页面，不进侧边栏、不进图谱
const META_PAGES = new Set(['index.md', 'catalog.md', 'graph.md'])

function collectNotes(dir) {
  const entries = readdirSync(dir).sort((a, b) => a.localeCompare(b, 'zh-CN'))
  const notes = []
  for (const name of entries) {
    if (name.startsWith('.')) continue
    const full = join(dir, name)
    const stat = statSync(full)
    if (stat.isDirectory()) {
      notes.push(...collectNotes(full))
    } else if (name.endsWith('.md') && !META_PAGES.has(name)) {
      notes.push({
        abs: full,
        id: relative(docsDir, full).replace(/\\/g, '/').replace(/\.md$/, ''),
        label: basename(name, '.md'),
        group: relative(docsDir, dir).replace(/\\/g, '/').split('/')[0]
      })
    }
  }
  return notes
}

// 去掉围栏代码块，避免示例代码里出现的链接被当成笔记互链
function stripFences(content) {
  const out = []
  let inFence = false
  let fenceChar = ''
  let fenceLen = 0
  for (const line of content.split('\n')) {
    const fence = line.match(/^ {0,3}(```+|~~~+)/)
    if (fence) {
      const char = fence[1][0]
      const len = fence[1].length
      if (!inFence) {
        inFence = true
        fenceChar = char
        fenceLen = len
        continue
      }
      if (char === fenceChar && len >= fenceLen) {
        inFence = false
        continue
      }
    }
    if (!inFence) out.push(line)
  }
  return out.join('\n')
}

// 解析笔记互链（markdown 相对链接 + Obsidian 双链 [[...]]），构建节点与边
function buildGraph() {
  const notes = collectNotes(docsDir)
  const byId = new Map(notes.map((n) => [n.id, n]))
  const byName = new Map() // 笔记名 → id（同名取第一个，与 Obsidian 短名解析近似）
  for (const n of notes) if (!byName.has(n.label)) byName.set(n.label, n.id)

  const linksRe = /\[[^\]]*\]\(([^)\s]+)\)/g
  const wikiRe = /\[\[([^\]|#]+)(?:#[^\]|]*)?(?:\|[^\]]*)?\]\]/g
  const edgeSet = new Set()
  for (const n of notes) {
    const body = stripFences(readFileSync(n.abs, 'utf-8'))
    const targets = []
    for (const m of body.matchAll(linksRe)) {
      let raw
      try { raw = decodeURI(m[1]) } catch { continue }
      if (/^(https?:|mailto:|#|\/)/i.test(raw)) continue
      if (!raw.toLowerCase().endsWith('.md')) continue
      const abs = join(dirname(n.abs), raw.split('#')[0])
      targets.push(relative(docsDir, abs).replace(/\\/g, '/').replace(/\.md$/, ''))
    }
    for (const m of body.matchAll(wikiRe)) {
      const t = m[1].trim()
      if (byId.has(t)) {
        targets.push(t)
        continue
      }
      const id = byName.get(basename(t.replace(/\.md$/, '')))
      if (id) targets.push(id)
    }
    for (const target of targets) {
      if (target === n.id || !byId.has(target)) continue
      edgeSet.add(`${n.id}\u0000${target}`)
    }
  }

  const degree = {}
  const edges = [...edgeSet].map((e) => {
    const [source, target] = e.split('\u0000')
    degree[source] = (degree[source] || 0) + 1
    degree[target] = (degree[target] || 0) + 1
    return { source, target }
  })
  const nodes = notes.map((n) => ({ id: n.id, label: n.label, group: n.group, degree: degree[n.id] || 0 }))
  return { nodes, edges }
}

// 配置加载时（dev 启动 / build）生成一次图谱数据到 public，前端页面 fetch 使用
mkdirSync(join(docsDir, 'public'), { recursive: true })
writeFileSync(join(docsDir, 'public', 'graph.json'), JSON.stringify(buildGraph()))

// ---------- 本地全文搜索：中文按字切分 + 英文/数字按词切分，保证 CJK 召回 ----------
function cjkTokenize(text) {
  const tokens = []
  for (const m of text.toLowerCase().matchAll(/[a-z0-9_]+|[\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff\uac00-\ud7af]/g)) {
    tokens.push(m[0])
  }
  return tokens
}

export default withMermaid({
  base: "/KnowledgeBase/", // 二级仓库必须配置
  title: "KnowledgeBase",
  description: "个人技术知识库",
  ignoreDeadLinks: true,
  markdown: {
    math: true,
    // 代码块显示行号
    lineNumbers: true,
    config: (md) => {
      // ```python-run 代码块 → PythonRunner 组件（浏览器端 Pyodide 在线运行）
      const defaultFence = md.renderer.rules.fence
      md.renderer.rules.fence = (tokens, idx, options, env, self) => {
        const info = tokens[idx].info.trim()
        if (info.startsWith('python-run')) {
          const code = JSON.stringify(tokens[idx].content)
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
          return `<PythonRunner :code="${code}"/>\n`
        }
        return defaultFence(tokens, idx, options, env, self)
      }
    }
  },
  themeConfig: {
    // 关闭右侧 “On this page” 大纲（与左侧目录重复）
    aside: false,
    // 导航栏
    nav: [
      { text: "首页", link: "/" },
      { text: "全部文章", link: "/catalog" },
      { text: "知识图谱", link: "/graph" }
    ],
    // 本地全文搜索（纯静态，无需后端）
    search: {
      provider: 'local',
      options: {
        translations: {
          button: { buttonText: '搜索文章', buttonAriaLabel: '搜索文章' },
          modal: {
            noResultsText: '没有找到相关结果',
            resetButtonTitle: '清除查询条件',
            footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' }
          }
        },
        miniSearch: {
          options: { tokenize: cjkTokenize },
          searchOptions: { tokenize: cjkTokenize }
        }
      }
    },
    // 侧边栏：自动扫描 docs 目录，文章名 + 按标题层级嵌套成树，均可折叠
    sidebar: buildSidebar(docsDir)
  },
  // 覆写 VitePress 内部的 VPSidebarItem，实现「当前/祖先展开，其余折叠」的手风琴效果
  vite: {
    resolve: {
      alias: [
        {
          find: /^\.\/VPSidebarItem\.vue$/,
          replacement: fileURLToPath(new URL('./theme/VPSidebarItem.vue', import.meta.url))
        }
      ]
    }
  }
})
