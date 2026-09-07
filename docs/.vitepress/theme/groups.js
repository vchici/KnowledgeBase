/**
 * 侧边栏分组共享工具：分类图标、文章统计
 * 数据来源统一为 themeConfig.sidebar（config.js 启动时扫描 docs 目录自动生成）
 */
export const GROUP_ICONS = {
  'AI-Code-Calibration': '🤖',
  Agent: '🤖',
  DataBase: '🗄️',
  DesignPattern: '🧩',
  FullStack: '🥞',
  GC: '♻️',
  GitSkill: '🌿',
  Go: '🐹',
  Health: '❤️',
  JavaWeb: '☕',
  js: '🟨',
  LangChain: '🦜',
  LangGraph: '🦜',
  Magnet: '🧲',
  Maven: '📦',
  MovieQuote: '🎬',
  News: '📰',
  Python: '🐍',
  SmartPerfetto: '🔍',
  Thread: '🧵',
  Tool: '🔧',
  UML: '📐'
}

export function iconOf(group) {
  return GROUP_ICONS[group] || '📁'
}

// 递归收集分组下的文章链接（按页面去重，忽略标题锚点）
export function collectPages(items, out = []) {
  const seen = new Set(out.map((p) => p.link))
  for (const item of items || []) {
    if (item.items) {
      collectPages(item.items, out)
    } else if (item.link) {
      const page = item.link.split('#')[0]
      if (!seen.has(page)) {
        seen.add(page)
        out.push({ text: item.text, link: page })
      }
    }
  }
  return out
}
