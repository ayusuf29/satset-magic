# Product Requirement Document (PRD) - E-Commerce Platform Baseline

## 1. Executive Summary
Platform e-commerce modern berskala enterprise yang tangguh, modular, dan memiliki skalabilitas tinggi, mencakup pengalaman belanja pelanggan, manajemen katalog produk, pemrosesan pesanan, integrasi gateway pembayaran, serta dashboard admin.

## 2. Core Functional Requirements
1. **User & Identity Service**:
   - Autentikasi JWT / OAuth2, Role-based Access Control (Customer, Merchant, Admin).
2. **Product Catalog Service**:
   - Pencarian produk multi-kriteria, kategori berjenjang, inventaris real-time, manajemen varian.
3. **Cart & Checkout Service**:
   - Keranjang belanja persisten (Redis + DB), kalkulasi ongkir, diskon & kupon.
4. **Order & Payment Service**:
   - State machine pemesanan (PENDING, PAID, SHIPPED, COMPLETED, CANCELLED).
   - Idempotent payment webhook handling, integrasi payment gateway.
5. **Notification Service**:
   - Email transactional, push notifications, dan Webhook notification.

## 3. Non-Functional Requirements
- **Availability**: 99.9% uptime target.
- **Latency**: P95 response time < 200ms untuk browsing katalog.
- **Scalability**: Horizontal auto-scaling container.
- **Security**: OWASP Top 10 compliance, input sanitization, data encryption at rest & in transit.
- **Test Coverage**: Minimum 70% line & branch coverage wajib terpenuhi.
