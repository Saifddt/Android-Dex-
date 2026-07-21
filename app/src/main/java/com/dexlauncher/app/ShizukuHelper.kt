package com.dexlauncher.app

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {

    private const val REQUEST_CODE = 7001

    // Chequea si la app Shizuku esta instalada y corriendo en el celular
    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    // Pide el permiso de Shizuku (aparece un dialogo desde la app Shizuku)
    fun requestPermission(activity: AppCompatActivity) {
        if (!isAvailable()) {
            Toast.makeText(
                activity,
                "Shizuku no esta corriendo. Abri la app Shizuku primero y activala.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (hasPermission()) {
            Toast.makeText(activity, "Shizuku ya esta activo", Toast.LENGTH_SHORT).show()
            return
        }
        Shizuku.requestPermission(REQUEST_CODE)
    }

    // Ejecuta un comando de shell con los privilegios que da Shizuku (como adb shell).
    // Se usa reflexion porque Shizuku oculta este metodo en versiones nuevas de la libreria
    // (Google Play no deja publicarlo directo), pero sigue existiendo internamente.
    fun runCommand(command: String): String {
        if (!isAvailable() || !hasPermission()) {
            return "Shizuku no disponible o sin permiso"
        }
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            output.ifBlank { "OK" }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
