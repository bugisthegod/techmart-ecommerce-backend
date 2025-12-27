-- Insert Roles
INSERT INTO roles (id, name, code, description, status, created_at, updated_at) VALUES
(1, 'Customer', 'CUSTOMER', 'Regular customer with basic access', 1, NOW(), NOW()),
(2, 'Super Admin', 'SUPER_ADMIN', 'Full system access', 1, NOW(), NOW()),
(3, 'Order Admin', 'ORDER_ADMIN', 'Manage orders and customer data', 1, NOW(), NOW()),
(4, 'Product Admin', 'PRODUCT_ADMIN', 'Manage products and categories', 1, NOW(), NOW());

-- Insert Admin User
-- Password is 'admin123' hashed with BCrypt
INSERT INTO users (id, username, password, email, phone, status, created_at, updated_at) VALUES
(1, 'admin', '$2a$12$J.BdO7u1z2I3UWJBp/qvLObsK0I0CyfT0FUJyzqsRR52UCE0aZBbe', 'admin@ecommerce.com', '1234567890', 1, NOW(), NOW());

-- Insert Test Customer User
-- Password is 'customer123' hashed with BCrypt
INSERT INTO users (id, username, password, email, phone, status, created_at, updated_at) VALUES
(2, 'customer', '$2a$12$583OwFSk43nhT2Mp7c.GlOsQ3YfrMwW2WbwP0veaPIfjcfKGd8.he', 'customer@test.com', '0987654321', 1, NOW(), NOW());

-- Assign Roles to Users
INSERT INTO user_roles (user_id, role_id, created_at) VALUES
(1, 2, NOW()), -- admin has SUPER_ADMIN role
(2, 1, NOW()); -- customer has CUSTOMER role

-- Insert Categories (hierarchical structure)
INSERT INTO categories (id, name, parent_id, icon, sort_order, status, created_at, updated_at) VALUES
-- Top-level categories
(1, 'Electronics', 0, 'fa-laptop', 1, 1, NOW(), NOW()),
(2, 'Clothing', 0, 'fa-tshirt', 2, 1, NOW(), NOW()),
(3, 'Books', 0, 'fa-book', 3, 1, NOW(), NOW()),
(4, 'Home & Garden', 0, 'fa-home', 4, 1, NOW(), NOW()),
(5, 'Sports', 0, 'fa-football', 5, 1, NOW(), NOW()),

-- Sub-categories for Electronics
(6, 'Laptops', 1, 'fa-laptop-code', 1, 1, NOW(), NOW()),
(7, 'Smartphones', 1, 'fa-mobile-alt', 2, 1, NOW(), NOW()),
(8, 'Accessories', 1, 'fa-headphones', 3, 1, NOW(), NOW()),

-- Sub-categories for Clothing
(9, 'Men', 2, 'fa-male', 1, 1, NOW(), NOW()),
(10, 'Women', 2, 'fa-female', 2, 1, NOW(), NOW()),
(11, 'Kids', 2, 'fa-child', 3, 1, NOW(), NOW());

-- Insert Sample Products
INSERT INTO products (id, name, category_id, price, original_price, description, main_image, images, stock, sales, status, created_at, updated_at) VALUES
-- Electronics - Laptops
(1, 'MacBook Pro 16"', 6, 2499.00, 2799.00, 'Apple M2 Max chip, 16GB RAM, 512GB SSD. Powerful laptop for professionals.',
 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8',
 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8,https://images.unsplash.com/photo-1593642532842-98d0fd5ebc1a',
 50, 120, 1, NOW(), NOW()),

(2, 'Dell XPS 15', 6, 1899.00, 2199.00, 'Intel Core i7, 16GB RAM, 1TB SSD. Premium Windows laptop with stunning display.',
 'https://images.unsplash.com/photo-1593642532842-98d0fd5ebc1a',
 'https://images.unsplash.com/photo-1593642532842-98d0fd5ebc1a',
 30, 85, 1, NOW(), NOW()),

(3, 'Lenovo ThinkPad X1', 6, 1599.00, 1899.00, 'Business-class laptop with excellent keyboard and battery life.',
 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed',
 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed',
 40, 60, 1, NOW(), NOW()),

-- Electronics - Smartphones
(4, 'iPhone 15 Pro', 7, 999.00, 1099.00, 'Latest iPhone with A17 Pro chip, 128GB storage, Titanium design.',
 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5',
 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5',
 100, 250, 1, NOW(), NOW()),

(5, 'Samsung Galaxy S24', 7, 849.00, 999.00, 'Android flagship with amazing camera and display.',
 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c',
 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c',
 80, 180, 1, NOW(), NOW()),

-- Electronics - Accessories
(6, 'Sony WH-1000XM5', 8, 349.00, 399.00, 'Industry-leading noise canceling wireless headphones.',
 'https://images.unsplash.com/photo-1546435770-a3e426bf472b',
 'https://images.unsplash.com/photo-1546435770-a3e426bf472b',
 150, 320, 1, NOW(), NOW()),

(7, 'Apple AirPods Pro', 8, 249.00, 279.00, 'Active noise cancellation, Spatial audio, Premium sound quality.',
 'https://images.unsplash.com/photo-1606841837239-c5a1a4a07af7',
 'https://images.unsplash.com/photo-1606841837239-c5a1a4a07af7',
 200, 450, 1, NOW(), NOW()),

-- Clothing - Men
(8, 'Classic Denim Jacket', 9, 89.00, 129.00, 'Timeless denim jacket for casual style.',
 'https://images.unsplash.com/photo-1551028719-00167b16eac5',
 'https://images.unsplash.com/photo-1551028719-00167b16eac5',
 60, 95, 1, NOW(), NOW()),

(9, 'Cotton T-Shirt Pack', 9, 29.99, 39.99, 'Pack of 3 premium cotton t-shirts in various colors.',
 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab',
 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab',
 200, 380, 1, NOW(), NOW()),

-- Clothing - Women
(10, 'Summer Floral Dress', 10, 79.00, 99.00, 'Lightweight floral print dress perfect for summer.',
 'https://images.unsplash.com/photo-1595777457583-95e059d581b8',
 'https://images.unsplash.com/photo-1595777457583-95e059d581b8',
 75, 140, 1, NOW(), NOW()),

-- Books
(11, 'Clean Code by Robert Martin', 3, 39.99, 49.99, 'A must-read for software developers. Learn to write better code.',
 'https://images.unsplash.com/photo-1532012197267-da84d127e765',
 'https://images.unsplash.com/photo-1532012197267-da84d127e765',
 120, 280, 1, NOW(), NOW()),

(12, 'Design Patterns: Elements of Reusable OO Software', 3, 44.99, 54.99, 'Classic book on software design patterns.',
 'https://images.unsplash.com/photo-1544947950-fa07a98d237f',
 'https://images.unsplash.com/photo-1544947950-fa07a98d237f',
 90, 150, 1, NOW(), NOW()),

-- Home & Garden
(13, 'Robot Vacuum Cleaner', 4, 299.00, 399.00, 'Smart robot vacuum with app control and scheduling.',
 'https://images.unsplash.com/photo-1558317374-067fb5f30001',
 'https://images.unsplash.com/photo-1558317374-067fb5f30001',
 45, 78, 1, NOW(), NOW()),

-- Sports
(14, 'Yoga Mat Premium', 5, 39.99, 59.99, 'Non-slip yoga mat with carrying strap. 6mm thick.',
 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f',
 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f',
 100, 220, 1, NOW(), NOW()),

(15, 'Adjustable Dumbbell Set', 5, 249.00, 299.00, 'Space-saving adjustable dumbbells, 5-52.5 lbs per hand.',
 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438',
 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438',
 30, 65, 1, NOW(), NOW());
