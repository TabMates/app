package de.tabmates.features.tabgroup.data.network.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Mirror of server `SplitDto` (sealed type, JSON-discriminated via Jackson `@JsonTypeInfo(use=NAME, property="type")`).
 *
 * Wire shapes:
 *  - Equal:       `{"type":"EQUAL"}`
 *  - ExactAmount: `{"type":"EXACT_AMOUNT","amount":12.5}`
 *  - Percentage:  `{"type":"PERCENTAGE","percentage":33.3}`
 *  - Shares:      `{"type":"SHARES","shares":2.0}`
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class WsSplitDto {
    @Serializable
    @SerialName("EQUAL")
    data object Equal : WsSplitDto()

    @Serializable
    @SerialName("EXACT_AMOUNT")
    data class ExactAmount(val amount: Double) : WsSplitDto()

    @Serializable
    @SerialName("PERCENTAGE")
    data class Percentage(val percentage: Double) : WsSplitDto()

    @Serializable
    @SerialName("SHARES")
    data class Shares(val shares: Double) : WsSplitDto()
}
