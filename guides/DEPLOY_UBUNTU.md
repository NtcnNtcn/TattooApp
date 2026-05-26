# Развертывание бэкенда TattooApp на Ubuntu Server

Эта инструкция описывает пошаговое развертывание бэкенда (FastAPI + PostgreSQL) на сервере под управлением Ubuntu 22.04/24.04 LTS.

---

## 1. Подготовка сервера

### 1.1 Обновление системы

```bash
sudo apt update && sudo apt upgrade -y
```

### 1.2 Установка базовых инструментов

```bash
sudo apt install -y curl wget git nano ufw
```

---

## 2. Установка Docker и Docker Compose

Рекомендуемый способ развертывания — через Docker Compose.

### 2.1 Установка Docker

```bash
# Добавление официального GPG-ключа Docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Добавление репозитория Docker
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Установка Docker Engine
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### 2.2 Проверка установки Docker

```bash
sudo docker --version
sudo docker compose version
```

### 2.3 Добавление текущего пользователя в группу docker (опционально)

```bash
sudo usermod -aG docker $USER
newgrp docker
```

---

## 3. Клонирование проекта

```bash
cd ~
git clone <URL_репозитория> TattooApp
cd TattooApp
```

> Если проект переносится вручную, загрузите папку `backend/` и `docker-compose.yml` на сервер через `scp` или `rsync`.

---

## 4. Конфигурация окружения

### 4.1 Создание файла переменных окружения

```bash
cd ~/TattooApp/backend
cp .env.example .env
nano .env
```

### 4.2 Обязательные параметры для заполнения

| Переменная | Описание | Пример |
|---|---|---|
| `DATABASE_URL` | Строка подключения к PostgreSQL | `postgresql+asyncpg://tattoo_user:tattoo_pass@db:5432/tattoo_db` |
| `SECRET_KEY` | Секретный ключ для JWT (минимум 32 символа) | `openssl rand -hex 32` |
| `DEFAULT_OWNER_EMAIL` | Email владельца по умолчанию | `owner@studio.ru` |
| `DEFAULT_OWNER_PASSWORD` | Пароль владельца | сгенерируйте надежный пароль |
| `DEFAULT_ADMIN_EMAIL` | Email администратора | `admin@studio.ru` |
| `DEFAULT_ADMIN_PASSWORD` | Пароль администратора | сгенерируйте надежный пароль |
| `CORS_ORIGINS` | Домены, с которых разрешены запросы | `https://yourdomain.com` |

Генерация секретного ключа:

```bash
openssl rand -hex 32
```

### 4.3 Пример минимального `.env` для Docker

```env
DATABASE_URL=postgresql+asyncpg://tattoo_user:tattoo_pass@db:5432/tattoo_db
SECRET_KEY=your-generated-64-char-hex-key-here
ACCESS_TOKEN_EXPIRE_MINUTES=30
REFRESH_TOKEN_EXPIRE_DAYS=7
UPLOAD_DIR=/app/uploads
MAX_FILE_SIZE_MB=10
APP_NAME=TattooStudio API
APP_VERSION=1.0.0
DEBUG=false
CORS_ORIGINS=https://yourdomain.com,https://app.yourdomain.com
DEFAULT_OWNER_EMAIL=owner@studio.ru
DEFAULT_OWNER_PASSWORD=VerySecureOwnerPass123!
DEFAULT_ADMIN_EMAIL=admin@studio.ru
DEFAULT_ADMIN_PASSWORD=VerySecureAdminPass123!
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASS=your-app-password
SMTP_FROM="Tattoo Studio <your-email@gmail.com>"
USE_MOCK_EMAIL=false
```

---

## 5. Запуск через Docker Compose (рекомендуется)

### 5.1 Переход в корень проекта

```bash
cd ~/TattooApp
```

### 5.2 Запуск сервисов

```bash
docker compose up -d
```

Эта команда запустит:
- PostgreSQL 15 (`tattoo_db`)
- FastAPI backend (`tattoo_backend`)
- Сервис миграций Alembic (`tattoo_migrate`, выполнится один раз)

### 5.3 Проверка статуса контейнеров

```bash
docker compose ps
```

Все сервисы должны иметь статус `running` (для `migrate` — `Exited (0)`).

### 5.4 Просмотр логов

```bash
# Логи backend
sudo docker logs -f tattoo_backend

# Логи БД
sudo docker logs -f tattoo_db

# Логи миграций
sudo docker logs tattoo_migrate
```

### 5.5 Проверка работоспособности

```bash
curl http://localhost:8000/health
```

Ожидаемый ответ:
```json
{"status":"ok","version":"1.0.0"}
```

Документация API будет доступна по адресу: `http://<IP_сервера>:8000/docs`

---

## 6. Настройка фаервола (UFW)

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 8000/tcp
sudo ufw enable
```

> Для продакшена рекомендуется использовать Nginx как reverse proxy и открывать только порты 80/443.

---

## 7. Настройка Nginx как Reverse Proxy (рекомендуется для продакшена)

### 7.1 Установка Nginx

```bash
sudo apt install -y nginx
```

### 7.2 Создание конфигурации

```bash
sudo nano /etc/nginx/sites-available/tattoo-api
```

Содержимое:

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;

    client_max_body_size 20M;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    location /uploads {
        alias /var/lib/docker/volumes/tattooapp_uploads_data/_data/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 7.3 Активация конфигурации

```bash
sudo ln -s /etc/nginx/sites-available/tattoo-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 7.4 Обновление UFW для Nginx

```bash
sudo ufw delete allow 8000
sudo ufw allow 'Nginx Full'
```

---

## 8. Настройка HTTPS (Let's Encrypt)

### 8.1 Установка Certbot

```bash
sudo apt install -y certbot python3-certbot-nginx
```

### 8.2 Получение сертификата

```bash
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com
```

### 8.3 Автообновление сертификатов

```bash
sudo systemctl status certbot.timer
```

---

## 9. Ручное развертывание без Docker (альтернатива)

> Используйте этот способ только если Docker по каким-либо причинам недоступен.

### 9.1 Установка Python 3.11

```bash
sudo apt install -y software-properties-common
sudo add-apt-repository -y ppa:deadsnakes/ppa
sudo apt update
sudo apt install -y python3.11 python3.11-venv python3.11-dev python3-pip
```

### 9.2 Установка PostgreSQL 15

```bash
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget -qO- https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo tee /etc/apt/trusted.gpg.d/pgdg.asc >/dev/null
sudo apt update
sudo apt install -y postgresql-15 postgresql-client-15
```

### 9.3 Создание базы данных и пользователя

```bash
sudo -u postgres psql <<EOF
CREATE DATABASE tattoo_db;
CREATE USER tattoo_user WITH ENCRYPTED PASSWORD 'tattoo_pass';
GRANT ALL PRIVILEGES ON DATABASE tattoo_db TO tattoo_user;
EOF
```

### 9.4 Установка системных зависимостей

```bash
sudo apt install -y libpq-dev gcc g++ libffi-dev
```

### 9.5 Создание виртуального окружения

```bash
cd ~/TattooApp/backend
python3.11 -m venv venv
source venv/bin/activate
```

### 9.6 Установка Python-зависимостей

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 9.7 Настройка переменных окружения

Отредактируйте `.env`, указав локальное подключение к БД:

```env
DATABASE_URL=postgresql+asyncpg://tattoo_user:tattoo_pass@localhost:5432/tattoo_db
```

### 9.8 Запуск миграций

```bash
cd ~/TattooApp/backend
source venv/bin/activate
alembic upgrade head
psql postgresql://tattoo_user:tattoo_pass@localhost:5432/tattoo_db -f sql/init.sql
```

### 9.9 Запуск приложения через Uvicorn

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

### 9.10 Настройка systemd-сервиса

Создайте файл:

```bash
sudo nano /etc/systemd/system/tattoo-backend.service
```

Содержимое:

```ini
[Unit]
Description=TattooApp FastAPI Backend
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/TattooApp/backend
Environment="PATH=/home/ubuntu/TattooApp/backend/venv/bin"
EnvironmentFile=/home/ubuntu/TattooApp/backend/.env
ExecStart=/home/ubuntu/TattooApp/backend/venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target
```

Активация:

```bash
sudo systemctl daemon-reload
sudo systemctl enable tattoo-backend
sudo systemctl start tattoo-backend
sudo systemctl status tattoo-backend
```

---

## 10. Управление и обслуживание

### 10.1 Перезапуск сервисов (Docker)

```bash
cd ~/TattooApp

# Пересобрать и перезапустить
docker compose down
docker compose up -d --build

# Только перезапустить backend
docker compose restart backend
```

### 10.2 Обновление кода

```bash
cd ~/TattooApp
git pull origin main

# Если были изменения в зависимостях — пересобрать
docker compose down
docker compose up -d --build
```

### 10.3 Резервное копирование базы данных

```bash
# Docker
sudo docker exec tattoo_db pg_dump -U tattoo_user tattoo_db > tattoo_db_backup_$(date +%F).sql

# Локальная PostgreSQL
pg_dump -U tattoo_user tattoo_db > tattoo_db_backup_$(date +%F).sql
```

### 10.4 Восстановление из резервной копии

```bash
# Docker
cat tattoo_db_backup.sql | sudo docker exec -i tattoo_db psql -U tattoo_user -d tattoo_db

# Локальная PostgreSQL
psql -U tattoo_user -d tattoo_db < tattoo_db_backup.sql
```

---

## 11. Устранение неполадок

### Проблема: `migrate` падает с ошибкой подключения к БД

- Убедитесь, что `tattoo_db` имеет статус `healthy` в `docker compose ps`.
- Проверьте логи: `sudo docker logs tattoo_db`.

### Проблема: порт 8000 уже занят

```bash
sudo lsof -i :8000
sudo kill -9 <PID>
```

Или измените порт в `docker-compose.yml`:

```yaml
ports:
  - "8001:8000"
```

### Проблема: permission denied на папку uploads

```bash
# Docker
sudo chown -R 1000:1000 /var/lib/docker/volumes/tattooapp_uploads_data/_data

# Локально
sudo chown -R $USER:$USER ~/TattooApp/backend/uploads
```

---

## Чек-лист перед выходом в продакшн

- [ ] `SECRET_KEY` изменен на криптостойкий (>= 32 символов)
- [ ] Пароли `DEFAULT_OWNER_PASSWORD` и `DEFAULT_ADMIN_PASSWORD` надежные
- [ ] `DEBUG=false`
- [ ] `CORS_ORIGINS` содержит только продакшен-домены
- [ ] Настроен Nginx + HTTPS
- [ ] Настроен фаервол (UFW), открыты только нужные порты
- [ ] Настроены регулярные бэкапы БД
- [ ] SMTP-настройки корректны (или `USE_MOCK_EMAIL=true` для тестов)

---

*Инструкция актуальна для TattooApp backend v1.0.0.*
