package com.kaleo.watts

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatterySample(
    val timeMs: Long,
    val watts: Double,
    val microAmps: Long,
    val milliVolts: Int,
    val tempC: Double,
    val level: Int,
    val state: ChargeState,
    val source: String,
    val health: String,
    val fullInMs: Long?,
    val currentSupported: Boolean,
)

/** Lê os dados da bateria que o próprio Android oferece. Nada de permissão especial. */
class BatteryReader(private val context: Context) {
    private val bm = context.getSystemService(BatteryManager::class.java)

    fun read(unit: CurrentUnit): BatterySample {
        val i: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val mv = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = if (lvl >= 0 && scale > 0) lvl * 100 / scale else 0
        val state = when (i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargeState.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargeState.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargeState.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargeState.NOT_CHARGING
            else -> ChargeState.UNKNOWN
        }
        val source = when (i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Tomada"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB (computador)"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Sem fio"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Base"
            else -> "Desconectado"
        }
        val health = when (i?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Boa"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Superaquecida"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Desgastada"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tensão alta"
            BatteryManager.BATTERY_HEALTH_COLD -> "Fria"
            else -> "—"
        }
        val raw = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: Int.MIN_VALUE
        val supported = raw != Int.MIN_VALUE
        val ua = if (supported) PowerMath.signed(PowerMath.toMicroAmps(raw, unit), state) else 0L
        val remaining = runCatching { bm?.computeChargeTimeRemaining() ?: -1L }.getOrDefault(-1L)
        return BatterySample(
            timeMs = System.currentTimeMillis(),
            watts = PowerMath.watts(ua, mv),
            microAmps = ua,
            milliVolts = mv,
            tempC = temp,
            level = level,
            state = state,
            source = source,
            health = health,
            fullInMs = remaining.takeIf { it > 0 },
            currentSupported = supported,
        )
    }
}
