from typing import TypedDict
from langgraph.graph import StateGraph, START, END

class SimpleState(TypedDict):
    input_text: str
    processed_text: str
    final_output: str

def node_to_upper(state: SimpleState):
    raw = state["input_text"]  # 这是一种什么引用属性的方式吗？
    return {"processed_text": raw.upper()}

def node_add_border(state: SimpleState):
    """节点 B：给处理后的文本加上修饰框"""
    text = state["processed_text"]
    decorated = f"====================\n  {text}\n===================="
    return {"final_output": decorated}

builder = StateGraph(SimpleState)

builder.add_node("step_upper", node_to_upper)
builder.add_node("step_border", node_add_border)

builder.add_edge(START, "step_upper")
builder.add_edge("step_upper", "step_border")
builder.add_edge("step_border", END)

app = builder.compile()

if __name__ == "__main__":
    init_data = {"input_text": "hello langgraph 101"}
    result = app.invoke(init_data)
    print(result["final_output"])   # 这是一种什么引用属性的方式吗？
