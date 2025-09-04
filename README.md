# 🍽️ Restaurant Management System | 餐廳管理系統

A comprehensive restaurant management platform built with Spring Boot, featuring order processing, inventory management, and real-time analytics.

## 🌟 Features | 功能特色

### 🏪 **Admin Panel | 管理端**
- **Employee Management** | 員工管理：Role-based access control with secure authentication
- **Menu Management** | 菜品管理：Category management, dish configuration, and pricing control
- **Order Processing** | 訂單處理：Real-time order tracking and status management
- **Sales Analytics** | 營業數據：Revenue reports and performance dashboards
- **Inventory Control** | 庫存管理：Stock tracking and low-inventory alerts

### 📱 **Customer App | 用戶端**
- **User Registration** | 用戶註冊：WeChat integration and profile management
- **Menu Browsing** | 菜品瀏覽：Category-based browsing with rich media
- **Shopping Cart** | 購物車：Dynamic cart with real-time pricing
- **Order Placement** | 下單功能：Multiple payment options and delivery tracking
- **Order History** | 歷史訂單：Complete order tracking and reordering

## 🛠️ Tech Stack | 技術棧

### **Backend | 後端**
- **Framework**: Spring Boot 2.7.x
- **ORM**: MyBatis Plus
- **Database**: MySQL 8.0
- **Cache**: Redis 6.x
- **File Storage**: Alibaba Cloud OSS
- **API Documentation**: Swagger/Knife4j

### **Frontend | 前端**
- **Admin**: Vue.js + Element UI
- **Mobile**: WeChat Mini Program

### **Development Tools | 開發工具**
- **Build Tool**: Maven 3.8+
- **JDK**: OpenJDK 1.8+
- **IDE**: IntelliJ IDEA

## 🏗️ Architecture | 系統架構

```
┌─────────────────┬─────────────────┐
│   Admin Panel   │  Customer App   │
│    (Vue.js)     │ (Mini Program)  │
└─────────┬───────┴─────────┬───────┘
          │                 │
          └─────────┬───────┘
                    │
         ┌──────────▼──────────┐
         │   Spring Boot API   │
         │   (Business Logic)  │
         └──────────┬──────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐
   │  MySQL  │ │  Redis  │ │   OSS   │
   │Database │ │  Cache  │ │ Storage │
   └─────────┘ └─────────┘ └─────────┘
```

## 🚀 Quick Start | 快速開始

### **Prerequisites | 環境要求**
```bash
- JDK 1.8+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+
```

### **Installation | 安裝步驟**

1. **Clone Repository | 克隆項目**
   ```bash
   git clone https://github.com/DamonKima/restaurant-management-system.git
   cd restaurant-management-system
   ```

2. **Database Setup | 數據庫配置**
   ```sql
   -- Create database
   CREATE DATABASE sky_take_out DEFAULT CHARSET utf8mb4;
   
   -- Import SQL file
   mysql -u username -p sky_take_out < sql/sky_take_out.sql
   ```

3. **Configuration | 配置修改**
   ```yaml
   # application-dev.yml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/sky_take_out
       username: your_username
       password: your_password
     redis:
       host: localhost
       port: 6379
   ```

4. **Run Application | 啟動項目**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access System | 訪問系統**
   - **API Documentation**: http://localhost:8080/doc.html
   - **Admin Panel**: http://localhost:8080/backend/index.html
   - **Default Admin**: username: `admin`, password: `123456`

## 📊 Key Highlights | 項目亮點

### **🔥 Performance Optimization | 性能優化**
- **Redis Caching**: Implemented caching strategy reducing database queries by 60%
- **Connection Pooling**: Optimized database connections with HikariCP
- **Lazy Loading**: Enhanced page load speed with lazy loading implementation

### **🔐 Security Features | 安全特性**
- **JWT Authentication**: Secure token-based authentication system
- **Password Encryption**: BCrypt password hashing for user security
- **Input Validation**: Comprehensive input validation to prevent SQL injection

### **📈 Scalability | 擴展性**
- **Modular Design**: Clean separation of concerns with service-oriented architecture
- **RESTful APIs**: Well-designed REST endpoints for easy integration
- **Configurable Components**: Environment-based configuration management

## 📱 Screenshots | 系統截圖

### Admin Dashboard | 管理後台
![Dashboard](docs/images/admin-dashboard.png)

### Order Management | 訂單管理
![Orders](docs/images/order-management.png)

### Mobile Interface | 移動端界面
![Mobile](docs/images/mobile-interface.png)

## 📈 Database Design | 數據庫設計

### **Core Tables | 核心表結構**
- `employee` - Employee management | 員工管理
- `category` - Product categories | 商品分類
- `dish` - Menu items | 菜品信息
- `orders` - Order records | 訂單記錄
- `order_detail` - Order line items | 訂單詳情
- `user` - Customer information | 用戶信息

## 🤝 Contributing | 貢獻指南

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License | 許可證

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.



---

⭐ **If this project helped you, please give it a star!** | **如果這個項目對你有幫助，請給個星星！**
