package com.ostarosto.app.data.repository

import com.ostarosto.app.core.network.ApiClient
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.network.map
import com.ostarosto.app.data.dto.BranchDto
import com.ostarosto.app.data.dto.CategoryDto
import com.ostarosto.app.data.dto.ComboDto
import com.ostarosto.app.data.dto.ProductDto
import com.ostarosto.app.data.dto.toDomain
import com.ostarosto.app.domain.model.Branch
import com.ostarosto.app.domain.model.Category
import com.ostarosto.app.domain.model.Combo
import com.ostarosto.app.domain.model.Product
import kotlinx.serialization.builtins.ListSerializer

class CatalogRepository(private val api: ApiClient) {

    suspend fun branches(): ApiResult<List<Branch>> =
        api.get("branches", ListSerializer(BranchDto.serializer())).map { list -> list.map { it.toDomain() } }

    suspend fun branch(ref: String): ApiResult<Branch> =
        api.get("branches/$ref", BranchDto.serializer()).map { it.toDomain() }

    suspend fun categories(branchId: String? = null): ApiResult<List<Category>> =
        api.get(
            "categories",
            ListSerializer(CategoryDto.serializer()),
            query = mapOf("branch_id" to branchId),
        ).map { list -> list.map { it.toDomain() } }

    suspend fun products(
        categoryId: Long? = null,
        branchId: String? = null,
        search: String? = null,
        page: Int = 1,
        perPage: Int = 20,
    ): ApiResult<List<Product>> = api.get(
        "products",
        ListSerializer(ProductDto.serializer()),
        query = mapOf(
            "category_id" to categoryId,
            "branch_id" to branchId,
            "search" to search,
            "page" to page,
            "per_page" to perPage,
        ),
    ).map { list -> list.map { it.toDomain() } }

    suspend fun product(ref: String, branchId: String? = null): ApiResult<Product> =
        api.get("products/$ref", ProductDto.serializer(), query = mapOf("branch_id" to branchId))
            .map { it.toDomain() }

    suspend fun combos(branchId: String? = null): ApiResult<List<Combo>> =
        api.get("combos", ListSerializer(ComboDto.serializer()), query = mapOf("branch_id" to branchId))
            .map { list -> list.map { it.toDomain() } }

    suspend fun combo(ref: String, branchId: String? = null): ApiResult<Combo> =
        api.get("combos/$ref", ComboDto.serializer(), query = mapOf("branch_id" to branchId))
            .map { it.toDomain() }
}
