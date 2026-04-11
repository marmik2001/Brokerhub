# BrokerHub

BrokerHub is a **family and group investment platform** that consolidates multiple brokerage accounts into a single dashboard. It enables families or groups of friends to view, manage, and analyze their combined portfolios with **role-based access control (RBAC)** and **privacy settings**.

---

## 🚀 Features

- **Unified Portfolio View**  
  Track holdings across multiple brokers in one place.

- **Role-Based Access Control (RBAC)**
  - **Admin**: Add/Delete/Invite members, manage broker credentials, edit all settings.
  - **Member**: Manage their own broker credentials, personal visibility, and profile.

- **Flexible Visibility Modes** for holdings
  - **Full View** – Show exact details.
  - **Abstract View** – Show generalized info (e.g., sector allocation, % changes).
  - **Hidden** – Completely private.

- **Family/Group Management**  
  Collaboratively view and compare portfolios while respecting privacy.

- **Extensible Broker Support**  
  Currently integrating **Dhan**, with ability to extend to Zerodha, Groww, AngelOne, and more.

- **Market Data Microservice**  
  A dedicated microservice powered by **yFinance** provides live market data (prices, P&L, etc.) with caching to reduce API calls.

---

## 🛠️ Tech Stack

- **Backend**: Java Spring Boot
- **Frontend**: React + Tailwind CSS
- **Database**: PostgreSQL
- **Market Data**: FASTAPI microservice using yFinance library

---

## 🐳 Run with Docker Compose (Recommended for local development)

You can run the entire stack (frontend + backend + market-data-service + postgres) with one command.

### 1) Prerequisites

- Install Docker Desktop
- Ensure Docker Desktop is running

### 2) First-time setup

From the project root:

```bash
cp .env.example .env
```

Open `.env` and update sensitive values:

- `JWT_SECRET`
- `APP_SECURITY_MASTER_KEY_BASE64` (base64-encoded 32-byte key)

### 3) Start all services

```bash
docker compose up --build
```

### 4) Access services

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Market data service: http://localhost:8000
- Postgres: localhost:5432

### 5) Useful commands

Stop all containers:

```bash
docker compose down
```

Stop and remove DB volume/data:

```bash
docker compose down -v
```

View logs for one service:

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f market-data-service
docker compose logs -f db
```

### 6) Notes

- Backend connects to Postgres using Docker hostname `db` (not localhost).
- Backend connects to market data service using Docker hostname `market-data-service`.
- Frontend uses Vite proxy to route `/api` requests to backend.
- `.env` is gitignored; keep real secrets there.

---
