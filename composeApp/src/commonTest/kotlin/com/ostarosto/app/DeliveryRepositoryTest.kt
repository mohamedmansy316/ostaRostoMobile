package com.ostarosto.app

import com.ostarosto.app.core.network.unwrap
import com.ostarosto.app.data.repository.DeliveryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeliveryRepositoryTest {

    @Test
    fun check_maps_available_zone_with_fee_and_eta() = runTest {
        val scope = testApi(
            ok(pathIs("/delivery/check"), """
                {"success":true,"data":{
                  "available":true,"zone_id":7,"zone_name":"مدينة نصر - المنطقة الأولى",
                  "branch_id":1,"branch_name":"فرع مدينة نصر","delivery_fee":25.0,
                  "distance_km":3.2,"estimated_time":"30-45 mins"
                }}
            """.trimIndent()),
        )
        val repo = DeliveryRepository(scope.api)

        val availability = repo.check(lat = 30.05, lng = 31.34, branchId = "b-1").unwrap()

        assertTrue(availability.available)
        assertEquals(7, availability.zoneId)
        assertEquals(25.0, availability.deliveryFee)
        assertEquals("30-45 mins", availability.estimatedTime)

        val url = scope.requests.single().url
        assertEquals("30.05", url.parameters["lat"])
        assertEquals("31.34", url.parameters["lng"])
        assertEquals("b-1", url.parameters["branch_id"])
    }

    @Test
    fun check_maps_unavailable_location() = runTest {
        val scope = testApi(
            ok(pathIs("/delivery/check"), """
                {"success":true,"data":{
                  "available":false,"zone_id":null,"zone_name":null,
                  "branch_id":null,"branch_name":null,"delivery_fee":0,
                  "distance_km":null,"estimated_time":null
                }}
            """.trimIndent()),
        )
        val repo = DeliveryRepository(scope.api)

        val availability = repo.check(lat = 29.0, lng = 31.0, branchId = "b-1").unwrap()

        assertFalse(availability.available)
        assertNull(availability.zoneId)
        assertEquals(0.0, availability.deliveryFee)
    }
}
