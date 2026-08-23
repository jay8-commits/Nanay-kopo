package com.kmjs.virtualcamera.runtime

import android.app.Application
import android.os.Build
import android.os.Process
import com.kmjs.virtualcamera.core.DiagnosticsLogger
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class ProcessDetectionResult(
    val packageName: String,
    val processName: String,
    val isMainApplicationProcess: Boolean,
    val isAuxiliaryProcess: Boolean,
    val isSupported: Boolean,
    val targetDefinition: TargetDefinition?,
    val reason: String
)

object TargetProcessDetector {

    private val AUXILIARY_PROCESS_SUFFIXES = listOf(
        ":push",
        ":remote",
        ":leakcanary",
        ":sandboxed",
        ":isolated",
        ":crash",
        ":downloader",
        ":playcore",
        ":service"
    )

    fun detectCurrentProcess(classLoader: ClassLoader? = null): ProcessDetectionResult {
        val processName = resolveProcessName()
        val packageName = extractPackageNameFromProcess(processName)

        val isAuxiliary = isAuxiliaryProcess(processName)
        val isMainApp = !isAuxiliary && (processName == packageName || processName.startsWith("$packageName:"))

        DiagnosticsLogger.process("Detected process='$processName', package='$packageName', isMainApp=$isMainApp, isAuxiliary=$isAuxiliary")

        if (isAuxiliary) {
            return ProcessDetectionResult(
                packageName = packageName,
                processName = processName,
                isMainApplicationProcess = false,
                isAuxiliaryProcess = true,
                isSupported = false,
                targetDefinition = null,
                reason = "Auxiliary/background process ($processName) ignored for virtual camera injection."
            )
        }

        val target = SupportedTargetRegistry.find(packageName)
        return if (target != null && target.enabled) {
            DiagnosticsLogger.target("Supported target matched: ${target.packageName} (Adapter: ${target.adapterType})")
            ProcessDetectionResult(
                packageName = packageName,
                processName = processName,
                isMainApplicationProcess = true,
                isAuxiliaryProcess = false,
                isSupported = true,
                targetDefinition = target,
                reason = "Target supported and active in SupportedTargetRegistry."
            )
        } else if (target != null && !target.enabled) {
            DiagnosticsLogger.target("Target matched but disabled in registry: $packageName")
            ProcessDetectionResult(
                packageName = packageName,
                processName = processName,
                isMainApplicationProcess = true,
                isAuxiliaryProcess = false,
                isSupported = false,
                targetDefinition = target,
                reason = "Target is registered but currently disabled."
            )
        } else {
            DiagnosticsLogger.target("Target $packageName not found in SupportedTargetRegistry (target unsupported)")
            ProcessDetectionResult(
                packageName = packageName,
                processName = processName,
                isMainApplicationProcess = true,
                isAuxiliaryProcess = false,
                isSupported = false,
                targetDefinition = null,
                reason = "Package not registered in SupportedTargetRegistry."
            )
        }
    }

    fun isAuxiliaryProcess(processName: String): Boolean {
        for (suffix in AUXILIARY_PROCESS_SUFFIXES) {
            if (processName.contains(suffix)) {
                return true
            }
        }
        return false
    }

    fun extractPackageNameFromProcess(processName: String): String {
        val colonIdx = processName.indexOf(':')
        return if (colonIdx > 0) {
            processName.substring(0, colonIdx)
        } else {
            processName
        }
    }

    fun resolveProcessName(): String {
        // Method 1: Android P+ Application.getProcessName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val pName = Application.getProcessName()
                if (!pName.isNullOrBlank()) return pName
            } catch (_: Throwable) {}
        }

        // Method 2: /proc/self/cmdline
        try {
            val cmdlineFile = File("/proc/self/cmdline")
            if (cmdlineFile.exists() && cmdlineFile.canRead()) {
                BufferedReader(FileReader(cmdlineFile)).use { reader ->
                    val line = reader.readLine()
                    if (!line.isNullOrBlank()) {
                        return line.trim().replace("\u0000", "")
                    }
                }
            }
        } catch (_: Throwable) {}

        // Fallback default
        return "com.photo.android.camera"
    }
}
