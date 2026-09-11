# Transformer 与注意力机制 ★

> **本质**：注意力 = **一次动态加权的查表**——用 Query 和每个 Key 算相似度，按相似度对 Value 加权求和。自注意力让序列里任意两个位置一步直连（长程依赖）且全序列并行计算（可规模化），这两点同时干掉了 RNN，是 LLM 时代的全部骨架。

## 核心考点

### 1. Self-Attention 必会公式（要能默写并解释每个部分）

```
Attention(Q, K, V) = softmax(Q·Kᵀ / √d_k) · V
```

- **Q/K/V**：同一输入经三个不同投影得到。Q=我在找什么，K=我有什么标签，V=我实际携带的内容。直觉：**软化的字典查询**——普通字典 key 命中返回 value，这里按匹配度按比例混合所有 value。
- **除以 √d_k**：d_k 大时点积方差变大 → softmax 进入饱和区 → 梯度消失。缩放是数值稳定手段。
- **softmax 沿 key 维**：得到一组和为 1 的注意力权重。

### 2. 三种注意力形态（掩码是关键）

- **双向 Self-Attention**：每个位置看全部位置（BERT）。
- **因果掩码（Causal Mask）Self-Attention**：把未来位置的权重置 -∞，只看过去（GPT，生成的前提）。
- **交叉注意力（Cross-Attention）**：Q 来自一方，K/V 来自另一方（翻译解码器看源句、多模态 Q-Former）。

### 3. Multi-Head Attention

- 把 d_model 切成 h 份，每份独立做注意力再拼接投影回去。
- **动机**：单头只能学一种「对齐模式」，多头在**不同子空间**并行关注不同关系（语法/指代/位置邻近）。
- 成本与单头近似相同（每头维度变小）。

### 4. 位置编码（没有它注意力就是无序词袋）

- 注意力本身对位置无感 → 必须注入位置信息。
- 演进：正弦绝对位置（原版）→ 可学习绝对位置（GPT-2）→ **相对位置 / RoPE（旋转位置编码）**。
- **RoPE**：把位置信息通过对 Q/K 做旋转让「注意力分数只依赖相对距离」。外推性好，是 LLaMA / Qwen / DeepSeek 的标配；长上下文扩展（NTK-aware 缩放、YaRN）都是在 RoPE 上做文章。

### 5. 整体结构（要能徒手画）

```
输入 token → Embedding(+位置信息)
  → N × [ 残差 + [ LayerNorm → Multi-Head Attention ]
          残差 + [ LayerNorm → FFN (SwiGLU) ] ]     ← Pre-LN 结构
  → 最后 LayerNorm → LM Head（投影回词表 logits）
```

- **FFN**：两层 MLP（升维 4 倍再降回），占约 2/3 参数量，直觉上是「逐 token 的知识存储库」。
- 每个子层带**残差连接**；Norm 用 Pre-LN / RMSNorm（原因见 [02-深度学习基础](02-深度学习基础.md)）。

### 6. 三种架构路线（必考对比）

| 架构 | 代表 | 注意力 | 训练目标 | 强项 |
|------|------|--------|---------|------|
| Encoder-only | BERT | 双向 | MLM（完形填空） | 理解任务、做 Embedding |
| **Decoder-only** | **GPT/LLaMA/Qwen** | 因果掩码 | **Next-Token-Prediction** | 生成、zero-shot、可无限堆大 |
| Encoder-Decoder | T5 / BART | 双向+交叉 | Seq2Seq | 明确的转换任务（翻译、摘要） |

- **为什么 Decoder-only 赢了**：训练目标与生成任务天然统一、每个 token 都提供监督信号（数据效率高）、架构简单便于规模化。

### 7. 复杂度与优化伏笔

- 自注意力计算/显存 **O(n²)**（n=序列长）→ 长上下文的根本瓶颈。
- 应对：FlashAttention（省显存，数学等价）、稀疏/滑窗注意力、线性注意力 → 详见 [08-推理优化与部署](08-推理优化与部署.md)。

### 8. 注意力变体速记（KV Cache 显存优化线）

- **MHA**（多头，标准）→ **MQA**（所有头共享一组 KV，省 8 倍 KV cache 但掉点）→ **GQA**（分组共享，折中，LLaMA-3/Qwen2 标配）→ **MLA**（DeepSeek，低秩压缩 KV，更省）。
- 一句话：**推理时 KV Cache 太占显存，所以想尽办法压缩 KV 头数。**

## 面试高频速答

### 为什么除以 √d_k？

<details>
<summary>答案</summary>
点积的方差随维度 d 线性增长，大维度下 logits 过大，softmax 输出接近 one-hot，梯度趋近 0。除以 √d 把方差归一回 1，保持 softmax 处在有梯度的区间。
</details>

### GQA 是什么？为什么省显存？

<details>
<summary>答案</summary>
Grouped-Query Attention：把 Q 的 h 个头分成 g 组，每组共享一组 K/V。KV Cache 大小从 h 份降到 g 份（如 32Q/8K=省 4 倍），质量接近 MHA。推理时 KV Cache 是显存大头，这是最划算的压缩。
</details>

### Transformer 为什么比 RNN 快？

<details>
<summary>答案</summary>
RNN 逐步串行，第 t 步依赖第 t-1 步，无法并行；Transformer 全序列一次性矩阵运算（训练时完全并行），且任意位置直连不衰减。代价是 O(n²) 注意力和推理时仍需自回归逐 token。
</details>

## 互链

- 残差/Norm/优化器的原理：[02-深度学习基础](02-深度学习基础.md)
- 这个骨架如何被「喂大」：[05-预训练与模型格局](05-预训练与模型格局.md)
- O(n²) 与 KV Cache 的工程解法：[08-推理优化与部署](08-推理优化与部署.md)
