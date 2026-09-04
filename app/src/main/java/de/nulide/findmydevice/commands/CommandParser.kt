package de.nulide.findmydevice.commands


sealed class ParserResult {
    // For valid syntax
    data class Success(
        // fmd
        val triggerWord: String,
        // mypass -> hashed
        val passwordHash: String?,
        // locate
        val command: Command,
        // gps
        val args: List<String>,
    ) : ParserResult()

    // For invalid syntax

    object EmptyInvalid : ParserResult()

    data class TriggerWordMismatch(
        val actual: String,
        val expected: String,
    ) : ParserResult()
}


/**
 * Parses a raw command string (such as `fmd mypin locate gps`) into a [ParserResult].
 */
class CommandParser(
    val expectedTriggerWord: String,
    val helpCommand: Command,
    val availableCommands: List<Command>,
    val hashPassword: suspend (String) -> String,
) {

    suspend fun parse(raw: String): ParserResult {
        val tokens = splitBySpaceWithQuotes(raw)
        val iter = tokens.iterator()

        if (!iter.hasNext()) {
            return ParserResult.EmptyInvalid
        }
        val firstToken = iter.next()

        // Not an FMD command
        if (firstToken.lowercase() != expectedTriggerWord.lowercase()) {
            return ParserResult.TriggerWordMismatch(firstToken, expectedTriggerWord)
        }

        // No command ==> show help
        // Note that this is valid syntax, hence we return "Success".
        if (!iter.hasNext()) {
            return ParserResult.Success(
                triggerWord = expectedTriggerWord,
                passwordHash = null,
                command = helpCommand,
                args = emptyList(),
            )
        }
        val secondToken = iter.next()

        // Catch "help loop": https://gitlab.com/fmd-foss/fmd-android/-/work_items/442
        // Only check the keyword (not the usage) because the special characters may confuse it.
        val commandMatches = availableCommands.count { raw.contains(it.keyword) }
        if (commandMatches >= 3) {
            return ParserResult.EmptyInvalid
        }

        // Check if correct PIN is present.
        val matchesKnownCommand = availableCommands.any {
            it.keyword.lowercase() == secondToken.lowercase()
        }
        var passwordHash: String? = null
        if (!matchesKnownCommand) {
            // If the second token does not match any known command,
            // then we assume that it is the access password.
            val password = secondToken
            passwordHash = hashPassword(password)
        }

        val commandKeyword: String
        if (passwordHash != null) {
            // Pin but no command ==> show help
            if (!iter.hasNext()) {
                return ParserResult.Success(
                    triggerWord = expectedTriggerWord,
                    passwordHash,
                    command = helpCommand,
                    args = emptyList(),
                )
            }
            // Valid PIN: treat the third token as the command
            val thirdToken = iter.next()
            commandKeyword = thirdToken
        } else {
            // No PIN or invalid PIN: treat second token as the command
            commandKeyword = secondToken
        }

        // All remaining tokens are the arguments for the command
        val args = iter.asSequence().toList()

        for (command in availableCommands) {
            if (command.keyword.lowercase() == commandKeyword.lowercase()) {
                return ParserResult.Success(
                    triggerWord = expectedTriggerWord,
                    passwordHash,
                    command,
                    args,
                )
            }
        }

        // Show the help if the user sent an invalid command
        return ParserResult.Success(
            triggerWord = expectedTriggerWord,
            passwordHash,
            helpCommand,
            emptyList(),
        )
    }
}
