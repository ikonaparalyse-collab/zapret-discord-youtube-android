package com.flowseal.zapret.data

data class Preset(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val tag: String,
    val splitPos: Int = 1,
    val disorder: Boolean = true,
    val fakePacket: Boolean = true,
    val fakeTtl: Int = 4,
    val tlsRecordSplit: Boolean = true,
    val customArgs: String = ""
)

object PresetRepository {
    val PRESETS = listOf(
        Preset(
            id = "general",
            title = "⚡ General (Универсальный)",
            subtitle = "Оптимально для YouTube и Discord",
            description = "Разбиение TLS ClientHello на позиции 1 с фейковым пакетом и защитой от спуфинга. Работает у большинства провайдеров РФ.",
            tag = "Рекомендуемый",
            splitPos = 1,
            disorder = true,
            fakePacket = true,
            fakeTtl = 4,
            tlsRecordSplit = true
        ),
        Preset(
            id = "youtube_fake_split",
            title = "🔴 YouTube Fast (Fake + Split 2)",
            subtitle = "Максимальная скорость 4K / 1080p",
            description = "Агрессивная десинхронизация видеопотоков googlevideo.com со смещением сплита на 2 байта и фейковым TLS.",
            tag = "YouTube",
            splitPos = 2,
            disorder = true,
            fakePacket = true,
            fakeTtl = 3,
            tlsRecordSplit = true
        ),
        Preset(
            id = "youtube_disorder",
            title = "🔴 YouTube Alt (Disorder / Очередь)",
            subtitle = "Для провайдеров, блокирующих Fake-пакеты",
            description = "Перестановка TCP-сегментов ClientHello без отправки фейкового пакета. Помогает на мобильных сетях МТС/Теле2/Мегафон/Билайн.",
            tag = "YouTube",
            splitPos = 1,
            disorder = true,
            fakePacket = false,
            fakeTtl = 0,
            tlsRecordSplit = false
        ),
        Preset(
            id = "discord_fix",
            title = "🟣 Discord (Медиа + Голос)",
            subtitle = "Обход блокировки звонков и картинок",
            description = "Специализированная стратегия под шлюзы Discord voice и медиасерверы с поддержкой UDP-трафика.",
            tag = "Discord",
            splitPos = 1,
            disorder = true,
            fakePacket = true,
            fakeTtl = 5,
            tlsRecordSplit = true
        ),
        Preset(
            id = "custom",
            title = "⚙️ Пользовательский режим",
            subtitle = "Ручная настройка параметров",
            description = "Позволяет самостоятельно задавать позицию разделения (split), режим disorder и TTL фейкового пакета.",
            tag = "Экспертный",
            splitPos = 1,
            disorder = true,
            fakePacket = false,
            fakeTtl = 3,
            tlsRecordSplit = false
        )
    )

    fun getById(id: String): Preset {
        return PRESETS.find { it.id == id } ?: PRESETS.first()
    }
}
