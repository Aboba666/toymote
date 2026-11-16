package com.example.domain

import com.example.data.MoodEntryEntity

data class StreakStats(
  val currentStreak: Int,
  val longestStreak: Int,
  val hasEntryToday: Boolean,
)

data class EmotionFrequency(
  val emotion: EmotionDef,
  val count: Int,
  val share: Float,
)

data class DailyMoodPoint(
  val date: ToymoteDate,
  val averageValence: Float?,
  val entryCount: Int,
  val primaryEmotion: EmotionDef?,
)

data class AdviceCard(
  val id: String,
  val title: String,
  val body: String,
  val tag: String,
  val emotionHint: EmotionDef?,
)

data class MotivationalQuote(
  val text: String,
  val subtext: String,
)

object AnalyticsEngine {

  val quotes: List<MotivationalQuote> =
    listOf(
      MotivationalQuote(
        text = "Даже один маленький пиксель радости способен изменить весь экран дня.",
        subtext = "Поддерживай серию каждый вечер",
      ),
      MotivationalQuote(
        text = "Замечать свои эмоции — это уже половина пути к внутреннему спокойствию.",
        subtext = "Честность с собой важнее идеального настроения",
      ),
      MotivationalQuote(
        text = "У каждого уровня в жизни есть свои боссы, но ты всегда можешь перевести дух и сохранить прогресс.",
        subtext = "Не забывай про отдых и перезагрузку",
      ),
      MotivationalQuote(
        text = "Твоё настроение — не приговор, а живой сигнал о том, что тебе сейчас нужно.",
        subtext = "Прислушивайся к себе с теплом",
      ),
      MotivationalQuote(
        text = "Маленькие ежедневные отметки складываются в удивительную карту твоего пути.",
        subtext = "Каждый день имеет значение",
      ),
      MotivationalQuote(
        text = "Даже если сегодня лицо «Похуюш» или «Усталый», завтра обязательно откроется новая глава.",
        subtext = "Любая эмоция достойна места в дневнике",
      ),
      MotivationalQuote(
        text = "Твоя серия — это обещание себе уделять хотя бы минуту в день своему состоянию.",
        subtext = "Ты справляешься лучше, чем кажется",
      ),
    )

  fun computeStreaks(entries: List<MoodEntryEntity>, today: ToymoteDate = ToymoteDate.today()): StreakStats {
    if (entries.isEmpty()) {
      return StreakStats(currentStreak = 0, longestStreak = 0, hasEntryToday = false)
    }

    val uniqueDays = entries.map { it.dateEpochDay }.distinct().sorted()
    val todayEpoch = today.epochDay
    val daySet = uniqueDays.toHashSet()
    val hasEntryToday = daySet.contains(todayEpoch)

    // Current streak: count backwards from today (if logged today) or yesterday (grace until evening)
    var current = 0
    var cursor =
      when {
        hasEntryToday -> todayEpoch
        daySet.contains(todayEpoch - 1) -> todayEpoch - 1
        else -> Long.MIN_VALUE
      }
    if (cursor != Long.MIN_VALUE) {
      while (daySet.contains(cursor)) {
        current++
        cursor--
      }
    }

    // Longest streak across entire sorted history
    var longest = 0
    var run = 0
    var prevDay: Long? = null
    for (day in uniqueDays) {
      if (prevDay == null || day == prevDay + 1) {
        run++
      } else {
        run = 1
      }
      if (run > longest) longest = run
      prevDay = day
    }

    return StreakStats(
      currentStreak = current,
      longestStreak = longest,
      hasEntryToday = hasEntryToday,
    )
  }

  fun topEmotionsForDays(
    entries: List<MoodEntryEntity>,
    daysWindow: Int,
    today: ToymoteDate = ToymoteDate.today(),
  ): List<EmotionFrequency> {
    val startEpoch = today.epochDay - (daysWindow - 1)
    val filtered = entries.filter { it.dateEpochDay in startEpoch..today.epochDay }
    if (filtered.isEmpty()) return emptyList()

    val total = filtered.size.toFloat()
    return filtered
      .groupBy { it.emotionId }
      .map { (emotionId, list) ->
        EmotionFrequency(
          emotion = EmotionCatalog.findById(emotionId),
          count = list.size,
          share = list.size / total,
        )
      }
      .sortedByDescending { it.count }
  }

  fun moodChartPoints(
    entries: List<MoodEntryEntity>,
    daysCount: Int,
    today: ToymoteDate = ToymoteDate.today(),
  ): List<DailyMoodPoint> {
    val byDay = entries.groupBy { it.dateEpochDay }
    val startEpoch = today.epochDay - (daysCount - 1)
    return (0 until daysCount).map { offset ->
      val epoch = startEpoch + offset
      val date = ToymoteDate.fromEpochDay(epoch)
      val dayEntries = byDay[epoch].orEmpty().sortedByDescending { it.timestampMillis }
      val avg =
        if (dayEntries.isEmpty()) null
        else dayEntries.map { EmotionCatalog.findById(it.emotionId).valence }.average().toFloat()
      DailyMoodPoint(
        date = date,
        averageValence = avg,
        entryCount = dayEntries.size,
        primaryEmotion = dayEntries.firstOrNull()?.let { EmotionCatalog.findById(it.emotionId) },
      )
    }
  }

  fun generateAdvice(
    entries: List<MoodEntryEntity>,
    streakStats: StreakStats,
    today: ToymoteDate = ToymoteDate.today(),
  ): List<AdviceCard> {
    if (entries.isEmpty()) {
      return listOf(
        AdviceCard(
          id = "onboarding_1",
          title = "Начни свой первый день",
          body =
            "Выбери Roblox-лицо, которое лучше всего передаёт твой настрой прямо сейчас. Уже через 3 дня появятся первые персональные подсказки!",
          tag = "Старт",
          emotionHint = EmotionCatalog.findById("kaifyush"),
        ),
        AdviceCard(
          id = "onboarding_2",
          title = "Вечерний ритуал заботы",
          body =
            "Лучше всего отмечать эмоции вечером около 20:30 — за минуту перед сном легко подвести итог дня и запустить ударную серию 🔥.",
          tag = "Привычка",
          emotionHint = EmotionCatalog.findById("spokush"),
        ),
      )
    }

    val recentStart = today.epochDay - 13
    val recent = entries.filter { it.dateEpochDay >= recentStart }
    val sample = recent.ifEmpty { entries.take(14) }
    val counts = sample.groupingBy { it.emotionId }.eachCount()
    val avgValence = sample.map { EmotionCatalog.findById(it.emotionId).valence }.average()

    val advice = mutableListOf<AdviceCard>()

    val tiredCount = (counts["ustalyi"] ?: 0) + (counts["skuchnyush"] ?: 0)
    val sadCount = (counts["grustyush"] ?: 0) + (counts["razocharovannyi"] ?: 0)
    val intenseCount =
      (counts["ubyush"] ?: 0) + (counts["razdrazhennyi"] ?: 0) + (counts["trevozhnyi"] ?: 0)
    val positiveCount =
      (counts["kaifyush"] ?: 0) +
        (counts["veselyi"] ?: 0) +
        (counts["schastlivyi"] ?: 0) +
        (counts["vlyublennyi"] ?: 0) +
        (counts["sleinyush"] ?: 0)
    val calmCount =
      (counts["spokush"] ?: 0) +
        (counts["neitralnyi"] ?: 0) +
        (counts["pohuyush"] ?: 0) +
        (counts["zagadochnaya"] ?: 0)

    if (tiredCount >= 2) {
      advice +=
        AdviceCard(
          id = "tired",
          title = "Сигнал об усталости и скуке",
          body =
            "Ты часто отмечаешь усталость или скуку в последние дни — попробуй сегодня лечь пораньше, отложить телефон за 40 минут до сна и дать мозгу тишины.",
          tag = "Восстановление",
          emotionHint = EmotionCatalog.findById("ustalyi"),
        )
    }

    if (intenseCount >= 2) {
      advice +=
        AdviceCard(
          id = "intense",
          title = "Сбрось лишнее напряжение",
          body =
            "Заметно немало острых или тревожных отметок. Поможет короткая прогулка без наушников, стакан воды и дыхание по схеме 4–4–4 в течение пары минут.",
          tag = "Антистресс",
          emotionHint = EmotionCatalog.findById("trevozhnyi"),
        )
    }

    if (sadCount >= 2) {
      advice +=
        AdviceCard(
          id = "sad",
          title = "Бережное отношение к себе",
          body =
            "Грусть и разочарование тоже важные эмоции. Порадуй себя чем-то простым: тёплым чаем, любимым треком или разговором с тем, кто тебя понимает.",
          tag = "Поддержка",
          emotionHint = EmotionCatalog.findById("grustyush"),
        )
    }

    if (positiveCount >= (sample.size * 0.45).coerceAtLeast(2.0)) {
      advice +=
        AdviceCard(
          id = "positive",
          title = "Много позитивных дней! Так держать",
          body =
            "Твой эмоциональный фон сейчас на высоте — запиши в комментарии к сегодняшней отметке, что именно подарило тебе это тепло, чтобы помнить в сложные дни.",
          tag = "Энергия",
          emotionHint = EmotionCatalog.findById("kaifyush"),
        )
    }

    if (calmCount >= 2) {
      advice +=
        AdviceCard(
          id = "calm",
          title = "Ровный баланс и загадочный дзен",
          body =
            "Спокойные и нейтральные состояния отлично берегут ресурс. Если хочется лёгкой искры, добавь в завтрашний день одно маленькое новое впечатление.",
          tag = "Баланс",
          emotionHint = EmotionCatalog.findById("spokush"),
        )
    }

    if (streakStats.currentStreak >= 3) {
      advice +=
        AdviceCard(
          id = "streak_boost",
          title = "Серия уже ${ToymoteDate.formatDaysStreakRu(streakStats.currentStreak)} 🔥",
          body =
            "Регулярность формирует отличную привычку! Даже 15 секунд в день помогают видеть закономерности, которые раньше ускользали.",
          tag = "Ударная серия",
          emotionHint = EmotionCatalog.findById("sleinyush"),
        )
    } else if (!streakStats.hasEntryToday) {
      advice +=
        AdviceCard(
          id = "today_nudge",
          title = "Сегодняшняя отметка ещё ждёт",
          body =
            "Не забудь заглянуть в календарь до конца дня — выбери лицо дня и сохрани текущую волну эмоций.",
          tag = "Напоминание",
          emotionHint = EmotionCatalog.findById("veselyi"),
        )
    }

    if (advice.isEmpty()) {
      val fallbackBody =
        if (avgValence >= 3.5) {
          "Хорошая и ровная динамика настроения! Продолжай добавлять короткие заметки — так статистика за месяц станет ещё информативнее."
        } else {
          "Помни, что любые перепады настроения временны. Сделай сегодня паузу на 10 минут только для себя и отметь, как изменилось самочувствие."
        }
      advice +=
        AdviceCard(
          id = "general",
          title = "Персональный фокус дня",
          body = fallbackBody,
          tag = "Дневник",
          emotionHint = EmotionCatalog.findById("neitralnyi"),
        )
    }

    return advice
  }
}
