package com.kingid.codesdk.util

import java.io.File
import java.security.MessageDigest

fun File.readTextSafe(): String {
    return try { this.readText() } catch (e: Exception) { "" }
}

fun File.writeTextSafe(text: String) {
    this.parentFile.mkdirs()
    this.writeText(text)
}

fun fingerprint(text: String): String {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
