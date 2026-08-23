package com.kmjs.virtualcamera.camera

import com.kmjs.virtualcamera.core.CameraApiType
import com.kmjs.virtualcamera.core.HookStatus
import com.kmjs.virtualcamera.virtualcamera.VirtualFrameProvider

interface CameraHook {
    val apiType: CameraApiType
    val status: HookStatus

    fun initialize(classLoader: ClassLoader): Boolean
    fun installHook(): Boolean
    fun uninstallHook(): Boolean
    fun setFrameProvider(provider: VirtualFrameProvider)
    fun getHookSummary(): String
}
