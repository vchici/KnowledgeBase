# 结构化输出 & 解析 防坑层

> AI 生成的解析代码最常见的问题是：解析失败不重试、Prompt 要求的格式和解析器期望的不一致、把非结构化输出当结构化数据强行解析。

---

## 一、JSON 输出不可靠直接解析

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **不指定输出格式** | 让 LLM "返回 JSON"，但没说字段和类型 | 用 JSON Schema 或 Pydantic 模型约束输出 |
| **不启用 JSON Mode** | 没开 `response_format: {"type": "json_object"}`，LLM 在 JSON 外包文字 | 走支持 structured output 的 API（OpenAI json_mode / 各家的 structured output） |
| **直接 json.loads 不兜底** | `json.loads(llm_response)` — 多一个逗号就挂 | 先 try，失败后把错误信息喂给 LLM 重试 |
| **JSON 被 markdown 包裹** | LLM 输出 ` ```json\n{...}\n``` `，直接 strings.strip 暴力处理 | 用正则提取：`re.search(r'\{[\s\S]*\}', text)` 或要求 LLM 不要包裹代码块 |
| **输出被截断** | 长 JSON 超过 max_tokens 被截断，`json.loads` 直接报错 | 设置足够的 max_tokens，或检测截断并重试 |
| **JSON 里有注释/尾逗号** | `{"name": "张三", "age": 30,}` | 用宽松解析器如 `json5`，或在 prompt 里明确"不要注释、不要尾逗号" |

### 健壮的 JSON 解析模式

```python
import json
import re

def safe_json_parse(llm_output: str, max_retries: int = 3):
    """解析 LLM 输出的 JSON，自动处理常见问题并重试"""
    
    for attempt in range(max_retries):
        text = llm_output.strip()
        
        # 尝试直接解析
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            pass
        
        # 提取 markdown 代码块中的 JSON
        match = re.search(r'```(?:json)?\s*([\s\S]*?)```', text)
        if match:
            try:
                return json.loads(match.group(1).strip())
            except json.JSONDecodeError:
                pass
        
        # 提取第一个 { ... } 大括号块
        match = re.search(r'\{[\s\S]*\}', text)
        if match:
            try:
                return json.loads(match.group(0).strip())
            except json.JSONDecodeError as e:
                if attempt == max_retries - 1:
                    raise
                # 把错误信息给 LLM，让它修正
                llm_output = llm.invoke(
                    f"以下 JSON 解析失败，请修正：\n错误：{e}\n原文：\n{text}"
                ).content
                continue
        
    raise ValueError(f"无法解析 JSON：{llm_output[:200]}")
```

---

## 二、Schema 与实际输出不一致

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **字段类型不匹配** | Prompt 要求 `age: 30`（整数），LLM 输出 `"age": "30"`（字符串） | 用 Pydantic 校验，不通过就重试 |
| **字段缺失** | Schema 有 5 个字段，LLM 只输出了 4 个 | Pydantic 校验缺失字段，告诉 LLM 补上 |
| **多余字段** | LLM 凭空加了 `note` 字段，解析器忽略但占据 token | Prompt 里加"只输出指定字段，不要添加额外字段" |
| **枚举值不对** | 要求 `status: "pending"或"done"`，LLM 输出 `"waiting"` | 在 schema 里用 enum 约束 |
| **嵌套结构不对** | 要求 `items: [{name: str, price: float}]`，LLM 输出 `items: ["a", "b"]` | Schema 递归定义嵌套结构 |

### Pydantic 校验 + 自动修正

```python
from pydantic import BaseModel, Field
from typing import Literal
import json

class ProductReview(BaseModel):
    product_name: str = Field(description="产品名称")
    rating: int = Field(ge=1, le=5, description="评分 1-5")
    sentiment: Literal["positive", "negative", "neutral"]
    key_points: list[str] = Field(description="关键评价要点，至少一个")
    summary: str = Field(description="简短总结")

def parse_with_pydantic(llm_output: str, model: type[BaseModel], max_retries=3):
    for attempt in range(max_retries):
        data = safe_json_parse(llm_output)
        
        try:
            return model(**data)
        except Exception as e:
            if attempt == max_retries - 1:
                raise
            # 把校验错误反馈给 LLM
            correction_prompt = (
                f"以下数据不符合要求：\n{json.dumps(data, ensure_ascii=False)}\n"
                f"错误：{e}\n"
                f"预期格式：{model.model_json_schema()}\n"
                f"请修正后只返回 JSON。"
            )
            llm_output = llm.invoke(correction_prompt).content
```

---

## 三、非结构化文本中提取结构化信息

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **直接正则提取** | `re.findall(r'\d{11}', text)` 提取手机号 — 非结构化文本中格式不统一 | 先让 LLM 提取，再正则做二次校验 |
| **一次解析所有字段** | 把一段长文本塞给 LLM 要求输出全部字段 | 分步提取：先找实体，再补详细信息 |
| **不处理歧义** | "我上个月买的那件红色的" — LLM 可能猜错颜色或时间 | 返回置信度 score，低置信度的让人工确认 |
| **漏掉否定信息** | "不是 500 元，是 300 元" — LLM 可能提取 500 | Prompt 强调"提取最终确认的值，不是被否定的值" |
| **多实体混在一个输出里** | 一段对话提到 3 个人，LLM 输出一个人名 | 明确要求返回列表，用 `list[Person]` 类型 |

### 分步提取模式

```python
# ❌ 一次让 LLM 干太多事
prompt_bad = "从以下对话中提取：主持人、嘉宾、话题、关键观点、争议点、结论、时间线..."

# ✅ 分步
def extract_interview_info(text):
    # 第1步：识别参与者
    participants = llm.invoke(
        "从以下对话中识别所有参与者的名字，返回 list[str]：\n" + text
    )
    
    # 第2步：提取关键观点（已有参与者列表做上下文）
    key_points = llm.invoke(
        f"参与者：{participants}\n提取每位参与者的核心观点，返回 list[{{name, point}}]：\n" + text
    )
    
    # 第3步：提取争议点
    controversies = llm.invoke(
        f"对话内容：{text}\n{key_points}\n识别其中的争议点或分歧。"
    )
    
    return {"participants": participants, "key_points": key_points, "controversies": controversies}
```

---

## 四、类型与边界处理

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **None 返回没处理** | LLM 返回 `{"name": null}`，Pydantic 报错 | 字段用 `Optional[str]` 或设 `default=None` |
| **空列表 vs None** | LLM 可能不输出空字段，导致解析出来的 dict 缺少 key | 统一处理：`data.get("items", [])` |
| **数字类型混乱** | `"price": "99.5"` 按字符串解析进 `float` 字段 | 允许 Pydantic 自动转换（`price: float` 可以接收 `"99.5"`） |
| **日期格式不统一** | `"2024-01-01"` vs `"2024/01/01"` vs `"1 Jan 2024"` | 在 prompt 里指定标准格式 ISO 8601 |
| **中英文混合 JSON key** | `{"姓名": "张三", "age": 30}` | 统一用英文 key，中文内容放 value |

---

## 五、Function Calling 作为结构化输出的最佳实践

| 反模式 | 表现 | 纠正 |
|--------|------|------|
| **不用 Function Calling 而拼 prompt 要求 JSON** | 效果差且不稳定 | 优先用 function calling / tool use 来约束输出 |
| **把 Function Calling 当普通工具用** | 定义了一个 `format_output` 工具，只为了拿结构化输出 | 这是推荐做法！比拼 prompt 稳定得多 |
| **Function Calling 参数的 description 太简略** | `"age": {"type": "integer"}` 就没了 | 每个参数写详细 description，这就是最好的 prompt |

```python
# ✅ 用 Function Calling 做结构化输出（比 "请返回 JSON" 稳定 10 倍）
response = llm.invoke(
    messages=[{"role": "user", "content": "分析这段代码：..."}],
    tools=[{
        "type": "function",
        "function": {
            "name": "output_code_review",
            "description": "输出代码审查结果",
            "parameters": {
                "type": "object",
                "properties": {
                    "has_bug": {"type": "boolean", "description": "是否有 bug"},
                    "severity": {"type": "string", "enum": ["low", "medium", "high", "critical"]},
                    "issues": {"type": "array", "items": {"type": "string"}, "description": "问题描述列表"},
                    "suggestion": {"type": "string", "description": "改进建议"}
                },
                "required": ["has_bug", "severity", "issues"]
            }
        }
    }],
    tool_choice={"type": "function", "function": {"name": "output_code_review"}}
)
# 直接取 tool_call.arguments，就是干净的 JSON
```

---

## 检查清单

- [ ] 是否用了 JSON Mode / Function Calling 而非裸拼 prompt？
- [ ] `json.loads` 前有没有 try/catch？失败后有没有重试机制？
- [ ] 有没有处理 markdown 代码块包裹的情况？
- [ ] 用 Pydantic 校验了类型吗？校验失败有没有反馈给 LLM 修正？
- [ ] `max_tokens` 够不够装下完整的 JSON 输出？
- [ ] 空字段/缺失字段有默认值兜底吗？
- [ ] 如果提取多个实体，是否用 `list[Entity]` 而非单个对象？
