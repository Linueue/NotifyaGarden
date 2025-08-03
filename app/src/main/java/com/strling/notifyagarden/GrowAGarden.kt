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
    SEEDS, GEARS, EGGS, EVENTS, WEATHER;

    override fun toString(): String {
        return when(this) {
            SEEDS -> "Seeds"
            GEARS -> "Gears"
            EGGS -> "Eggs"
            EVENTS -> "Event"
            WEATHER -> "Weather"
        }
    }
}

data class ShopDataView (
    val name: String = "",
    val stock: Int = 0,
    val color: Color = Color.White,
    val icon: String = "",
)

data class ShopDataViews (
    val items: HashMap<Categories, MutableList<ShopDataView>> = hashMapOf(),
)

class GrowAGarden {
    private val _uiState = MutableStateFlow(GrowAGardenData())
    val uiState = _uiState.asStateFlow()

    val api = GrowAGardenAPI()
    val favorites = mutableStateOf(setOf<String>())

    suspend fun fetchStocks(): Boolean
    {
        var data = _uiState.value
        val retrieved = api.fetchStocks()

        if(!retrieved.isSuccess)
            return false

        data = retrieved.getOrNull()!!
        _uiState.update { currentState ->
            currentState.copy(data.updatedAt, data.itemShops, data.weather)
        }

        return true
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
            for(category in Categories.entries) {
                if (uiState.value.itemShops[category]!!.items.containsKey(favorite))
                    fn(uiState.value.itemShops[category]!!.items[favorite]!!)
            }
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

        for(category in Categories.entries)
            views.items.put(category, mutableListOf())

        val addShopView: (Categories, String, Long, String) -> Unit = { category, name, color, icon ->
            val shops = stocks.itemShops[category]
            var stock = 0
            if(shops != null)
                stock = shops.items.getOrDefault(name.trim(), Item()).stock
            val view = ShopDataView(name, stock, Color(color), icon)
            if(stock != 0 && favorites.value.contains(name))
                views.items[category]!!.add(0, view)
            else
                views.items[category]!!.add(view)
        }

        for(item in gameItems.seedsList)
            addShopView(Categories.SEEDS, item.name, item.color, item.icon)

        for(item in gameItems.gearsList)
            addShopView(Categories.GEARS, item.name, item.color, item.icon)

        for(item in gameItems.eggsList)
            addShopView(Categories.EGGS, item.name, item.color, item.icon)

        for(item in gameItems.eventsList)
            addShopView(Categories.EVENTS, item.name, item.color, item.icon)

        for(item in gameItems.)
            addShopView(Categories.EVENTS, item.name, item.color, item.icon)

        return views
    }
}