package com.example.domain

import androidx.annotation.DrawableRes
import com.example.R

enum class EmotionCategory(val label: String) {
  ALL("Все"),
  SIGNATURE("Фирменные"),
  POSITIVE("Позитивные"),
  CALM("Спокойные"),
  TIRED_SAD("Усталость"),
  INTENSE("Острые"),
}

enum class VectorFaceStyle {
  VESELYI,
  SCHASTLIVYI,
  VLYUBLENNYI,
  UDIVLENNYI,
  USTALYI,
  TREVOZHNYI,
  NEITRALNYI,
  RAZOCHAROVANNYI,
  RAZDRAZHENNYI,
}

sealed interface FaceRender {
  data class ImageFace(
    @DrawableRes val resId: Int,
    val isDarkArtwork: Boolean = false,
  ) : FaceRender

  data class VectorFace(val style: VectorFaceStyle) : FaceRender
}

data class EmotionDef(
  val id: String,
  val name: String,
  val subtitle: String,
  val category: EmotionCategory,
  val valence: Int, // 1 (heavy) .. 5 (uplifted)
  val accentColorHex: Long,
  val render: FaceRender,
) {
  val isSignatureCustom: Boolean
    get() = render is FaceRender.ImageFace
}

object EmotionCatalog {
  val all: List<EmotionDef> =
    listOf(
      // --- 9 Фирменных загруженных лиц из таблицы пользователя ---
      EmotionDef(
        id = "kaifyush",
        name = "Кайфюш",
        subtitle = "Лёгкость и сияние",
        category = EmotionCategory.POSITIVE,
        valence = 5,
        accentColorHex = 0xFFFF66A6,
        render = FaceRender.ImageFace(R.drawable.face_kaifyush, isDarkArtwork = false),
      ),
      EmotionDef(
        id = "sleinyush",
        name = "Слэйнюш",
        subtitle = "Уверенный настрой",
        category = EmotionCategory.POSITIVE,
        valence = 5,
        accentColorHex = 0xFFD9386A,
        render = FaceRender.ImageFace(R.drawable.face_sleinyush, isDarkArtwork = true),
      ),
      EmotionDef(
        id = "spokush",
        name = "Спокуш",
        subtitle = "Мягкое спокойствие",
        category = EmotionCategory.CALM,
        valence = 4,
        accentColorHex = 0xFF4E8EFF,
        render = FaceRender.ImageFace(R.drawable.face_spokush, isDarkArtwork = false),
      ),
      EmotionDef(
        id = "soidyush",
        name = "Сойдюш",
        subtitle = "Дерзко, но сойдёт",
        category = EmotionCategory.INTENSE,
        valence = 3,
        accentColorHex = 0xFF47B85D,
        render = FaceRender.ImageFace(R.drawable.face_soidyush, isDarkArtwork = false),
      ),
      EmotionDef(
        id = "pohuyush",
        name = "Похуюш",
        subtitle = "Полный пофигизм",
        category = EmotionCategory.CALM,
        valence = 3,
        accentColorHex = 0xFF8C4A68,
        render = FaceRender.ImageFace(R.drawable.face_pohuyush, isDarkArtwork = true),
      ),
      EmotionDef(
        id = "zagadochnaya",
        name = "Загадочная",
        subtitle = "Тайна в темноте",
        category = EmotionCategory.CALM,
        valence = 3,
        accentColorHex = 0xFF6B52B8,
        render = FaceRender.ImageFace(R.drawable.face_zagadochnaya, isDarkArtwork = true),
      ),
      EmotionDef(
        id = "skuchnyush",
        name = "Скучнюш",
        subtitle = "Скука и раздражение",
        category = EmotionCategory.TIRED_SAD,
        valence = 2,
        accentColorHex = 0xFF6BBF59,
        render = FaceRender.ImageFace(R.drawable.face_skuchnyush, isDarkArtwork = false),
      ),
      EmotionDef(
        id = "grustyush",
        name = "Грустюш",
        subtitle = "Грусть со слезой",
        category = EmotionCategory.TIRED_SAD,
        valence = 1,
        accentColorHex = 0xFF4B8CD9,
        render = FaceRender.ImageFace(R.drawable.face_grustyush, isDarkArtwork = false),
      ),
      EmotionDef(
        id = "ubyush",
        name = "Убьюш",
        subtitle = "Зловещий вайб",
        category = EmotionCategory.INTENSE,
        valence = 1,
        accentColorHex = 0xFFE53955,
        render = FaceRender.ImageFace(R.drawable.face_ubyush, isDarkArtwork = false),
      ),

      // --- 9 Классических векторных Roblox-лиц ---
      EmotionDef(
        id = "veselyi",
        name = "Весёлый",
        subtitle = "Игривое настроение",
        category = EmotionCategory.POSITIVE,
        valence = 5,
        accentColorHex = 0xFF7B4DFF,
        render = FaceRender.VectorFace(VectorFaceStyle.VESELYI),
      ),
      EmotionDef(
        id = "schastlivyi",
        name = "Счастливый",
        subtitle = "Море радости",
        category = EmotionCategory.POSITIVE,
        valence = 5,
        accentColorHex = 0xFF2EC492,
        render = FaceRender.VectorFace(VectorFaceStyle.SCHASTLIVYI),
      ),
      EmotionDef(
        id = "vlyublennyi",
        name = "Влюблённый",
        subtitle = "Тепло и сердечки",
        category = EmotionCategory.POSITIVE,
        valence = 5,
        accentColorHex = 0xFFFF5CA8,
        render = FaceRender.VectorFace(VectorFaceStyle.VLYUBLENNYI),
      ),
      EmotionDef(
        id = "udivlennyi",
        name = "Удивлённый",
        subtitle = "Вот это поворот!",
        category = EmotionCategory.INTENSE,
        valence = 4,
        accentColorHex = 0xFFFFA726,
        render = FaceRender.VectorFace(VectorFaceStyle.UDIVLENNYI),
      ),
      EmotionDef(
        id = "neitralnyi",
        name = "Нейтральный",
        subtitle = "Ровный баланс",
        category = EmotionCategory.CALM,
        valence = 3,
        accentColorHex = 0xFF7E74A6,
        render = FaceRender.VectorFace(VectorFaceStyle.NEITRALNYI),
      ),
      EmotionDef(
        id = "ustalyi",
        name = "Усталый",
        subtitle = "Нужна перезагрузка",
        category = EmotionCategory.TIRED_SAD,
        valence = 2,
        accentColorHex = 0xFF6B80B8,
        render = FaceRender.VectorFace(VectorFaceStyle.USTALYI),
      ),
      EmotionDef(
        id = "trevozhnyi",
        name = "Тревожный",
        subtitle = "Мысли крутятся",
        category = EmotionCategory.INTENSE,
        valence = 2,
        accentColorHex = 0xFF9C5CFF,
        render = FaceRender.VectorFace(VectorFaceStyle.TREVOZHNYI),
      ),
      EmotionDef(
        id = "razocharovannyi",
        name = "Разочарованный",
        subtitle = "Ожидалось лучше",
        category = EmotionCategory.TIRED_SAD,
        valence = 2,
        accentColorHex = 0xFF726A8A,
        render = FaceRender.VectorFace(VectorFaceStyle.RAZOCHAROVANNYI),
      ),
      EmotionDef(
        id = "razdrazhennyi",
        name = "Раздражённый",
        subtitle = "Всё бесит",
        category = EmotionCategory.INTENSE,
        valence = 1,
        accentColorHex = 0xFFFF5252,
        render = FaceRender.VectorFace(VectorFaceStyle.RAZDRAZHENNYI),
      ),
    )

  private val byId: Map<String, EmotionDef> = all.associateBy { it.id }

  fun findById(id: String): EmotionDef = byId[id] ?: all.first()

  fun filterBy(category: EmotionCategory): List<EmotionDef> =
    when (category) {
      EmotionCategory.ALL -> all
      EmotionCategory.SIGNATURE -> all.filter { it.isSignatureCustom }
      else -> all.filter { it.category == category }
    }
}
