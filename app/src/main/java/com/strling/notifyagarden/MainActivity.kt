package com.strling.notifyagarden

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.strling.notifyagarden.proto.GameItemsOuterClass
import com.strling.notifyagarden.ui.theme.NotifyAGardenTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    val TOP_BAR_HEIGHT: Int = 280

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
        enableEdgeToEdge()
        setContent {
            NotifyAGardenTheme {
                content()
            }
        }
    }

    @Composable
    fun test()
    {
        Scaffold(topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
            }
        }, modifier = Modifier.fillMaxSize()) { innerPadding ->
            innerPadding
        }
    }

    class ExpandableAppBarConnection(val maxTopBarHeight: Int) : NestedScrollConnection
    {
        var appBarHeight: Int by mutableIntStateOf(maxTopBarHeight)
            private set

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y.toInt()
            if(delta > 0.0f || appBarHeight <= 0.0f)
                return Offset.Zero
            val maxAppBarHeight = maxTopBarHeight
            val newAppBarHeight = appBarHeight + delta / 2
            val previousAppBarHeight = appBarHeight
            appBarHeight = newAppBarHeight.coerceIn(0, maxAppBarHeight)
            val consumed = appBarHeight - previousAppBarHeight
            return available
            return Offset(0.0f, consumed.toFloat())
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y.toInt()
            if(delta < 0.0f)
                return Offset.Zero
            val maxAppBarHeight = maxTopBarHeight
            val newAppBarHeight = appBarHeight + delta / 2
            val previousAppBarHeight = appBarHeight
            appBarHeight = newAppBarHeight.coerceIn(0, maxAppBarHeight)
            val consumed = appBarHeight - previousAppBarHeight
            return available
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if(appBarHeight == maxTopBarHeight || appBarHeight == 0)
            {
                return Velocity.Zero
            }

            val target = (if(appBarHeight > (maxTopBarHeight / 2.0f)) maxTopBarHeight else 0)
            val animate = Animatable(appBarHeight.toFloat())
            animate.animateTo(targetValue = target.toFloat(), animationSpec = tween(durationMillis = 500))
            {
                appBarHeight = value.toInt()
            }

            return available
        }
    }

    @Composable
    fun content()
    {
        val timerViewModel: TimerViewModel by viewModels()
        val editMode = remember { mutableStateOf(false) }
        val scheduler = NotifyAlarmManager(this)
        val preferences = NotifyDataStore(this)
        val favorites = preferences.favorites.collectAsState(emptySet())
        NotifyData.game.favorites.value = favorites.value
        val gameItemsFlow: Flow<GameItemsOuterClass.GameItems> = gameItemsDataStore.data
        val gameItems = gameItemsFlow.collectAsState(GameItemsOuterClass.GameItems.getDefaultInstance()).value
        val context = this

        val currentSelected = remember { mutableStateOf(Categories.SEEDS) }
        val scrollStates = List(Categories.entries.size) { rememberScrollState() }
        val connections = List(Categories.entries.size) { remember { ExpandableAppBarConnection(TOP_BAR_HEIGHT) } }
        //val connection = remember(currentSelected) { ExpandableAppBarConnection(TOP_BAR_HEIGHT) }
        val connection = connections[currentSelected.value.ordinal]

        LaunchedEffect(Unit)
        {
            timerViewModel.fetch(false)

            val gameItemsFirst = context.gameItemsDataStore.data.first()
            GameItemsAPI.update(gameItemsFirst.version, context)
            timerViewModel.updateTimer()
        }
        Scaffold(topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                expandedAppBar(height = connection.appBarHeight, currentSelected = currentSelected.value, editable = editMode)
                collapsedAppBar(height = connection.appBarHeight, editable = editMode, scheduler = scheduler, preferences = preferences)
            }
        }, bottomBar = { bottomAppBar(currentSelected = currentSelected, gameItems = gameItems) }, modifier = Modifier.fillMaxSize().nestedScroll(connection), containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
            val uiState = NotifyData.game.uiState.collectAsState()
            val views = NotifyData.game.getData(uiState.value, NotifyData.game.favorites, gameItems)

            displayCategory(
                modifier = Modifier.padding(innerPadding),
                items = views.items[currentSelected.value]!!,
                scrollState = scrollStates[currentSelected.value.ordinal],
                editMode = editMode,
                scheduler = scheduler,
                preferences = preferences,
                timerViewModel = timerViewModel,
            )
        }
    }

    @Composable
    fun displayShopView(view: ShopDataView, editable: MutableState<Boolean>, notifyStocks: MutableState<Set<String>>)
    {
        Box(
            modifier = Modifier.fillMaxWidth().height(88.dp).padding(5.dp).clip(
                RoundedCornerShape(15.dp)
            )
                .background(MaterialTheme.colorScheme.surface)
                .then(if(editable.value) Modifier.clickable(onClick = {
                    val setMut = notifyStocks.value.toMutableSet()
                    if(notifyStocks.value.contains(view.name))
                        setMut.remove(view.name)
                    else
                        setMut.add(view.name)
                    notifyStocks.value = setMut
                }) else Modifier)
                .then(if(notifyStocks.value.contains(view.name)) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(15.dp)) else Modifier)
        )
        {
            Box(modifier = Modifier.wrapContentSize(unbounded = true, align = Alignment.TopStart).size(100.dp).offset(250.dp, 25.dp).clip(CircleShape).background(view.color))
            Box(modifier = Modifier.wrapContentSize(unbounded = true, align = Alignment.TopStart).size(55.dp).offset(345.dp, (-22).dp).clip(CircleShape).background(view.color))
            Row(modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start)
            {
                Text(view.icon, fontSize = 20.sp, modifier = Modifier.padding(10.dp))
                Column(modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center)
                {
                    Text(view.name, fontSize = 20.sp)
                    Text(
                        "x" + view.stock,
                        fontSize = 16.sp,
                        modifier = Modifier.graphicsLayer { alpha = 0.8f }
                    )
                }
            }
        }
    }

    @Composable
    fun displayCategory(modifier: Modifier = Modifier, items: List<ShopDataView>, scrollState: ScrollState, editMode: MutableState<Boolean>, scheduler: NotifyAlarmManager, preferences: NotifyDataStore, timerViewModel: TimerViewModel)
    {
        Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(scrollState))
        {
            val timerState = timerViewModel.uiState.collectAsState()

            Column(
                modifier = Modifier.fillMaxWidth().height(88.dp).padding(5.dp).clip(
                    RoundedCornerShape(15.dp)
                )
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            )
            {
                Text("Next Restock", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(NotifyData.timer.formatTimer(timerState.value), fontSize = 20.sp)
            }
            Column(
                modifier = Modifier.fillMaxWidth().height(88.dp).padding(5.dp).clip(
                    RoundedCornerShape(15.dp)
                )
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            )
            {
                Text("Weather", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                val weather = NotifyData.game.uiState.collectAsState().value.weather
                var weatherString = ""
                if(weather.isNotEmpty())
                    weatherString = weather[0].name
                for(i in 1..<weather.size)
                    weatherString += ", ${weather[i].name}"
                Text(weatherString, fontSize = 16.sp)
            }

            items.forEach { view ->
                displayShopView(view, editMode, NotifyData.game.favorites)
            }
        }
    }

    @Composable
    fun bottomAppBar(modifier: Modifier = Modifier, currentSelected: MutableState<Categories>, gameItems: GameItemsOuterClass.GameItems)
    {
        val layoutDirection = LocalLayoutDirection.current
        val navigationPadding = WindowInsets.navigationBars.asPaddingValues()
        val horizontal = navigationPadding.calculateLeftPadding(layoutDirection) + navigationPadding.calculateRightPadding(layoutDirection)
        //val contentPadding = PaddingValues(horizontal = horizontal, vertical = 0.dp)
        val contentPadding = navigationPadding
        BottomAppBar(
            actions = {
                Row(modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.background,
                    )
                    Categories.entries.forEach { category ->
                        if(category == Categories.EVENTS && gameItems.eventsList.isEmpty())
                            return@forEach

                        Button(
                            modifier = Modifier.weight(1.0f),
                            colors = colors,
                            onClick = { currentSelected.value = category }) {
                            Text(
                                category.toString(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (currentSelected.value == category) FontWeight.Bold else FontWeight.Normal,
                                style = TextStyle(
                                    textDecoration = if (currentSelected.value == category)
                                        TextDecoration.Underline
                                    else
                                        TextDecoration.None
                                )
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.height(80.dp),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun expandedAppBar(modifier: Modifier = Modifier, height: Int, currentSelected: Categories, editable: MutableState<Boolean>)
    {
        val insets = WindowInsets.statusBars.asPaddingValues()
        val padding = insets.calculateTopPadding()
        var progress = (height / TOP_BAR_HEIGHT.toFloat()).coerceIn(0.0f, 1.0f)
        progress = 2.0f * (progress - 0.5f)
        val title = if(editable.value) "Notify me on" else "Stock"
        val totalHeight = (height + 56) / 2.0

        TopAppBar(
            modifier = modifier.height(height.dp).padding(top = padding),
            title = {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().height(height.dp),
                    contentAlignment = Alignment.Center) {
                    Text(
                        title,
                        fontSize = 40.sp,
                        modifier = Modifier.graphicsLayer { alpha = progress }
                            .offset(y = (totalHeight - (height / 2)).dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            )
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun collapsedAppBar(modifier: Modifier = Modifier, height: Int, editable: MutableState<Boolean>, scheduler: NotifyAlarmManager, preferences: NotifyDataStore)
    {
        var progress = (height / TOP_BAR_HEIGHT.toFloat()).coerceIn(0.0f, 1.0f)
        progress = 2.0f * (progress)
        val title = if(editable.value) "Notify me on" else "Stock"
        TopAppBar(
            modifier = modifier,
            title = {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.graphicsLayer { alpha = (1.0f - progress) })
                    Spacer(modifier = Modifier.weight(1.0f))
                    IconButton(onClick = {
                        editable.value = !editable.value

                        if(!editable.value)
                            NotifyData.game.saveFavorites(preferences)
                    }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Edit",
                            imageVector = ImageVector.vectorResource(R.drawable.edit),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            )
        )
    }
}
