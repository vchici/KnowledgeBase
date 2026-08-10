import os
from tavily import TavilyClient

tavily = TavilyClient(api_key="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")

print("🚀 正在发起 Tavily 智能搜索...\n")
response = tavily.search(
    query="STARS-631",
    search_depth="advanced",  # 可选 "basic" 或 "advanced"（深度搜索，会深入抓取网页）
    max_results=3,             # 返回前 3 条结果
    include_answer=True        # 🌟 核心亮点：让 Tavily 直接帮你用 AI 总结一个回答
)

print("================ 🤖 Tavily 提炼的直接回答 ================")
print(response.get("answer"))
print("\n" + "="*58 + "\n")

print("================ 📄 详细网页来源与清洗内容 ================")
for idx, result in enumerate(response.get("results", []), 1):
    print(f"[{idx}] 标题: {result.get('title')}")
    print(f"    网址: {result.get('url')}")
    print(f"    相关度得分 (Score): {result.get('score')}")
    # 注意看：它的 content 不是简短的网页摘要，而是被深度清洗后的长文本/Markdown
    print(f"    清洗后的正文片段:\n{result.get('content')[:300]}...\n")
    print("-" * 58)