package com.ostarosto.app

import com.ostarosto.app.core.network.unwrap
import com.ostarosto.app.data.repository.CatalogRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogRepositoryTest {

    @Test
    fun products_parse_price_discount_and_modifier_tree() = runTest {
        val scope = testApi(
            ok(pathIs("/products/9"), """
                {"success":true,"data":{
                  "id":9,"foodics_id":"p-9","name":"ساندويتش فراخ","description":"لذيذ","image":"https://x/p.png",
                  "price":145.0,"has_discount":true,"original_price":160.0,
                  "is_pickup_available":true,"is_delivery_available":false,"is_out_of_stock":false,
                  "category_id":3,
                  "nutrition_facts":{"calories":520,"protein":30,"carbs":40,"fats":18},
                  "modifiers":[
                    {"id":4,"foodics_id":"m-4","name":"إضافات","selection_type":"multiple","min_selection":0,"max_selection":3,
                     "options":[{"id":11,"foodics_id":"o-11","name":"جبنة","price":10.0,"image":null,"is_out_of_stock":false}]}
                  ]
                }}
            """.trimIndent()),
        )
        val repo = CatalogRepository(scope.api)

        val product = repo.product("9", branchId = "b-1").unwrap()

        assertEquals(145.0, product.price)
        assertTrue(product.hasDiscount)
        assertEquals(160.0, product.originalPrice)
        assertEquals(1, product.modifiers.size)
        assertEquals("جبنة", product.modifiers.first().options.first().name)
        assertEquals(520.0, product.nutrition.calories)
    }

    @Test
    fun product_list_forwards_branch_and_category_query_params() = runTest {
        val scope = testApi(
            ok(pathIs("/products"), """{"success":true,"data":[],"meta":{"current_page":1,"per_page":30,"has_more":false,"total":0}}"""),
        )
        val repo = CatalogRepository(scope.api)

        repo.products(categoryId = 3, branchId = "b-9", search = "فراخ", page = 2)

        val url = scope.requests.single().url
        assertEquals("3", url.parameters["category_id"])
        assertEquals("b-9", url.parameters["branch_id"])
        assertEquals("فراخ", url.parameters["search"])
        assertEquals("2", url.parameters["page"])
    }

    @Test
    fun branches_map_open_state_and_hours() = runTest {
        val scope = testApi(
            ok(pathIs("/branches"), """
                {"success":true,"data":[
                  {"id":1,"foodics_id":"b-1","name":"مدينة نصر","address":"ش 9","phone":"0100","latitude":30.0,"longitude":31.0,
                   "is_open":false,"accepts_pickup":true,"accepts_delivery":true,"hours_today":{"from":"10:00","to":"23:00"}}
                ]}
            """.trimIndent()),
        )
        val repo = CatalogRepository(scope.api)

        val branch = repo.branches().unwrap().single()

        assertEquals("مدينة نصر", branch.name)
        assertEquals(false, branch.isOpen)
        assertEquals("23:00", branch.hoursToday?.to)
    }
}
