package com.wanderwildwood.ibasho.commands

import kotlin.collections.filter


/**
 * Splits strings such as `fmd "this is my password" locate`.
 */
internal fun splitBySpaceWithQuotes(raw: String): List<String> =
    splitBySpaceWithQuotesKeepingQuotes(raw).map { it.trim('"') }

/**
 * The same tokens as [splitBySpaceWithQuotes], each still with its quotation marks, so a
 * token can be replaced whole in the text it came from.
 */
internal fun splitBySpaceWithQuotesKeepingQuotes(raw: String): List<String> {
    val tokens = mutableListOf<String>()
    var startPosition = 0
    var inQuote = false

    for (currentPosition in raw.indices) {
        if (raw[currentPosition] == '"') {
            inQuote = !inQuote
            continue
        }
        if (!inQuote && raw[currentPosition] == ' ') {
            val newToken = raw.substring(startPosition..currentPosition)
            tokens.add(newToken)
            startPosition = currentPosition + 1
        }
    }
    val lastToken = raw.substring(startPosition)
    tokens.add(lastToken)

    // Remove leading + trailing whitespaces. Remove tokens that are empty without their quotes.
    return tokens
        .map { it.trim() }
        .filter { it.trim('"').isNotBlank() }
}
