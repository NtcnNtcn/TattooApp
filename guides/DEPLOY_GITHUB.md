# Деплой TattooApp на сервер через GitHub Actions (CI/CD)

Эта инструкция с нуля описывает, как:
1. Залить проект на GitHub.
2. Настроить автоматический деплой бэкенда на Ubuntu-сервер при каждом пуше в ветку `main`.

---

## Требования

- Аккаунт на [GitHub](https://github.com).
- Сервер на **Ubuntu 22.04/24.04 LTS** с root-доступом по SSH.
- Установленный [Git](https://git-scm.com/download/win) на локальной машине (Windows).

---

## 1. Создание репозитория на GitHub

1. Открой [github.com](https://github.com) и авторизуйся.
2. Нажми **New** (зелёная кнопка) → дай имя репозиторию `TattooApp`.
3. **Важно:** оставь галочку «Add a README file» **снятой** (у нас уже есть структура проекта).
4. Нажми **Create repository**.
5. Скопируй URL репозитория (HTTPS или SSH, например `git@github.com:yourusername/TattooApp.git`).

---

## 2. Авторизация GitHub в PowerShell

GitHub больше не принимает обычный пароль аккаунта при пуше по HTTPS. Нужно использовать **Personal Access Token (classic)**.

### 2.1 Создание токена

1. На GitHub зайди: **Settings → Developer settings → Personal access tokens → Tokens (classic)**.
2. Нажми **Generate new token (classic)**.
3. Дай имя токену (например, `TattooApp-Windows`), выбери срок действия.
4. В разделе **Select scopes** поставь галочку **`repo`** (доступ к репозиториям).
5. Нажми **Generate token**.
6. **Скопируй токен** — он показывается только один раз.

### 2.2 Настройка Git в PowerShell

Открой PowerShell и выполни:

```powershell
git config --global credential.helper manager
git config --global user.name "Твое Имя"
git config --global user.email "your-email@example.com"
```

При первом `git push` (см. следующий шаг) PowerShell спросит логин и пароль:
- **Логин:** твой username на GitHub.
- **Пароль:** вставь скопированный **Personal Access Token** (не пароль от аккаунта!).
- Git Credential Manager запомнит токен, и вводить его повторно не придётся.

---

## 3. Первый пуш проекта с локальной машины

Открой терминал (PowerShell или Git Bash) в корне проекта `TattooApp`:

```powershell
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/TattooApp.git
git push -u origin main
```

После этого весь код будет на GitHub.

---

## 3. Подготовка сервера (Ubuntu)

Зайди на сервер по SSH:

```bash
ssh root@YOUR_SERVER_IP
```

### 3.1 Установка Docker и Docker Compose
Выполни шаги из `guides/DEPLOY_UBUNTU.md` (разделы **1. Подготовка сервера** и **2. Установка Docker**).

### 3.2 Создание пользователя для деплоя (опционально, но рекомендуется)

```bash
adduser deployer
usermod -aG docker deployer
```

Далее в инструкции подразумевается пользователь `deployer`. Если используешь `root` или `ubuntu`, подставь своё имя.

### 3.3 Первое ручное клонирование
На сервере:

```bash
cd ~
git clone https://github.com/YOUR_USERNAME/TattooApp.git
```

### 3.4 Создание SSH-ключа для GitHub Actions
На сервере, от имени пользователя `deployer`:

```bash
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github_actions -N ""
cat ~/.ssh/github_actions.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

Скопируй **приватный** ключ:

```bash
cat ~/.ssh/github_actions
```

Выдели и скопируй весь текст (начинается с `-----BEGIN OPENSSH PRIVATE KEY-----`).

---

## 4. Настройка секретов в GitHub

В репозитории на GitHub перейди:  
**Settings → Secrets and variables → Actions → New repository secret**

Добавь 4 секрета:

| Название | Значение |
|---|---|
| `SSH_HOST` | IP-адрес твоего сервера |
| `SSH_USER` | `deployer` (или твой пользователь) |
| `SSH_PRIVATE_KEY` | Весь текст приватного ключа из шага 3.4 |
| `ENV_FILE` | Содержимое production-файла `backend/.env` (см. `guides/DEPLOY_UBUNTU.md` → раздел 4) |

---

## 5. Создание workflow для автодеплоя

В проекте создай файл: `.github/workflows/deploy.yml`

Содержимое:

```yaml
name: Deploy to Ubuntu Server

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Deploy via SSH
        uses: appleboy/ssh-action@v1.0.0
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          envs: ENV_FILE
          script: |
            set -e
            cd ~/TattooApp
            git pull origin main
            echo "$ENV_FILE" > backend/.env
            docker compose down
            docker compose up -d --build
            docker compose ps
```

Закоммить и запушь:

```powershell
git add .github/workflows/deploy.yml
git commit -m "Add GitHub Actions deploy workflow"
git push origin main
```

---

## 6. Проверка деплоя

1. На GitHub открой вкладку **Actions** — там появится запуск workflow `Deploy to Ubuntu Server`.
2. Дождись зелёной галочки (✅).
3. На сервере проверь статус:

```bash
docker compose ps
curl http://localhost:8000/health
```

4. Открой в браузере: `http://YOUR_SERVER_IP:8000/docs`

---

## 7. Как работает обновление

Теперь любой `git push` в `main` автоматически:
- подтянет изменения на сервер;
- пересоберёт Docker-образы;
- перезапустит контейнеры;
- применит миграции (сервис `migrate` в `docker-compose.yml`).

Если нужно откатить изменения:
- Зайди на сервер и выполни `docker compose down && git reset --hard HEAD~1 && docker compose up -d --build`.

---

## Полезные ссылки

- [DEPLOY_UBUNTU.md](./DEPLOY_UBUNTU.md) — ручной деплой, настройка Nginx и HTTPS.
- [GitHub Docs: Encrypted secrets](https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions)

---

*Инструкция актуальна для TattooApp v1.0.0.*
