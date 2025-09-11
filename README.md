# E-Commerce Platform Backend

A comprehensive **Spring Boot** e-commerce backend system with advanced features including user management, product catalog, shopping cart, order processing, and secure JWT authentication.

## 🚀 Project Overview

This is a **production-ready** e-commerce backend API built with modern Java technologies, featuring:

- **RESTful API Design** with complete CRUD operations
- **JWT Authentication & Authorization** with role-based access control
- **Advanced Order Management** with state machine workflow
- **Real-time Inventory Tracking** with stock validation
- **Comprehensive Business Logic** with transaction management
- **Professional Architecture** following Spring Boot best practices

## 🛠️ Technology Stack

| Category | Technologies |
|----------|-------------|
| **Backend Framework** | Spring Boot 3.5.4, Spring Security, Spring Data JPA |
| **Database** | MySQL 8.0+ with JPA/Hibernate ORM |
| **Caching** | Redis for session management |
| **Authentication** | JWT (JSON Web Tokens) with BCrypt encryption |
| **API Documentation** | Swagger/OpenAPI 3.0 |
| **Build Tool** | Maven |
| **Java Version** | Java 17+ |

## 📦 Core Features

### 🔐 User Management
- **User Registration & Authentication** with email validation
- **JWT Token-based Security** with refresh token support
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
- **Transaction Management** ensuring data consistency
- **Payment Integration** ready (currently simulated)
- **Order Tracking** with status updates
- **Inventory Reservation** during order processing

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │────│    Services     │────│  Repositories   │
│   (API Layer)   │    │ (Business Logic)│    │  (Data Access)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ├── JWT Filter           ├── OrderFacade        ├── Custom Queries
         ├── Exception Handler    ├── ValidationService  ├── Pagination Support
         └── Response Formatter   └── TransactionMgmt    └── Entity Relationships
```

### 🎯 Advanced Features

- **OrderFacade Pattern** - Complex business operation orchestration
- **Custom Exception Handling** - Meaningful error messages and proper HTTP status codes  
- **Entity-First Design** - Clean separation between data transfer and business logic
- **Multi-table Transactions** - ACID compliance for complex operations
- **Advanced JPA Queries** - Optimized database operations with custom repository methods

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

## 🚀 Quick Start

### Prerequisites
- **Java 17+**
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis Server** (optional, for caching)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/ecommerce-backend.git
cd ecommerce-backend
```

2. **Configure Database**
```bash
# Create MySQL database
mysql -u root -p
CREATE DATABASE ecommerce_db;
```

3. **Update Application Properties**
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

4. **Run the Application**
```bash
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

Run the test suite:
```bash
mvn test
```

For integration testing:
```bash
mvn verify
```

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

### Production Checklist
- [ ] Configure environment-specific properties
- [ ] Set up proper logging configuration
- [ ] Configure external database
- [ ] Set up Redis for session management
- [ ] Configure JWT secret from environment variables
- [ ] Set up monitoring and health checks

### Docker Support (Optional)
```dockerfile
# Basic Dockerfile structure
FROM openjdk:17-jre-slim
COPY target/ecommerce-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📋 Project Status

- ✅ **User Management** - Complete with JWT authentication
- ✅ **Product Catalog** - Full CRUD with advanced filtering
- ✅ **Shopping Cart** - Real-time cart management
- ✅ **Order System** - Complete order lifecycle with state management
- ✅ **Address Management** - Multiple shipping addresses
- ✅ **API Documentation** - Comprehensive Swagger documentation
- 🔄 **Frontend Integration** - In Progress
- 📋 **Payment Gateway** - Ready for integration
- 📋 **Deployment Scripts** - Planned

## 👨‍💻 Author

**Abel** - Full Stack Developer

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

> **Note**: This is a learning project demonstrating advanced Spring Boot concepts including transaction management, security implementation, and complex business logic orchestration.
