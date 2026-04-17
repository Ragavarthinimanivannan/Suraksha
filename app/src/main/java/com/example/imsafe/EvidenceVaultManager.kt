package com.example.imsafe

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class EvidenceVaultManager(private val context: Context) {
    private val vaultDir = "EvidenceVault"
    private val prefs: SharedPreferences = context.getSharedPreferences("EvidenceVault", Context.MODE_PRIVATE)

    fun saveEvidence(filePath: String, sosId: String, isLocked: Boolean = true): String {
        val originalFile = File(filePath)
        val evidenceId = generateEvidenceId()

        // Create vault directory
        val vaultPath = File(context.filesDir, vaultDir)
        if (!vaultPath.exists()) vaultPath.mkdirs()

        // Copy file to vault
        val evidenceFile = File(vaultPath, "EVIDENCE_${evidenceId}_${originalFile.name}")
        originalFile.copyTo(evidenceFile, overwrite = true)

        // Create metadata
        val metadata = mapOf(
            "evidence_id" to evidenceId,
            "sos_id" to sosId,
            "file_path" to evidenceFile.absolutePath,
            "original_path" to filePath,
            "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "hash" to calculateFileHash(evidenceFile),
            "is_locked" to isLocked,
            "can_delete" to false
        )

        // Save metadata
        prefs.edit().putString(evidenceId, metadata.toString()).apply()

        Log.d("EVIDENCE", "🔒 Evidence saved: $evidenceId")
        return evidenceId
    }

    fun getEvidence(evidenceId: String): Map<String, String>? {
        val metadataStr = prefs.getString(evidenceId, null)
        return parseMetadata(metadataStr)
    }

    fun getAllEvidence(): List<Map<String, String>> {
        return prefs.all.mapNotNull { (_, value) ->
            parseMetadata(value.toString())
        }
    }

    fun deleteEvidence(evidenceId: String): Boolean {
        val metadata = getEvidence(evidenceId)
        return if (metadata?.get("can_delete") == "true") {
            val filePath = metadata["file_path"]
            if (!filePath.isNullOrEmpty()) {
                File(filePath).delete()
            }
            prefs.edit().remove(evidenceId).apply()
            true
        } else {
            false
        }
    }

    fun verifyEvidence(evidenceId: String): Boolean {
        val metadata = getEvidence(evidenceId) ?: return false
        val filePath = metadata["file_path"] ?: return false
        val storedHash = metadata["hash"] ?: return false

        val file = File(filePath)
        if (!file.exists()) return false

        val currentHash = calculateFileHash(file)
        return storedHash == currentHash
    }

    private fun generateEvidenceId(): String {
        return "EVID_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())}"
    }

    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun parseMetadata(metadataStr: String?): Map<String, String>? {
        if (metadataStr.isNullOrEmpty()) return null
        return metadataStr.removePrefix("{").removeSuffix("}").split(", ")
            .associate { it.split("=").let { pair -> pair[0] to pair.getOrElse(1) { "" } } }
    }
}