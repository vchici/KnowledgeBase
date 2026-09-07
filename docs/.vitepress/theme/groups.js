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

// 递归收集分组下的文章（条目 = 文章名，即 .md 前的文本；跳过文章下的标题锚点子项）
export function collectPages(items, out = []) {
  const seen = new Set(out.map((p) => p.link))
  for (const item of items || []) {
    if (item.link && !item.link.includes('#')) {
      if (!seen.has(item.link)) {
        seen.add(item.link)
        out.push({ text: item.text, link: item.link })
      }
    } else if (!item.link && item.items) {
      collectPages(item.items, out) // 分组目录节点
    }
  }
  return out
}
