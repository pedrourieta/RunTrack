package com.pedrourieta.runtrack

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * RunTrack - Monitor deportivo
 * Lee el acelerometro y el giroscopio para detectar movimiento
 * y cambios de direccion del corredor.
 */
class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var giroscopio: Sensor? = null

    private lateinit var tvEstado: TextView
    private lateinit var tvDireccion: TextView
    private lateinit var tvAcelerometro: TextView
    private lateinit var tvGiroscopio: TextView
    private lateinit var tvContadores: TextView
    private lateinit var tvHistorial: TextView
    private lateinit var scrollHistorial: ScrollView

    // Ultimos valores aceptados por el filtro
    private val ultimoAcel = FloatArray(3)
    private val ultimoGiro = FloatArray(3)

    // Umbrales minimos de cambio para aceptar una nueva lectura
    private val UMBRAL_ACEL = 0.3f   // m/s2
    private val UMBRAL_GIRO = 0.1f   // rad/s

    private var eventosRecibidos = 0
    private var eventosProcesados = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvEstado = findViewById(R.id.tvEstado)
        tvDireccion = findViewById(R.id.tvDireccion)
        tvAcelerometro = findViewById(R.id.tvAcelerometro)
        tvGiroscopio = findViewById(R.id.tvGiroscopio)
        tvContadores = findViewById(R.id.tvContadores)
        tvHistorial = findViewById(R.id.tvHistorial)
        scrollHistorial = findViewById(R.id.scrollHistorial)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // 1. Verificar que el dispositivo tenga acelerometro
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (acelerometro == null) {
            Toast.makeText(this, "El dispositivo no tiene acelerómetro", Toast.LENGTH_LONG).show()
            tvAcelerometro.text = "Sensor no disponible"
        }

        // Se obtiene el giroscopio
        giroscopio = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // 2. Registrar los listeners
        acelerometro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        giroscopio?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Se liberan los sensores al cerrar la app
        sensorManager.unregisterListener(this)
    }

    // 3. Callback que recibe cada lectura del sensor
    override fun onSensorChanged(event: SensorEvent) {
        eventosRecibidos++

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Filtro: solo se procesa si el cambio supera el umbral
                if (hayCambio(event.values, ultimoAcel, UMBRAL_ACEL)) {
                    event.values.copyInto(ultimoAcel)
                    eventosProcesados++
                    mostrarAcelerometro(event.values)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                if (hayCambio(event.values, ultimoGiro, UMBRAL_GIRO)) {
                    event.values.copyInto(ultimoGiro)
                    eventosProcesados++
                    mostrarGiroscopio(event.values)
                }
            }
        }

        tvContadores.text = "Eventos recibidos: $eventosRecibidos | procesados: $eventosProcesados"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se utiliza en esta practica
    }

    // Compara la lectura nueva con la ultima aceptada en los tres ejes
    private fun hayCambio(nuevos: FloatArray, anteriores: FloatArray, umbral: Float): Boolean {
        return abs(nuevos[0] - anteriores[0]) > umbral ||
                abs(nuevos[1] - anteriores[1]) > umbral ||
                abs(nuevos[2] - anteriores[2]) > umbral
    }

    private fun mostrarAcelerometro(v: FloatArray) {
        tvAcelerometro.text = "X: %.2f   Y: %.2f   Z: %.2f".format(v[0], v[1], v[2])

        // Magnitud del vector menos la gravedad para saber si hay movimiento
        val magnitud = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        val aceleracion = abs(magnitud - SensorManager.GRAVITY_EARTH)
        tvEstado.text = if (aceleracion > 1.5f) "Estado: En movimiento" else "Estado: Reposo"

        agregarHistorial("ACEL", v)
    }

    private fun mostrarGiroscopio(v: FloatArray) {
        tvGiroscopio.text = "X: %.2f   Y: %.2f   Z: %.2f".format(v[0], v[1], v[2])

        // El eje Z indica el giro sobre el plano (izquierda / derecha)
        tvDireccion.text = when {
            v[2] > 0.5f -> "Dirección: Giro a la izquierda"
            v[2] < -0.5f -> "Dirección: Giro a la derecha"
            else -> "Dirección: Recto"
        }

        agregarHistorial("GIRO", v)
    }

    // Agrega la lectura al historial en pantalla
    private fun agregarHistorial(tipo: String, v: FloatArray) {
        tvHistorial.append("$tipo -> %.2f, %.2f, %.2f\n".format(v[0], v[1], v[2]))
        scrollHistorial.post { scrollHistorial.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
