<script setup>
/**
 * PythonRunner — 浏览器端运行 Python 并展示结果
 * 基于 Pyodide (WebAssembly)，无需后端，懒加载运行时
 */
import { ref, computed } from 'vue'

const props = defineProps({
  code: { type: String, required: true }
})

// 按顺序尝试的 Pyodide 运行时来源：优先从站点本地加载（与页面同源，无需外部 CDN），
// 本地缺失时依次回退多个 CDN 镜像，避免单个 CDN 在国内不可达导致一直卡住
const PYODIDE_CDNS = [
  { name: '站点本地', script: import.meta.env.BASE_URL + 'pyodide/pyodide.js', indexURL: import.meta.env.BASE_URL + 'pyodide/' },
  { name: 'jsdelivr', script: 'https://cdn.jsdelivr.net/pyodide/v0.26.4/full/pyodide.js', indexURL: 'https://cdn.jsdelivr.net/pyodide/v0.26.4/full/' },
  { name: 'jsdelivr(fastly)', script: 'https://fastly.jsdelivr.net/pyodide/v0.26.4/full/pyodide.js', indexURL: 'https://fastly.jsdelivr.net/pyodide/v0.26.4/full/' },
  { name: 'jsdelivr(gcore)', script: 'https://gcore.jsdelivr.net/pyodide/v0.26.4/full/pyodide.js', indexURL: 'https://gcore.jsdelivr.net/pyodide/v0.26.4/full/' }
]
const LOAD_TIMEOUT = 20000 // 单个 CDN 加载超时（毫秒）

function loadScript(src) {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    const timer = setTimeout(() => {
      script.remove()
      reject(new Error(`加载超时（${LOAD_TIMEOUT / 1000}s）`))
    }, LOAD_TIMEOUT)
    script.src = src
    script.onload = () => { clearTimeout(timer); resolve() }
    script.onerror = () => { clearTimeout(timer); script.remove(); reject(new Error('网络不可达')) }
    document.head.appendChild(script)
  })
}

// 模块级缓存，多个实例共享同一个 Pyodide 运行时
let pyodidePromise = null

function loadPyodide() {
  if (!pyodidePromise) {
    pyodidePromise = (async () => {
      let lastError = null
      for (const cdn of PYODIDE_CDNS) {
        try {
          await loadScript(cdn.script)
          return await window.loadPyodide({ indexURL: cdn.indexURL })
        } catch (e) {
          lastError = new Error(`从 ${cdn.name} 加载 Pyodide 失败：${e.message}`)
        }
      }
      throw lastError
    })()
  }
  return pyodidePromise
}

const status = ref('idle') // idle | loading | running | done
const output = ref('')
const isError = ref(false)
const elapsed = ref('')
// 可编辑的代码内容，初始为笔记里的示例代码
const editableCode = ref(props.code)
const codeLineCount = computed(() => editableCode.value.split('\n').length)
const isModified = computed(() => editableCode.value !== props.code)

const buttonText = computed(() => {
  if (status.value === 'loading') return '加载 Python 运行时…'
  if (status.value === 'running') return '运行中…'
  return '▶ 运行'
})

function reset() {
  editableCode.value = props.code
  output.value = ''
  isError.value = false
  status.value = 'idle'
}

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

    await py.runPythonAsync(editableCode.value)
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
      <div class="py-runner-actions">
        <button v-if="isModified" class="py-runner-reset" @click="reset">↺ 重置</button>
        <button class="py-runner-btn" :disabled="status === 'loading' || status === 'running'" @click="run">
          {{ buttonText }}
        </button>
      </div>
    </div>

    <textarea
      v-model="editableCode"
      class="py-runner-code"
      :rows="codeLineCount"
      spellcheck="false"
      aria-label="Python 代码（可编辑）"
    ></textarea>

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
.py-runner-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
.py-runner-reset {
  border: 1px solid var(--vp-c-divider);
  color: var(--vp-c-text-2);
  background: transparent;
  border-radius: 6px;
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.py-runner-reset:hover {
  background: var(--vp-c-bg);
  color: var(--vp-c-text-1);
}
.py-runner-code {
  display: block;
  width: 100%;
  box-sizing: border-box;
  margin: 0;
  padding: 12px 16px;
  border: none;
  border-radius: 0;
  background: transparent;
  color: inherit;
  font-family: var(--vp-font-family-mono);
  font-size: 13px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  tab-size: 4;
}
.py-runner-code:focus {
  background: color-mix(in srgb, var(--vp-c-brand-soft) 15%, transparent);
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
