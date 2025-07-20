package com.strling.notifyagarden

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.strling.notifyagarden.proto.GameItemsOuterClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Categories {
    SEEDS, GEARS, EGGS, EVENTS,
}

data class ShopDataView (
    val name: String = "",
    val stock: Int = 0,
    val color: Color = Color.White,
    val icon: String = "",
)

data class ShopDataViews (
    val seeds: MutableList<ShopDataView>  = mutableListOf<ShopDataView>(),
    val gears: MutableList<ShopDataView>  = mutableListOf<ShopDataView>(),
    val eggs: MutableList<ShopDataView>   = mutableListOf<ShopDataView>(),
    val events: MutableList<ShopDataView> = mutableListOf<ShopDataView>(),
)

class GrowAGarden {
    private val _uiState = MutableStateFlow(GrowAGardenData())
    val uiState = _uiState.asStateFlow()

    val api = GrowAGardenAPI()
    val favorites = mutableStateOf(setOf<String>())

    suspend fun fetchStocks(): Boolean
    {
        var fetched = false
        try {
            var data = _uiState.value
            for(i in 1..5) {
                val retrieved = api.fetchStocks()

                if(!retrieved.isSuccess)
                    break
                data = retrieved.getOrNull()!!
                if(data.updatedAt != _uiState.value.updatedAt) {
                    fetched = true
                    break
                }
                delay(5000)
            }
            _uiState.update { currentState ->
                currentState.copy(data.updatedAt, data.seedShop, data.gearShop, data.eggShop)
            }
        } catch (e: Exception) {
            println(e.toString())
        }

        return fetched
    }

    fun saveFavorites(dataStore: NotifyDataStore)
    {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.setFavorites(favorites.value)
        }
    }

    fun notifyFavorites(fn: (Item) -> Unit, favorites: Set<String>)
    {
        for(favorite in favorites)
        {
            if(uiState.value.seedShop.items.containsKey(favorite))
                fn(uiState.value.seedShop.items[favorite]!!)
            if(uiState.value.gearShop.items.containsKey(favorite))
                fn(uiState.value.gearShop.items[favorite]!!)
            if(uiState.value.eggShop.items.containsKey(favorite))
                fn(uiState.value.eggShop.items[favorite]!!)
            if(uiState.value.eventShop.items.containsKey(favorite))
                fn(uiState.value.eventShop.items[favorite]!!)
        }
    }

    fun reset()
    {
        _uiState.update { currentState ->
            currentState.copy(updatedAt = 0)
        }
    }

    fun getData(stocks: GrowAGardenData, favorites: MutableState<Set<String>>, gameItems: GameItemsOuterClass.GameItems): ShopDataViews
    {
        val views = ShopDataViews()

        val addShopView: (Categories, String, Long, String) -> Unit = { category, name, color, icon ->
            val (shop, views) = when(category) {
                Categories.SEEDS  -> Pair(stocks.seedShop, views.seeds)
                Categories.GEARS  -> Pair(stocks.gearShop, views.gears)
                Categories.EGGS   -> Pair(stocks.eggShop, views.eggs)
                Categories.EVENTS -> Pair(stocks.eventShop, views.events)
            }

            val item = shop.items.getOrDefault(name, Item())
            val view = ShopDataView(name, item.stock, Color(color), icon)
            if(item.stock != 0 && favorites.value.contains(name))
                views.add(0, view)
            else
                views.add(view)
        }

        for(item in gameItems.seedsList)
            addShopView(Categories.SEEDS, item.name, item.color, item.icon)

        for(item in gameItems.gearsList)
            addShopView(Categories.GEARS, item.name, item.color, item.icon)

        for(item in gameItems.eggsList)
            addShopView(Categories.EGGS, item.name, item.color, item.icon)

        for(item in gameItems.eventsList)
            addShopView(Categories.EVENTS, item.name, item.color, item.icon)

        return views
    }
}