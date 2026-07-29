# KaspiTracker

**Kaspi Bank PDF Statement Parser & Expense Analytics**

Автоматизирует разбор PDF-выписок Kaspi Bank, категоризирует транзакции
через fuzzy matching и отображает аналитику расходов через веб-интерфейс,
доступный с мобильного телефона.

> Полный roadmap, архитектурные решения и scope проекта —
> в [Technical Specification.md](Technical%20Specification.md).

---

## Быстрый старт (локальная разработка)

### Требования

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Настроить переменные окружения

```bash
# Скопируй шаблон и задай свои значения
cp .env.example .env
# Отредактируй .env — измени DB_PASSWORD на что-то надёжное
```

### 2. Поднять PostgreSQL

```bash
docker compose up -d
```

Убедись, что контейнер здоров:

```bash
docker compose ps
# kaspitracker-postgres должен быть в статусе "healthy"
```

### 3. Запустить приложение

```bash
mvn spring-boot:run
```

Приложение стартует на `http://localhost:8080`.
Flyway автоматически применит миграции при первом запуске.

### 4. Остановить БД

```bash
docker compose down        # контейнер + данные сохраняются в volume
docker compose down -v     # + удалить volume с данными БД
```

---

## Аутентификация

Для защиты личных финансовых данных приложение закрыто базовой аутентификацией 
(только один пользователь).

При локальном запуске по умолчанию логин `admin` и пароль `changeme`. 

Для переопределения используйте переменные окружения (например, через файл `.env`):
- `APP_USERNAME` (логин, строка)
- `APP_PASSWORD` (пароль, **обязательно BCrypt-хэш**)

**ВАЖНО**: Пароль должен передаваться в виде сгенерированного BCrypt-хэша, а не в открытом виде!
Сгенерировать хэш можно через любой [онлайн-генератор](https://bcrypt-generator.com/) 
или запустив простой Java-код:
```java
System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("ваш_пароль"));
```

Пример `.env`:
```env
APP_USERNAME=admin
# Хэш для "changeme"
APP_PASSWORD=$2a$10$pl5W3xJJpejkcFZ2DEaZ6.hPWk.z36LjORBmTv8jL4fUr5.lEOKf6
```

Обязательно смените дефолтные значения перед деплоем на публичный сервер!

---

## Сборка

```bash
mvn clean install          # сборка + тесты (требует запущенный Docker)
mvn clean package -DskipTests   # только сборка без тестов
```

---

## Deployment (Render + Supabase)

Проект настроен для деплоя на **Render** (веб-сервис) с базой данных на **Supabase** (PostgreSQL Session Pooler).

Вам потребуется задать следующие Environment Variables в настройках Web Service на Render:
- `SPRING_PROFILES_ACTIVE=prod` (активирует `application-prod.yml`)
- `PORT` (порт, на котором запускается приложение. Задаётся Render, мы просто используем эту переменную)
- `DATABASE_URL` — ссылка на пул соединений Supabase, например: `jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`
- `DATABASE_USERNAME` — логин Supabase (с учётом пулера: `postgres.<project-ref>`)
- `DATABASE_PASSWORD` — пароль от базы данных
- `APP_USERNAME` — логин для авторизации в KaspiTracker (например, `admin`)
- `APP_PASSWORD` — **BCrypt-хэш** пароля для авторизации

> **Примечание по Supabase Free Tier:**
> На бесплатном тарифе Supabase приостанавливает базу данных (переводит в спящий режим) после 1 недели неактивности.
> Если вы долго не пользовались приложением, и оно перестало работать — зайдите в дашборд Supabase и нажмите кнопку "Restore / Wake up" для вашего проекта.

---

## Структура проекта

```
src/main/java/com/sultan/kaspitracker/
├── KaspiTrackerApplication.java   # точка входа
├── controller/                    # REST controllers (Milestone 6)
├── service/                       # бизнес-логика (Milestones 2–5)
├── repository/                    # Spring Data JPA репозитории (Milestone 4)
├── entity/                        # JPA сущности: Statement, Transaction, Category... (Milestone 4)
├── dto/                           # Request/Response DTO (Milestone 6)
└── config/                        # Spring-конфиги: Security, OpenAPI (Milestones 6, 8)

src/main/resources/
├── application.yml                # конфигурация (всё через переменные окружения)
└── db/migration/
    └── V1__init.sql               # Flyway baseline (пустой, таблицы — в V2+)
```

---

## Текущий статус

| Milestone | Описание | Статус |
|-----------|----------|--------|
| 1 | Скелет проекта: Spring Boot + PostgreSQL + Flyway | ✅ Done |
| 2 | PDF → сырой текст (PDFBox) | ✅ Done |
| 3 | Сырой текст → структурированные транзакции | ✅ Done |
| 4 | Сохранение в БД, защита от дублей | ✅ Done |
| 5 | Категоризация через fuzzy matching | ✅ Done |
| 6 | REST API | ✅ Done |
| 7 | Веб-интерфейс (Thymeleaf) | ✅ Done |
| 8 | Spring Security (аутентификация) | ✅ Done |
| 9 | Тесты (JUnit 5, Testcontainers) | ⬜ Planned |
| 10 | Docker Compose, GitHub Actions CI | ✅ Done |
| 11 | Деплой (Railway/Render/Fly.io) | ✅ Done |
