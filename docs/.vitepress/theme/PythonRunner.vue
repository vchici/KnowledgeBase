<script setup>
/**
 * PythonRunner — 浏览器端运行 Python 并展示结果
 * 基于 Pyodide (WebAssembly)，无需后端，懒加载运行时
 */
import { ref, computed } from 'vue'

const props = defineProps({
  code: { type: String, required: true }
})

// 模块级缓存，多个实例共享同一个 Pyodide 运行时
let pyodidePromise = null

function loadPyodide() {
  if (!pyodidePromise) {
    pyodidePromise = (async () => {
      await new Promise((resolve, reject) => {
        const script = document.createElement('script')
        script.src = 'https://cdn.jsdelivr.net/pyodide/v0.26.4/full/pyodide.js'
        script.onload = resolve
        script.onerror = () => reject(new Error('Pyodide 加载失败，请检查网络'))
        document.head.appendChild(script)
      })
      return window.loadPyodide({
        indexURL: 'https://cdn.jsdelivr.net/pyodide/v0.26.4/full/'
      })
    })()
  }
  return pyodidePromise
}

const status = ref('idle') // idle | loading | running | done
const output = ref('')
const isError = ref(false)
const elapsed = ref('')

const buttonText = computed(() => {
  if (status.value === 'loading') return '加载 Python 运行时…'
  if (status.value === 'running') return '运行中…'
  return '▶ 运行'
})

async function run() {
  if (status.value === 'running') return
  status.value = 'loading'
  output.value = ''
  isError.value = false
  const start = performance.now()
  try {
    const py = await loadPyodide()
    status.value = 'running'

    // 捕获 stdout / stderr
    let captured = ''
    py.setStdout({ batched: (text) => { captured += text + '\n' } })
    py.setStderr({ batched: (text) => { captured += text + '\n' } })

    await py.runPythonAsync(props.code)
    output.value = captured.trimEnd()
  } catch (e) {
    isError.value = true
    output.value = String(e.message || e)
  } finally {
    elapsed.value = ((performance.now() - start) / 1000).toFixed(2) + 's'
    status.value = 'done'
  }
}
</script>

<template>
  <div class="py-runner">
    <div class="py-runner-head">
      <span class="py-runner-label">Python 在线运行</span>
      <button class="py-runner-btn" :disabled="status === 'loading' || status === 'running'" @click="run">
        {{ buttonText }}
      </button>
    </div>

    <pre class="py-runner-code"><code>{{ code }}</code></pre>

    <div v-if="status === 'done'" class="py-runner-result">
      <div class="py-runner-result-head" :class="{ error: isError }">
        <span>{{ isError ? '运行出错' : '运行结果' }}</span>
        <span class="py-runner-elapsed">{{ elapsed }}</span>
      </div>
      <pre class="py-runner-output" :class="{ error: isError }">{{ output }}</pre>
    </div>
  </div>
</template>

<style scoped>
.py-runner {
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  overflow: hidden;
  margin: 16px 0;
  background: var(--vp-c-bg-soft);
}
.py-runner-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--vp-c-bg-alt);
  border-bottom: 1px solid var(--vp-c-divider);
}
.py-runner-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--vp-c-text-2);
}
.py-runner-btn {
  border: 1px solid var(--vp-c-brand-1);
  color: var(--vp-c-brand-1);
  background: transparent;
  border-radius: 6px;
  padding: 3px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.py-runner-btn:hover:not(:disabled) {
  background: var(--vp-c-brand-1);
  color: var(--vp-c-bg);
}
.py-runner-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.py-runner-code {
  margin: 0;
  padding: 12px 16px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
}
.py-runner-result {
  border-top: 1px solid var(--vp-c-divider);
}
.py-runner-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--vp-c-brand-1);
  background: color-mix(in srgb, var(--vp-c-brand-soft) 40%, transparent);
}
.py-runner-result-head.error {
  color: var(--vp-c-danger-1);
  background: color-mix(in srgb, var(--vp-c-danger-soft) 40%, transparent);
}
.py-runner-elapsed {
  font-weight: 400;
  color: var(--vp-c-text-3);
}
.py-runner-output {
  margin: 0;
  padding: 12px 16px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.py-runner-output.error {
  color: var(--vp-c-danger-1);
}
</style>
