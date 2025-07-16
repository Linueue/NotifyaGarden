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

    suspend fun fetchStocks(): Boolean
    {
        var fetched = false
        try {
            var data = _uiState.value
            for(i in 1..5) {
                data = api.fetchStocks()

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

        for(item in gameItems.seedsList)
        {
            addSeedShopView(item.name, item.color, item.icon)
        }
        for(item in gameItems.gearsList)
        {
            addGearShopView(item.name, item.color, item.icon)
        }
        for(item in gameItems.eggsList)
        {
            addEggShopView(item.name, item.color, item.icon)
        }

        return views
    }
}