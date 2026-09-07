import DefaultTheme from 'vitepress/theme'
import PythonRunner from './PythonRunner.vue'
import CatalogPage from './CatalogPage.vue'
import GraphView from './GraphView.vue'
import Layout from './Layout.vue'
import './custom.css'

export default {
  extends: DefaultTheme,
  // 自定义 Layout 包装默认布局，注入首页插槽内容（分类卡片等）
  Layout,
  enhanceApp({ app }) {
    app.component('PythonRunner', PythonRunner)
    app.component('CatalogPage', CatalogPage)
    app.component('GraphView', GraphView)
  }
}
