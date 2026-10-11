# Design System & Technical Architecture Specification

## 1. System Topology
- **API Gateway**: Reverse proxy & rate limiting (Spring Cloud Gateway / Envoy / Nginx).
- **Service Architecture**: Modular Microservices atau Modular Monolith (ditentukan pada Phase 1).
- **Message Broker**: Event-driven architecture menggunakan Apache Kafka atau RabbitMQ.
- **Caching Layer**: Redis Cluster untuk session store, hot catalog cache, dan cart persistence.
- **Database Strategy**: Database-per-service (PostgreSQL untuk transactional data) dengan migrasi Flyway/Liquibase.

## 2. UI / UX Design Tokens (Target Frontend)
- **Design Paradigm**: Headless UI + Tailwind CSS (atau token yang diekstrak dari Figma).
- **Color Palette Baseline**:
  - Primary: `#2563EB` (Blue 600)
  - Secondary: `#4F46E5` (Indigo 600)
  - Accent: `#F59E0B` (Amber 500)
  - Surface Dark: `#0F172A` (Slate 900)
  - Surface Light: `#F8FAFC` (Slate 50)
- **Typography**: Inter / Outfit sans-serif, modular type scale (12px, 14px, 16px, 20px, 24px, 32px).
- **Component Primitives**: Buttons, Inputs, Modals, Drawers, Toast Notifications, Responsive Tables.
