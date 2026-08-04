package de.tabmates.core.designsystem.textfields

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

/**
 * Folds an email field to lower case as it is typed or pasted, so what the user sees is what gets
 * sent. Mail hosts treat addresses case-insensitively, and an account registered as "Max@Web.DE"
 * must still be reachable when signing in as "max@web.de".
 *
 * Uses [Char.lowercaseChar] per character rather than [String.lowercase] on purpose: the mapping is
 * one character to one, so the length is unchanged and the cursor stays where the user put it.
 */
object EmailInputTransformation : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val text = asCharSequence().toString()
        text.forEachIndexed { index, char ->
            val lowercase = char.lowercaseChar()
            if (char != lowercase) replace(index, index + 1, lowercase.toString())
        }
    }
}
