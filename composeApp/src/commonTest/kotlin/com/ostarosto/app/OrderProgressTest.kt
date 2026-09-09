package com.ostarosto.app

import com.ostarosto.app.domain.model.OrderProgress
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderProgressTest {

    @Test
    fun steps_map_to_the_backend_progress_step_values() {
        assertEquals(OrderProgress.Cancelled, OrderProgress.fromStep(0))
        assertEquals(OrderProgress.Received, OrderProgress.fromStep(1))
        assertEquals(OrderProgress.Preparing, OrderProgress.fromStep(2))
        assertEquals(OrderProgress.Ready, OrderProgress.fromStep(3))
        assertEquals(OrderProgress.Completed, OrderProgress.fromStep(4))
    }

    @Test
    fun an_unknown_step_falls_back_to_received() {
        assertEquals(OrderProgress.Received, OrderProgress.fromStep(99))
        assertEquals(OrderProgress.Received, OrderProgress.fromStep(-1))
    }
}
