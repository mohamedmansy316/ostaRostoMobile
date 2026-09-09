package com.ostarosto.app.feature.productdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ostarosto.app.core.network.ApiResult
import com.ostarosto.app.data.repository.CatalogRepository
import com.ostarosto.app.domain.model.Modifier
import com.ostarosto.app.domain.model.ModifierOption
import com.ostarosto.app.domain.model.Product
import com.ostarosto.app.domain.model.SelectedOption
import com.ostarosto.app.feature.cart.CartStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val CHOICE_KEYWORDS = listOf("اختيار", "اختار", "choice", "select", "الحجم", "size")

/** A modifier must be answered before the product can be added. Mirrors the web heuristic. */
fun Modifier.isRequired(): Boolean =
    minSelection > 0 || CHOICE_KEYWORDS.any { name.contains(it, ignoreCase = true) }

data class ProductDetailUiState(
    val loading: Boolean = true,
    val product: Product? = null,
    val quantity: Int = 1,
    /** modifierId -> set of selected option foodics ids */
    val selections: Map<Long, Set<String>> = emptyMap(),
    val notes: String = "",
    val showErrors: Boolean = false,
    val error: String? = null,
    val added: Boolean = false,
) {
    fun optionUnitPrice(product: Product): Double = product.modifiers.sumOf { modifier ->
        val chosen = selections[modifier.id].orEmpty()
        modifier.options.filter { it.foodicsId in chosen }.sumOf { it.price }
    }

    fun lineUnitPrice(product: Product): Double = product.price + optionUnitPrice(product)

    /** Ids of required modifiers the customer still hasn't answered. */
    fun unsatisfied(product: Product): Set<Long> = product.modifiers
        .filter { it.isRequired() }
        .filter { selections[it.id].orEmpty().size < maxOf(1, it.minSelection) }
        .map { it.id }
        .toSet()

    fun requiredSatisfied(product: Product): Boolean = unsatisfied(product).isEmpty()
}

class ProductDetailViewModel(
    private val catalog: CatalogRepository,
    private val cart: CartStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductDetailUiState())
    val state: StateFlow<ProductDetailUiState> = _state.asStateFlow()

    fun load(ref: String, branchRef: String?) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = catalog.product(ref, branchRef)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, product = r.value, selections = defaultSelections(r.value))
                }
                is ApiResult.HttpError -> _state.update { it.copy(loading = false, error = r.message) }
                is ApiResult.NetworkError -> _state.update { it.copy(loading = false, error = r.cause.message) }
            }
        }
    }

    fun setQuantity(q: Int) = _state.update { it.copy(quantity = q.coerceAtLeast(1)) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    fun toggleOption(modifier: Modifier, option: ModifierOption) {
        val id = option.foodicsId ?: return
        if (option.isOutOfStock) return
        _state.update { s ->
            val current = s.selections[modifier.id].orEmpty()
            val next = when {
                id in current -> if (modifier.minSelection > 0 && current.size == 1) current else current - id
                modifier.maxSelection <= 1 -> setOf(id)
                current.size < modifier.maxSelection -> current + id
                else -> current
            }
            s.copy(selections = s.selections + (modifier.id to next), showErrors = false, error = null)
        }
    }

    fun addToCart() {
        val s = _state.value
        val product = s.product ?: return

        if (s.unsatisfied(product).isNotEmpty()) {
            _state.update { it.copy(showErrors = true, error = "يرجى اختيار الخيارات المطلوبة") }
            return
        }

        val options = product.modifiers.flatMap { modifier ->
            val chosen = s.selections[modifier.id].orEmpty()
            modifier.options.filter { it.foodicsId in chosen }.map {
                SelectedOption(it.foodicsId!!, it.name, it.price)
            }
        }
        cart.addProduct(product, s.quantity, options, s.notes.ifBlank { null })
        _state.update { it.copy(added = true) }
    }

    /** Only auto-select a required group when there is a single in-stock option (no real choice). */
    private fun defaultSelections(product: Product): Map<Long, Set<String>> =
        product.modifiers.associate { modifier ->
            val inStock = modifier.options.filter { !it.isOutOfStock }
            val preselect = if (modifier.isRequired() && inStock.size == 1) {
                inStock.first().foodicsId?.let { setOf(it) } ?: emptySet()
            } else {
                emptySet()
            }
            modifier.id to preselect
        }
}
