package com.lightiptv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.lightiptv.databinding.ActivityMainBinding
import com.lightiptv.models.Category
import com.lightiptv.models.Channel
import com.lightiptv.models.XtreamCredentials
import com.lightiptv.parser.M3UParser
import com.lightiptv.parser.XtreamParser
import com.lightiptv.ui.CategoryAdapter
import com.lightiptv.ui.ChannelAdapter
import com.lightiptv.utils.NetworkUtils
import com.lightiptv.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter
    
    private var categories: List<Category> = emptyList()
    private var currentCategoryIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PrefsManager(this)
        
        setupUI()
        checkAndLoadData()
    }
    
    private fun setupUI() {
        // Kategori listesi - Dikey
        categoryAdapter = CategoryAdapter { category, position ->
            currentCategoryIndex = position
            showChannels(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = categoryAdapter
            setHasFixedSize(true)
        }
        
        // Kanal listesi - Grid
        channelAdapter = ChannelAdapter { channel ->
            playChannel(channel)
        }
        binding.rvChannels.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = channelAdapter
            setHasFixedSize(true)
        }
        
        // Ayarlar butonu
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Yenile butonu
        binding.btnRefresh.setOnClickListener {
            loadData()
        }
    }
    
    private fun checkAndLoadData() {
        val hasM3U = prefs.m3uUrl.isNotEmpty()
        val hasXtream = prefs.xtreamCredentials != null
        
        if (!hasM3U && !hasXtream) {
            // İlk kurulum - ayarlara yönlendir
            startActivity(Intent(this, SettingsActivity::class.java))
        } else {
            loadData()
        }
    }
    
    private fun loadData() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                categories = when (prefs.sourceType) {
                    0 -> loadM3U()
                    1 -> loadXtream()
                    else -> emptyList()
                }
                
                if (categories.isNotEmpty()) {
                    categoryAdapter.submitList(categories)
                    showChannels(categories[0])
                } else {
                    showError("Kanal bulunamadı")
                }
            } catch (e: Exception) {
                showError("Hata: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }
    
    private suspend fun loadM3U(): List<Category> {
        val url = prefs.m3uUrl
        if (url.isEmpty()) return emptyList()
        
        // Önce cache kontrol
        val cache = prefs.m3uCache
        if (cache.isNotEmpty()) {
            val cached = M3UParser.parse(cache)
            if (cached.isNotEmpty()) {
                // Arka planda güncelle
                lifecycleScope.launch {
                    val content = NetworkUtils.fetchContent(url)
                    if (content != null) {
                        prefs.m3uCache = content
                    }
                }
                return cached
            }
        }
        
        val content = NetworkUtils.fetchContent(url) ?: return emptyList()
        prefs.m3uCache = content
        return M3UParser.parse(content)
    }
    
    private suspend fun loadXtream(): List<Category> {
        val credentials = prefs.xtreamCredentials ?: return emptyList()
        val parser = XtreamParser(credentials)
        
        val auth = parser.authenticate()
        if (auth == null || auth.status != "Active") {
            withContext(Dispatchers.Main) {
                showError("Xtream hesabı aktif değil")
            }
            return emptyList()
        }
        
        val liveCategories = parser.getLiveCategories().toMutableList()
        
        // Her kategoriye kanalları yükle
        liveCategories.forEach { category ->
            val channels = parser.getLiveStreams(category.id)
            category.channels.addAll(channels)
        }
        
        return liveCategories.filter { it.channels.isNotEmpty() }
    }
    
    private fun showChannels(category: Category) {
        binding.tvCategoryTitle.text = category.name
        channelAdapter.submitList(category.channels)
        
        // İlk kanala focusla
        binding.rvChannels.post {
            binding.rvChannels.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
        }
    }
    
    private fun playChannel(channel: Channel) {
        prefs.lastChannelId = channel.id
        
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("channel_name", channel.name)
            putExtra("channel_url", channel.url)
            putExtra("channel_logo", channel.logo)
        }
        startActivity(intent)
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutContent.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    // TV Remote kontrolleri
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (binding.rvChannels.hasFocus()) {
                    binding.rvCategories.requestFocus()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (binding.rvCategories.hasFocus()) {
                    binding.rvChannels.requestFocus()
                    return true
                }
            }
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                if (currentCategoryIndex > 0) {
                    currentCategoryIndex--
                    showChannels(categories[currentCategoryIndex])
                    categoryAdapter.setSelected(currentCategoryIndex)
                }
                return true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (currentCategoryIndex < categories.size - 1) {
                    currentCategoryIndex++
                    showChannels(categories[currentCategoryIndex])
                    categoryAdapter.setSelected(currentCategoryIndex)
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
