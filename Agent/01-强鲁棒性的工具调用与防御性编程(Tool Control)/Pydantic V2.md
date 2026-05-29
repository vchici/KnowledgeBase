**Pydantic V2 深度进阶**：

- **学习重点**：不要只用 Pydantic 声明简单的字段类型。要深入掌握自定义校验器（`@field_validator`）、复杂嵌套结构（Nested Models）以及动态 Schema 生成。
    
- **核心痛点**：大模型有时会把日期格式传错、把字符串类型的 ID 传成数字，你必须在 Pydantic 层做强行拦截和类型清洗。