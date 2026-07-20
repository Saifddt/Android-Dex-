package com.dexlauncher.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var desktopGrid: RecyclerView
    private lateinit var allAppsList: RecyclerView
    private lateinit var runningAppsBar: RecyclerView
    private lateinit var startMenu: android.widget.LinearLayout
    private lateinit var searchBox: EditText
    private lateinit var wallpaperView: ImageView
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var wifiIcon: ImageView
    private lateinit var batteryText: TextView

    private lateinit var desktopAdapter: AppAdapter
    private lateinit var allAppsAdapter: AppAdapter
    private lateinit var taskbarAdapter: TaskbarAdapter

    private var allApps: List<AppInfo> = emptyList()
    private val runningApps: MutableList<AppInfo> = mutableListOf()

    private val prefs by lazy { getSharedPreferences("dex_prefs", MODE_PRIVATE) }
    private val clockHandler = Handler(Looper.getMainLooper())

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { saveWallpaper(it) }
    }

    // Se actualiza cada vez que cambia el nivel de batería del celular real
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                val percent = (level * 100) / scale
                batteryText.text = "$percent%"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        desktopGrid = findViewById(R.id.desktopGrid)
        allAppsList = findViewById(R.id.allAppsList)
        runningAppsBar = findViewById(R.id.runningAppsBar)
        startMenu = findViewById(R.id.startMenu)
        searchBox = findViewById(R.id.searchBox)
        wallpaperView = findViewById(R.id.desktopWallpaper)
        clockText = findViewById(R.id.clockText)
        dateText = findViewById(R.id.dateText)
        wifiIcon = findViewById(R.id.wifiIcon)
        batteryText = findViewById(R.id.batteryText)

        val btnStart: ImageButton = findViewById(R.id.btnStart)
        val btnChangeWallpaper: android.widget.Button = findViewById(R.id.btnChangeWallpaper)

        setupGrids()
        loadInstalledApps()
        loadSavedWallpaper()
        startClock()
        updateWifiStatus()

        btnStart.setOnClickListener { toggleStartMenu() }
        btnChangeWallpaper.setOnClickListener { pickImageLauncher.launch("image/*") }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        updateWifiStatus()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // ya estaba desregistrado, no pasa nada
        }
    }

    private fun setupGrids() {
        desktopGrid.layoutManager = GridLayoutManager(this, 4)
        desktopAdapter = AppAdapter(emptyList()) { app -> launchApp(app) }
        desktopGrid.adapter = desktopAdapter

        allAppsList.layoutManager = LinearLayoutManager(this)
        allAppsAdapter = AppAdapter(emptyList()) { app ->
            launchApp(app)
            toggleStartMenu()
        }
        allAppsList.adapter = allAppsAdapter

        runningAppsBar.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        taskbarAdapter = TaskbarAdapter(
            runningApps,
            onClick = { app -> launchApp(app) },
            onLongClick = { app, position ->
                taskbarAdapter.removeAt(position)
                Toast.makeText(this, "${app.label} quitada de la barra", Toast.LENGTH_SHORT).show()
            }
        )
        runningAppsBar.adapter = taskbarAdapter
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        allApps = resolveInfos.map {
            AppInfo(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(pm)
            )
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }

        desktopAdapter.updateList(allApps)
        allAppsAdapter.updateList(allApps)
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        allAppsAdapter.updateList(filtered)
    }

    private fun launchApp(app: AppInfo) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
            addToRunningApps(app)
        }
    }

    private fun addToRunningApps(app: AppInfo) {
        val alreadyOpen = runningApps.any { it.packageName == app.packageName }
        if (!alreadyOpen) {
            runningApps.add(app)
            taskbarAdapter.updateList(runningApps)
        }
    }

    private fun toggleStartMenu() {
        startMenu.visibility =
            if (startMenu.visibility == android.view.View.VISIBLE) android.view.View.GONE
            else android.view.View.VISIBLE
    }

    private fun saveWallpaper(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val file = File(filesDir, "wallpaper.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            prefs.edit().putString("wallpaper_path", file.absolutePath).apply()
            wallpaperView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSavedWallpaper() {
        val path = prefs.getString("wallpaper_path", null) ?: return
        val file = File(path)
        if (file.exists()) {
            wallpaperView.setImageBitmap(BitmapFactory.decodeFile(path))
        }
    }

    // Chequea si hay conexión de datos activa (WiFi o datos móviles) y cambia el ícono
    private fun updateWifiStatus() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        val connected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        wifiIcon.setImageResource(if (connected) R.drawable.ic_wifi_on else R.drawable.ic_wifi_off)
    }

    private fun startClock() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val runnable = object : Runnable {
            override fun run() {
                val now = Date()
                clockText.text = timeFormat.format(now)
                dateText.text = dateFormat.format(now)
                updateWifiStatus()
                clockHandler.postDelayed(this, 1000 * 30)
            }
        }
        clockHandler.post(runnable)
    }

    override fun onBackPressed() {
        if (startMenu.visibility == android.view.View.VISIBLE) {
            startMenu.visibility = android.view.View.GONE
        }
    }
}
