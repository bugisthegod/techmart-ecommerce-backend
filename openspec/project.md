# Project Context

## Purpose
E-commerce backend platform providing REST APIs for product catalog, shopping cart, order management, user authentication, and high-concurrency flash sale (seckill) functionality. Supports distributed transactions, caching, and async message processing for scalable retail operations.

## Tech Stack
- **Java 17** with **Spring Boot 3.5.4**
- **MySQL 8.0+** (primary datastore, JPA/Hibernate ORM)
- **Redis** (caching, distributed locks via Redisson, stock management)
- **RabbitMQ** (async message queuing for seckill orders)
- **JWT** (java-jwt 3.8.1 for stateless authentication)
- **Swagger/OpenAPI** (API documentation)
- **Maven** (build tool)

## Project Conventions

### Code Style
- **Package structure**: `com.abel.ecommerce.{domain}` (e.g., `controller`, `service`, `repository`, `entity`, `dto`)
- **Naming**: PascalCase for classes, camelCase for methods/variables
- **DTOs**: Request/Response suffix (e.g., `ProductRequest`, `ProductResponse`)
- **Logging**: SLF4J with pattern `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n`
- **Debug logging**: Enabled for `com.abel.ecommerce` package and SQL in development

### Architecture Patterns
**Layered Architecture**:
- **Controllers** → **Facades** → **Services** → **Repositories** → **Entities**

**Key Patterns**:
1. **Facade Pattern**: Complex orchestration (e.g., `OrderFacade` coordinates cart, product, order, address services with distributed locks)
2. **Entity-First Design**: Services return and work with entities; controllers convert entities to DTOs for responses
3. **ResponseResult Wrapper**: All API responses wrapped in `ResponseResult<T>` with success/error status
4. **Custom Exception Hierarchy**: All exceptions extend `BaseException` with HTTP status codes (404, 400, 409, etc.)
5. **Distributed Locking**: Redisson multi-locks for concurrent stock operations (sorted by product ID to prevent deadlock)

**Critical Business Flows**:
- **Order Creation**: Multi-service transaction with distributed locks (10s wait, 30s lease)
- **Seckill (Flash Sale)**: Redis stock → RabbitMQ → Async order creation with idempotency via `ReliableMessage` table

### Testing Strategy
- **Unit Tests**: `@Transactional` for auto-rollback, mock Redis/RabbitMQ
- **Controller Tests**: `@WebMvcTest` for endpoint testing
- **Repository Tests**: `@DataJpaTest` for data access layer
- **Integration Tests**: Require MySQL and Redis running
- **Test Class Naming**: `{ClassName}Test` (e.g., `OrderServiceTest`)
- **Run specific test**: `mvn test -Dtest=OrderServiceTest`

### Git Workflow
- **Main Branch**: `main` (for PRs and production)
- **Current Status**: Working tree has modifications to `.gitignore`, `pom.xml`, `application*.properties`
- **Commit Style**: Imperative mood (e.g., "Add rate limit filter", "Remove Spring Data JPA native methods")

## Domain Context

### Entity Relationships
- **User** ↔ **Role**: Many-to-Many (join table)
- **Category**: Self-referential tree (parentId for hierarchy)
- **Product** → **Category**: Many-to-One
- **CartItem** → **User**, **Product**: Many-to-One
- **Order** → **User**: Many-to-One
- **OrderItem** → **Order**, **Product**: Many-to-One
- **Address** → **User**: Many-to-One

### Order State Machine
```
PENDING_PAYMENT (0) → PAID (1) → SHIPPED (2) → COMPLETED (3)
                  ↓
              CANCELLED (4)
```
Transitions validated via `canBePaid()`, `canBeShipped()`, `canBeCompleted()`, `canBeCancelled()` methods.

### Seckill (Flash Sale) Flow
1. `StockWarmer` preloads product stock into Redis at startup (`@PostConstruct`)
2. `SeckillService.doSeckill()` deducts Redis stock atomically (Lua script)
3. Creates `SeckillMessage` in database
4. `SeckillMessageTask` (scheduled every 5s) sends pending messages to RabbitMQ
5. `SeckillOrderConsumer` processes with idempotency check (`ReliableMessage` table)
6. Creates order and deducts database stock

### Authentication Flow
1. Login generates JWT via `JwtTokenUtil.generateToken(username, userId, roles)`
2. `JwtAuthenticationFilter` validates token on every request
3. Sets `SecurityContextHolder` with authentication
4. `TokenBlacklistService` handles logout (revoked tokens stored in Redis)

## Important Constraints

### Technical Constraints
1. **Distributed Lock**: Always unlock in `finally` block, check `isHeldByCurrentThread()`
2. **Transaction Boundaries**: `@Transactional` on service methods only, not controllers
3. **DTO Conversion**: Services return entities; controllers convert to DTOs for responses
4. **Order Status**: Always validate state transitions before updating status
5. **Redis Stock**: Use Lua scripts for atomic operations; never `get` then `set`
6. **RabbitMQ ACK**: Always ACK/NACK messages; use `false` for `multiple` parameter
   - `basicAck(deliveryTag, false)`: acknowledge only this message
   - `basicNack(deliveryTag, false, true)`: nack only this message, requeue=true
7. **Idempotency**: Check `ReliableMessage` table before processing duplicate seckill orders

### Business Constraints
- JWT secret stored in `application-local.properties` (never commit production secrets)
- Stock deduction requires distributed locks to prevent overselling
- Order creation must clear selected cart items atomically
- Freight costs calculated and added to order total

### Regulatory/Security
- Public endpoints: `/api/users/register`, `/api/users/login`, `/swagger-ui/**`, `/v3/api-docs/**`
- All other endpoints require valid JWT token
- CORS configured in `SecurityConfig.java`
- Rate limiting implemented via Bucket4j + Redis

## External Dependencies

### Required Services
Must be running before application startup:
1. **MySQL** (port 3306, database `ecommerce_db`)
2. **Redis** (port 6379) - caching, distributed locks, stock management, token blacklist
3. **RabbitMQ** (default ports) - seckill message queuing

### Redis Usage Patterns
- **Caching**: Product info (1-hour TTL)
- **Stock Management**: Atomic deduction via Lua scripts (`StockService`)
- **Distributed Locks**: Redisson multi-locks (`product:stockLock:{productId}`)
- **Token Blacklist**: Revoked JWT tokens with expiration

### RabbitMQ Configuration
- **Exchange**: `seckill.order.exchange` (topic)
- **Queue**: `seckill.order.queue`
- **Routing Key**: `seckill.order`
- **Configured in**: `RabbitMQConfig.java`

### API Documentation
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI 3.0 specification available at `/v3/api-docs`

### Build & Run
```bash
# Build
mvn clean install

# Run (development)
mvn spring-boot:run

# Run (production)
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Run tests
mvn test
```

### Logging
- Log file: `logs/ecommerce.log`
- Debug level for `com.abel.ecommerce` package
- SQL logging enabled in development profile