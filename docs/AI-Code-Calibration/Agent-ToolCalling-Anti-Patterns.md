# Agent & Tool Calling 防坑层

> AI 生成的 Agent 代码最密集的错误集中在：工具定义和实际调用不匹配、ReAct 循环跑飞、多工具协作时状态混乱。

---

## 一、工具定义与调用不匹配

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **参数名不一致** | 工具定义 `parameters: {city}`，实际函数签名 `def get_weather(location)` | 工具 schema 和函数参数名必须完全一致 |
| **参数类型不匹配** | schema 声明 `type: integer`，实际函数接收 `str` | 严格对齐类型 |
| **描述不够详细** | `description: "查天气"` — LLM 不知道怎么用 | 写明：能做什么、参数含义、返回什么、什么时候该用时什么不该用 |
| **缺少 required 标记** | 必填参数没标 required | 加上 `"required": ["city"]` |
| **返回结果格式乱** | 工具返回 Python 对象，LLM 收到的是一串 `<__main__.Weather object>` | 返回 JSON 字符串或明确的 dict，LLM 才看得懂 |
| **一个工具干太多事** | `do_everything(action, params)` — 名字模糊、参数万能 | 单一职责，一个工具只干一件事 |

### 正确示例

```python
# ❌ AI 常生成这样
tools = [{"name": "search", "description": "搜索"}]

# ✅ 应该是这样
tools = [{
    "name": "search_knowledge_base",
    "description": "在内部知识库中搜索指定关键词的文档。"
                   "适用于：查找产品文档、FAQ、技术规范。"
                   "不适用于：实时数据查询、个人数据查询。",
    "parameters": {
        "type": "object",
        "properties": {
            "query": {
                "type": "string",
                "description": "搜索关键词，支持中英文，建议使用具体的术语而非模糊的描述"
            },
            "top_k": {
                "type": "integer",
                "description": "返回结果数量，默认 5，最大 10",
                "default": 5
            }
        },
        "required": ["query"]
    }
}]
```

---

## 二、ReAct 循环失控

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **无限循环** | 工具返回错误 → LLM 反复调用同一工具 → 永远不停 | 设置 `max_iterations`（一般 10-15） |
| **循环但不推进** | 反复调工具但不往答案靠近 | 加 stop condition：连续 N 次无进展就停 |
| **跳过 Observation 直接 Final Answer** | 没等工具返回就自己编答案 | 框架层面强制 Action 后必须等 Observation |
| **Thought 空洞** | `Thought: 我需要查一下` — 重复、无意义 | 要求 thought 包含具体推理："因为用户问天气，所以需要调 get_weather" |
| **忘记用户原问题** | 第三次迭代已经忘了最初要干什么 | 每轮 prompt 都带着原始用户问题 |
| **工具调用参数幻觉** | 传了不存在的参数名，或传了工具不支持的值 | 工具返回明确的 validation error，让 LLM 修正 |

### 防御代码

```python
MAX_ITERATIONS = 10
STALL_THRESHOLD = 3

iteration = 0
stall_count = 0
last_action = None

while iteration < MAX_ITERATIONS:
    response = llm.invoke(messages, tools=tools)
    
    if not response.tool_calls:
        break  # LLM 觉得可以结束了
    
    current_action = response.tool_calls[0].name
    
    # 检查是否在重复同一个无意义的调用
    if current_action == last_action:
        stall_count += 1
    else:
        stall_count = 0
    
    if stall_count >= STALL_THRESHOLD:
        messages.append({"role": "system", "content": "请换个思路，不要重复之前的操作"})
        stall_count = 0
    
    last_action = current_action
    
    # 执行工具...
    iteration += 1
```

---

## 三、多 Agent 协作反模式

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **Router 分错任务** | "这个功能怎么退款" → Router 分给了"功能咨询"而非"退款处理" | Router 的 prompt 里加边界示例 |
| **子 Agent 输出不一致** | 研究员 Agent 返回 list，写手 Agent 期望 dict | 定义统一的 Agent 间通信格式 |
| **上下文丢失** | 子 Agent 看不到用户原始问题，答非所问 | 子 Agent 的 prompt 始终包含用户原始输入 |
| **子 Agent 不知道彼此做了什么** | 两个子 Agent 搜了同样的东西 | 主 Agent 汇总后去重，或设信息共享池 |
| **没有超时机制** | 一个子 Agent 卡住，整个流程死锁 | 每个子 Agent 调用必须有 timeout |
| **层级太深** | 主 Agent → 子 Agent → 孙 Agent → 曾孙 Agent | 最多两层，超过就该重构 |

---

## 四、错误处理与 Fallback

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **工具挂了不处理** | 工具抛异常 → Agent 直接崩 | try/catch，返回 `{"error": "..."}` 让 LLM 看到错误 |
| **静默失败** | 工具出错但返回空 → LLM 以为没搜到数据，瞎编答案 | 错误结果必须明确标明是"出错"而非"无结果" |
| **Fallback 链无终点** | 主模型→备模型→备备模型→...→最后返回 None | 链末端必须有兜底："系统繁忙，请稍后重试" |
| **重试无退避** | 失败后 0ms 重试 → 连打服务导致雪崩 | 指数退避：1s, 2s, 4s, 8s... |
| **不区分错误类型** | 超时和参数错误都用同一套重试 | 4xx 错误不重试（参数错了重试 100 次也是错），5xx/超时才重试 |

### 工具调用的健壮模式

```python
import time

def safe_tool_call(tool_name, args, max_retries=3):
    for attempt in range(max_retries):
        try:
            result = execute_tool(tool_name, args)
            return {"success": True, "data": result}
        except ValidationError as e:
            # 参数错误，不重试
            return {"success": False, "error": f"参数错误: {e}", "retry": False}
        except TimeoutError:
            if attempt == max_retries - 1:
                return {"success": False, "error": "操作超时，请稍后重试", "retry": False}
            time.sleep(2 ** attempt)  # 指数退避
        except Exception as e:
            if attempt == max_retries - 1:
                return {"success": False, "error": f"操作失败: {e}", "retry": False}
            time.sleep(2 ** attempt)
```

---

## 五、工具返回结果的祸害

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **返回全量数据** | 搜索返回 1000 条，全塞给 LLM → token 爆炸 | 工具层面截断，只返回 top_k |
| **返回 Python 对象** | `return Weather(temp=22)` → LLM 看到 `<__main__.Weather>` | `return json.dumps({"temp": 22, "condition": "晴"})` |
| **返回不带上下文** | `return ["doc1", "doc2"]` — LLM 不知道这是文档标题还是 id | 每条记录带来源、类型、摘要 |
| **返回含敏感信息** | 数据库查询结果含密码哈希 | 工具层面脱敏后再返回 |
| **结果太长没截断** | 一篇 5000 字的文章全量返回 | 截断到安全长度 + 提示"内容较长，已截断" |

---

## 检查清单

- [ ] 每个工具的描述是否详细到"什么时候该用、什么时候不该用"？
- [ ] 工具参数名和实际函数参数名是否完全一致？
- [ ] 是否有 max_iterations 限制？是否有 stall detection？
- [ ] 工具出错时返回的是 JSON 错误信息还是直接抛异常？
- [ ] 重试是否有退避策略？是否区分了可重试和不可重试的错误？
- [ ] 工具返回结果是否截断了？是否序列化为 JSON？
- [ ] 多 Agent 间是否有统一的通信格式？
- [ ] 子 Agent 是否能看到用户原始问题？
