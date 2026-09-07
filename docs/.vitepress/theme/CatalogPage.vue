<script setup>
/**
 * CatalogPage — 全站文章索引
 * 从 themeConfig.sidebar 读取全部分组与文章，按目录分组列出，供总览与快速跳转
 */
import { computed } from 'vue'
import { useData, withBase } from 'vitepress'
import { iconOf, collectPages } from './groups.js'

const { theme } = useData()

const groups = computed(() =>
  (theme.value.sidebar || [])
    .map((group) => ({
      text: group.text,
      icon: iconOf(group.text),
      pages: collectPages(group.items)
    }))
    .filter((g) => g.pages.length > 0)
)

const total = computed(() => groups.value.reduce((sum, g) => sum + g.pages.length, 0))
</script>

<template>
  <div class="catalog">
    <h1 class="catalog-title">📚 全部文章</h1>
    <p class="catalog-total">共 {{ total }} 篇笔记 · {{ groups.length }} 个分类</p>

    <section v-for="g in groups" :key="g.text" :id="g.text" class="catalog-group">
      <h2 class="catalog-group-title">
        <span class="catalog-group-icon">{{ g.icon }}</span>
        {{ g.text }}
        <span class="catalog-group-count">{{ g.pages.length }}</span>
      </h2>
      <ul class="catalog-list">
        <li v-for="p in g.pages" :key="p.link" class="catalog-item">
          <a :href="withBase(p.link)">{{ p.text }}</a>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.catalog {
  max-width: 960px;
  margin: 0 auto;
  padding: 8px 24px 64px;
}
.catalog-title {
  margin: 24px 0 4px;
  font-size: 28px;
}
.catalog-total {
  margin: 0 0 28px;
  font-size: 14px;
  color: var(--vp-c-text-3);
}
.catalog-group {
  /* 偏移锚点落点，避免被吸顶导航遮住标题 */
  scroll-margin-top: calc(var(--vp-nav-height) + 24px);
}
.catalog-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 32px 0 12px;
  padding-bottom: 8px;
  font-size: 20px;
  border-bottom: 1px solid var(--vp-c-divider);
}
.catalog-group-icon {
  font-size: 20px;
}
.catalog-group-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--vp-c-brand-1);
  background: var(--vp-c-brand-soft);
  border-radius: 999px;
  padding: 1px 10px;
}
.catalog-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 4px 24px;
}
.catalog-item a {
  display: block;
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--vp-c-text-1);
  transition: background 0.2s, color 0.2s;
}
.catalog-item a:hover {
  background: var(--vp-c-brand-soft);
  color: var(--vp-c-brand-1);
  text-decoration: none;
}
</style>
