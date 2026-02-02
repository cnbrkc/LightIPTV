package com.lightiptv

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lightiptv.databinding.ActivitySettingsBinding
import com.lightiptv.models.XtreamCredentials
import com.lightiptv.parser.XtreamParser
import com.lightiptv.utils.PrefsManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PrefsManager(this)
        
        setupUI()
        loadSettings()
    }
    
    private fun setupUI() {
        // Kaynak tipi seçimi
        val sourceTypes = arrayOf("M3U URL/Dosya", "Xtream Codes")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sourceTypes)
        binding.spinnerSourceType.adapter = adapter
        
        binding.spinnerSourceType.setOnItemSelectedListener { _, _, position, _ ->
            updateSourceUI(position)
        }
        
        // Buffer boyutu
        val bufferSizes = arrayOf("1 Saniye", "2 Saniye", "3 Saniye", "5 Saniye", "10 Saniye")
        val bufferValues = arrayOf(1000, 2000, 3000, 5000, 10000)
        binding.spinnerBuffer.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bufferSizes)
        
        // Decoder tipi
        val decoderTypes = arrayOf("Otomatik", "Hardware (Önerilen)", "Software")
        binding.spinnerDecoder.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, decoderTypes)
        
        // Kaydet butonu
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
        
        // Test butonu (Xtream için)
        binding.btnTestXtream.setOnClickListener {
            testXtream()
        }
        
        // Geri butonu
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun updateSourceUI(position: Int) {
        binding.layoutM3U.visibility = if (position == 0) android.view.View.VISIBLE else android.view.View.GONE
        binding.layoutXtream.visibility = if (position == 1) android.view.View.VISIBLE else android.view.View.GONE
    }
    
    private fun loadSettings() {
        binding.spinnerSourceType.setSelection(prefs.sourceType)
        updateSourceUI(prefs.sourceType)
        
        binding.etM3uUrl.setText(prefs.m3uUrl)
        
        prefs.xtreamCredentials?.let {
            binding.etXtreamServer.setText(it.server)
            binding.etXtreamUsername.setText(it.username)
            binding.etXtreamPassword.setText(it.password)
        }
        
        // Buffer
        val bufferIndex = when (prefs.bufferSize) {
            1000 -> 0
            2000 -> 1
            3000 -> 2
            5000 -> 3
            10000 -> 4
            else -> 1
        }
        binding.spinnerBuffer.setSelection(bufferIndex)
        
        binding.spinnerDecoder.setSelection(prefs.decoderType)
    }
    
    private fun saveSettings() {
        prefs.sourceType = binding.spinnerSourceType.selectedItemPosition
        
        if (prefs.sourceType == 0) {
            // M3U
            val url = binding.etM3uUrl.text.toString().trim()
            if (url.isEmpty()) {
                showError("M3U URL giriniz")
                return
            }
            prefs.m3uUrl = url
            prefs.m3uCache = "" // Cache'i temizle
        } else {
            // Xtream
            val server = binding.etXtreamServer.text.toString().trim()
            val username = binding.etXtreamUsername.text.toString().trim()
            val password = binding.etXtreamPassword.text.toString().trim()
            
            if (server.isEmpty() || username.isEmpty() || password.isEmpty()) {
                showError("Tüm alanları doldurunuz")
                return
            }
            
            prefs.xtreamCredentials = XtreamCredentials(server, username, password)
        }
        
        // Buffer
        val bufferValues = arrayOf(1000, 2000, 3000, 5000, 10000)
        prefs.bufferSize = bufferValues[binding.spinnerBuffer.selectedItemPosition]
        
        prefs.decoderType = binding.spinnerDecoder.selectedItemPosition
        
        Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun testXtream() {
        val server = binding.etXtreamServer.text.toString().trim()
        val username = binding.etXtreamUsername.text.toString().trim()
        val password = binding.etXtreamPassword.text.toString().trim()
        
        if (server.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError("Tüm alanları doldurunuz")
            return
        }
        
        binding.btnTestXtream.isEnabled = false
        binding.btnTestXtream.text = "Test ediliyor..."
        
        lifecycleScope.launch {
            val parser = XtreamParser(XtreamCredentials(server, username, password))
            val result = parser.authenticate()
            
            binding.btnTestXtream.isEnabled = true
            binding.btnTestXtream.text = "Bağlantıyı Test Et"
            
            if (result != null) {
                val message = """
                    Bağlantı başarılı!
                    Durum: ${result.status}
                    Son Kullanma: ${result.expDate}
                    Aktif/Max: ${result.activeCons}/${result.maxConnections}
                """.trimIndent()
                Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
            } else {
                showError("Bağlantı başarısız!")
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
