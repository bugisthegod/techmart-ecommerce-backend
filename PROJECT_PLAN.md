# TechMart 电商平台 - 项目规划文档

## 项目概述

全栈电商项目，集成AI智能客服，实现高并发场景下的稳定运行。

**目标完成时间**：1个月
**完成后计划**：投递简历，作为面试项目

---

## 技术栈

### 后端
- Spring Boot
- MySQL
- Redis
- Redisson（分布式锁）
- RabbitMQ（延迟队列 + 死信队列）

### 前端
- React/Vue
- Tailwind CSS

### AI集成
- Spring AI
- OpenAI API

### DevOps
- Docker
- Jenkins / GitHub Actions（CI/CD）
- JUnit（单元测试）

---

## 核心功能实现计划

### Week 1: 核心功能 + 防超卖方案

#### 1. 防超卖方案（重点）

**技术方案**：Redis缓存 + 延迟队列 + 死信队列 + 双写数据库 + Redisson分布式锁

**流程图**：
```
用户下单请求
    ↓
① Redisson分布式锁（多服务器场景，防止并发扣库存）
    ↓
② Redis扣减库存（DECR原子操作）
    ↓
③ 发送到延迟队列（订单待支付，15分钟TTL）
    ↓
④ 双写数据库（订单数据持久化到MySQL）
    ↓
⑤ 15分钟后未支付 → 死信队列 → 监听器回滚库存
```

**组件说明**：
- **Redisson分布式锁**：集群环境下保证同一商品库存扣减串行化
- **Redis缓存库存**：快速扣减，避免频繁查DB，QPS高
- **延迟队列**：订单创建后自动检查支付状态（15分钟）
- **死信队列**：处理超时未支付订单，触发库存回滚
- **双写数据库**：先写MySQL，再更新Redis（Cache-Aside模式）

**面试重点**：
- 如何保证高并发下不超卖？
- Redis和MySQL数据一致性如何保证？
- 延迟队列的作用和实现原理？

---

### Week 2: 前端优化 + Docker部署

#### 2. 前端美化（Tailwind CSS）

**技术选型**：
- Tailwind CSS（实用优先的CSS框架）
- 可选UI组件库：DaisyUI / shadcn/ui / Headless UI
- 图标库：Heroicons / Lucide
- 动画：Framer Motion

**目标**：
- 现代化响应式界面
- 移动端适配
- 良好的用户体验

#### 3. Docker容器化

**实现**：
- Dockerfile编写
- Docker Compose多容器编排
- 一键启动整个项目环境

---

### Week 3: AI智能客服

#### 4. AI客服实现（项目亮点）

**技术方案**：Spring AI + OpenAI Function Calling

**核心功能**：
1. **订单查询**
   - "我的订单什么时候到？"
   - "订单号123456的状态"
   - "最近的订单有哪些？"

2. **商品推荐**
   - "推荐一款手机"
   - "1000元以内的耳机"
   - "适合运动的鞋子"

3. **购物车操作**
   - "帮我把这个加入购物车"
   - "购物车里有什么？"

**工作流程**：
```
用户输入
  ↓
Spring AI ChatClient 理解意图
  ↓
Function Calling 调用后端服务
  ↓
AI生成友好回复
```

**面试亮点**：
- 2025年热门技术（AI集成）
- Spring AI官方框架支持
- Function Calling调用真实业务服务

---

### Week 4: 测试 + CI/CD

#### 5. JUnit单元测试

**为什么要写测试？**
- 防止改A坏B
- 快速验证业务逻辑
- 证明代码质量（面试加分）
- 作为代码文档

**测试重点**：
- 核心业务逻辑（订单创建、库存扣减）
- 复杂计算（价格计算、折扣）
- 边界条件（库存不足、参数为null）

**目标**：
- 编写20-30个核心测试用例
- 测试覆盖率 > 70%

#### 6. CI/CD自动化部署

**技术选型**：
- Jenkins（传统方案）
- GitHub Actions（推荐，更简单）

**实现目标**：
```
git push
  ↓
自动触发构建
  ↓
运行测试
  ↓
构建Docker镜像
  ↓
自动部署到服务器
  ↓
（5分钟完成）
```

**Jenkins Pipeline示例**：
```groovy
pipeline {
    agent any
    stages {
        stage('拉取代码') {
            steps {
                git 'https://github.com/你的仓库.git'
            }
        }
        stage('运行测试') {
            steps {
                sh 'mvn test'
            }
        }
        stage('构建Docker镜像') {
            steps {
                sh 'docker build -t ecommerce-backend:latest .'
            }
        }
        stage('部署') {
            steps {
                sh 'docker-compose up -d'
            }
        }
    }
}
```

---

## 项目亮点总结

### 技术亮点
1. **完整的防超卖方案**（Redisson + Redis + 延迟队列 + 死信队列）
2. **AI智能客服**（Spring AI + OpenAI）
3. **CI/CD自动化部署**（Jenkins/GitHub Actions）
4. **单元测试覆盖**（JUnit，70%+覆盖率）
5. **现代化前端**（Tailwind CSS响应式设计）

### 面试时如何介绍

**30秒电梯演讲**：
> "这是一个全栈电商平台，后端使用Spring Boot + Redis + MySQL，实现了完整的购物车和订单系统。重点做了两个创新：
> 1. **高并发优化**：使用Redisson分布式锁 + 延迟队列解决秒杀超卖问题，配合Redis缓存，QPS从500提升到5000
> 2. **AI智能客服**：集成Spring AI，用户可以通过对话查询订单、获取商品推荐，大幅提升用户体验"

---

## 简历上如何写

**项目名称**：AI驱动的分布式电商平台

**项目描述**：
全栈电商项目，支持商品浏览、购物车管理、订单处理等核心功能。采用分布式架构，实现高并发场景下的稳定运行；集成AI智能客服，提升用户体验。

**技术栈**：
- 后端：Spring Boot, Redis, MySQL, Redisson, RabbitMQ
- 前端：React, Tailwind CSS, TypeScript
- AI：Spring AI, OpenAI API
- 部署：Docker, Jenkins, Nginx

**核心成果**：
- 使用Redisson分布式锁 + 延迟队列实现完整防超卖方案，保证数据一致性
- 通过Redis缓存优化查询性能，QPS提升10倍
- 集成AI智能客服，支持自然语言订单查询和商品推荐
- 搭建CI/CD流水线，实现自动化测试和部署
- 单元测试覆盖率75%，保证代码质量

---

## 时间规划（1个月）

| 周次 | 任务 | 状态 |
|------|------|------|
| Week 1 | 前后端核心功能 + 防超卖方案实现 | ⏳ 待开始 |
| Week 2 | Tailwind CSS美化 + Docker容器化 | ⏳ 待开始 |
| Week 3 | AI智能客服集成 | ⏳ 待开始 |
| Week 4 | JUnit测试 + CI/CD部署 + 压测优化 | ⏳ 待开始 |

---

## 优先级分类

### P0（必须有）
- [x] 核心业务功能（订单、购物车、用户认证）
- [x] 分布式锁（Redisson）
- [x] Redis缓存
- [ ] 延迟队列 + 死信队列
- [ ] AI客服（订单查询 + 商品推荐）

### P1（强烈建议）
- [ ] 前端美化（Tailwind CSS）
- [ ] Docker容器化部署
- [ ] Swagger API文档
- [ ] JUnit核心测试用例

### P2（时间允许）
- [ ] CI/CD自动化部署
- [ ] JMeter压测报告
- [ ] 监控系统（Prometheus + Grafana）
- [ ] 日志系统（ELK Stack）

---

## 注意事项

1. **不要贪多**：先保证P0功能质量，再考虑P1、P2
2. **Demo一定要能跑**：面试时当场演示
3. **AI客服是亮点**：但不要占用太多时间（1周足够）
4. **准备好技术细节**：面试官会深挖分布式锁、缓存一致性等原理
5. **压测数据要真实**：不要瞎编，实际压测后再写

---

## 参考资源

### Spring AI官方文档
- https://docs.spring.io/spring-ai/reference/

### Redisson分布式锁
- https://github.com/redisson/redisson

### RabbitMQ延迟队列
- https://www.rabbitmq.com/delayed-message-exchange.html

### Tailwind CSS
- https://tailwindcss.com/docs

---

*文档创建时间：2025-11-06*
*预计完成时间：2025-12-06*
*目标：投递简历，技术面试*
