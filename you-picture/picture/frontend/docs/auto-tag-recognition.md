# 图片标签自动识别方案

> 版本: v1.0 | 日期: 2026-04-15

## 1. 需求描述

当用户上传图片后跳转到图片编辑页面时，前端自动识别图片内容并推荐标签，预填到标签字段中。用户可以修改、删除或新增标签。

---

## 2. 方案对比

| 方案 | 实现方式 | 优点 | 缺点 |
|------|---------|------|------|
| A. 前端纯本地识别 | 使用 TensorFlow.js + MobileNet 模型在浏览器端推理 | 无后端依赖、无额外费用、隐私性好 | 模型体积 ~16MB、标签为英文需翻译映射、分类粗糙 |
| B. 后端集成 AI API | 调用百度/阿里/腾讯云图像识别 API | 识别精度高、中文标签、类别丰富 | 需后端新增接口、有 API 调用费用 |
| C. 后端自建模型 | 后端部署 CLIP/BLIP 等模型 | 不依赖第三方、可定制 | 部署成本高、需要 GPU |
| D. 基于颜色+格式的规则标签 | 前端用 Canvas 提取主色调，结合图片尺寸、格式等生成标签 | 零依赖、轻量 | 无法识别内容语义 |

---

## 3. 推荐方案: A — 前端纯本地识别 (TensorFlow.js + MobileNet)

### 3.1 选择理由

- **零后端改动**：完全在前端实现，不需要修改后端代码或引入第三方 API
- **零费用**：不依赖云服务，无调用成本
- **用户体验好**：模型加载后推理极快（<500ms），可离线使用
- **隐私友好**：图片不离开浏览器

### 3.2 技术方案

#### 依赖安装

```bash
npm install @tensorflow/tfjs @tensorflow-models/mobilenet
```

#### 核心流程

```
用户上传图片
    ↓
跳转到编辑页面
    ↓
加载图片到 HTMLImageElement
    ↓
并行执行：
  ├─ MobileNet 分类推理 → 英文标签
  ├─ Canvas 提取主色调 → 颜色标签
  └─ 读取图片尺寸/格式 → 属性标签
    ↓
英文标签 → 中文映射表 → 中文标签
    ↓
合并去重 → 预填到标签字段
    ↓
用户可修改/删除/新增
```

#### 关键实现

```typescript
import * as mobilenet from '@tensorflow-models/mobilenet'

// 模型单例（懒加载）
let modelPromise: Promise<mobilenet.MobileNet> | null = null

function getModel() {
  if (!modelPromise) {
    modelPromise = mobilenet.load({ version: 2, alpha: 1.0 })
  }
  return modelPromise
}

// 识别标签
async function recognizeTags(imageUrl: string): Promise<string[]> {
  const img = new Image()
  img.crossOrigin = 'anonymous'
  img.src = imageUrl

  await new Promise((resolve, reject) => {
    img.onload = resolve
    img.onerror = reject
  })

  const model = await getModel()
  const predictions = await model.classify(img, 5) // Top 5 预测

  // 过滤低置信度（< 10%）
  const tags = predictions
    .filter(p => p.probability > 0.1)
    .map(p => mapToChineseTag(p.className))
    .flat()

  return [...new Set(tags)] // 去重
}
```

#### 英文→中文标签映射

维护一个常见标签映射表（约 200 条），覆盖 MobileNet ImageNet 1000 类中的常用类别：

```typescript
const TAG_MAP: Record<string, string[]> = {
  // 动物
  'cat': ['猫', '动物', '宠物'],
  'dog': ['狗', '动物', '宠物'],
  'bird': ['鸟', '动物'],
  'butterfly': ['蝴蝶', '昆虫'],
  // 风景
  'mountain': ['山', '风景', '自然'],
  'ocean': ['海洋', '风景', '自然'],
  'sunset': ['日落', '风景'],
  'sky': ['天空', '风景'],
  // 人文
  'building': ['建筑', '城市'],
  'bridge': ['桥', '建筑'],
  'church': ['教堂', '建筑'],
  // 食物
  'pizza': ['美食', '食物'],
  'cake': ['蛋糕', '美食'],
  // 植物
  'flower': ['花', '植物', '自然'],
  'tree': ['树', '植物', '自然'],
  // ... 更多映射
}

function mapToChineseTag(englishLabel: string): string[] {
  const lower = englishLabel.toLowerCase()
  // 精确匹配
  for (const [key, tags] of Object.entries(TAG_MAP)) {
    if (lower.includes(key)) return tags
  }
  // 无映射时返回原始英文标签
  return [englishLabel]
}
```

#### Canvas 提取主色调（补充颜色标签）

```typescript
function extractDominantColor(img: HTMLImageElement): string {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')!
  canvas.width = 50 // 缩小采样
  canvas.height = 50
  ctx.drawImage(img, 0, 0, 50, 50)
  const data = ctx.getImageData(0, 0, 50, 50).data

  let r = 0, g = 0, b = 0, count = 0
  for (let i = 0; i < data.length; i += 4) {
    r += data[i]
    g += data[i + 1]
    b += data[i + 2]
    count++
  }

  r = Math.round(r / count)
  g = Math.round(g / count)
  b = Math.round(b / count)

  return mapColorToTag(r, g, b) // 如 "暖色调"、"冷色调"、"黑白"
}
```

### 3.3 用户交互设计

1. 编辑页加载时，标签区域显示 `AI 智能识别中...` 的加载状态
2. 识别完成后，自动推荐的标签以蓝色高亮样式展示，与用户手动添加的标签区分
3. 用户可以删除任何标签（包括 AI 推荐的）
4. 用户可以继续手动添加标签
5. 若识别失败或超时（5s），静默降级，不影响正常编辑流程

### 3.4 性能优化

| 措施 | 说明 |
|------|------|
| 模型懒加载 | 首次进入编辑页时才加载模型，后续复用单例 |
| 预加载 hint | 上传成功跳转前，提前触发模型加载 `getModel()` |
| 超时降级 | 5 秒内未完成推理则跳过，不阻塞用户编辑 |
| 图片缩放 | 推理前将图片缩放到 224×224，减少计算量 |

### 3.5 模型体积

- `@tensorflow/tfjs`: ~650KB (gzipped)
- `@tensorflow-models/mobilenet`: ~16MB (模型文件，从 CDN 加载)
- 模型文件从 TensorFlow Hub CDN 按需加载，不打包进应用

### 3.6 浏览器兼容性

TensorFlow.js 支持 WebGL 后端，兼容所有现代浏览器（Chrome、Firefox、Safari、Edge）。不支持 WebGL 的设备自动降级到 CPU 后端。

---

## 4. 替代方案备忘

若后续需要更高精度识别，可迁移到 **方案 B（后端 AI API）**：

- 推荐：阿里云「通用图像标签」API（费用低，支持中文标签）
- 后端新增 `/api/picture/recognize/tags` 接口
- 前端改为调用后端接口，其余交互逻辑不变

---

## 5. 实施计划

| 步骤 | 说明 | 状态 |
|------|------|------|
| 1 | 方案设计 & 文档沉淀 | ✅ 完成 |
| 2 | 用户验收方案 | ⏳ 等待确认 |
| 3 | 安装依赖 (`@tensorflow/tfjs`, `@tensorflow-models/mobilenet`) | 待开始 |
| 4 | 实现标签映射表 | 待开始 |
| 5 | 集成到 PictureEditView.vue | 待开始 |
| 6 | 测试 & 优化 | 待开始 |
