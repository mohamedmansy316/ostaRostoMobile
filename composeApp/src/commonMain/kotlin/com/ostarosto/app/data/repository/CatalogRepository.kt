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

    /**
     * Pulls the branch's whole active catalogue in as few requests as possible.
     * The menu holds this list in memory and filters categories client-side, so
     * switching category tabs is instant and never hits the network again.
     */
    suspend fun allProducts(branchId: String? = null): ApiResult<List<Product>> {
        val acc = mutableListOf<Product>()
        var page = 1
        while (page <= MAX_CATALOG_PAGES) {
            when (val res = products(branchId = branchId, page = page, perPage = CATALOG_PAGE_SIZE)) {
                is ApiResult.Success -> {
                    acc += res.value
                    if (res.value.size < CATALOG_PAGE_SIZE) return ApiResult.Success(acc)
                    page++
                }
                is ApiResult.HttpError -> return res
                is ApiResult.NetworkError -> return res
            }
        }
        return ApiResult.Success(acc)
    }

    suspend fun product(ref: String, branchId: String? = null): ApiResult<Product> =
        api.get("products/$ref", ProductDto.serializer(), query = mapOf("branch_id" to branchId))
            .map { it.toDomain() }

    suspend fun combos(branchId: String? = null): ApiResult<List<Combo>> =
        api.get("combos", ListSerializer(ComboDto.serializer()), query = mapOf("branch_id" to branchId))
            .map { list -> list.map { it.toDomain() } }

    suspend fun combo(ref: String, branchId: String? = null): ApiResult<Combo> =
        api.get("combos/$ref", ComboDto.serializer(), query = mapOf("branch_id" to branchId))
            .map { it.toDomain() }

    private companion object {
        /** Big enough to bring a typical branch menu back in one request. */
        const val CATALOG_PAGE_SIZE = 200

        /** Safety cap so a runaway paginator can't loop forever. */
        const val MAX_CATALOG_PAGES = 15
    }
}
