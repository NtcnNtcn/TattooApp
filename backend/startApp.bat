@echo off
chcp 65001 >nul
echo Активация виртуального окружения (venv)...

call venv\Scripts\activate.bat
if %errorlevel% neq 0 (
    echo Ошибка: не удалось активировать venv.
    exit /b %errorlevel%
)

echo Запуск Uvicorn сервера (--reload активен)...
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
