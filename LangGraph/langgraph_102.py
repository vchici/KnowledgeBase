import operator
from typing import Annotated, TypedDict
from langgraph.graph import StateGraph, START, END

class AudidateState(TypedDict):
    input_code: str
    logs: Annotated[list[str], operator.add]
    score: int

def node_check(state: AudidateState):
    code = state["input_code"]
    score = 30 if "eval" in code else 90
    return {
        "logs": [f"评估完成，检测代码长度为 {len(code)}，得分: {score}"],
        "score": score
    }

def node_pass(state: AudidateState):
    """节点 2a：通过分支"""
    return {"logs": ["🎉 审核通过：代码符合安全规范。"]}

def node_fail(state: AudidateState):
    """节点 2b：拒绝通过分支"""
    return {"logs": ["❌ 审核拒绝：代码包含安全风险。"]}

def route_decision(state: AudidateState) -> str:
    """路由节点：根据得分判断是否通过"""
    if state["score"] >= 60:
        return "go_pass"
    else:
        return "go_fail"

builder = StateGraph(AudidateState)

builder.add_node("checker", node_check)
builder.add_node("pass_handler", node_pass)
builder.add_node("fail_handler", node_fail)

builder.add_edge(START, "checker")

builder.add_conditional_edges(
    "checker",
    route_decision,
    {
        "go_pass": "pass_handler",
        "go_fail": "fail_handler"
    }
)

builder.add_edge("pass_handler", END)
builder.add_edge("fail_handler", END)

app = builder.compile()

if __name__ == "__main__":
    init_state = {
        "input_code": "print('hello')",
        "logs": ["系统初始化..."],
        "score": 0
    }
    print("🚀 开始流式执行工作流:\n")
    for event in app.stream(init_state):
        for node_name, updated_state in event.items():
            print(f"📍 进入节点 [{node_name}]")
            print(f"   最新追加的日志: {updated_state.get('logs')}\n")