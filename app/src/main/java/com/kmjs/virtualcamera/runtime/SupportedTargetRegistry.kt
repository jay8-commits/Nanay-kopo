package com.kmjs.virtualcamera.runtime

import com.kmjs.virtualcamera.core.CameraApiType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class TargetDefinition(
    val packageName: String,
    val processNameFilter: String? = null,
    val supportedApis: List<CameraApiType> = listOf(CameraApiType.CAMERA2),
    val adapterType: String = "Camera2Adapter",
    val enabled: Boolean = true,
    val compatibilityNotes: String = ""
)

object SupportedTargetRegistry {
    private val targetsMap = ConcurrentHashMap<String, TargetDefinition>()
    private val _targetsListFlow = MutableStateFlow<List<TargetDefinition>>(emptyList())
    val targetsListFlow: StateFlow<List<TargetDefinition>> = _targetsListFlow.asStateFlow()

    init {
        // Register default targets
        register(
            TargetDefinition(
                packageName = "com.photo.android.camera",
                processNameFilter = "com.photo.android.camera",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.CAMERAX),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Verified primary test target for virtual camera injection."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.android.camera",
                processNameFilter = "com.android.camera",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.LEGACY),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Standard AOSP Camera reference."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.google.android.GoogleCamera",
                processNameFilter = "com.google.android.GoogleCamera",
                supportedApis = listOf(CameraApiType.CAMERA2),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Google Pixel Camera pipeline."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.whatsapp",
                processNameFilter = "com.whatsapp",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.CAMERAX),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "WhatsApp Video preview & capture surface."
            )
        )
        register(
            TargetDefinition(
                packageName = "org.telegram.messenger",
                processNameFilter = "org.telegram.messenger",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.CAMERAX),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Telegram In-App Camera capture."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.snapchat.android",
                processNameFilter = "com.snapchat.android",
                supportedApis = listOf(CameraApiType.CAMERA2),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Snapchat Camera2 texture stream."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.instagram.android",
                processNameFilter = "com.instagram.android",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.CAMERAX),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Instagram Stories & Live capture."
            )
        )
    }

    fun register(definition: TargetDefinition) {
        targetsMap[definition.packageName] = definition
        sync()
    }

    fun unregister(packageName: String) {
        targetsMap.remove(packageName)
        sync()
    }

    fun setEnabled(packageName: String, enabled: Boolean) {
        targetsMap[packageName]?.let {
            targetsMap[packageName] = it.copy(enabled = enabled)
            sync()
        }
    }

    fun find(packageName: String): TargetDefinition? {
        return targetsMap[packageName]
    }

    fun getAll(): List<TargetDefinition> {
        return targetsMap.values.toList()
    }

    fun isSupported(packageName: String): Boolean {
        val target = targetsMap[packageName] ?: return false
        return target.enabled
    }

    fun resetToDefaults() {
        targetsMap.clear()
        register(
            TargetDefinition(
                packageName = "com.photo.android.camera",
                processNameFilter = "com.photo.android.camera",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.CAMERAX),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Verified primary test target for virtual camera injection."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.android.camera",
                processNameFilter = "com.android.camera",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.LEGACY),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Standard AOSP Camera reference."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.google.android.GoogleCamera",
                processNameFilter = "com.google.android.GoogleCamera",
                supportedApis = listOf(CameraApiType.CAMERA2),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Google Pixel Camera pipeline."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.whatsapp",
                processNameFilter = "com.whatsapp",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.CAMERAX),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "WhatsApp Video preview & capture surface."
            )
        )
        register(
            TargetDefinition(
                packageName = "org.telegram.messenger",
                processNameFilter = "org.telegram.messenger",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.CAMERAX),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Telegram In-App Camera capture."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.snapchat.android",
                processNameFilter = "com.snapchat.android",
                supportedApis = listOf(CameraApiType.CAMERA2),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Snapchat Camera2 texture stream."
            )
        )
        register(
            TargetDefinition(
                packageName = "com.instagram.android",
                processNameFilter = "com.instagram.android",
                supportedApis = listOf(CameraApiType.CAMERA2, CameraApiType.CAMERAX),
                adapterType = "Camera2Hook",
                enabled = true,
                compatibilityNotes = "Instagram Stories & Live capture."
            )
        )
    }

    private fun sync() {
        _targetsListFlow.value = targetsMap.values.sortedBy { it.packageName }
    }
}
