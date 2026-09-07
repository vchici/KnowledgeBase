<script setup>
/**
 * GraphView — 知识图谱可视化
 * 力导向图展示笔记间的互链关系（markdown 相对链接 + Obsidian 双链，config.js 构建时生成 graph.json）
 * 节点颜色 = 顶层目录，节点大小 = 被链接次数，点击节点跳转笔记
 */
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useData, useRouter, withBase } from 'vitepress'

const { isDark } = useData()
const router = useRouter()

const el = ref(null)
const loading = ref(true)
const error = ref('')
const stats = ref({ nodes: 0, edges: 0 })
const groups = ref([]) // [{ name, color }]

let cy = null
let resizeObserver = null

// 20 色调色板，按分组出现顺序循环取色
const PALETTE = [
  '#7c6cf2', '#3e9ffb', '#f2984a', '#4ac97e', '#ef6b9d', '#8bd450',
  '#f2c14a', '#5ad0c8', '#b07cf2', '#f26d5b', '#5b8ff2', '#63c7b1',
  '#e58cc7', '#8fa34a', '#c98a5b', '#6b9ef2', '#d46b6b', '#50b8d0',
  '#a68cf2', '#7fb24a'
]

function applyStyle() {
  cy.style([
    {
      selector: 'node',
      style: {
        'background-color': 'data(color)',
        label: 'data(label)',
        color: isDark.value ? '#9aa7b8' : '#4a5668',
        'font-size': 8,
        width: 'data(size)',
        height: 'data(size)',
        'border-width': 0,
        'text-valign': 'bottom',
        'text-halign': 'center',
        'text-margin-y': 3,
        'font-family': 'inherit',
        'overlay-padding': 2,
        'overlay-color': 'transparent'
      }
    },
    {
      selector: 'node:active',
      style: { 'overlay-color': 'transparent' }
    },
    {
      selector: 'edge',
      style: {
        width: 1,
        'line-color': isDark.value ? '#39424f' : '#c9d2de',
        'curve-style': 'bezier',
        opacity: 0.65
      }
    }
  ])
}

onMounted(async () => {
  try {
    const res = await fetch(withBase('/graph.json'))
    if (!res.ok) throw new Error(`加载 graph.json 失败：HTTP ${res.status}`)
    const data = await res.json()

    // 按分组出现顺序分配颜色，并计算节点大小（度数越大越大）
    const colorMap = new Map()
    const groupOrder = []
    const sizeOf = (degree) => 14 + Math.min(degree, 12) * 2.4
    const elements = [
      ...data.nodes.map((n) => {
        if (!colorMap.has(n.group)) {
          colorMap.set(n.group, PALETTE[colorMap.size % PALETTE.length])
          groupOrder.push({ name: n.group, color: colorMap.get(n.group) })
        }
        return {
          data: {
            id: n.id,
            label: n.label,
            group: n.group,
            color: colorMap.get(n.group),
            size: sizeOf(n.degree)
          }
        }
      }),
      ...data.edges.map((e, i) => ({ data: { id: `e${i}`, source: e.source, target: e.target } }))
    ]
    groups.value = groupOrder
    stats.value = { nodes: data.nodes.length, edges: data.edges.length }

    const cytoscape = (await import('cytoscape')).default
    const coseBilkent = (await import('cytoscape-cose-bilkent')).default
    cytoscape.use(coseBilkent)

    cy = cytoscape({
      container: el.value,
      elements,
      layout: {
        name: 'cose-bilkent',
        animate: false,
        idealEdgeLength: 70,
        nodeRepulsion: 9000,
        edgeElasticity: 0.4,
        padding: 30,
        randomize: true
      },
      minZoom: 0.2,
      maxZoom: 2.5,
      wheelSensitivity: 0.25,
      autoungrabify: true // 只读浏览，不允许拖动节点导致布局错乱
    })

    applyStyle()
    cy.fit(undefined, 40)
    cy.on('tap', 'node', (evt) => router.go(withBase('/' + evt.target.data('id'))))
    cy.on('layoutstop', () => cy.fit(undefined, 40))

    resizeObserver = new ResizeObserver(() => cy.resize())
    resizeObserver.observe(el.value)
  } catch (e) {
    error.value = String(e.message || e)
  } finally {
    loading.value = false
  }
})

watch(isDark, () => {
  if (cy) applyStyle()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  cy?.destroy()
  cy = null
})
</script>

<template>
  <div class="graph-view">
    <div class="graph-toolbar">
      <span class="graph-stats">{{ stats.nodes }} 篇笔记 · {{ stats.edges }} 条关联</span>
      <div class="graph-legend">
        <span v-for="g in groups" :key="g.name" class="graph-legend-item">
          <span class="graph-legend-dot" :style="{ background: g.color }"></span>{{ g.name }}
        </span>
      </div>
    </div>

    <p v-if="error" class="graph-error">图谱加载失败：{{ error }}</p>
    <div v-else class="graph-canvas" :class="{ loading }">
      <div ref="el" class="graph-canvas-el"></div>
      <span v-if="loading" class="graph-loading">正在计算布局…</span>
    </div>
    <p class="graph-hint">提示：滚轮缩放，空白处拖拽平移，点击节点跳转到对应笔记</p>
  </div>
</template>

<style scoped>
.graph-view {
  max-width: 100%;
  padding: 0 24px 32px;
}
.graph-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  margin-bottom: 12px;
}
.graph-stats {
  font-size: 13px;
  color: var(--vp-c-text-3);
}
.graph-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
}
.graph-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--vp-c-text-2);
}
.graph-legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}
.graph-canvas {
  position: relative;
  height: clamp(440px, 72vh, 820px);
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  overflow: hidden;
  background: var(--vp-c-bg-soft);
}
.graph-canvas.loading {
  display: flex;
  align-items: center;
  justify-content: center;
}
.graph-canvas-el {
  position: absolute;
  inset: 0;
}
.graph-loading {
  font-size: 14px;
  color: var(--vp-c-text-3);
}
.graph-error {
  padding: 24px;
  border: 1px dashed var(--vp-c-danger-1);
  border-radius: 10px;
  color: var(--vp-c-danger-1);
  font-size: 14px;
}
.graph-hint {
  margin: 10px 2px 0;
  font-size: 12px;
  color: var(--vp-c-text-3);
}
</style>
