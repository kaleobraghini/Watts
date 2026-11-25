package com.kaleo.watts

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class Session(val peakW: Double, val averageW: Double, val energyWh: Double, val seconds: Double)

class WattsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("watts", Context.MODE_PRIVATE)
    private val reader = BatteryReader(app)
    private val stats = SessionStats()
    private val estimator = ConsumptionEstimator(prefs.getFloat("consumo", -1f).takeIf { it > 0 }?.toDouble())

    /** Consumo estimado do celular (medido fora da tomada). Null até ter uma medição. */
    val consumptionW = MutableStateFlow(estimator.value)
    private var lastTime = 0L

    val unit = MutableStateFlow(runCatching { CurrentUnit.valueOf(prefs.getString("unit", "AUTO")!!) }.getOrDefault(CurrentUnit.AUTO))
    val keepScreenOn = MutableStateFlow(prefs.getBoolean("screen_on", false))

    /** Últimas 2 minutos de leituras, para o gráfico. */
    val history = MutableStateFlow<List<Double>>(emptyList())
    val session = MutableStateFlow(Session(0.0, 0.0, 0.0, 0.0))

    /** Só mede enquanto a tela do app está aberta: nada roda em segundo plano. */
    val sample: StateFlow<BatterySample?> = flow {
        while (true) {
            emit(reader.read(unit.value))
            delay(1000)
        }
    }.flowOn(Dispatchers.Default)
        .onEach { s ->
            val dt = if (lastTime == 0L) 0.0 else (s.timeMs - lastTime) / 1000.0
            lastTime = s.timeMs
            if (s.state == ChargeState.CHARGING) stats.add(s.watts, dt)
            if (s.state == ChargeState.DISCHARGING && s.currentSupported) {
                estimator.addDischarge(s.watts)
                consumptionW.value = estimator.value
                estimator.value?.let { prefs.edit().putFloat("consumo", it.toFloat()).apply() }
            }
            session.value = Session(stats.peakW, stats.averageW, stats.energyWh, stats.seconds)
            history.value = (history.value + s.watts).takeLast(120)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3000), null)

    fun chargerWatts(batteryInW: Double): Double? = estimator.chargerWatts(batteryInW)

    fun resetSession() {
        stats.reset()
        history.value = emptyList()
        session.value = Session(0.0, 0.0, 0.0, 0.0)
    }

    fun setUnit(u: CurrentUnit) {
        unit.value = u
        prefs.edit().putString("unit", u.name).apply()
        history.value = emptyList()
    }

    fun setKeepScreenOn(v: Boolean) {
        keepScreenOn.value = v
        prefs.edit().putBoolean("screen_on", v).apply()
    }
}
