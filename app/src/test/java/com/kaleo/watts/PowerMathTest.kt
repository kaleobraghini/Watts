package com.kaleo.watts

import org.junit.Assert.assertEquals
import org.junit.Test

class PowerMathTest {
    @Test fun autoDetectsMilliamps() {
        assertEquals(2_500_000L, PowerMath.toMicroAmps(2500, CurrentUnit.AUTO)) // Samsung: 2500 mA
        assertEquals(2_500_000L, PowerMath.toMicroAmps(2_500_000, CurrentUnit.AUTO)) // padrão: µA
        assertEquals(-450_000L, PowerMath.toMicroAmps(-450, CurrentUnit.AUTO))
        assertEquals(0L, PowerMath.toMicroAmps(0, CurrentUnit.AUTO))
    }

    @Test fun manualUnits() {
        assertEquals(3000L, PowerMath.toMicroAmps(3000, CurrentUnit.MICRO))
        assertEquals(3_000_000L, PowerMath.toMicroAmps(3000, CurrentUnit.MILLI))
    }

    @Test fun signFollowsState() {
        assertEquals(2_000_000L, PowerMath.signed(-2_000_000, ChargeState.CHARGING))
        assertEquals(-300_000L, PowerMath.signed(300_000, ChargeState.DISCHARGING))
    }

    @Test fun watts() {
        // 4,2 V × 4 A = 16,8 W
        assertEquals(16.8, PowerMath.watts(4_000_000, 4200), 1e-9)
    }

    @Test fun session() {
        val s = SessionStats()
        repeat(3600) { s.add(10.0, 1.0) } // 10 W por 1 hora
        assertEquals(10.0, s.energyWh, 1e-6)
        assertEquals(10.0, s.averageW, 1e-6)
        s.add(25.0, 1.0)
        assertEquals(25.0, s.peakW, 1e-9)
        s.add(5.0, 60.0) // lacuna grande (app em segundo plano): ignorada
        assertEquals(3601.0, s.seconds, 1e-9)
    }
}
