# 校园美食平台

Spring Cloud 微服务架构的校园美食社区平台，集成 AI 智能助手，支持文章发布、优惠券秒杀、个性化推荐等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2 + Spring Cloud 2023 |
| 网关 | Spring Cloud Gateway |
| RPC | Apache Dubbo 3.2 |
| 数据库 | MySQL 8.0 + MyBatis-Plus |
| 缓存 | Redis 7 + Redisson |
| 消息队列 | RabbitMQ |
| 向量数据库 | Milvus 2.4 |
| AI 接入 | Spring AI (OpenAI 兼容) + Ollama |
| 前端 | Vue 3 + Vite + Vue Router |
| 容器化 | Docker Compose |

## 项目架构

```
food-platform/
├── food-gateway/          # API 网关（端口 8080）
│   ├── JWT 鉴权过滤器
│   └── 接口限流（令牌桶）
│
├── food-user/             # 用户服务（端口 8081）
│   ├── 注册/登录（JWT）
│   ├── 商家入驻/审核
│   └── Dubbo RPC 接口
│
├── food-article/          # 文章服务（端口 8082）
│   ├── 文章 CRUD + 搜索
│   ├── 评论/回复
│   ├── 互动（点赞/收藏/浏览）
│   ├── WebSocket 消息通知
│   └── Dubbo RPC 接口
│
├── food-order/            # 订单服务（端口 8083）
│   ├── 优惠券创建/发放
│   ├── 秒杀（Redis 原子扣减 + MQ 异步下单）
│   └── 券码核销
│
├── food-ai-agent/         # AI 智能助手（端口 8084）
│   ├── ReAct Agent 循环
│   ├── BM25 + HNSW 混合召回
│   ├── RRF 倒排分数融合
│   ├── Cross-Encoder 精排
│   ├── 用户偏好记忆
│   └── 快速通道优化
│
├── food-common/           # 公共模块
│   ├── common-core/       # 统一响应、异常处理
│   ├── common-mybatis/    # 分页、自动填充
│   └── common-redis/      # Redis 工具类
│
├── food-api/              # Dubbo RPC 接口定义
├── food-platform-frontend/ # Vue 3 前端
├── scripts/               # 测试数据初始化脚本
└── sql/                   # 建表 SQL
```

## 核心功能

### 1. 用户系统

**技术实现：**
- JWT Token 无状态认证，Gateway 统一拦截校验
- 角色体系：普通用户（role=0）/ 商家（role=1）
- 商家入驻后通过 Dubbo RPC 同步用户信息到文章服务

**关键接口：**
- `POST /api/auth/register` — 注册（返回 accessToken + refreshToken）
- `POST /api/auth/login` — 登录
- `POST /api/merchant/apply` — 商家入驻申请

### 2. 文章系统

**技术实现：**
- MyBatis-Plus 分页查询，支持关键词模糊搜索
- 热门文章定时任务（ScheduledTask）按点赞/收藏数排序缓存到 Redis
- WebSocket 实时推送评论通知
- 文章数据通过 Dubbo RPC 暴露给 AI Agent 服务

**关键接口：**
- `POST /api/article` — 发布文章（商家）
- `GET /api/article/list?keyword=xxx` — 搜索文章
- `GET /api/article/{id}` — 文章详情（含评论）
- `GET /api/article/top-liked` — 点赞排行
- `GET /api/article/top-collected` — 收藏排行

### 3. 优惠券系统

**技术实现：**
- 三种券类型：满减券（type=1）、折扣券（type=2）、免费券（type=3）
- **普通领取**：数据库乐观锁（version 字段）+ 唯一索引防重
- **秒杀抢券**：Redis DECR 原子扣减库存 → RabbitMQ 异步创建订单 → 消费者落库
- 库存预热：创建优惠券时同步初始化 Redis 库存键 `coupon:stock:{id}`
- 限购控制：Redis 键 `coupon:limit:{id}:{userId}` 记录用户领取数

**秒杀流程：**
```
用户请求 → Gateway 限流 → Order 服务
  → Redis DECR 原子扣减（失败直接返回"已抢光"）
  → Redis INCR 用户限购计数
  → 生成券码 → 发送 RabbitMQ 消息
  → Consumer 异步落库（UserCoupon + CouponOrder）
```

**关键接口：**
- `POST /api/coupon` — 创建优惠券（商家）
- `POST /api/coupon/{id}/claim` — 领取优惠券
- `POST /api/coupon/{id}/flash-claim` — 秒杀抢券
- `POST /api/coupon/verify` — 核销券码（商家）

### 4. AI 智能助手

**技术架构：**

```
用户消息
  │
  ▼
┌─────────────────────────────────┐
│ 意图识别（正则匹配）              │
│ search / ranking / coupon / chat │
└─────────────────────────────────┘
  │                    │
  ▼                    ▼
快速通道              ReAct Agent
（跳过 LLM #1）      （推理-行动循环）
  │                    │
  ▼                    ▼
┌─────────────────────────────────┐
│ 工具调用层                        │
│ ├── search_articles              │
│ │    ├── BM25 关键词召回          │
│ │    ├── HNSW 向量召回（Milvus）  │
│ │    ├── RRF 融合                 │
│ │    └── Cross-Encoder 精排       │
│ ├── get_top_liked                │
│ ├── get_top_collected            │
│ ├── get_my_coupons               │
│ └── get_coupon_expiry            │
└─────────────────────────────────┘
  │
  ▼
LLM 生成回复（带推荐理由）
  │
  ▼
异步更新用户偏好记忆
```

**搜索流程详解：**

```
query: "想吃鸡肉"
  │
  ├─→ BM25 召回（关键词匹配）
  │     中文 2-gram 分词 → TF-IDF 评分 → top 20
  │
  ├─→ HNSW 向量召回（语义匹配）
  │     Ollama bge-m3 嵌入 → Milvus COSINE 搜索 → top 20
  │
  ▼
RRF 融合（k=60）
  score(d) = Σ 1/(60 + rank_i(d))
  BM25 排名 + HNSW 排名 → 合并候选 → top 10
  │
  ▼
Cross-Encoder 精排
  (query, doc) pairs → relevance_score → 降序排列 → top 3
```

**用户偏好系统：**
- **短期记忆**：最近 5 轮对话（ConcurrentHashMap，进程内存）
- **长期偏好**：Redis 存储（键 `ai:memory:{userId}`，TTL 30 天）
- 每次对话后异步调用 LLM 提取偏好关键词，增量合并
- 泛化查询（如"有没有推荐的美食"）时读取偏好，LLM 生成个性化搜索词

**快速通道优化：**
- 意图识别命中 → 跳过 LLM 推理，直接调用工具
- 模板回复替代 LLM 生成，响应时间从 ~13s 降到 <10ms
- 泛化查询自动读取用户偏好扩展搜索词

## 外部依赖与 API Key

本项目需要以下外部服务和 API Key：

### 必需

| 服务 | 用途 | 配置项 | 获取方式 |
|------|------|--------|----------|
| **OpenAI 兼容 API** | LLM 对话推理（Agent 推理 + 偏好提取） | `AI_API_KEY` | [小米 MiMo API](https://api.xiaomimimo.com) 或其他 OpenAI 兼容服务 |
| **Ollama** | 文本嵌入（bge-m3 模型，1024 维向量） | `spring.ai.ollama.base-url` | [Ollama 官网](https://ollama.com) 本地安装 |
| **Milvus** | 向量存储与 HNSW 检索 | `spring.ai.vectorstore.milvus.uri` | Docker Compose 自带 |

### 可选（精排服务，二选一）

| 服务 | 用途 | 配置项 | 获取方式 |
|------|------|--------|----------|
| **阿里 DashScope** | 云端 Cross-Encoder 精排 | `DASHSCOPE_API_KEY` | [DashScope 控制台](https://dashscope.console.aliyun.com) 开通 |
| **本地 vLLM** | 本地 Cross-Encoder 精排（需 GPU） | `food.rerank.local.base-url` | Docker Compose 自带（需 NVIDIA GPU） |

### 环境变量汇总

```bash
# 必需：LLM 对话 API
export AI_API_KEY=your-api-key                    # OpenAI 兼容 API Key
export AI_BASE_URL=https://api.xiaomimimo.com     # API 地址（默认小米 MiMo）
export AI_CHAT_MODEL=mimo-v2.5-pro                # 模型名（默认 mimo-v2.5-pro）

# 必需：数据库
export DB_PASSWORD=root123456                      # MySQL 密码

# 可选：精排服务（二选一）
export RERANK_PROVIDER=dashscope                   # local 或 dashscope
export DASHSCOPE_API_KEY=your-dashscope-key        # 阿里 DashScope API Key

# 可选：消息队列
export RABBITMQ_PASSWORD=guest                     # RabbitMQ 密码
```

### AI 模型说明

| 模型 | 用途 | 运行方式 | 维度 |
|------|------|----------|------|
| `mimo-v2.5-pro` | LLM 对话推理 | 云端 API | — |
| `bge-m3` | 文本嵌入（文章 → 向量） | 本地 Ollama | 1024 |
| `qwen3-vl-rerank` | Cross-Encoder 精排 | 云端 DashScope | — |
| `BAAI/bge-reranker-v2-m3` | Cross-Encoder 精排（本地替代） | 本地 Docker + vLLM | — |

## 快速开始

### 1. 环境准备

```bash
# 克隆仓库
git clone https://github.com/lazybonecy/food-platform.git
cd food-platform

# 启动基础设施（MySQL、Redis、RabbitMQ、Milvus）
docker-compose up -d

# 安装 Ollama 并拉取嵌入模型
ollama pull bge-m3
```

### 2. 初始化数据库

```bash
# MySQL 启动后自动执行 init.sql（Docker entrypoint）
# 如需手动执行：
mysql -h 127.0.0.1 -P 3307 -u root -proot123456 < sql/init.sql
```

### 3. 配置 API Key

本项目使用 Spring Profile 机制管理敏感配置。`application.yml` 中使用占位符（可安全提交 git），本地开发通过 `application-local.yml` 覆盖实际值。

在每个服务的 `src/main/resources/` 下创建 `application-local.yml`（已在 `.gitignore` 中，不会被提交）：

**food-ai-agent/src/main/resources/application-local.yml：**

```yaml
spring:
  datasource:
    password: root123456
  ai:
    openai:
      api-key: your-actual-api-key
    vectorstore:
      milvus:
        uri: http://localhost:19530

food:
  rerank:
    dashscope:
      api-key: your-dashscope-key
```

**food-article / food-order / food-user 各自的 application-local.yml：**

```yaml
spring:
  datasource:
    password: root123456
```

**IDEA 中激活 local profile：**

Run Configuration → VM options 添加：

```
-Dspring.profiles.active=local
```

**命令行启动：**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**或使用环境变量（CI/CD 推荐）：**

```bash
export AI_API_KEY=your-actual-api-key
export DASHSCOPE_API_KEY=your-dashscope-key
export DB_PASSWORD=root123456
```

### 4. 启动后端服务

```bash
# 按顺序启动（各服务独立 Maven 模块）
cd food-gateway   && mvn spring-boot:run &
cd food-user      && mvn spring-boot:run &
cd food-article   && mvn spring-boot:run &
cd food-order     && mvn spring-boot:run &
cd food-ai-agent  && mvn spring-boot:run &
```

### 5. 启动前端

```bash
cd food-platform-frontend
npm install
npm run dev
```

访问 http://localhost:5173

### 6. 初始化测试数据

```bash
# 确保后端服务已启动
python scripts/init_data.py
```

创建 5 个商家 + 5 个学生 + 21 篇文章 + 评论。所有账号密码：`123456`

### 7. 重建向量索引

```bash
# AI Agent 启动后，将文章数据嵌入 Milvus
curl -X POST http://localhost:8084/api/ai/reindex
```

## 端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | API 统一入口 |
| User | 8081 | 用户/商家服务 |
| Article | 8082 | 文章/评论服务 |
| Order | 8083 | 优惠券/订单服务 |
| AI Agent | 8084 | AI 智能助手 |
| MySQL | 3307 | 数据库 |
| Redis | 6379 | 缓存/会话 |
| RabbitMQ | 5672 | 消息队列 |
| RabbitMQ 管理 | 15672 | Web 管理界面（guest/guest） |
| Milvus | 19530 | 向量数据库 |
| Ollama | 11434 | 本地嵌入模型 |
| vLLM Rerank | 8100 | 本地精排模型（可选） |
| Nacos | 8848 | 服务注册发现 |
| 前端 | 5173 | Vue 开发服务器 |
