package com.kingid.codesdk.parser

/**
 * محلل رمزي (Tokenizer) خفيف الوزن يُستخدم كحلّ افتراضي (fallback) quando لا تتوفر مكتبات AST خارجية.
 * يكفي لاكتشاف الأخطاء النحوية الأساسية (توازن الأقواس، السلاسل غير المنتهية، إلخ).
 */
class Tokenizer {

    data class Token(
        val type: String,
        val text: String,
        val offset: Int,
        val line: Int,
        val column: Int
    )

    fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var line = 1
        var column = 1
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '\n' -> { line++; column = 1; i++ }
                ch == '\r' -> { i++ }
                ch.isWhitespace() -> { column++; i++ }
                ch == '{' || ch == '}' || ch == '(' || ch == ')' || ch == '[' || ch == ']' -> {
                    tokens.add(Token("PUNCT", ch.toString(), i, line, column))
                    column++; i++
                }
                ch == '"' || ch == '\'' -> {
                    val start = i
                    val quote = ch
                    i++
                    while (i < text.length && text[i] != quote) {
                        if (text[i] == '\n') break
                        if (text[i] == '\\' && i + 1 < text.length) i++
                        i++
                        column++
                    }
                    if (i < text.length) i++
                    tokens.add(Token("STRING", text.substring(start, i.coerceAtMost(text.length)), start, line, column))
                    column += (i - start)
                }
                else -> {
                    val start = i
                    while (i < text.length &&
                        (Character.isLetterOrDigit(text[i].code) || text[i] == '_')) {
                        column++; i++
                    }
                    if (i > start) {
                        val identStartColumn = column - (i - start)
                        tokens.add(Token("IDENT", text.substring(start, i), start, line, identStartColumn))
                    } else {
                        column++; i++
                    }
                }
            }
        }
        return tokens
    }
}
