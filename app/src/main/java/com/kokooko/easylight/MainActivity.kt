package com.kokooko.easylight

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.kokooko.easylight.databinding.ActivityMainBinding
import java.util.Calendar


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isFlashlightOn = false
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null

    private var firstHour = 0
    private var secondHour = 0
    private var firstMinute = 0
    private var secondMinute = 0

    private var showColon = true



    private lateinit var countDownTimer: CountDownTimer
    private val updateInterval: Long = 1000 // milliseconds

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        val constrainLayout = findViewById<ConstraintLayout>(R.id.constrainLayout)

        // Перевірка наявності функції фонаря на пристрої
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            // Функція фонаря не підтримується на цьому пристрої
            //binding.firstHour.isEnabled = false
            return
        }

        // Ініціалізація CameraManager
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        try {
            // Отримання ID камери, яка має функцію фонаря
            cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager?.getCameraCharacteristics(id)
                    ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    ?: false
            }

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        // Запуск таймера оновлення зображень
        startClockUpdateTimer()

        showBatteryLevel()

        binding.hours.setTextColor(Color.BLUE)
        binding.colon.setTextColor(Color.BLUE)
        binding.minutes.setTextColor(Color.BLUE)
        binding.second.setTextColor(Color.BLUE)

    }


    private fun toggleFlashlight() {
        try {
            if (isFlashlightOn) {
                // Вимкнути фонарь
                cameraManager?.setTorchMode(cameraId!!, false)
            } else {
                // Увімкнути фонарь
                cameraManager?.setTorchMode(cameraId!!, true)
            }
            // Зміна стану флага
            isFlashlightOn = !isFlashlightOn
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    override fun onStop() {
        super.onStop()
        // Вимкнення фонаря, коли активність зупиняється (виходить з фону або закривається)
        if (isFlashlightOn) {
            toggleFlashlight()
        }
    }

    private fun setScreenBright(bright: Float) {
        val lp = window.attributes
        lp.screenBrightness = bright
        window.attributes = lp
    }

    private lateinit var powerManager: PowerManager

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private fun screenTurnOff() {
        Log.i("TRYTURNOFF","TRYTURNOFF 2")
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        }
    }


    private fun startClockUpdateTimer() {
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, updateInterval) {
            override fun onTick(millisUntilFinished: Long) {

                val currentCalendar = Calendar.getInstance()
                binding.hours.text = formatNumber(currentCalendar.get(Calendar.HOUR))
                binding.minutes.text = formatNumber(currentCalendar.get(Calendar.MINUTE))
                binding.second.text = formatNumber(currentCalendar.get(Calendar.SECOND))

                if(showColon){
                    binding.colon.visibility = View.INVISIBLE
                    showColon = false
                }else{
                    binding.colon.visibility = View.VISIBLE
                    showColon = true
                }

            }

            override fun onFinish() {
                // Метод onFinish викликається, якщо таймер відрахував усі мілісекунди, але у цьому випадку, таймер буде відраховувати назавжди,
                // тому цей метод може залишатися порожнім або ви можете додати якусь поведінку, якщо необхідно.
            }
        }

        // Запуск таймера
        countDownTimer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Зупинка таймера і звільнення ресурсів
        countDownTimer.cancel()
    }



    private fun showBatteryLevel(){
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager

        // Отримуємо рівень заряду батареї
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)


        Log.i("BATTARY", "$batteryLevel")
    }

    fun formatNumber(number: Int): String {
        return if (number in 0..9) {
            String.format("%02d", number)
        } else {
            number.toString()
        }
    }
}