package com.ahmetkupelikilinc.memorygame

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val level: Level,
    val requirement: Int,
    var progress: Int = 0,
    var isUnlocked: Boolean = false,
    var wasShown: Boolean = false,
    val rewardPoints: Int
) : Parcelable {
    enum class Category {
        SCORE,      // Skor başarımları
        SPEED,      // Hız başarımları
        STREAK,     // Günlük görev streak başarımları
        SPECIAL     // Özel başarımlar (hint kullanmadan bitirme vb.)
    }
    
    enum class Level {
        BRONZE, SILVER, GOLD
    }
    
    companion object {
        fun createAchievements(): List<Achievement> {
            return listOf(
                // Skor Başarımları
                Achievement(
                    id = "score_1000",
                    title = "Başlangıç Skoru",
                    description = "1000 puan topla",
                    category = Category.SCORE,
                    level = Level.BRONZE,
                    requirement = 1000,
                    rewardPoints = 100
                ),
                Achievement(
                    id = "score_5000",
                    title = "Puan Avcısı",
                    description = "5000 puan topla",
                    category = Category.SCORE,
                    level = Level.SILVER,
                    requirement = 5000,
                    rewardPoints = 500
                ),
                Achievement(
                    id = "score_10000",
                    title = "Puan Ustası",
                    description = "10000 puan topla",
                    category = Category.SCORE,
                    level = Level.GOLD,
                    requirement = 10000,
                    rewardPoints = 1000
                ),
                
                // Hız Başarımları
                Achievement(
                    id = "speed_level_30",
                    title = "Hızlı Bitiş",
                    description = "Bir leveli 30 saniyede bitir",
                    category = Category.SPEED,
                    level = Level.BRONZE,
                    requirement = 30,
                    rewardPoints = 200
                ),
                Achievement(
                    id = "speed_level_20",
                    title = "Şimşek Gibi",
                    description = "Bir leveli 20 saniyede bitir",
                    category = Category.SPEED,
                    level = Level.SILVER,
                    requirement = 20,
                    rewardPoints = 400
                ),
                Achievement(
                    id = "speed_level_15",
                    title = "Süper Hız",
                    description = "Bir leveli 15 saniyede bitir",
                    category = Category.SPEED,
                    level = Level.GOLD,
                    requirement = 15,
                    rewardPoints = 800
                ),
                
                // Streak Başarımları
                Achievement(
                    id = "streak_3",
                    title = "Düzenli Oyuncu",
                    description = "3 gün üst üste Daily Challenge tamamla",
                    category = Category.STREAK,
                    level = Level.BRONZE,
                    requirement = 3,
                    rewardPoints = 300
                ),
                Achievement(
                    id = "streak_5",
                    title = "Sadık Oyuncu",
                    description = "5 gün üst üste Daily Challenge tamamla",
                    category = Category.STREAK,
                    level = Level.SILVER,
                    requirement = 5,
                    rewardPoints = 600
                ),
                Achievement(
                    id = "streak_7",
                    title = "Efsane Oyuncu",
                    description = "7 gün üst üste Daily Challenge tamamla",
                    category = Category.STREAK,
                    level = Level.GOLD,
                    requirement = 7,
                    rewardPoints = 1000
                ),
                
                // Özel Başarımlar
                Achievement(
                    id = "no_hint",
                    title = "Doğal Yetenek",
                    description = "Hint kullanmadan bir level bitir",
                    category = Category.SPECIAL,
                    level = Level.BRONZE,
                    requirement = 1,
                    rewardPoints = 250
                ),
                Achievement(
                    id = "perfect_match",
                    title = "Kusursuz Eşleşme",
                    description = "Hiç hata yapmadan bir level bitir",
                    category = Category.SPECIAL,
                    level = Level.GOLD,
                    requirement = 1,
                    rewardPoints = 1000
                ),
                Achievement(
                    id = "combo_master",
                    title = "Kombo Ustası",
                    description = "5 eşleşmeyi art arda doğru yap",
                    category = Category.SPECIAL,
                    level = Level.SILVER,
                    requirement = 5,
                    rewardPoints = 500
                )
            )
        }
    }
    
    fun updateProgress(value: Int) {
        if (isUnlocked) return
        
        progress = minOf(requirement, progress + value)
        if (progress >= requirement) {
            isUnlocked = true
        }
    }
    
    fun getProgressPercentage(): Int {
        return ((progress.toFloat() / requirement.toFloat()) * 100).toInt()
    }
} 