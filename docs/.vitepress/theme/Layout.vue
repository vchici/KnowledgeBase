<script setup>
/**
 * Layout — 包装 VitePress 默认布局，向首页插槽注入自定义区块
 */
import DefaultTheme from 'vitepress/theme'
import CategoryCards from './CategoryCards.vue'

const { Layout } = DefaultTheme
</script>

<template>
  <Layout>
    <template #home-hero-after>
      <CategoryCards />
    </template>
  </Layout>
</template>
