package de.tabmates.features.tabgroup.domain.group

sealed class GroupValidationError {
    data object TitleRequired : GroupValidationError()

    data object TitleTooLong : GroupValidationError()

    data object DescriptionTooLong : GroupValidationError()

    data object CurrencyRequired : GroupValidationError()
}

object GroupValidator {
    const val MAX_TITLE_LENGTH = 255
    const val MAX_DESCRIPTION_LENGTH = 1000

    fun validate(
        title: String,
        description: String,
        defaultCurrencyCode: String,
    ): GroupValidationError? =
        when {
            title.isEmpty() -> GroupValidationError.TitleRequired
            title.length > MAX_TITLE_LENGTH -> GroupValidationError.TitleTooLong
            description.length > MAX_DESCRIPTION_LENGTH -> GroupValidationError.DescriptionTooLong
            defaultCurrencyCode.isBlank() -> GroupValidationError.CurrencyRequired
            else -> null
        }
}
