package com.ostarosto.app

import com.ostarosto.app.core.address.SavedAddressStore
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedAddressStoreTest {

    @Test
    fun first_added_address_becomes_default_automatically() {
        val store = SavedAddressStore(MapSettings())

        val home = store.add(label = "Home", latitude = 30.0, longitude = 31.0, addressText = null)

        assertTrue(home.isDefault)
        assertEquals(home.id, store.default()?.id)
    }

    @Test
    fun setting_a_new_default_clears_the_previous_one() {
        val store = SavedAddressStore(MapSettings())
        val home = store.add(label = "Home", latitude = 30.0, longitude = 31.0, addressText = null)
        val work = store.add(label = "Work", latitude = 30.1, longitude = 31.1, addressText = null)

        store.setDefault(work.id)

        assertEquals(work.id, store.default()?.id)
        assertEquals(false, store.addresses.value.first { it.id == home.id }.isDefault)
    }

    @Test
    fun deleting_the_default_promotes_another_address() {
        val store = SavedAddressStore(MapSettings())
        val home = store.add(label = "Home", latitude = 30.0, longitude = 31.0, addressText = null)
        val work = store.add(label = "Work", latitude = 30.1, longitude = 31.1, addressText = null)
        store.setDefault(home.id)

        store.delete(home.id)

        assertEquals(work.id, store.default()?.id)
    }

    @Test
    fun deleting_the_only_address_leaves_no_default() {
        val store = SavedAddressStore(MapSettings())
        val home = store.add(label = "Home", latitude = 30.0, longitude = 31.0, addressText = null)

        store.delete(home.id)

        assertNull(store.default())
        assertTrue(store.addresses.value.isEmpty())
    }

    @Test
    fun state_survives_a_fresh_store_over_the_same_settings() {
        val settings = MapSettings()
        SavedAddressStore(settings).add(label = "Home", latitude = 30.0, longitude = 31.0, addressText = "12 Nile St")

        val reopened = SavedAddressStore(settings)

        assertEquals(1, reopened.addresses.value.size)
        assertEquals("Home", reopened.default()?.label)
    }
}
