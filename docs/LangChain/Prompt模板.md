# Prompt Templates（提示词模板）

动态构建prompt的工具，避免硬编码，实现prompt的复用和参数化

## 核心概念

- 将固定部分和可变部分分离
- 支持变量注入、条件逻辑
- 可组合、可嵌套

## 基本使用

```python
from langchain_core.prompts import PromptTemplate

# 定义模板，变量用花括号包裹
template = "你是一个{role}，请用{style}风格回答：{question}"

# 创建模板对象
prompt = PromptTemplate.from_template(template)

# 填充变量
final_prompt = prompt.format(
    role="Python专家",
    style="简洁",
    question="什么是装饰器"
)

print(final_prompt)
# 输出：你是一个Python专家，请用简洁风格回答：什么是装饰器
```

## ChatPromptTemplate（对话模板）

针对对话场景，支持消息角色

```python
from langchain_core.prompts import ChatPromptTemplate

# 定义对话模板
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}"),
    ("human", "{question}")
])

# 填充变量
messages = prompt.format_messages(
    role="翻译助手",
    question="hello怎么翻译"
)
```

## 模板组合

多个模板可以拼接组合

```python
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}"),
    MessagesPlaceholder("chat_history"),  # 占位符，用于插入历史消息
    ("human", "{question}")
])
```

## 常用占位符

- `{variable}` - 普通变量
- `MessagesPlaceholder` - 消息列表占位符
- 支持 Jinja2 模板语法（复杂逻辑场景）