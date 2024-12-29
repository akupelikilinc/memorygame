package com.ahmetkupelikilinc.memorygame

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AchievementsActivity : AppCompatActivity() {
    private lateinit var achievements: List<Achievement>
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)
        
        // Başarımları yükle
        achievements = loadAchievements()
        
        // ViewPager ve TabLayout kurulumu
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        
        setupViewPager()
        setupTabLayout()
    }
    
    private fun loadAchievements(): List<Achievement> {
        val savedAchievements = Achievement.createAchievements()
        
        // Kayıtlı ilerlemeyi yükle
        getSharedPreferences("achievements", MODE_PRIVATE).apply {
            savedAchievements.forEach { achievement ->
                achievement.progress = getInt("${achievement.id}_progress", 0)
                achievement.isUnlocked = getBoolean("${achievement.id}_unlocked", false)
            }
        }
        
        return savedAchievements
    }
    
    private fun setupViewPager() {
        viewPager.adapter = AchievementsPagerAdapter(this, achievements)
    }
    
    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> "Score"
                1 -> "Speed"
                2 -> "Streak"
                else -> "Special"
            }
        }.attach()
    }
    
    class AchievementsPagerAdapter(
        activity: AppCompatActivity,
        private val achievements: List<Achievement>
    ) : FragmentStateAdapter(activity) {
        
        override fun getItemCount(): Int = 4
        
        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            val category = when(position) {
                0 -> Achievement.Category.SCORE
                1 -> Achievement.Category.SPEED
                2 -> Achievement.Category.STREAK
                else -> Achievement.Category.SPECIAL
            }
            
            return AchievementListFragment.newInstance(
                achievements.filter { it.category == category }
            )
        }
    }
    
    class AchievementListFragment : androidx.fragment.app.Fragment() {
        private lateinit var achievements: List<Achievement>
        
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            achievements = arguments?.getParcelableArrayList("achievements") ?: listOf()
        }
        
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val recyclerView = RecyclerView(requireContext()).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = AchievementAdapter(achievements)
            }
            
            return recyclerView
        }
        
        companion object {
            fun newInstance(achievements: List<Achievement>): AchievementListFragment {
                return AchievementListFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList("achievements", ArrayList(achievements))
                    }
                }
            }
        }
    }
    
    class AchievementAdapter(
        private val achievements: List<Achievement>
    ) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val medalIcon: ImageView = view.findViewById(R.id.medalIcon)
            val titleText: TextView = view.findViewById(R.id.titleText)
            val descriptionText: TextView = view.findViewById(R.id.descriptionText)
            val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
            val progressText: TextView = view.findViewById(R.id.progressText)
            val rewardText: TextView = view.findViewById(R.id.rewardText)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_achievement, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val achievement = achievements[position]
            
            // Madalya ikonu
            holder.medalIcon.setImageResource(when(achievement.level) {
                Achievement.Level.BRONZE -> R.drawable.ic_medal_bronze
                Achievement.Level.SILVER -> R.drawable.ic_medal_silver
                Achievement.Level.GOLD -> R.drawable.ic_medal_gold
            })
            
            // Başlık ve açıklama
            holder.titleText.text = achievement.title
            holder.descriptionText.text = achievement.description
            
            // İlerleme
            holder.progressBar.max = achievement.requirement
            holder.progressBar.progress = achievement.progress
            holder.progressText.text = "${achievement.progress}/${achievement.requirement}"
            
            // Ödül puanı
            holder.rewardText.text = "+${achievement.rewardPoints}"
            
            // Kilidi açılmış başarımlar için görsel feedback
            if (achievement.isUnlocked) {
                holder.itemView.alpha = 1f
                holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(R.color.green)
                )
            } else {
                holder.itemView.alpha = 0.7f
                holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(R.color.blue)
                )
            }
        }
        
        override fun getItemCount() = achievements.size
    }
} 