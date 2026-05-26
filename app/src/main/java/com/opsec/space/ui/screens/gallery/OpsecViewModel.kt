package com.opsec.space.ui.screens.opsec

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsec.space.data.model.ImageItem
import com.opsec.space.data.repository.ImageRepository
import com.opsec.space.utils.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SECRET_TAP_COUNT = 10
private const val SEQUENCE_RESET_DELAY_MS = 2000L

data class OpsecUiState(
    val images: List<ImageItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_ADDED,
    val showSortMenu: Boolean = false,
    val showSearchBar: Boolean = false,
    // Secret: FAB is only shown after GA(3)-LL(3)-ERY(3) sequence
    val isUnlocked: Boolean = false,
    val isAscending: Boolean = false
)

@HiltViewModel
class OpsecViewModel @Inject constructor(
    private val repository: ImageRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    private val _isAscending = MutableStateFlow(false) // Default descending (newest first)
    private val _showSortMenu = MutableStateFlow(false)
    private val _showSearchBar = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _isUnlocked = MutableStateFlow(false)

    // Tap counter for unlocking the FAB
    private var tapCount = 0
    private var tapResetJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _images: StateFlow<List<ImageItem>> = combine(
        _searchQuery,
        _sortOrder,
        _isAscending
    ) { query, sort, asc -> Triple(query, sort, asc) }
        .flatMapLatest { (query, sort, asc) ->
            if (query.isBlank()) {
                repository.getImages(sort, asc)
            } else {
                repository.searchImages(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<OpsecUiState> = combine(
        _images,
        _isLoading,
        _isRefreshing,
        _searchQuery,
        _sortOrder,
        _showSortMenu,
        _showSearchBar,
        _isAscending
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val images = args[0] as List<ImageItem>
        val isLoading = args[1] as Boolean
        val isRefreshing = args[2] as Boolean
        val searchQuery = args[3] as String
        val sortOrder = args[4] as SortOrder
        val showSortMenu = args[5] as Boolean
        val showSearchBar = args[6] as Boolean
        val isAscending = args[7] as Boolean
        OpsecUiState(
            images = images,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            searchQuery = searchQuery,
            sortOrder = sortOrder,
            showSortMenu = showSortMenu,
            showSearchBar = showSearchBar,
            isUnlocked = _isUnlocked.value,
            isAscending = isAscending
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OpsecUiState()
    )

    // Separate flows for unlock state (not combined above to avoid recompose issues)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    init {
        viewModelScope.launch {
            repository.cleanupRevokedPermissions()
            _isLoading.value = false
        }
    }

    /** Called every time the user taps the "Opsec" title text */
    fun onOpsecTitleTap() {
        tapResetJob?.cancel()
        tapCount++
        
        if (tapCount >= SECRET_TAP_COUNT) {
            _isUnlocked.value = true
            tapCount = 0
        } else {
            // Schedule a reset if user stops tapping
            tapResetJob = viewModelScope.launch {
                delay(SEQUENCE_RESET_DELAY_MS)
                tapCount = 0
            }
        }
    }

    fun onImagesSelected(uris: List<Uri>) {
        viewModelScope.launch {
            repository.addImages(uris)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChanged(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
        _showSortMenu.value = false
    }

    fun toggleSortDirection() {
        _isAscending.value = !_isAscending.value
    }

    fun toggleSortMenu() {
        _showSortMenu.value = !_showSortMenu.value
    }

    fun dismissSortMenu() {
        _showSortMenu.value = false
    }

    fun toggleSearchBar() {
        _showSearchBar.value = !_showSearchBar.value
        if (!_showSearchBar.value) {
            _searchQuery.value = ""
        }
    }

    fun batchDelete(images: List<ImageItem>) {
        viewModelScope.launch {
            images.forEach { repository.moveToTrash(it.id) }
        }
    }

    fun batchFavorite(images: List<ImageItem>, favorite: Boolean) {
        viewModelScope.launch {
            images.forEach { repository.updateFavorite(it.id, favorite) }
        }
    }

    fun batchMoveToFolder(images: List<ImageItem>, folderName: String) {
        viewModelScope.launch {
            images.forEach { repository.moveImageToFolder(it.id, folderName) }
        }
    }

    fun batchMoveToVault(images: List<ImageItem>, vaulted: Boolean) {
        viewModelScope.launch {
            images.forEach { repository.updateVaulted(it.id, vaulted) }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.cleanupRevokedPermissions()
            _isRefreshing.value = false
        }
    }
}
