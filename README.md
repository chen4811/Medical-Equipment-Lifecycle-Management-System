# MELMS — Medical Equipment Lifecycle Management System

Developed by team RETURN FALSE for DI41009 — Industrial Team Project (SEM 1 25/26).

## Overview
MELMS manages the full lifecycle of hospital equipment: onboarding, usage logging, maintenance/repairs, procurement, and end‑of‑life scrapping. It provides role‑based workflows for admins, equipment managers, procurement, and department users.

## Tech Stack
- Backend: Spring Boot 3, Java 17, MyBatis, MySQL
- Frontend: Vue 3, Vite, Pinia, Vue Router, ApexCharts

## Repository Structure
- `frontend/` — Vue 3 SPA (Vite)
- `src/` — Spring Boot backend (Java)
- `pom.xml` — Maven project

## Prerequisites
- Java 17+, Maven 3.9+
- Node.js 18+, npm
- MySQL 8.x

## Quick Start
### Backend
1) Configure database/mail in `src/main/resources/application.yml` or via environment variables mapping to Spring properties.
   - `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
   - `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`
   - `upload.base-dir`
2) Start API:
```bash
mvn spring-boot:run
```
Default port: 8080

### Frontend
1) Install & run:
```bash
cd frontend
npm install
npm run dev
```
Dev server: `http://localhost:5173`

## Production Build
- Backend (fat jar):
```bash
mvn clean package
java -jar target/MELMS-0.0.1-SNAPSHOT.jar
```
- Frontend (static assets):
```bash
cd frontend
npm run build
```
Outputs to `frontend/dist/` (serve via Nginx/Apache or behind a reverse proxy).

## Key Modules
- Admin: users/orgs, settings, logs
- Equipment: onboarding, ledger, repairs, scrapping
- Department: my devices, usage, repair requests
- Procurement: plans, orders, bids, vendors, contracts, receiving
- Auth: login and role‑based access

## Notes
- Never commit secrets; use environment variables for sensitive settings.
- Ensure database schema/seed data are prepared before running.

## License
Academic use for DI41009 course project. © Team RETURN FALSE.
