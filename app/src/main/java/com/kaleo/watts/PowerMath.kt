package com.kaleo.watts

import kotlin.math.abs

/** Unidade em que o aparelho informa a corrente. Cada fabricante faz de um jeito. */
enum class CurrentUnit(val label: String) { AUTO("Automático"), MICRO("Microampères (µA)"), MILLI("Miliampères (mA)") }

enum class ChargeState { CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN }

/** Contas puras (sem Android), testadas na JVM. */
object PowerMath {

    /**
     * Converte o valor bruto para µA.
     * No automático: valores abaixo de 20.000 só fazem sentido em mA (20 mA em µA seria quase nada),
     * então são multiplicados por 1000. Samsung e alguns outros informam em mA.
     */
    fun toMicroAmps(raw: Int, unit: CurrentUnit): Long = when (unit) {
        CurrentUnit.MICRO -> raw.toLong()
        CurrentUnit.MILLI -> raw * 1000L
        CurrentUnit.AUTO -> if (raw != 0 && abs(raw) < 20_000) raw * 1000L else raw.toLong()
    }

    /**
     * Ajusta o sinal pelo estado da bateria, porque alguns aparelhos invertem:
     * positivo = entrando na bateria, negativo = saindo.
     */
    fun signed(microAmps: Long, state: ChargeState): Long = when (state) {
        ChargeState.CHARGING -> abs(microAmps)
        ChargeState.DISCHARGING, ChargeState.NOT_CHARGING -> -abs(microAmps)
        else -> microAmps
    }

    fun watts(microAmps: Long, milliVolts: Int): Double = (microAmps / 1_000_000.0) * (milliVolts / 1000.0)
}

/** Estatísticas da sessão de carga: pico, média e energia acumulada. */
class SessionStats {
    var peakW = 0.0; private set
    var energyWh = 0.0; private set
    var seconds = 0.0; private set
    val averageW: Double get() = if (seconds > 0) energyWh * 3600 / seconds else 0.0

    /** Soma uma leitura de [watts] que durou [dtSeconds]. Só conta energia entrando. */
    fun add(watts: Double, dtSeconds: Double) {
        if (dtSeconds <= 0 || dtSeconds > 10 || watts <= 0) return
        if (watts > peakW) peakW = watts
        energyWh += watts * dtSeconds / 3600
        seconds += dtSeconds
    }

    fun reset() {
        peakW = 0.0; energyWh = 0.0; seconds = 0.0
    }
}
