package com.kingid.codesdk.util

fun requireNonNull(value: Any?, name: String): Any? {
    if (value == null) throw IllegalArgumentException("$name يجب ألا يكون null")
    return value
}

fun requireNotEmpty(value: String?, name: String): String {
    requireNonNull(value, name)
    if (value.isBlank()) throw IllegalArgumentException("$name يجب ألا يكون فارغاً")
    return value
}
