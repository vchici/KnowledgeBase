# RAG（检索增强生成）

结合外部知识库，让模型获取最新、私有的知识

## 为什么需要RAG

模型知识有时效性和范围限制：
- 训练截止日期后的新知识
- 企业私有数据
- 特定领域知识

## 核心流程

```
用户问题 ---> 检索相关文档 ---> 文档+问题喂给模型 ---> 生成回答
```

## 基本组件

### 1. Document Loaders（文档加载器）

```python
from langchain_community.document_loaders import TextLoader, PyPDFLoader, WebBaseLoader

# 文本文件
loader = TextLoader("file.txt")
docs = loader.load()

# PDF
loader = PyPDFLoader("file.pdf")
docs = loader.load()

# 网页
loader = WebBaseLoader("https://example.com")
docs = loader.load()
```

### 2. Text Splitters（文本分割器）

将长文档切分成小块

```python
from langchain_text_splitters import RecursiveCharacterTextSplitter

splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,      # 每块最大字符数
    chunk_overlap=50,    # 块之间的重叠
    length_function=len
)

chunks = splitter.split_documents(docs)
```

### 3. Embeddings（嵌入模型）

将文本转换为向量

```python
from langchain_openai import OpenAIEmbeddings

embeddings = OpenAIEmbeddings()

# 文本转向量
vector = embeddings.embed_query("你好")
# 批量转
vectors = embeddings.embed_documents(["你好", "世界"])
```

### 4. Vector Stores（向量数据库）

存储和检索向量

```python
from langchain_community.vectorstores import Chroma, FAISS

# 创建向量库
vectorstore = Chroma.from_documents(
    documents=chunks,
    embedding=embeddings,
    persist_directory="./chroma_db"
)

# 相似度检索
docs = vectorstore.similarity_search("查询内容", k=3)

# 转为检索器
retriever = vectorstore.as_retriever(search_kwargs={"k": 3})
```

## 完整RAG示例

```python
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import Chroma
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough

# 1. 加载文档
loader = TextLoader("knowledge.txt")
docs = loader.load()

# 2. 分割
splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
chunks = splitter.split_documents(docs)

# 3. 向量化存储
embeddings = OpenAIEmbeddings()
vectorstore = Chroma.from_documents(chunks, embeddings)
retriever = vectorstore.as_retriever()

# 4. 构建prompt
prompt = ChatPromptTemplate.from_template("""
根据以下上下文回答问题：
{context}

问题：{question}
""")

# 5. 构建链
model = ChatOpenAI()
parser = StrOutputParser()

def format_docs(docs):
    return "\n\n".join(doc.page_content for doc in docs)

chain = {
    "context": retriever | format_docs,
    "question": RunnablePassthrough()
} | prompt | model | parser

# 6. 使用
result = chain.invoke("什么是RAG？")
```

## 检索策略

### 相似度检索

```python
retriever = vectorstore.as_retriever(
    search_type="similarity",  # 默认
    search_kwargs={"k": 3}
)
```

### MMR（最大边际相关性）

减少重复，增加多样性

```python
retriever = vectorstore.as_retriever(
    search_type="mmr",
    search_kwargs={"k": 3, "fetch_k": 10}
)
```

### 相似度阈值

只返回高于阈值的结果

```python
retriever = vectorstore.as_retriever(
    search_type="similarity_score_threshold",
    search_kwargs={"k": 3, "score_threshold": 0.8}
)
```

## 向量数据库对比

| 数据库 | 特点 | 适用场景 |
|--------|------|----------|
| FAISS | 本地、快速 | 开发测试 |
| Chroma | 本地持久化 | 中小规模应用 |
| Pinecone | 云托管、可扩展 | 生产环境 |
| Milvus | 分布式、高性能 | 大规模应用 |
| Weaviate | 开源、功能丰富 | 企业应用 |

## RAG优化方向

1. **文档处理** - 更好的分割策略、元数据管理
2. **检索优化** - 混合检索、重排序
3. **生成优化** - prompt工程、引用来源
4. **评估** - 检索准确性、回答相关性