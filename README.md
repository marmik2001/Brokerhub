# BrokerHub

BrokerHub is an open-source **family and group investment platform** that consolidates multiple brokerage accounts into a single dashboard.  
It enables families or groups of friends to view, manage, and analyze their combined portfolios with **role-based access control (RBAC)** and **privacy settings**.

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
