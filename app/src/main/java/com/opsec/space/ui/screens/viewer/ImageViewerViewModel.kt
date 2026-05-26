package com.opsec.space.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsec.space.data.model.ImageItem
import com.opsec.space.data.repository.ImageRepository
import com.opsec.space.utils.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val images: List<ImageItem> = emptyList(),
    val isLoading: Boolean = true,
    val showInfo: Boolean = false,
    val showControls: Boolean = true
)

@HiltViewModel
class ImageViewerViewModel @Inject constructor(
    private val repository: ImageRepository
) : ViewModel() {

    private val _showInfo = MutableStateFlow(false)
    private val _showControls = MutableStateFlow(true)

    val images: StateFlow<List<ImageItem>> = repository.getImages(SortOrder.DATE_ADDED)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val showInfo: StateFlow<Boolean> = _showInfo
    val showControls: StateFlow<Boolean> = _showControls

    fun removeImage(image: ImageItem) {
        viewModelScope.launch {
            repository.removeImage(image)
        }
    }

    fun toggleFavorite(image: ImageItem) {
        viewModelScope.launch {
            repository.updateFavorite(image.id, !image.isFavorite)
        }
    }

    fun renameImage(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameImage(id, newName)
        }
    }

    fun duplicateImage(image: ImageItem) {
        viewModelScope.launch {
            repository.duplicateImage(image)
        }
    }

    fun moveImage(id: Long, folderName: String) {
        viewModelScope.launch {
            repository.moveImageToFolder(id, folderName)
        }
    }

    fun moveToVault(id: Long, isVaulted: Boolean) {
        viewModelScope.launch {
            repository.updateVaulted(id, isVaulted)
        }
    }

    fun toggleInfo() {
        _showInfo.value = !_showInfo.value
    }

    fun toggleControls() {
        _showControls.value = !_showControls.value
    }

    fun hideControls() {
        _showControls.value = false
        _showInfo.value = false
    }

    fun showControls() {
        _showControls.value = true
    }
}
