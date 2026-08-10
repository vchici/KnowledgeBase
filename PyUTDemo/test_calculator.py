"""calculator 模块的单元测试，使用 pytest。"""

import pytest
from calculator import add, subtract, multiply, divide, is_even


# ========== 基本用法：普通测试函数 ==========

class TestCalculator:
    """用类组织测试（推荐结构清晰）"""

    def test_add_positive(self):
        assert add(1, 2) == 3
        assert add(100, 200) == 300

    def test_add_negative(self):
        assert add(-1, -2) == -3
        assert add(-5, 5) == 0

    def test_subtract(self):
        assert subtract(5, 3) == 2
        assert subtract(0, 5) == -5

    def test_multiply(self):
        assert multiply(3, 4) == 12
        assert multiply(-2, 3) == -6
        assert multiply(0, 100) == 0

    def test_divide(self):
        assert divide(10, 2) == 5.0
        assert divide(7, 2) == 3.5

    # ========== 测试异常场景 ==========

    def test_divide_by_zero(self):
        with pytest.raises(ValueError, match="除数不能为 0"):
            divide(10, 0)

    # ========== 参数化测试 ==========

    @pytest.mark.parametrize("n,expected", [
        (2, True),
        (3, False),
        (0, True),
        (-1, False),
        (-4, True),
    ])
    def test_is_even_parametrized(self, n, expected):
        """用参数化，一组测试数据测多个场景"""
        assert is_even(n) == expected

    # ========== fixture 复用 ==========

    @pytest.fixture
    def sample_numbers(self):
        """提供测试用的样本数据（每个测试用例独立调用）"""
        print("  [fixture] 准备数据...")
        return {"a": 6, "b": 2}

    def test_add_with_fixture(self, sample_numbers):
        assert add(sample_numbers["a"], sample_numbers["b"]) == 8

    def test_divide_with_fixture(self, sample_numbers):
        assert divide(sample_numbers["a"], sample_numbers["b"]) == 3.0

    # ========== 标记跳过 ==========

    @pytest.mark.skip(reason="这个测试只是演示跳过功能，暂时不运行")
    def test_placeholder(self):
        assert False  # 不会执行到这里
