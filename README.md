# E-Commerce Platform Backend

An **enterprise-grade** Spring Boot e-commerce backend system with advanced distributed features including flash sales (seckill), distributed locking, message queue processing, and comprehensive testing infrastructure.

## 🚀 Project Overview

This is a **production-ready** e-commerce backend API built with modern Java technologies and enterprise patterns, featuring:

- **RESTful API Design** with complete CRUD operations
- **JWT Authentication & Authorization** with role-based access control and token blacklist
- **High-Concurrency Flash Sale System (Seckill)** with Redis + RabbitMQ
- **Distributed Locking** with Redisson for race condition prevention
- **Reliable Message Queue** with idempotency guarantees
- **Advanced Order Management** with state machine workflow and facade pattern
- **Atomic Stock Management** using Redis Lua scripts
- **Comprehensive Test Suite** with 19+ passing JUnit tests
- **Docker Compose** for local development environment
- **CI/CD Pipeline** with GitHub Actions
- **Professional Architecture** following Spring Boot best practices

## 🛠️ Technology Stack

| Category | Technologies |
|----------|-------------|
| **Backend Framework** | Spring Boot 3.5.4, Spring Security, Spring Data JPA |
| **Database** | MySQL 8.0+ with JPA/Hibernate ORM |
| **Caching & Locking** | Redis with Spring Data Redis, Redisson 3.32.0 for distributed locks |
| **Message Queue** | RabbitMQ with Spring AMQP for async processing |
| **Authentication** | JWT (java-jwt 3.8.1) with BCrypt encryption, Token Blacklist |
| **Payment Gateway** | Stripe 24.3.0 (configured, ready for integration) |
| **API Documentation** | Swagger/OpenAPI 2.2.0 |
| **Build Tool** | Maven |
| **Java Version** | Java 17+ |
| **DevOps** | Docker Compose, GitHub Actions CI/CD |

## 📦 Core Features

### 🔐 User Management
- **User Registration & Authentication** with email validation
- **JWT Token-based Security** with 24-hour token expiration
- **Token Blacklist Service** - Logout support with Redis-backed revoked token storage
- **Role-based Authorization** (Customer, Admin, Super Admin)
- **Password Encryption** using BCrypt
- **Profile Management** with user data validation

### 🏷️ Category Management
- **Hierarchical Category Structure** with parent-child relationships
- **Dynamic Category Tree** with unlimited nesting levels
- **Category Validation** preventing deletion of categories with products
- **Bulk Category Operations** for administrative tasks

### 📦 Product Catalog
- **Advanced Product Management** with detailed attributes
- **Multi-parameter Filtering** (category, price range, status)
- **Product Search** with fuzzy matching
- **Inventory Tracking** with real-time stock updates
- **Pagination & Sorting** for large datasets

### 🛒 Shopping Cart
- **Persistent Cart Management** across user sessions
- **Real-time Price Calculations** with tax and shipping
- **Item Selection/Deselection** for flexible checkout
- **Stock Validation** preventing overselling
- **Cart Synchronization** across multiple devices

### 📍 Address Management
- **Multiple Shipping Addresses** per user
- **Default Address Management** with automatic selection
- **Address Validation** ensuring delivery accuracy
- **Geographic Integration** ready for shipping calculations

### 🛍️ Order Processing System
- **Complete Order Lifecycle** management
- **State Machine Implementation** (Pending → Paid → Shipped → Completed)
- **OrderFacade Pattern** - Orchestrates complex multi-service operations
- **Distributed Locking** - Redisson multi-lock prevents race conditions (10s wait, 30s lease)
- **Transaction Management** ensuring data consistency across multiple tables
- **Payment Integration** ready (Stripe configured)
- **Order Tracking** with status updates
- **Inventory Reservation** during order processing with automatic rollback

### ⚡ Seckill/Flash Sale System (High-Concurrency)
- **Redis Stock Preloading** - StockWarmer preloads inventory at startup for sub-millisecond reads
- **Atomic Stock Deduction** - Lua scripts ensure atomic Redis operations (no race conditions)
- **One Participation Per User** - 24-hour Redis-based deduplication per product
- **Async Order Processing** - RabbitMQ decouples stock deduction from order creation
- **Reliable Message Queue** - SeckillMessage entity tracks pending/failed messages
- **Idempotency Guarantees** - ReliableMessage table prevents duplicate processing
- **Scheduled Retry Mechanism** - Failed messages retried every 5 seconds with exponential backoff
- **Smart Failure Handling** - Distinguishes transient failures (requeue) from permanent (discard)
- **Message Consumer** - RabbitMQ listener with manual ACK/NACK for reliable processing

**Seckill Flow:**
```
1. User clicks "Buy Now" → SeckillService.doSeckill()
2. Check user eligibility (Redis: seckill:user:{productId}:{userId})
3. Deduct Redis stock atomically (Lua script)
4. Create SeckillMessage (PENDING status) in database
5. SeckillMessageTask (scheduled @5s) sends PENDING messages to RabbitMQ
6. SeckillOrderConsumer receives message
7. Check ReliableMessage for idempotency (messageId + consumerName)
8. Create order, deduct database stock, mark message SUCCESS
9. ACK message to RabbitMQ
```

### 🔒 Distributed Lock System
- **Redisson Multi-Lock** - Prevents concurrent modification of shared resources
- **Deadlock Prevention** - Products locked in sorted order (by ID) to avoid circular waits
- **Lock Parameters** - 10-second wait timeout, 30-second lease time
- **Automatic Unlock** - Always releases locks in finally block, checks `isHeldByCurrentThread()`
- **Use Cases** - Order creation with multiple products, stock reservation

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Client Layer                                │
└─────────────────────────────────────────────────────────────────────┘
                                    │
┌─────────────────────────────────────────────────────────────────────┐
│                  Controllers (REST API Layer)                        │
│  UserController │ ProductController │ OrderController │ SeckillController
│  ├── JWT Authentication Filter                                      │
│  ├── Global Exception Handler                                       │
│  └── Response Wrapper (ResponseResult)                              │
└─────────────────────────────────────────────────────────────────────┘
                                    │
┌─────────────────────────────────────────────────────────────────────┐
│                      Service Layer                                   │
│  UserService │ ProductService │ OrderFacade │ SeckillService         │
│  CartService │ AddressService │ StockService │ TokenBlacklistService │
│  ├── Business Logic & Validation                                    │
│  ├── Transaction Management (@Transactional)                        │
│  └── Distributed Lock Orchestration (Redisson)                      │
└─────────────────────────────────────────────────────────────────────┘
                                    │
┌─────────────────────────────────────────────────────────────────────┐
│                    Repository Layer (JPA)                            │
│  UserRepository │ ProductRepository │ OrderRepository │ CartRepository
│  ├── Custom Queries (JPQL, Native SQL)                              │
│  ├── Pagination & Sorting                                           │
│  └── Entity Relationship Management                                 │
└─────────────────────────────────────────────────────────────────────┘
                                    │
┌───────────────┬──────────────────────────────┬──────────────────────┐
│               │                              │                      │
│    MySQL      │         Redis                │      RabbitMQ        │
│  (Primary DB) │  ┌─────────────────────┐     │  ┌─────────────────┐│
│               │  │ Stock Cache         │     │  │ Seckill Queue   ││
│               │  │ Token Blacklist     │     │  │ Payment Timeout ││
│               │  │ User Participation  │     │  │ Message Retry   ││
│               │  │ Distributed Locks   │     │  └─────────────────┘│
│               │  └─────────────────────┘     │                      │
└───────────────┴──────────────────────────────┴──────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                   Background Workers                                 │
│  SeckillMessageTask (Scheduled @5s) → Retry failed messages          │
│  SeckillOrderConsumer (RabbitMQ Listener) → Process async orders     │
│  StockWarmer (@PostConstruct) → Preload Redis stock at startup       │
└─────────────────────────────────────────────────────────────────────┘
```

### 🎯 Advanced Architectural Patterns

- **Facade Pattern** - `OrderFacade` orchestrates complex multi-service transactions
- **Message Queue Pattern** - Async processing with RabbitMQ for high-concurrency scenarios
- **Distributed Lock Pattern** - Redisson multi-lock prevents race conditions
- **Idempotency Pattern** - `ReliableMessage` entity prevents duplicate processing
- **Custom Exception Hierarchy** - 17 custom exceptions with proper HTTP status codes
- **Entity-First Design** - Services return entities, controllers convert to DTOs
- **Multi-table Transactions** - ACID compliance with Spring's `@Transactional`
- **Advanced JPA Queries** - Custom repository methods with JPQL and native SQL
- **Lua Script Atomicity** - Redis operations use Lua for atomic multi-command execution
- **Scheduled Task Retry** - Exponential backoff for failed message processing

## 📚 API Endpoints

### Authentication
```http
POST /api/users/register     # User registration
POST /api/users/login        # User authentication
GET  /api/users/{username}   # Get user profile
```

### Product Catalog
```http
GET  /api/products           # List products with filtering & pagination
GET  /api/products/{id}      # Get product details
POST /api/products           # Create product (Admin)
PUT  /api/products/{id}      # Update product (Admin)
GET  /api/products/search    # Search products
```

### Shopping Cart
```http
GET    /api/cart             # Get user cart
POST   /api/cart/add         # Add item to cart
PUT    /api/cart/update/{id} # Update cart item quantity
DELETE /api/cart/remove/{id} # Remove item from cart
DELETE /api/cart/clear       # Clear entire cart
```

### Order Management
```http
POST /api/orders             # Create order from cart
GET  /api/orders             # Get user orders
GET  /api/orders/{id}        # Get order details
POST /api/orders/{id}/pay    # Process payment
POST /api/orders/{id}/ship   # Ship order (Admin)
POST /api/orders/{id}/cancel # Cancel order
```

### Flash Sale (Seckill)
```http
POST /api/seckill/{productId}        # Participate in flash sale
POST /api/seckill/enable/{productId} # Enable product for flash sale (Admin)
```

### Address Management
```http
GET    /api/addresses        # Get user addresses
POST   /api/addresses        # Create new address
PUT    /api/addresses/{id}   # Update address
DELETE /api/addresses/{id}   # Delete address
POST   /api/addresses/{id}/default # Set default address
```

### Categories
```http
GET    /api/categories       # List all categories
GET    /api/categories/{id}  # Get category details
POST   /api/categories       # Create category (Admin)
PUT    /api/categories/{id}  # Update category (Admin)
DELETE /api/categories/{id}  # Delete category (Admin)
```

## 🚀 Quick Start

### Prerequisites
- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+** (required)
- **Redis Server** (required for distributed locks, caching, and seckill)
- **RabbitMQ** (required for async message processing)

### Option 1: Docker Compose (Recommended)

The easiest way to run the entire stack locally:

```bash
# Clone the repository
git clone https://github.com/yourusername/ecommerce-backend.git
cd ecommerce-backend

# Start all services (MySQL, Redis, RabbitMQ, Spring Boot app)
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

**Included Services:**
- MySQL 8.0 (port 3306)
- Redis 7.0 (port 6379)
- RabbitMQ 3.12 with Management UI (ports 5672, 15672)
- Spring Boot Application (port 8080)

All services configured with health checks and automatic restart policies.

### Option 2: Manual Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/ecommerce-backend.git
cd ecommerce-backend
```

2. **Install Required Services**
- MySQL 8.0+
- Redis Server
- RabbitMQ

3. **Configure Database**
```bash
mysql -u root -p
CREATE DATABASE ecommerce_db;
```

4. **Update Application Properties**
```properties
# src/main/resources/application-local.properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# RabbitMQ configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

5. **Build and Run**
```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

## 📖 API Documentation

Once the application is running, access the **Swagger UI** documentation at:
```
http://localhost:8080/swagger-ui/index.html
```

This provides interactive API testing with request/response examples.

## 🧪 Testing

### Comprehensive Test Suite (19+ Passing Tests)

The project includes extensive unit and integration tests covering all layers:

**Run All Tests:**
```bash
mvn test
```

**Run Specific Test Class:**
```bash
mvn test -Dtest=ProductServiceImplTest
```

**Run Tests with Coverage:**
```bash
mvn verify
```

**Test Structure:**
- **Service Layer Tests** (19+ tests):
  - `ProductServiceImplTest` - Product CRUD, filtering, pagination
  - `CategoryServiceImplTest` - Hierarchical category operations
  - `CartServiceImplTest` - Cart operations with stock validation
  - `AddressServiceImplTest` - Address management, default address
  - `UserServiceImplTest` - Registration, authentication, role management

- **Repository Tests**:
  - `ProductRepositoryTest` - Custom queries, filtering
  - `CategoryRepositoryTest` - Hierarchy queries
  - `CartItemRepositoryTest` - User-specific queries
  - `UserRepositoryTest` - User lookups
  - `AddressRepositoryTest` - Address queries

- **Controller Tests** (MockMvc):
  - `ProductControllerTest` - REST endpoint testing
  - `CategoryControllerTest` - Category API testing
  - `CartControllerTest` - Cart API testing
  - `UserControllerTest` - Authentication endpoints
  - `AddressControllerTest` - Address API testing

**Testing Patterns:**
- `@Transactional` for automatic rollback
- Mockito for service mocking
- `@WebMvcTest` for controller tests
- `@DataJpaTest` for repository tests
- Integration tests require MySQL and Redis

**GitHub Actions CI/CD:**
- Automated test execution on every push
- Test reports generated with dorny/test-reporter
- PR checks for test failures

## 🔧 Configuration

### JWT Configuration
- **Token Expiration**: 24 hours (configurable)
- **Secret Key**: Stored in application properties (use environment variables in production)
- **Algorithm**: HMAC256

### Database Configuration
- **Connection Pooling**: HikariCP (default)
- **JPA/Hibernate**: Auto DDL generation (disable in production)
- **Transaction Management**: Spring's @Transactional

## 🚀 Deployment

### CI/CD Pipeline (GitHub Actions)

Automated workflow configured in `.github/workflows/ci.yml`:

- **Trigger**: Push to `main` branch
- **Steps**:
  1. Checkout code
  2. Set up Java 17
  3. Build with Maven (`mvn clean install`)
  4. Run JUnit tests (`mvn test`)
  5. Generate test reports with dorny/test-reporter
  6. Fail PR if tests fail

**View Build Status:**
Check the Actions tab on GitHub for build and test results.

### Railway Deployment (Production)

This project is configured for deployment on Railway:

**Deployment Steps:**
1. Connect GitHub repository to Railway
2. Configure environment variables:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `JWT_SECRET`
   - `REDIS_HOST` and `REDIS_PORT`
   - `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
3. Railway auto-deploys on git push to main
4. Access application at Railway-provided URL

**Services Required on Railway:**
- MySQL (Railway MySQL plugin)
- Redis (Railway Redis plugin)
- RabbitMQ (CloudAMQP add-on or custom deployment)

See `RAILWAY_DEPLOYMENT.md` for detailed deployment guide.

### Docker Deployment

**Using Docker Compose (Full Stack):**
```bash
docker-compose up -d
```

**Production Dockerfile:**
```dockerfile
FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/ecommerce-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build and Run:**
```bash
mvn clean package -DskipTests
docker build -t ecommerce-backend .
docker run -p 8080:8080 --env-file .env ecommerce-backend
```

### Production Checklist

- [x] Docker Compose for local development
- [x] GitHub Actions CI/CD pipeline
- [x] Comprehensive test suite
- [x] Environment-specific properties (dev, local, prod)
- [x] Logging configuration (logs/ecommerce.log)
- [ ] Configure production database (MySQL on Railway)
- [ ] Set up Redis in production
- [ ] Set up RabbitMQ in production
- [ ] Configure JWT secret from environment variables
- [ ] Set up monitoring and health checks
- [ ] Configure SSL/TLS certificates
- [ ] Set up backup and disaster recovery

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📋 Project Status

### ✅ Completed Features

- **User Management** - JWT authentication, token blacklist, role-based authorization
- **Product Catalog** - Full CRUD with advanced filtering, pagination, search
- **Category Management** - Hierarchical categories with unlimited nesting
- **Shopping Cart** - Real-time cart management with stock validation
- **Order System** - Complete order lifecycle with OrderFacade pattern and distributed locking
- **Address Management** - Multiple shipping addresses with default address support
- **Seckill/Flash Sale System** - High-concurrency flash sales with Redis + RabbitMQ
- **Distributed Locking** - Redisson multi-lock for race condition prevention
- **Message Queue** - Reliable async processing with idempotency guarantees
- **Testing Infrastructure** - 19+ JUnit tests with GitHub Actions CI/CD
- **Docker Compose** - Local development environment with all services
- **API Documentation** - Comprehensive Swagger/OpenAPI documentation
- **Deployment Pipeline** - GitHub Actions CI/CD with automated testing

### 🔄 In Progress

- **Frontend Integration** - React frontend with Vercel deployment
- **Payment Gateway Integration** - Stripe configured, integration in progress

### 📋 Planned Enhancements

- **Metrics & Monitoring** - Prometheus/Grafana integration
- **Email Notifications** - Order confirmation, shipping updates
- **Advanced Search** - Elasticsearch integration for full-text search
- **Image Upload** - AWS S3 integration for product images
- **Rate Limiting** - API rate limiting with Redis
- **Caching Optimization** - Multi-level caching strategy

## 👨‍💻 Author

**Abel** - Full Stack Developer

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🌟 Project Highlights

This project demonstrates **enterprise-grade** Spring Boot development with advanced distributed systems patterns:

- **High-Concurrency Handling** - Flash sale system handling thousands of concurrent requests
- **Distributed Systems** - Redis-based distributed locking and caching
- **Async Processing** - RabbitMQ message queue with retry mechanisms
- **Idempotency Guarantees** - Duplicate request prevention in distributed environment
- **Comprehensive Testing** - 19+ unit/integration tests with CI/CD pipeline
- **Production-Ready** - Docker Compose, environment configs, health checks, logging
- **Clean Architecture** - Facade pattern, entity-first design, custom exception hierarchy

**Perfect for:**
- Learning advanced Spring Boot concepts
- Understanding distributed systems patterns
- Interview preparation (demonstrates real-world scenarios)
- Portfolio project showcasing enterprise-level development skills
