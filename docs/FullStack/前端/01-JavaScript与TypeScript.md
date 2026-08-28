# JavaScript 与 TypeScript

## 核心地位

前端一切框架（React/Vue）都建立在 JS 之上，TypeScript 是生产环境的标配。
**先精通 JS 语言本身，再学框架**，否则框架用法只是死记硬背。

## JavaScript 必会

### 异步编程（前端最重要的能力）

| 概念 | 说明 | 必须掌握 |
|------|------|---------|
| 回调函数 | 最基础的异步方式 | 回调地狱的产生与危害 |
| Promise | 状态机：pending / fulfilled / rejected | then/catch/finally、链式调用、all/race/allSettled |
| async/await | 语法糖，让异步代码像同步 | 与 Promise 的关系、错误处理 try/catch |
| 事件循环 | 宏任务 vs 微任务 | 执行顺序题、Node 与浏览器差异 |

```javascript
// 经典面试题：输出顺序
console.log('1');                       // 同步
setTimeout(() => console.log('2'), 0);  // 宏任务
Promise.resolve().then(() => console.log('3')); // 微任务
// 输出：1 3 2
```

### 语言核心机制

- **闭包**：函数 + 其词法作用域，理解防抖/节流、私有变量、柯里化的前提
- **原型链与 this**：`new` 的原理、call/apply/bind、箭头函数与普通函数 this 的区别
- **作用域与提升**：var/let/const 的区别、暂时性死区
- **深拷贝 vs 浅拷贝**：手写 deepClone（考虑循环引用）
- **高阶函数与函数式**：map/filter/reduce、纯函数、不可变数据
- **ES6+ 新特性**：解构、展开、可选链 `?.`、空值合并 `??`、Proxy/Reflect、生成器

## TypeScript 必会

### 基础类型系统

- 基础类型、字面量类型、联合类型 `|`、交叉类型 `&`
- 接口 interface vs 类型别名 type 的选择
- 泛型：函数泛型、泛型约束 `extends`、泛型工具类型

### 进阶类型操作

| 工具类型 | 作用 | 示例 |
|---------|------|------|
| `Partial<T>` | 所有属性可选 | 更新接口的部分字段 |
| `Required<T>` | 所有属性必填 | |
| `Pick<T, K>` | 挑选部分属性 | |
| `Omit<T, K>` | 排除部分属性 | |
| `Record<K, V>` | 构造对象类型 | 枚举映射 |
| `ReturnType<T>` | 提取函数返回类型 | |

### 类型收窄（Narrowing）

- `typeof`、`instanceof`、`in` 判断
- 可辨识联合（discriminated union）：用 `type` 字段区分

```typescript
type Shape =
  | { kind: 'circle'; radius: number }
  | { kind: 'square'; side: number }

function area(s: Shape): number {
  if (s.kind === 'circle') return Math.PI * s.radius ** 2
  return s.side ** 2  // TS 自动收窄为 square
}
```

## 常见面试题

1. 手写防抖（debounce）与节流（throttle）
2. 手写 Promise.all / 深拷贝 / 数组扁平化
3. `let` 与 `var` 的区别？暂时性死区是什么？
4. 事件循环：宏任务微任务执行顺序
5. 闭包在项目中实际用在哪些地方？
6. 如何用 TS 给一个对象类型加只读约束？（Readonly）
