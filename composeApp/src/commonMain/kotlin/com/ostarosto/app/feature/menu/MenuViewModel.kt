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
    val allProducts: List<Product> = emptyList(), // the branch's whole catalogue, fetched once
    val loadingCatalog: Boolean = false, // one-time full-catalogue fetch
    val refreshing: Boolean = false, // pull-to-refresh
    val error: String? = null,
) {
    val branchRef: String? get() = selectedBranch?.let { it.foodicsId ?: it.id.toString() }

    /**
     * The visible grid: the in-memory catalogue narrowed by the selected category
     * tab and the search box. Pure filter — switching tabs never touches the network.
     */
    val products: List<Product>
        get() {
            val term = search.trim()
            return allProducts.filter { p ->
                (selectedCategoryId == null || p.categoryId == selectedCategoryId) &&
                    (
                        term.isEmpty() ||
                            p.name.contains(term, ignoreCase = true) ||
                            p.description?.contains(term, ignoreCase = true) == true
                    )
            }
        }
}

class MenuViewModel(
    private val catalog: CatalogRepository,
    private val selection: SelectionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    private var catalogJob: Job? = null

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
                    loadCatalog()
                }
                is ApiResult.HttpError -> _state.update { it.copy(loadingShell = false, error = branches.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(loadingShell = false, error = branches.cause.message) }
            }
        }
    }

    fun selectBranch(branch: Branch) {
        if (_state.value.selectedBranch?.id == branch.id) return
        selection.setBranch(branch)
        // Prices, stock and category visibility are branch-specific — reload both.
        _state.update {
            it.copy(
                selectedBranch = branch,
                selectedCategoryId = null,
                categories = emptyList(),
                allProducts = emptyList(),
            )
        }
        loadCategories()
        loadCatalog()
    }

    /** Instant: only flips the active tab; the grid re-filters the catalogue in memory. */
    fun selectCategory(id: Long?) {
        if (_state.value.selectedCategoryId == id) return
        _state.update { it.copy(selectedCategoryId = id) }
    }

    /** Instant: local substring filter, no debounce, no request. */
    fun onSearch(term: String) {
        _state.update { it.copy(search = term) }
    }

    /** Pull-to-refresh: re-pull branches, categories and the whole catalogue. */
    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            (catalog.branches() as? ApiResult.Success)?.let { b ->
                _state.update { it.copy(branches = b.value) }
            }
            loadCategories()
            when (val r = catalog.allProducts(branchId = _state.value.branchRef)) {
                is ApiResult.Success -> _state.update { it.copy(refreshing = false, allProducts = r.value, error = null) }
                is ApiResult.HttpError -> _state.update { it.copy(refreshing = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(refreshing = false, error = r.cause.message) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val cats = catalog.categories(branchId = _state.value.branchRef)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loadingShell = false,
                        categories = cats.value,
                        selectedCategoryId = it.selectedCategoryId
                            ?.takeIf { id -> cats.value.any { c -> c.id == id } }
                            ?: cats.value.firstOrNull()?.id,
                    )
                }
                else -> _state.update { it.copy(loadingShell = false) }
            }
        }
    }

    /** One shot: pull the branch's entire catalogue into memory for client-side filtering. */
    private fun loadCatalog() {
        catalogJob?.cancel()
        _state.update { it.copy(loadingCatalog = true, error = null) }
        catalogJob = viewModelScope.launch {
            when (val r = catalog.allProducts(branchId = _state.value.branchRef)) {
                is ApiResult.Success -> _state.update { it.copy(loadingCatalog = false, allProducts = r.value) }
                is ApiResult.HttpError -> _state.update { it.copy(loadingCatalog = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(loadingCatalog = false, error = r.cause.message) }
            }
        }
    }
}
