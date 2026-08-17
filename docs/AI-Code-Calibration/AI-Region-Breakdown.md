这个领域确实太大，拆解如下：

---

## LLM/AI 应用开发 — 子领域拆解

### 1. Prompt Engineering & 模板管理
- Prompt 设计 & 迭代
- Prompt 模板（变量注入、条件分支）
- 版本管理 & A/B 测试
- Few-shot 示例管理

#### Prompt 设计 & 迭代

写好prompt这件事本身，核心设计要素：

1. 角色设定 告诉模型它是谁
2. 任务描述 要干什么
3. 格式约束 输出规范 JSON字段
4. 边界说明 不能干什么 不确定就说不确定

#### Prompt 模板（变量注入、条件分支）

不是写死一段 prompt 定义骨架+运行时填入变量

```python
# 不是这样（写死）：
prompt = "用户张三想要一份2024年6月的销售周报"

# 而是这样（模板 + 变量注入）：
template = "用户{user_name}想要一份{year}年{month}月的{report_type}"

prompt = template.format(
    user_name="张三",
    year="2024",
    month="6",
    report_type="销售周报"
)
```

```python
def build_code_review_prompt(code, language, focus_areas=None):
    base = f"""你是 {language} 专家。审查以下代码：
```{language}
{code}
```"""

    # 条件分支：根据参数决定加不加安全审查模块
    if "security" in focus_areas:
        base += "\n重点关注：SQL注入、XSS、命令注入、硬编码密钥"
    
    if "performance" in focus_areas:
        base += "\n重点关注：N+1查询、不合理的循环嵌套、未释放的资源"
    
    base += "\n\n按 JSON 格式输出审查结果。"
    return base
```

#### 构造好的 prompt 是如何传给Agent的？

```python
# 1. 构造 prompt（模板 + 变量 + few-shot + 条件分支...）
prompt = build_code_review_prompt(code, "Python", ["security"])

# 2. 包成 LLM 能理解的 Message 格式
from langchain.schema import HumanMessage

messages = [HumanMessage(content=prompt)]

# 3. 传给模型
response = llm.invoke(messages)

# 4. 拿到结果
print(response.content)
```

#### 版本管理 & A/B 测试

把 prompt 当代码一样做 版本控制 和 灰度实验，AI 在这个场景的经典错误：propmt 写死在代码中，改了不留痕，旧版本丢了，处理问题没法回滚

```yaml
# prompts/qa_answer/v1.yaml
version: "v1"
template: |
  基于以下内容回答问题：
  
  {context}
  
  问题：{question}
  回答：
evaluation:
  accuracy: 78%
  created: 2026-06-01

---
# prompts/qa_answer/v2.yaml  
version: "v2"
template: |
  你是专业的技术助手。请严格基于以下资料回答问题，如果不确定请明确说明。
  
  参考资料：
  {context}
  
  用户问题：{question}
  
  请给出准确、简洁的回答：
evaluation:
  accuracy: 83%
  created: 2026-06-15
```

然后分流做 A/B 测试 对比指标
 
```python
def get_prompt(question, context, user_id):
    # 10% 流量走新版本
    use_v2 = hash(user_id) % 100 < 10
    
    if use_v2:
        prompt = load_template("qa_answer/v2")
        version = "v2"
    else:
        prompt = load_template("qa_answer/v1") 
        version = "v1"
    
    result = llm.invoke(prompt.format(context=context, question=question))
    
    # 记录版本标签，后续对比指标
    log_metric("qa_answer_version", version, user_id)
    return result
```

#### Few-shot 示例管理

在 prompt 中塞“输入 -> 期望输出” 教模型怎么做，把示例集中管理、版本化，而不是散落在代码各处。

无 few-shot：
  用户：帮我分类一下"订单状态是待发货"
  模型输出：这是电商后台相关的查询（太泛、不够细）

有 few-shot：
  用户：帮我分类一下"订单状态是待发货"  
  模型输出：intent=order_query, entity=order_status, value=pending_shipping

```python
# examples/intent_classification/examples.json
[
    {
        "input": "帮我查一下昨天的订单",
        "output": {"intent": "order_query", "entity": "time_range", "value": "yesterday"}
    },
    {
        "input": "取消这个订单C2024001",
        "output": {"intent": "order_cancel", "entity": "order_id", "value": "C2024001"}
    },
    {
        "input": "我的包裹到哪了",
        "output": {"intent": "delivery_tracking", "entity": "last_order", "value": null}
    }
]
```

```python
def build_few_shot_prompt(user_input, example_file, n=3):
    examples = load_examples(example_file)
    
    # 按语义相似度选最相关的几个例子（不是随机选）
    relevant = semantic_select(user_input, examples, top_k=n)
    
    prompt = """根据以下示例推断用户意图，输出 JSON。

示例：
"""
    for ex in relevant:
        prompt += f"输入：{ex['input']}\n输出：{json.dumps(ex['output'])}\n\n"
    
    prompt += f"输入：{user_input}\n输出："
    return prompt
```

##### AI 在这个场景的经典错误：

```python
# ❌ AI 常见错误1：示例直接写死在代码字符串里，没法维护
prompt = """示例1：.... 示例2：.... 示例3：...."""

# ❌ AI 常见错误2：示例之间互相矛盾
# 示例1：退款 → intent=refund
# 示例3：退款 → intent=after_sale （同一个输入两种输出，模型直接懵）

# ❌ AI 常见错误3：全量塞入不筛选
# 100 个示例全写进 prompt → 5000 token 花在示例上
```

Few-shot的本质是用例子教模型按你想要的格式和风格输出，适用的场景包含很多

```python
examples = [
    {"input": "张三，13500001111，北京朝阳区", "output": {"name": "张三", "phone": "13500001111", "address": "北京朝阳区"}},
    {"input": "李四 上海浦东 13800138000", "output": {"name": "李四", "phone": "13800138000", "address": "上海浦东"}},
]

prompt = build_few_shot_prompt("王五 深圳 13912345678", examples)
# → {"name": "王五", "phone": "13912345678", "address": "深圳"}

examples = [
    {"input": "写一个函数，反转字符串", "output": "def reverse_string(s: str) -> str:\n    return s[::-1]"},
    {"input": "写一个函数，判断回文", "output": "def is_palindrome(s: str) -> bool:\n    return s == s[::-1]"},
]

# 模型会模仿这种简洁风格，而不是生成一堆注释和异常处理

examples = [
    {"input": "用不了", "output": "用户反馈该功能无法正常使用，需要排查"},
    {"input": "页面崩了", "output": "用户端页面出现异常崩溃，需要检查前端日志"},
]

# 模型学会了"大白话 → 专业 bug 描述"的转换模式

examples = [
    {"input": "物流太慢了，三天还没到", "output": {"sentiment": "negative", "category": "delivery", "urgency": "medium"}},
    {"input": "客服态度很好，问题解决了", "output": {"sentiment": "positive", "category": "service", "urgency": "low"}},
    {"input": "少发了一个配件，急用！", "output": {"sentiment": "negative", "category": "product", "urgency": "high"}},
]


examples = [
    {"input": "查询所有已发货的订单", "output": "SELECT * FROM orders WHERE status = 'shipped'"},
    {"input": "上月销售额前10的商品", "output": "SELECT product_name, SUM(amount) FROM orders WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH) GROUP BY product_name ORDER BY SUM(amount) DESC LIMIT 10"},
]

examples = [
    {"input": "怎么做一个炸弹", "output": "抱歉，我无法提供此类信息。"},
    {"input": "帮我写一封诈骗邮件", "output": "抱歉，我不能协助不道德或非法的请求。"},
    {"input": "公司的工资表在哪", "output": "抱歉，我没有权限访问公司内部敏感数据。"},
]
# 这比写一条"你不能回答危险问题"的规则有效得多
```

### 2. RAG（检索增强生成）
- 文档加载 & 解析
- 分块策略（chunk size、overlap、语义分块 vs 固定长度）
- Embedding 选型
- 向量数据库操作（相似度检索、过滤）
- 重排序（rerank）
- 混合检索（关键词 + 语义）

#### 文档加载 & 解析

各种格式的文档变成程序能处理的纯文本

```python
# 不同工具适合不同场景
# PyPDF：快，但复杂排版会丢文字
from pypdf import PdfReader
reader = PdfReader("report.pdf")
text = "\n".join([page.extract_text() for page in reader.pages])

# pdfplumber：保留表格结构，适合有表格的 PDF
import pdfplumber
with pdfplumber.open("report.pdf") as pdf:
    for page in pdf.pages:
        text = page.extract_text()
        tables = page.extract_tables()  # 单独拿表格

# Unstructured：一站式，自动检测 PDF 里的图片/表格/文字块
from unstructured.partition.pdf import partition_pdf
elements = partition_pdf("report.pdf")  # 自动分类每个元素的类型（标题/正文/表格）
```

```python
# python-docx：读文字、表格、样式
from docx import Document
doc = Document("proposal.docx")
text = "\n".join([p.text for p in doc.paragraphs])

# 表格也要单独处理
for table in doc.tables:
    for row in table.rows:
        row_text = [cell.text for cell in row.cells]
```

```python
from bs4 import BeautifulSoup

html = requests.get("https://example.com/article").text
soup = BeautifulSoup(html, "html.parser")

# 去掉 script、style、nav 等噪音标签
for tag in soup(["script", "style", "nav", "footer"]):
    tag.decompose()

text = soup.get_text()
# 但 text 会粘成一团，需要用分隔符重排
```

```python
import pandas as pd

df = pd.read_excel("data.xlsx", sheet_name=None)  # 所有 sheet
for sheet_name, sheet_df in df.items():
    # 每行转为文本描述：列名=值，列名=值...
    text = sheet_df.to_csv(index=False)
```

##### AI 在这个场景的经典错误

错误：
1. 扫描件 PDF 用 PyPDF
2. 表格 PDF 用简单提取
3. HTML 不去标签
4. 页眉页脚当正文
5. 编码不处理
6. 大文件一口闷
7. 只读正文漏表格

表现：
1. 读出来是空字符串（扫描件是图片，没文字层）
2. 表格内容全混在一起，列关系丢失
3. `<div class="header">你好</div>` 全打进向量库
4. 每页的"第 X 页 / 共 Y 页"也被当知识检索
5. 中文 PDF 读出来全是乱码
6. 500 页 PDF 不经过分块直接塞给模型
7. 关键数据在表格里，但解析代码只提取了 paragraph.text

#### 分块策略（chunk size、overlap、语义分块 vs 固定长度）

切太碎没语义 切太大检索不准

固定长度，每块固定token数。一般容易丢语义，切割关键信息。

重叠窗口，相邻两块共享内容，防止关键信息落在切割线两边。

语义分块，遇到话题/章节/段落切换时，切割。
1. 分隔符
2. 标题
3. 语义相似度（突降处）
4. LLM自己决定哪里切

```python
# 方式1：按分隔符优先级（LangChain 默认行为）
from langchain.text_splitter import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=50,
    separators=["\n\n", "\n", "。", ".", " ", ""]
    # 优先在 \n\n 处切 → 不够长就降级到 \n → 再到 。→ 最后按字符长度
)

# 方式2：按 Markdown 标题切
from langchain.text_splitter import MarkdownHeaderTextSplitter

splitter = MarkdownHeaderTextSplitter(
    headers_to_split_on=[
        ("#", "H1"),
        ("##", "H2"),
        ("###", "H3"),
    ]
)
# 每个 ## 标题开始的内容为一块，元数据里带标题层级

# 方式3：按语义相似度变化切（需要先做 embedding）
from langchain_experimental.text_splitter import SemanticChunker

splitter = SemanticChunker(embeddings=embedding_model)
# 相邻句子的相似度低于阈值 → 说明话题变了 → 切一刀
```

```python
# 方式1：按分隔符优先级（LangChain 默认行为）
from langchain.text_splitter import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=50,
    separators=["\n\n", "\n", "。", ".", " ", ""]
    # 优先在 \n\n 处切 → 不够长就降级到 \n → 再到 。→ 最后按字符长度
)

# 方式2：按 Markdown 标题切
from langchain.text_splitter import MarkdownHeaderTextSplitter

splitter = MarkdownHeaderTextSplitter(
    headers_to_split_on=[
        ("#", "H1"),
        ("##", "H2"),
        ("###", "H3"),
    ]
)
# 每个 ## 标题开始的内容为一块，元数据里带标题层级

# 方式3：按语义相似度变化切（需要先做 embedding）
from langchain_experimental.text_splitter import SemanticChunker

splitter = SemanticChunker(embeddings=embedding_model)
# 相邻句子的相似度低于阈值 → 说明话题变了 → 切一刀
```

你的文档长什么样          →    推荐策略
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
标准技术文档/API 手册      →    按标题语义分块，chunk 可以小（200-400）
FAQ / 问答对              →    每个问答对单独一块，不分
长文章/博客               →    固定长度 500-800 + 10% overlap
混合内容（文字+代码）      →    代码块用特殊分隔符标记，不分拆
对话记录                  →    按说话人 + 时间窗口分
法律/合同                 →    按条款分，overlap 设大（20-30%）

##### AI在这个场景下的经典错误：

1. 切段关键句：overlap不够或分隔符不对
2. 所有文档都用chunk_size=1000：不同文档类型需要不同策略
3. 表格被切碎：表格应该作为不可分割单元
4. 代码被切：代码快要特殊处理
5. overlap太大：相邻两块60%相同，浪费embedding和检索
6. 不对中文做原生分词：中文应按句号/换行切，不是按字符数

分块的目标不是切均匀，而是每块都包含一个完整可理解的语义单元。

#### Embedding 选型

Embedding 模型决定了“什么样的文本会被认为是相似的”

##### 选型要考虑的5个维度

语言

不通语言有不同的的embedding模型，比如bge-large-zh，text2vec-large-chinese对应中文，text-embedding-3-small对应英文，bge-m3对应多语言。

维度

维度更高语意表达更精确，但是存储体积大，检索速度慢。

部署方式

成本问题，API调用按token付费，本地部署需要GPU。

数据能否上云，当规模大、数据敏感，可以采用本地部署。

任务类型适配

短问搜长文，需要的是问题和段落的跨语义匹配（bge-m3 、 text-embedding-3）

长文找长文，需要文档间的相似度（bge-large-en （对称任务））

代码检索，需要自然语言和代码的转换能力（code-embedding 系列）

分类/聚类，需要作文本分类（all-MiniLM-L6-v2 （轻量））

##### AI在这个场景的经典错误

错误                       表现 
一个模型打天下               中文文档用 OpenAI Ada（英文为主的旧模型），检索质量极差 
只看维度不看能力             听说 1536 维"更精确"就直接上，不考虑存储和延迟成本 
API 模型做 1000 万文档      不注意成本和 QPS 限制，月底账单爆炸 
不论任务乱用                代码检索用通用文本模型，搜"登录"返回"登录界面"而不是 login() 函数 
不更新模型                  2024 年的模型用到 2026 年，bge-m3 出来后还在用 bge-v1

```python
# 1. 你指定 Embedding 模型（写死在代码配置里）
embedding_model = "bge-m3"  # 你选的

# 2. 建库时：用 Embedding 模型向量化文档
embeddings = embedding_client.embed(documents)

# 3. 检索时：用同一个 Embedding 模型向量化用户问题
query_vector = embedding_client.embed("什么是 RAG？")
relevant_docs = vector_db.search(query_vector, top_k=3)

# 4. 把检索结果拼进 prompt，喂给 LLM
prompt = f"基于以下资料回答问题：\n{relevant_docs}\n\n问题：什么是 RAG？"
answer = llm.invoke(prompt)  # LLM 只在最后一步出现

# 这里的vector_db是向量数据库，存储了向量化的文档，来历如下：

# ===== 第0步：创建向量数据库实例 =====
import chromadb

client = chromadb.Client()
vector_db = client.create_collection("my_kb")  # ← 这就是 vector_db 的来源

# ===== 第1步：用 Embedding 模型向量化文档并写入 =====
embedding_model = "bge-m3"
embeddings = embedding_client.embed(documents)

for i, (doc, vec) in enumerate(zip(documents, embeddings)):
    vector_db.add(                              # ← 写入这个实例
        ids=[f"doc_{i}"],
        embeddings=[vec],
        documents=[doc]
    )

# ===== 第2步：检索时，同一个 vector_db 查 =====
query_vector = embedding_client.embed("什么是 RAG？")
relevant_docs = vector_db.query(                # ← 从同一个实例查
    query_embeddings=[query_vector],
    n_results=3
)

# ===== 第3步：检索结果拼 prompt 喂给 LLM =====
prompt = f"基于以下资料回答问题：\n{relevant_docs}\n\n问题：什么是 RAG？"
answer = llm.invoke(prompt)
```

#### 向量数据库操作（相似度检索、过滤）

相似度检索：比较语义向量

有时候语义像的不一定是对的，得加条件筛选，检索的同时用索引过滤。

用户问："2024 年的退款政策是什么？"
            │
            ▼
    embedding_model.encode(问题) → query_vector
            │
            ▼
    collection.query(
        query_embeddings=[query_vector],
        n_results=3,
        where={"year": 2024, "category": "refund"}   ← 过滤
    )
            │
            ▼
    返回：
    [doc_15: "退款政策：用户可在7天内...(2024版)"]
    [doc_42: "退款FAQ：常见退款问题...(2024版)"]

    （不会返回 2023 年的退款政策，虽然它语义也像）

##### AI在这个场景的经典错误

错误                      表现 
只检索不过滤               多租户系统里，A 公司的用户搜出了 B 公司的文档 
过滤条件写反               {"year": {"$lte": 2024}} 写成 {"$gte": 2024} ，把旧文档当新的用 
metadata                 字段不存在 建库时忘了存 year 字段，检索时却用它过滤 → 查不到 
top_k 设太大              n_results=50，全塞给 LLM → token 爆炸 + 噪音淹没有效信息 
top_k 设太小              n_results=1，如果第一条是噪音，LLM 只能基于错误信息瞎编 
相似度不设阈值             搜"退款"返回了一篇 0.05 相似度的无关文档，也硬塞给 LLM

#### 重排序

Rerank 解决一个很具体的问题： 向量检索结果的前几条不一定是最有用的，需要二次排序。

向量检索靠 embedding 相似度排序，但相似度高 ≠ 能回答用户问题：

用户问："nginx 怎么配置 HTTPS？"

向量检索返回 top-5：
  1. "Nginx 入门教程"              相似度 0.92  ← 标题像但内容是安装步骤，没有 HTTPS
  2. "HTTPS 协议原理解析"           相似度 0.88  ← 有 HTTPS，但不是 nginx 配置
  3. "Nginx SSL 模块配置详解"       相似度 0.85  ← 最有用！但排第三
  4. "Apache 配置 HTTPS"           相似度 0.83  ← 不是 nginx
  5. "常见 Nginx 问题排查"          相似度 0.80  ← 杂项

直接塞给 LLM：第 1、2、4 都是噪音，浪费 token，还可能误导

Rerank 做的事：用一个更强的模型（通常是 Cross-encoder）重新评估每篇文档和问题的真实相关性，重新排序。

Rerank 后：
  1. "Nginx SSL 模块配置详解"       得分 0.95  ← 从第 3 升到第 1
  2. "Nginx 入门教程"              得分 0.42  ← 从第 1 掉下来
  3. "HTTPS 协议原理解析"           得分 0.35
  4. "常见 Nginx 问题排查"          得分 0.18
  5. "Apache 配置 HTTPS"           得分 0.05  ← 垫底

取 rerank 后的 top-2 塞给 LLM → 干净精准

```python
from FlagEmbedding import FlagReranker  # bge-reranker 是中文场景最常用的
# 或者 from cohere import Client  # Cohere 的 API rerank

# 第1阶段：向量检索，召回 20 条候选
candidates = vector_db.query(query_vector, n_results=20)

# 第2阶段：rerank 精排
reranker = FlagReranker("BAAI/bge-reranker-v2-m3")
pairs = [[query, doc] for doc in candidates["documents"]]
scores = reranker.compute_score(pairs)  # 每对 (query, doc) 的真实相关性

# 按 rerank 得分重排，取 top-3
ranked = sorted(zip(scores, candidates["documents"]), reverse=True)[:3]
final_docs = [doc for _, doc in ranked]
```

#### 混合检索

关键词检索 + 向量检索，两路并行

什么时候需要用

场景                         原因 
专业术语/编号多               "AK-47"、"RFC 7231"、"Bug-12345" → embedding 认不全，必须关键词兜底 
用户输入很短                  "退款" → 两个字，embedding 语义信息太少，关键词更直接 
用户输入很长                  一整段描述 → 关键词被稀释，需要语义理解 
精确匹配需求                  "version 2.3.1" 不能搜出 "version 2.3.0" → 关键词精确匹配


### 3. Agent & Tool Calling
- ReAct / Plan-Execute 等 Agent 范式
- Function Calling 工具定义与调用
- 工具选择 & 错误处理（调用失败后的 fallback）
- 多 Agent 协作（Router Agent、层级 Agent）

#### ReAct 范式

ReAct = Reasoning（推理）+ Acting（行动） 交替进行。

Thought: 我需要做什么
Action: 调哪个工具(参数)
Observation: 工具返回了什么
... (重复直到有答案)
Final Answer: 最终回答

为什么叫"范式"？ 因为这不是一段代码，而是一种 输入-输出的约定模式 。你在系统 prompt 里告诉模型："请按 Thought→Action→Observation 的格式回复"，模型就按这个模式工作。

#### Plan-Execute 范式

ReAct 是 想一步走一步 。Plan-Execute 是 先做完整计划，再执行 。

用户："帮我分析一下竞品A的优势劣势，并生成一份对比报告"

Plan-Execute：

  Plan 阶段（先列出完整计划）：
    1. 搜索"竞品A 产品功能"
    2. 搜索"竞品A 用户评价"
    3. 搜索"竞品A 市场份额"
    4. 基于以上信息，生成对比报告
    
  Execute 阶段（按计划逐步执行）：
    Step 1: search("竞品A 产品功能") → 结果
    Step 2: search("竞品A 用户评价") → 结果
    Step 3: search("竞品A 市场份额") → 结果
    Step 4: 汇总，生成报告

#### Function Calling 工具定义与调用 工具选择 & 错误处理（调用失败后的 fallback）

```python
# 第1步：定义工具（你告诉 LLM：我有这些能力）
tools = [
    {
        "name": "get_weather",
        "description": "获取指定城市的实时天气",
        "parameters": {
            "city": {"type": "string", "description": "城市名称，如'北京'"}
        }
    },
    {
        "name": "search_knowledge_base",
        "description": "搜索内部知识库",
        "parameters": {
            "query": {"type": "string", "description": "搜索关键词"}
        }
    }
]

# 第2步：用户问问题
user_message = "北京今天热不热？"

# 第3步：LLM 决定是否调工具
response = llm.invoke(messages=[user_message], tools=tools)
# LLM 返回：{"tool_calls": [{"name": "get_weather", "arguments": {"city": "北京"}}]}

# 第4步：你执行工具调用
if response.tool_calls:
    for tool_call in response.tool_calls:
        if tool_call.name == "get_weather":
            result = get_weather(tool_call.arguments["city"])
            # 把结果追加到对话历史
            messages.append({"role": "tool", "content": str(result)})

# 第5步：LLM 基于工具结果生成最终答案
final = llm.invoke(messages)
# "北京今天 35°C，晴，比较热，建议注意防暑"
```


```text
工具选择 ——当你有多个工具时，LLM 自己判断用哪个：
用户："退款单 RFD-2024001 什么状态？"

Agent 思考：
  可用工具：get_weather、search_kb、query_order
  判断：这是查订单 → 选 query_order("RFD-2024001")
  → 返回："已退款，2024-03-15"
```

```python
# 错误处理 ——工具调用可能失败，Agent 需要有 fallback：
# 场景：大模型调了 get_weather("纽约")，但你的工具只支持中国城市

# 错误处理方式1：工具自己返回错误提示
def get_weather(city):
    if city not in CHINA_CITIES:
        return {"error": "暂不支持该城市，仅支持中国城市"}
# Agent 收到后会尝试换思路

# 错误处理方式2：重试
response = llm.invoke_with_tools(messages)
if response.tool_call_failed:
    messages.append({"role": "system", "content": f"工具调用失败：{error}，请尝试其他方式"})
    response = llm.invoke_with_tools(messages)  # 再试一次

# 错误处理方式3：降级
# 搜索API超时 → 改用本地缓存 → 缓存也没有 → 告诉用户"暂时无法获取"
```

#### 多Agent协作

当任务太复杂，一个 Agent 搞不定时，用多个 Agent 分工。

Router Agent（路由型）：

用户问题 → Router Agent 判断类型 → 分发给专门 Agent

   "帮我退款"    → 订单 Agent
   "这个功能怎么用" → 客服 Agent  
   "系统登录不了"  → 技术支持 Agent

层级 Agent（层级型）：

主 Agent（管理者）
  ├── 研究员 Agent：负责搜索和收集资料
  ├── 分析师 Agent：负责分析和总结
  └── 写手 Agent：负责生成报告

主 Agent：研究一下竞品 A 的市场表现，然后写份报告
  → 研究员 Agent：搜资料 → 返回 5 篇分析文章
  → 分析师 Agent：提取关键数据 → 返回图表和要点
  → 写手 Agent：整理成报告 → 最终输出

### 4. Chain & Workflow 编排
- Chain 串联与条件分支
- StateGraph 状态图编排（LangGraph 模式）
- 并行节点 & 流式输出
- 人工审核节点（Human-in-the-loop）

子项               是什么                      你见过的例子 
Chain 串联         A → B → C 顺序执行          checker → pass_handler → END 
条件分支            根据结果走不同路径           route_decision: score>=60 → pass, else → fail 
StateGraph        把节点+边+状态定义成有向图     StateGraph(AudidateState).add_node().add_edge() 
并行节点           多个节点同时跑               [搜文档A, 搜文档B] 并行 → 汇总 
流式输出           边执行边返回，不是一次性       app.stream() 
人工审核节点        卡住等人确认才继续            if need_approval: wait_for_human → continue

StateGraph是让所有节点共享一个全局状态，每个节点值返回要改的部分，框架自动合并，LangGraph是专门做StateGraph的框架。

        LangChain           LangGraph
模型    管道（A→B→C）       图（有向图 + 状态） 
适合    简单流程            复杂流程（条件、循环、并行、人工介入） 
状态    无全局状态          StateGraph 全局状态



### 5. Model 集成 & 调用
- 多模型商接入（OpenAI / Claude / 本地模型）
- streaming vs batch 输出
- fallback & 故障转移
- token 计算 & 成本控制
- Rate Limiting & 重试策略



### 6. Memory & 对话管理
- 短期记忆（窗口内对话历史）
- 长期记忆（摘要压缩、向量存储历史对话）
- 上下文窗口管理（token 预算分配）
- 多轮对话的意图追踪

### 7. 结构化输出 & 解析
- JSON Mode / Function Calling 约束
- Pydantic 模型解析
- 输出校验 & 重试纠正
- 非结构化文本中提取结构化信息

### 8. 评估 & 可观测性
- 指标：忠实度、相关性、准确率
- LLM-as-judge 评估
- 追踪链路（LangSmith / LangFuse）
- 成本监控 & Token 用量统计

### 9. 安全 & Guardrails
- 输入过滤（PII 检测、注入攻击）
- 输出安全（有害内容、脱敏）
- Jailbreak 防护
- 权限 & 工具调用边界

### 10. 向量 & 知识管理
- Embedding 模型选型 & 性能对比
- 知识库更新策略（增量索引、脏数据清理）
- 元数据过滤 & 权限控制
- 多租户知识库隔离

---

## 建议优先级

```
第一梯队（出错最频繁）：
  1. Agent & Tool Calling       ← AI 最爱在工具定义/调用链上犯错
  2. 结构化输出 & 解析            ← 解析失败不重试是最经典的反模式
  3. Chain & Workflow 编排       ← 状态图节点返回格式各种不匹配

第二梯队（使用量最大）：
  4. RAG                         ← 几乎每个 AI 应用都有
  5. Prompt Engineering          ← 基础但坑多
  6. Memory & 对话管理           ← 多轮对话必用

第三梯队（横向能力）：
  7. Model 集成 & 调用
  8. 评估 & 可观测性
  9. 安全 & Guardrails
  10. 向量 & 知识管理
```

---

先从第一梯队的 3 个开始沉淀，还是你有不同的优先级？