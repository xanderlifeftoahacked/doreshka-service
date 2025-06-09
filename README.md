# Doreshka Platform

**Doreshka Platform** — тестирующая система для проведения контестов.

## Архитектура

Платформа состоит из двух независимых микросервисов:

- **Contest Service** (port 8080) — управление контестами, задачами и пользователями
- **Judging Service** (port 8081) — тестирование отправленных решений (сделано отдельным сервисом для горизонтального масштабирования)

## Стек

- **Java 21**
- **Quarkus 3.22.3**
- **Maven**
- **PostgreSQL 15**
- **Hibernate Reactive**
- **Panache**

## Детали проекта

### Производительность
- **Quarkus** является значительно более быстрым фреймворком по сравнению со SpringBoot
- **Реактивное программирование** - весь код асинхронный
- **Нативная компиляция** с GraalVM (опционально)

### Функциональность
- **Управления пользователями** - система доступа к контестам
- **Управление соревнованиями** — создание и настройка контестов
- **Система задач** — загрузка и управление задачами, тестами
- **Тестирование** — проверка решений на тестовых данных

## Зависимости

- **Linux**
- **Java 21**
- **Maven**
- **Docker Compose**

## Запуск

### 1. Клонирование репозитория
```bash
git clone git@github.com:xanderlifeftoahacked/doreshka-service.git
cd doreshka-service
```

### 2. Запуск базы данных
```bash
sudo docker-compose up -d
```

### 3. Запуск сервисов (dev mode)

#### Contest Service
```bash
cd contest-service
mvn quarkus:dev
```

#### Judging Service
```bash
cd judging-service
mvn quarkus:dev
```

### 4. Проверка работоспособности

После запуска сервисы будут доступны по адресам:
- **Contest Service**: http://localhost:8080
- **Judging Service**: http://localhost:8081
- **Swagger UI Contest**: http://localhost:8080/q/swagger-ui
- **Swagger UI Judging**: http://localhost:8081/q/swagger-ui

## Настройка

### Конфигурация базы данных

По умолчанию используются следующие настройки для PostgreSQL:
```properties
Database: doreshka-service
Username: user
Password: user
Port: 5432
```

### JWT ключи

Для работы аутентификации необходимо сгенерировать ключики:

```bash
openssl genrsa -out privateKey.pem 2048
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
cp -t ./judging-service/src/main/resources privateKey.pem publicKey.pem 
cp -t ./contest-service/src/main/resources privateKey.pem publicKey.pem 
```

## Структура проекта

```
dorkacur/
├── contest-service/          # Основной сервис управления соревнованиями
│   ├── src/main/java/ru/doreshka/
│   │   ├── resource/         # REST контроллеры
│   │   ├── service/          # Бизнес-логика
│   │   ├── domain/           # Модели данных
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── security/         # Настройки безопасности
│   │   └── config/           # Конфигурация
│   └── src/main/resources/
├── judging-service/          # Сервис проверки решений
│   ├── src/main/java/ru/doreshka/judging/
│   │   ├── resource/         # REST контроллеры
│   │   ├── service/          # Логика тестирования
│   │   ├── entity/           # Модели данных 
│   │   └── dto/              # Data Transfer Objects
│   └── src/main/resources/
├── solutions/                # Директория с решениями пользователей
└── problem_tests/            # Тесты для задач 
```

## Тесты

На данный момент покрытие следующее (ничего не в игноре):

![](https://i.yapx.cc/ZWyC9.png)

## Развитие

Архитектура подразумевает хорошую возможность для расширения.
Базовый функционал реализован, но, конечно, до ЯКонтеста еще далеко. Но, имея такой фундамент и пару знакомых, можно довольно быстро довести проект до чего-то стоящего.

На самом деле, я совершил ошибку, начав писать реактивный код.

Особой необходимости в этом для такого сервиса на самом деле нет.

Так что перед развитем стоило бы произвести большой рефакторинг.


---

p.s. это MMMMVP (стоило начинать делать немного раньше)
