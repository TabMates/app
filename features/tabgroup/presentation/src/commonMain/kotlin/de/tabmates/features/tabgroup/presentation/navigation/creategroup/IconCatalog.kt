package de.tabmates.features.tabgroup.presentation.navigation.creategroup

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import tabmatesapp.features.tabgroup.presentation.generated.resources.Res
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_category_events
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_category_food
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_category_home
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_category_more
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_category_travel
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_color_blush
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_color_coral
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_color_mint
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_color_pink
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_color_tan
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_color_teal
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_bar
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_basketball
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_beach
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_bed
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_bike
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_boat
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_cake
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_car
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_cart
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_celebration
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_chair
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_coffee
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_drink
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_explore
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_fastfood
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_fitness
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_flight
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_florist
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_gas
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_gift
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_hiking
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_hotel
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_house
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_icecream
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_kitchen
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_map
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_mic
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_movie
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_music
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_pets
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_pizza
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_restaurant
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_sailing
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_school
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_shower
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_soccer
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_sofa
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_ticket
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_train
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_trophy
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_tv
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_wifi
import tabmatesapp.features.tabgroup.presentation.generated.resources.create_group_icon_label_work
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_beach_access
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_bed
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_cake
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_celebration
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_chair
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_coffee
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_directions_bike
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_directions_boat
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_directions_car
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_emoji_events
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_explore
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_fastfood
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_fitness_center
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_flight
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_hiking
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_home
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_hotel
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_icecream
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_kitchen
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_local_activity
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_local_bar
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_local_drink
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_local_florist
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_local_gas_station
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_local_pizza
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_map
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_mic
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_movie
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_music_note
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_pets
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_redeem
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_restaurant
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_sailing
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_school
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_shopping_cart
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_shower
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_sports_basketball
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_sports_soccer
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_train
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_tv
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_weekend
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_wifi
import tabmatesapp.features.tabgroup.presentation.generated.resources.ic_work

enum class IconCategory(val key: String, val titleRes: StringResource) {
    TRAVEL("travel", Res.string.create_group_icon_category_travel),
    FOOD("food", Res.string.create_group_icon_category_food),
    HOME("home", Res.string.create_group_icon_category_home),
    EVENTS("events", Res.string.create_group_icon_category_events),
    MORE("more", Res.string.create_group_icon_category_more),
}

data class IconOption(
    val key: String,
    val drawable: DrawableResource,
    val labelRes: StringResource,
)

data class IconColorOption(
    val key: String,
    val color: Color,
    val onColor: Color,
    val labelRes: StringResource,
)

object IconCatalog {
    val categories: List<IconCategory> = IconCategory.entries.toList()

    val iconsByCategory: Map<IconCategory, List<IconOption>> =
        mapOf(
            IconCategory.TRAVEL to
                listOf(
                    IconOption("sailing", Res.drawable.ic_sailing, Res.string.create_group_icon_label_sailing),
                    IconOption("airplane", Res.drawable.ic_flight, Res.string.create_group_icon_label_flight),
                    IconOption("car", Res.drawable.ic_directions_car, Res.string.create_group_icon_label_car),
                    IconOption("train", Res.drawable.ic_train, Res.string.create_group_icon_label_train),
                    IconOption("bike", Res.drawable.ic_directions_bike, Res.string.create_group_icon_label_bike),
                    IconOption("boat", Res.drawable.ic_directions_boat, Res.string.create_group_icon_label_boat),
                    IconOption("map", Res.drawable.ic_map, Res.string.create_group_icon_label_map),
                    IconOption("hotel", Res.drawable.ic_hotel, Res.string.create_group_icon_label_hotel),
                    IconOption("hiking", Res.drawable.ic_hiking, Res.string.create_group_icon_label_hiking),
                    IconOption("explore", Res.drawable.ic_explore, Res.string.create_group_icon_label_explore),
                    IconOption("beach", Res.drawable.ic_beach_access, Res.string.create_group_icon_label_beach),
                ),
            IconCategory.FOOD to
                listOf(
                    IconOption(
                        "restaurant",
                        Res.drawable.ic_restaurant,
                        Res.string.create_group_icon_label_restaurant,
                    ),
                    IconOption("bar", Res.drawable.ic_local_bar, Res.string.create_group_icon_label_bar),
                    IconOption("coffee", Res.drawable.ic_coffee, Res.string.create_group_icon_label_coffee),
                    IconOption("cake", Res.drawable.ic_cake, Res.string.create_group_icon_label_cake),
                    IconOption("fastfood", Res.drawable.ic_fastfood, Res.string.create_group_icon_label_fastfood),
                    IconOption("pizza", Res.drawable.ic_local_pizza, Res.string.create_group_icon_label_pizza),
                    IconOption("drink", Res.drawable.ic_local_drink, Res.string.create_group_icon_label_drink),
                    IconOption("icecream", Res.drawable.ic_icecream, Res.string.create_group_icon_label_icecream),
                ),
            IconCategory.HOME to
                listOf(
                    IconOption("house", Res.drawable.ic_home, Res.string.create_group_icon_label_house),
                    IconOption("bed", Res.drawable.ic_bed, Res.string.create_group_icon_label_bed),
                    IconOption("chair", Res.drawable.ic_chair, Res.string.create_group_icon_label_chair),
                    IconOption("sofa", Res.drawable.ic_weekend, Res.string.create_group_icon_label_sofa),
                    IconOption("shower", Res.drawable.ic_shower, Res.string.create_group_icon_label_shower),
                    IconOption("kitchen", Res.drawable.ic_kitchen, Res.string.create_group_icon_label_kitchen),
                    IconOption("tv", Res.drawable.ic_tv, Res.string.create_group_icon_label_tv),
                    IconOption("plant", Res.drawable.ic_local_florist, Res.string.create_group_icon_label_florist),
                    IconOption("wifi", Res.drawable.ic_wifi, Res.string.create_group_icon_label_wifi),
                ),
            IconCategory.EVENTS to
                listOf(
                    IconOption(
                        "celebration",
                        Res.drawable.ic_celebration,
                        Res.string.create_group_icon_label_celebration,
                    ),
                    IconOption("gift", Res.drawable.ic_redeem, Res.string.create_group_icon_label_gift),
                    IconOption("music", Res.drawable.ic_music_note, Res.string.create_group_icon_label_music),
                    IconOption(
                        "ticket",
                        Res.drawable.ic_local_activity,
                        Res.string.create_group_icon_label_ticket,
                    ),
                    IconOption("movie", Res.drawable.ic_movie, Res.string.create_group_icon_label_movie),
                    IconOption("mic", Res.drawable.ic_mic, Res.string.create_group_icon_label_mic),
                    IconOption("trophy", Res.drawable.ic_emoji_events, Res.string.create_group_icon_label_trophy),
                ),
            IconCategory.MORE to
                listOf(
                    IconOption("cart", Res.drawable.ic_shopping_cart, Res.string.create_group_icon_label_cart),
                    IconOption("pets", Res.drawable.ic_pets, Res.string.create_group_icon_label_pets),
                    IconOption("school", Res.drawable.ic_school, Res.string.create_group_icon_label_school),
                    IconOption("work", Res.drawable.ic_work, Res.string.create_group_icon_label_work),
                    IconOption(
                        "fitness",
                        Res.drawable.ic_fitness_center,
                        Res.string.create_group_icon_label_fitness,
                    ),
                    IconOption(
                        "basketball",
                        Res.drawable.ic_sports_basketball,
                        Res.string.create_group_icon_label_basketball,
                    ),
                    IconOption("soccer", Res.drawable.ic_sports_soccer, Res.string.create_group_icon_label_soccer),
                    IconOption("gas", Res.drawable.ic_local_gas_station, Res.string.create_group_icon_label_gas),
                ),
        )

    val colors: List<IconColorOption> =
        listOf(
            IconColorOption(
                "coral",
                Color(0xFFE8A87C),
                Color(0xFF4A2C1B),
                Res.string.create_group_icon_color_coral,
            ),
            IconColorOption("mint", Color(0xFFA8DADC), Color(0xFF1B3A3B), Res.string.create_group_icon_color_mint),
            IconColorOption(
                "blush",
                Color(0xFFF1D6C9),
                Color(0xFF4A2C1B),
                Res.string.create_group_icon_color_blush,
            ),
            IconColorOption("tan", Color(0xFFD9B58A), Color(0xFF3D2A14), Res.string.create_group_icon_color_tan),
            IconColorOption("pink", Color(0xFFE8C5C0), Color(0xFF4A1F1F), Res.string.create_group_icon_color_pink),
            IconColorOption("teal", Color(0xFF80CBC4), Color(0xFF13332E), Res.string.create_group_icon_color_teal),
        )

    val defaultIconKey: String = "airplane"
    val defaultColorKey: String = "coral"

    fun iconOption(key: String?): IconOption? = iconsByCategory.values.flatten().firstOrNull { it.key == key }

    fun colorOption(key: String?): IconColorOption = colors.firstOrNull { it.key == key } ?: colors.first()
}
