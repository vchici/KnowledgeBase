import DefaultTheme from 'vitepress/theme'
import PythonRunner from './PythonRunner.vue'
import './custom.css'

export default {
  ...DefaultTheme,
  enhanceApp({ app }) {
    app.component('PythonRunner', PythonRunner)
  }
}
