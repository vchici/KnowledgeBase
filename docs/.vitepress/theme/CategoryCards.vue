<script setup>
/**
 * CategoryCards — 首页分类卡片导航
 * 从 themeConfig.sidebar 读取分组，统计每组文章数，点击卡片跳转到索引页对应分组
 */
import { computed } from 'vue'
import { useData, withBase } from 'vitepress'
import { iconOf, collectPages } from './groups.js'

const { theme } = useData()

const categories = computed(() => {
  const sidebar = theme.value.sidebar || []
  return sidebar
    .map((group) => ({
      text: group.text,
      icon: iconOf(group.text),
      pages: collectPages(group.items)
    }))
    .filter((g) => g.pages.length > 0)
})

const total = computed(() => categories.value.reduce((sum, g) => sum + g.pages.length, 0))
</script>

<template>
  <div class="category-cards">
    <div class="category-head">
      <h2 class="category-title">分类浏览</h2>
      <span class="category-sub">共 {{ total }} 篇笔记 · {{ categories.length }} 个分类</span>
    </div>
    <div class="category-grid">
      <a
        v-for="g in categories"
        :key="g.text"
        class="category-card"
        :href="withBase('/catalog#' + g.text)"
      >
        <span class="category-icon">{{ g.icon }}</span>
        <span class="category-name">{{ g.text }}</span>
        <span class="category-count">{{ g.pages.length }} 篇</span>
      </a>
    </div>
  </div>
</template>

<style scoped>
.category-cards {
  max-width: 1152px;
  margin: 0 auto;
  padding: 8px 24px 48px;
  width: 100%;
  box-sizing: border-box;
}
.category-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}
.category-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  border-top: none;
  padding-top: 0;
}
.category-sub {
  font-size: 13px;
  color: var(--vp-c-text-3);
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
}
.category-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid var(--vp-c-divider);
  border-radius: 10px;
  background: var(--vp-c-bg-soft);
  transition: border-color 0.25s, transform 0.25s, box-shadow 0.25s;
}
.category-card:hover {
  border-color: var(--vp-c-brand-1);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  text-decoration: none;
}
.category-icon {
  font-size: 22px;
  line-height: 1;
}
.category-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--vp-c-text-1);
}
.category-count {
  font-size: 12px;
  color: var(--vp-c-text-3);
}
</style>
