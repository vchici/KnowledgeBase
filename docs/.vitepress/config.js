import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative, basename, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

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
    } else if (name.endsWith('.md') && name !== 'index.md') {
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

export default {
  base: "/KnowledgeBase/", // 二级仓库必须配置
  title: "KnowledgeBase",
  description: "个人技术知识库",
  ignoreDeadLinks: true,
  markdown: {
    math: true
  },
  themeConfig: {
    // 关闭右侧 “On this page” 大纲（与左侧目录重复）
    aside: false,
    // 导航栏
    nav: [
      { text: "首页", link: "/" }
    ],
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
}
