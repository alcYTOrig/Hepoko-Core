# HepokoCore

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20%20Beta1-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.20.2--1.26.2-%23DC3D24.svg?style=for-the-badge)](https://papermc.io)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-%23007396.svg?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-%233DA639.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

**HepokoCore** — многофункциональный высокооптимизированный плагин-ядро для серверов **Paper, Purpur** и аналогичных форков Minecraft. Объединяет утилиты для администрирования, авторизации и визуальной кастомизации, избавляя от необходимости устанавливать десятки сторонних плагинов.

---

## Содержание

- [📥 Установка](#-установка)
- [⚙️ Требования](#️-требования)
- [🛠️ Модули](#-модули)
- [📜 Команды](#-команды)
- [🔐 права](#-права)
- [📁 Конфигурация](#-конфигурация)
- [🔧 Сборка из исходников](#-сборка-из-исходников)
- [🤝 Вклад в проект](#-вклад-в-проект)
- [📄 Лицензия](#-лицензия)

---

## 📥 Установка

### быстрая установка

1. Скачайте последний `.jar` файл из [Releases](https://github.com/alcYTOrig/Hepoko-Core/releases)
2. Поместите файл в папку `plugins/` вашего сервера
3. Перезапустите сервер

### Сборка из исходников

См. раздел [🔧 Сборка из исходников](#-сборка-из-исходников)

---

## ⚙️ Требования

| Требование | Версия |
|------------|--------|
| **Java** | 21 или выше |
| **Minecraft** | 1.20.2 - 1.26.2+ |
| **Сервер** | Paper, Purpur, или их форки |
| **Vault** | Опционально (для интеграции прав) |

---

## 🛠️ Модули

### 📊 TAB & Анимации

Легковесная замена тяжелым плагинам на таб-лист.

**Возможности:**
- ✅ Динамические Header и Footer
- ✅ Поддержка системных переменных: `%player%`, `%online%`, `%max_online%`, `%ping%`, `%server%`, `%date%`
- ✅ Конфигурация анимаций через `AnimationsTabConfig.yml`
- ✅ Настраиваемый интервал смены кадров
- ✅ Авто-центрирование текста
- ✅ Отображение пинга цифрами (например: `15ms`)
- ✅ Поддержка цветовых кодов (`&`)

**Файл конфигурации:** `plugins/HepokoCore/AnimationsTabConfig.yml`

---

### 💬 Кастомный Чат

Полный контроль над форматированием сообщений.

**Возможности:**
- ✅ Настройка формата чата через `CustomConfig.yml`
- ✅ Поддержка цветовых кодов (`&a`, `&c`, и т.д.)
- ✅ Кастомизация системных сообщений (вход/выход игроков)
- ✅ Поддержка PlaceholderAPI переменных

**Файл конфигурации:** `plugins/HepokoCore/CustomConfig.yml`

---

### 🛡️ Управление и Модерация (Control)

Мощная система модерации с базой данных SQLite.

**Возможности:**
- ✅ Временные баны (`/tempban`)
- ✅ Постоянные баны
- ✅ Баны по IP (`/tempban-ip`)
- ✅ Временные муты (`/tempmute`)
- ✅ Разбаны (`/unban`, `/unban-ip`)
- ✅ Снятие мутов (`/unmute`)
- ✅ Просмотр информации о бане (`/baninfo`)
- ✅ Списки забаненных (`/banlist`, `/banlist-ip`)
- ✅ Глобальные оповещения (`/broadcast`)
- ✅ Автоматическое логирование всех действий

**Файл конфигурации:** `plugins/HepokoCore/ControlConfig.yml`

---

### 🔑 Авторизация (AuthEveryDay)

Система авторизации для повышенной безопасности.

**Возможности:**
- ✅ Ежедневная проверка авторизации
- ✅ Регистрация и вход (`/register`, `/login`)
- ✅ Двухфакторная аутентификация через Discord (`/2fa_discord`)
- ✅ Двухфакторная аутентификация через Authenticator App (`/2fa_app`)
- ✅ Защита от ботов
- ✅ Хранение данных в SQLite

**Файл конфигурации:** `plugins/HepokoCore/AuthConfig.yml`

---

### 📦 Мимики (MimicChest)

Уникальная игровая механика для развлечения игроков.

**Возможности:**
- ✅ Сундуки-мимики с случайными событиями
- ✅ Настраиваемая вероятность появления
- ✅ Различные типы мимиков (положительные/отрицательные)
- ✅ База данных SQLite для хранения статистики
- ✅ Интеграция с экономикой (если установлен Vault)

**Файл конфигурации:** `plugins/HepokoCore/MimicConfig.yml`

---

### 🔐 Система Прав (HepokoPerms)

Собственная система управления правами.

**Возможности:**
- ✅ Создание и удаление групп
- ✅ Назначение прав группам и игрокам
- ✅ Наследование прав
- ✅ Временные права
- ✅ Интеграция с Vault (опционально)
- ✅ Management через команду `/hperm`

**Команда:** `/hperm`

---

### 📋 Система Репортов

Удобная система жалоб для игроков.

**Возможности:**
- ✅ Подача жалобы (`/report <игрок> <причина>`)
- ✅ Просмотр списка жалоб (`/reportlist`)
- ✅ GUI интерфейс для управления репортами
- ✅ Хранение истории жалоб в SQLite
- ✅ Уведомления администрации

---

## 📜 Команды

### Администрирование

| Команда | Описание | Право |
|---------|----------|-------|
| `/hepokocore reload` | Перезагрузка всех конфигураций | `hepokocore.admin` |
| `/tabreload` | Перезагрузка TAB конфигураций | `hepokocore.admin` |
| `/hperm` | Управление правами | `hepokocore.admin` |

### Авторизация

| Команда | Описание |
|---------|----------|
| `/register <пароль> <повтор>` | Регистрация аккаунта |
| `/login <пароль>` | Вход в аккаунт |
| `/2fa_discord` | Настройка 2FA через Discord |
| `/2fa_app` | Настройка 2FA через Authenticator App |

### Модерация

| Команда | Описание | Право |
|---------|----------|-------|
| `/tempban <игрок> <время> <причина>` | Временный бан | `hepokocore.tempban` |
| `/tempban-ip <IP> <время> <причина>` | Временный бан по IP | `hepokocore.tempbanip` |
| `/unban <игрок>` | Снятие бана | `hepokocore.unban` |
| `/unban-ip <IP>` | Снятие бана с IP | `hepokocore.unbanip` |
| `/banlist` | Список забаненных игроков | `hepokocore.banlist` |
| `/banlist-ip` | Список забаненных IP | `hepokocore.banlistip` |
| `/baninfo <игрок/IP>` | Информация о бане | `hepokocore.baninfo` |
| `/tempmute <игрок> <время> <причина>` | Временный мут | `hepokocore.tempmute` |
| `/unmute <игрок>` | Снятие мута | `hepokocore.unmute` |
| `/broadcast <сообщение>` | Глобальное оповещение | `hepokocore.broadcast` |

### Репорты

| Команда | Описание | Право |
|---------|----------|-------|
| `/report <игрок> <причина>` | Подать жалобу | `hepokocore.report` |
| `/reportlist` | Просмотр и управление жалобами | `hepokocore.reportlist` |

---

## 🔐 Права

### Права по умолчанию

| Право | Описание | По умолчанию |
|-------|----------|--------------|
| `hepokocore.admin` | Полный доступ к админ-командам | OP |
| `hepokocore.tempban` | Временный бан игроков | true |
| `hepokocore.tempbanip` | Временный бан по IP | true |
| `hepokocore.unban` | Снятие бана | true |
| `hepokocore.unbanip` | Снятие бана с IP | true |
| `hepokocore.banlist` | Просмотр списка банов | true |
| `hepokocore.banlistip` | Просмотр списка банов IP | true |
| `hepokocore.baninfo` | Просмотр информации о бане | true |
| `hepokocore.tempmute` | Временный мут | true |
| `hepokocore.unmute` | Снятие мута | true |
| `hepokocore.broadcast` | Глобальные оповещения | true |
| `hepokocore.report` | Подача жалоб | true |
| `hepokocore.reportlist` | Управление жалобами | true |

---

## 📁 Конфигурация

После установки плагин создаст папку `plugins/HepokoCore/` со следующими файлами:

```
plugins/HepokoCore/
├── AuthConfig.yml          # Настройки авторизации
├── ControlConfig.yml        # Настройки модерации
├── CustomConfig.yml         # Настройки чата
├── GlobalConfig.yml         # Глобальные настройки (вкл/выкл модулей)
├── AnimationsTabConfig.yml  # Настройки анимаций TAB
├── MimicConfig.yml          # Настройки мимиков
└── data/                   # Базы данных SQLite
    ├── bans.db
    ├── mutes.db
    ├── reports.db
    ├── mimic.db
    └── permissions.db
```

### GlobalConfig.yml

В этом файле можно включать/отключать модули:

```yaml
AuthEveryDay: true      # Модуль авторизации
Control: true          # Модуль модерации
TabManager: true       # Модуль TAB
MimicChest: false      # Модуль мимиков (отключен по умолчанию)
```

---

## 🔧 Сборка из исходников

### Предварительные требования

- [Git](https://git-scm.com/)
- [Java 21 JDK](https://www.oracle.com/java/)
- [Gradle](https://gradle.org/) (опционально, можно использовать `gradlew`)

### Инструкции

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/alcYTOrig/Hepoko-Core.git
   cd Hepoko-Core
   ```

2. Соберите проект:
   ```bash
   # Windows
   gradlew build
   
   # Linux/macOS
   ./gradlew build
   ```

3. Готовый `.jar` файл будет в:
   ```
   build/libs/HepokoCore-<version>.jar
   ```

4. Поместите файл в папку `plugins/` вашего сервера и перезапустите.

---

## 🤝 Вклад в проект

Приветствуются пулл-реквесты! Пожалуйста, следуйте этим правилам:

1. Создайте форк репозитория
2. Создайте отдельную ветку для ваших изменений (`git checkout -b feature/your-feature`)
3. Убедитесь, что ваш код соответствует стилю проекта
4. Напишите тесты для нового функционала (если применимо)
5. Отправьте пулл-реквест

---

## 📄 Лицензия

Этот проект лицензирован под **MIT License** — см. файл [LICENSE](src/main/kotlin/org/alc/hepokoCore/LICENSE) для подробностей.

---

## 🙌 Благодарности

- [PaperMC](https://papermc.io) — за отличный серверный API
- [Kotlin](https://kotlinlang.org) — за замечательный язык программирования
- [Vault](https://www.spigotmc.org/resources/vault.34315/) — за систему управления правами

---

© 2026 ALC. All rights reserved.
