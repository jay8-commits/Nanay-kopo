package com.kmjs.virtualcamera.npatch

import com.kmjs.virtualcamera.core.DiagnosticsLogger
import com.kmjs.virtualcamera.runtime.KMJSModuleLoader

/**
 * Main entry point invoked when target application starts with embedded KMJS NPatch module.
 */
class NPatchModuleEntry {

    /**
     * Entry method called by NPatch dex loader upon package load.
     */
    fun onPackageLoaded(packageName: String, processName: String, classLoader: ClassLoader) {
        DiagnosticsLogger.module("NPatchModuleEntry: Received load notification for package='$packageName', process='$processName'")
        KMJSModuleLoader.startModule(classLoader)
    }

    companion object {
        @JvmStatic
        fun initFromNative(classLoader: ClassLoader) {
            DiagnosticsLogger.module("NPatchModuleEntry: Native bridge hook initialized")
            KMJSModuleLoader.startModule(classLoader)
        }
    }
}
