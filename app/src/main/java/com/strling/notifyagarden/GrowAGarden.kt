package com.strling.notifyagarden

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopDataView (
    val name: String = "",
    val stock: Int = 0,
    val color: Color = Color.White,
    val icon: String = "",
)

data class ShopDataViews (
    val seeds: MutableList<ShopDataView> = mutableListOf<ShopDataView>(),
    val gears: MutableList<ShopDataView> = mutableListOf<ShopDataView>(),
    val eggs: MutableList<ShopDataView> = mutableListOf<ShopDataView>(),
)

class GrowAGarden {
    private val _uiState = MutableStateFlow(GrowAGardenData())
    val uiState = _uiState.asStateFlow()

    val api = GrowAGardenAPI()
    val favorites = mutableStateOf(setOf<String>())

    suspend fun fetchStocks()
    {
        _uiState.update { currentState ->
            try {
                var data = currentState
                while(true) {
                    data = api.fetchStocks()

                    if(data.updatedAt != currentState.updatedAt)
                        break
                    delay(5000)
                }
                currentState.copy(data.updatedAt, data.seedShop, data.gearShop, data.eggShop)
            } catch (e: Exception) {
                println(e.toString())
                currentState
            }
        }
    }

    fun notifyFavorites(fn: (Item) -> Unit)
    {
        for(favorite in favorites.value)
        {
            if(uiState.value.seedShop.items.containsKey(favorite))
                fn(uiState.value.seedShop.items[favorite]!!)
            if(uiState.value.gearShop.items.containsKey(favorite))
                fn(uiState.value.gearShop.items[favorite]!!)
            if(uiState.value.eggShop.items.containsKey(favorite))
                fn(uiState.value.eggShop.items[favorite]!!)
        }
    }

    fun getData(stocks: GrowAGardenData, favorites: MutableState<Set<String>>): ShopDataViews
    {
        val views = ShopDataViews()

        val addSeedShopView: (String, Long, String) -> Unit = { name, color, icon ->
            val item = stocks.seedShop.items.getOrDefault(name, Item())
            val view = ShopDataView(name, item.stock, Color(color), icon)
            if(item.stock != 0 && favorites.value.contains(name))
                views.seeds.add(0, view)
            else
                views.seeds.add(view)
        }
        val addGearShopView: (String, Long, String) -> Unit = { name, color, icon ->
            val item = stocks.gearShop.items.getOrDefault(name, Item())
            val view = ShopDataView(name, item.stock, Color(color), icon)
            if(item.stock != 0 && favorites.value.contains(name))
                views.gears.add(0, view)
            else
                views.gears.add(view)
        }
        val addEggShopView: (String, Long, String) -> Unit = { name, color, icon ->
            val item = stocks.eggShop.items.getOrDefault(name, Item())
            val view = ShopDataView(name, item.stock, Color(color), icon)
            if(item.stock != 0 && favorites.value.contains(name))
                views.eggs.add(0, view)
            else
                views.eggs.add(view)
        }

        addSeedShopView("Carrot", 0xFFFFA500, "🥕")
        addSeedShopView("Strawberry", 0xFFdb3d21, "🍓")
        addSeedShopView("Blueberry", 0xFF4B0082, "🫐")
        addSeedShopView("Orange Tulip", 0xFFFFA500, "🌷")
        addSeedShopView("Tomato", 0xFFFF6347, "🍅")
        addSeedShopView("Daffodil", 0xFFFFFF00, "🌼")
        addSeedShopView("Watermelon", 0xFFFC6C85, "🍉")
        addSeedShopView("Pumpkin", 0xFFFF7518, "🎃")
        addSeedShopView("Apple", 0xFFFF0800, "🍎")
        addSeedShopView("Bamboo", 0xFF7BB661, "🎋")
        addSeedShopView("Coconut", 0xFF8B4513, "🥥")
        addSeedShopView("Cactus", 0xFF228B22, "🌵")
        addSeedShopView("Dragon Fruit", 0xFFFF2E93, "🥝")
        addSeedShopView("Mango", 0xFFFFC324, "🥭")
        addSeedShopView("Grape", 0xFF6F2DA8, "🍇")
        addSeedShopView("Mushroom", 0xFFDAB9, "🍄")
        addSeedShopView("Pepper", 0xFFFF0000, "🌶️")
        addSeedShopView("Beanstalk", 0xFF3CB371, "🌿")
        addSeedShopView("Ember Lily", 0xFFFF4500, "🌺")
        addSeedShopView("Sugar Apple", 0xFF7FFFD4, "🍏")
        addSeedShopView("Burning Bud", 0xFFB22222, "☀️")

        addGearShopView("Watering Can", 0xFFB0C4DE, "🪣")
        addGearShopView("Trowel", 0xFF8B4513, "🧑‍🌾")
        addGearShopView("Recall Wrench", 0xFF708090, "🔧")
        addGearShopView("Basic Sprinkler", 0xFFADD8E6, "💧")
        addGearShopView("Advanced Sprinkler", 0xFF00BFFF, "💦")
        addGearShopView("Godly Sprinkler", 0xFF1E90FF, "🌧️")
        addGearShopView("Magnifying Glass", 0xFFDAA520, "🔍")
        addGearShopView("Tanning Mirror", 0xFFFFF5EE, "🪞")
        addGearShopView("Master Sprinkler", 0xFF4682B4, "🌊")
        addGearShopView("Cleaning Spray", 0xFFFFE4C4, "🧴")
        addGearShopView("Favorite Tool", 0xFFDAA520, "🛠️")
        addGearShopView("Harvest Tool", 0xFFDEB887, "🌾")
        addGearShopView("Friendship Pot", 0xFF9ACD32, "🪴")

        addEggShopView("Common Egg", 0xFFF0EAD6, "🥚")
        addEggShopView("Common Summer Egg", 0xFFF0EAD6, "🥚")
        addEggShopView("Uncommon Egg", 0xFFFFFF00, "🟡")
        addEggShopView("Rare Egg", 0xFF1E90FF, "🔵")
        addEggShopView("Rare Summer Egg", 0xFF1E90FF, "🔵")
        addEggShopView("Legendary Egg", 0xFF8A2BE2, "🟣")
        addEggShopView("Mythical Egg", 0xFFFFD700, "🌟")
        addEggShopView("Paradise Egg", 0xFFFFD700, "🌟")
        addEggShopView("Bug Egg", 0xFF556B2F, "🐛")
        addEggShopView("Bee Egg", 0xFFFFC107, "🐝")

        return views
    }
}