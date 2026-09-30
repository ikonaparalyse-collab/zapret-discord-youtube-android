# Flowseal Zapret Mobile (Android) 📱⚡

Мобильная версия популярного пакета обхода блокировок и замедлений **Flowseal Zapret** для Android. 
Работает **без Root-прав** через системный `VpnService` и алгоритмы десинхронизации пакетов.

---

## ✨ Возможности

- 🚀 **Работа без Root**: запуск в один тап через встроенный Android VPN.
- 🎯 **Официальные пресеты Flowseal**:
  - ⚡ **General (Универсальный)** — базовый обход для YouTube, Discord и других ресурсов.
  - 🔴 **YouTube Fast (Fake + Split 2)** — максимальная скорость видеопотоков (1080p, 2K, 4K).
  - 🔴 **YouTube Alt (Disorder)** — для провайдеров со строгой фильтрацией фейк-пакетов.
  - 🟣 **Discord (Медиа + Голос)** — обход блокировки голосовых каналов и картинок.
  - ⚙️ **Пользовательский режим** — ручная настройка Split Pos, Fake TTL и режима Disorder.
- 🛡️ **Защита от DNS-спуфинга**: встроенные Cloudflare 1.1.1.1, Google 8.8.8.8, Quad9 и AdGuard.
- 📊 **Живой монитор логов**: отслеживание перехваченных TLS Handshake, SNI и статуса доставки сегментов.
- 🎨 **Киберпанк интерфейс**: современный Jetpack Compose UI с темной темой и неоновыми индикаторами.

---

## 🛠️ Как получить готовый APK файл (через GitHub Actions)

1. Создайте новый репозиторий на [GitHub](https://github.com/new) (например, `flowseal-zapret-android`).
2. Запушьте этот проект в свой репозиторий:
   ```bash
   git add .
   git commit -m "Initial commit of Flowseal Zapret Mobile"
   git remote add origin https://github.com/ВАШ_НИК/flowseal-zapret-android.git
   git branch -M main
   git push -u origin main
   ```
3. Перейдите во вкладку **Actions** в вашем репозитории на GitHub.
4. Дождитесь завершения сборки (зеленая галочка, около 1–2 минут).
5. В блоке **Artifacts** нажмите на **`Flowseal-Zapret-Mobile-v1.0`** и скачайте готовый APK прямо на телефон!

---

## 💡 Благодарности

- **Flowseal** — за оптимизированные стратегии обхода для Discord и YouTube.
- **bol-van** — за оригинальный проект [zapret](https://github.com/bol-van/zapret).
