"""一个简单的计算器模块，用于演示 Python 单元测试。"""


def add(a: int, b: int) -> int:
    """加法"""
    return a + b


def subtract(a: int, b: int) -> int:
    """减法"""
    return a - b


def multiply(a: int, b: int) -> int:
    """乘法"""
    return a * b


def divide(a: int, b: int) -> float:
    """除法，除以零时抛异常"""
    if b == 0:
        raise ValueError("除数不能为 0")
    return a / b


def is_even(n: int) -> bool:
    """判断是否为偶数"""
    return n % 2 == 0
