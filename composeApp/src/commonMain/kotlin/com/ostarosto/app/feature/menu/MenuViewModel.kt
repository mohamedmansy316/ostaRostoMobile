package com.ostarosto.app.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.core.state.SelectionStore
import com.ostarosto.app.data.repository.CatalogRepository
import com.ostarosto.app.domain.model.Branch
import com.ostarosto.app.domain.model.Category
import com.ostarosto.app.domain.model.Product
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MenuUiState(
    val loadingShell: Boolean = true, // first paint: branches/categories still loading
    val branches: List<Branch> = emptyList(),
    val selectedBranch: Branch? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val search: String = "",
    val products: List<Product> = emptyList(),
    val loadingProducts: Boolean = false, // full reload (category/search change)
    val loadingMore: Boolean = false, // appending the next page
    val page: Int = 1,
    val hasMore: Boolean = false,
    val error: String? = null,
) {
    val branchRef: String? get() = selectedBranch?.let { it.foodicsId ?: it.id.toString() }
}

class MenuViewModel(
    private val catalog: CatalogRepository,
    private val selection: SelectionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    private var productsJob: Job? = null
    private var searchJob: Job? = null

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loadingShell = true, error = null) }
        viewModelScope.launch {
            when (val branches = catalog.branches()) {
                is ApiResult.Success -> {
                    val chosen = _state.value.selectedBranch
                        ?: selection.branch.value
                        ?: branches.value.firstOrNull()
                    chosen?.let(selection::setBranch)
                    _state.update { it.copy(branches = branches.value, selectedBranch = chosen) }
                    loadCategories()
                }
                is ApiResult.HttpError -> _state.update { it.copy(loadingShell = false, error = branches.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(loadingShell = false, error = branches.cause.message) }
            }
        }
    }

    fun selectBranch(branch: Branch) {
        selection.setBranch(branch)
        // Categories are branch-specific; reloading them chains into a product reload.
        _state.update { it.copy(selectedBranch = branch, selectedCategoryId = null) }
        loadCategories()
    }

    fun selectCategory(id: Long?) {
        if (_state.value.selectedCategoryId == id) return
        _state.update { it.copy(selectedCategoryId = id) }
        reloadProducts()
    }

    fun onSearch(term: String) {
        _state.update { it.copy(search = term) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce keystrokes
            reloadProducts()
        }
    }

    /** Called when the grid nears its end. */
    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.loadingProducts || !s.hasMore) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            when (val r = fetch(page = s.page + 1)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loadingMore = false,
                        products = it.products + r.value,
                        page = it.page + 1,
                        hasMore = r.value.size >= PAGE_SIZE,
                    )
                }
                is ApiResult.HttpError -> _state.update { it.copy(loadingMore = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(loadingMore = false, error = r.cause.message) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val cats = catalog.categories(branchId = _state.value.branchRef)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            loadingShell = false,
                            categories = cats.value,
                            selectedCategoryId = it.selectedCategoryId ?: cats.value.firstOrNull()?.id,
                        )
                    }
                    reloadProducts()
                }
                else -> _state.update { it.copy(loadingShell = false) }
            }
        }
    }

    /** Fresh first page; keeps the previous grid visible until the new one arrives. */
    private fun reloadProducts() {
        productsJob?.cancel()
        _state.update { it.copy(loadingProducts = true, error = null) }
        productsJob = viewModelScope.launch {
            when (val r = fetch(page = 1)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loadingProducts = false,
                        products = r.value,
                        page = 1,
                        hasMore = r.value.size >= PAGE_SIZE,
                    )
                }
                is ApiResult.HttpError -> _state.update { it.copy(loadingProducts = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(loadingProducts = false, error = r.cause.message) }
            }
        }
    }

    private suspend fun fetch(page: Int): ApiResult<List<Product>> {
        val s = _state.value
        return catalog.products(
            categoryId = s.selectedCategoryId,
            branchId = s.branchRef,
            search = s.search.trim().ifBlank { null },
            page = page,
            perPage = PAGE_SIZE,
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
