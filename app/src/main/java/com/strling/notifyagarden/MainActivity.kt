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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.strling.notifyagarden.proto.GameItemsOuterClass
import com.strling.notifyagarden.ui.theme.NotifyAGardenTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    class ExpandableAppBarConnection(maxTopBarHeight: Int) : NestedScrollConnection
    {
        val maxTopBarHeight: Int = maxTopBarHeight
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
        val connection = remember { ExpandableAppBarConnection(TOP_BAR_HEIGHT) }
        val timerViewModel: TimerViewModel by viewModels()
        val editMode = remember { mutableStateOf(false) }
        val scheduler = NotifyAlarmManager(this)
        val preferences = NotifyDataStore(this)
        val favorites = preferences.favorites.collectAsState(emptySet())
        NotifyData.game.favorites.value = favorites.value
        val gameItemsFlow: Flow<GameItemsOuterClass.GameItems> = gameItemsDataStore.data
        val gameItems = gameItemsFlow.collectAsState(GameItemsOuterClass.GameItems.getDefaultInstance()).value
        val context = this
        LaunchedEffect(Unit)
        {
            val isRunning = scheduler.isRunning()
            NotifyState.setNotifyRunning(isRunning)
            timerViewModel.fetchIfRunning()

            val gameItemsFirst = context.gameItemsDataStore.data.first()
            GameItemsAPI.update(gameItemsFirst.version, context)
        }
        Scaffold(topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                expandedAppBar(height = connection.appBarHeight, editable = editMode)
                collapsedAppBar(height = connection.appBarHeight, editable = editMode, scheduler = scheduler, preferences = preferences)
            }
        }, modifier = Modifier.fillMaxSize().nestedScroll(connection), containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()))
            {
                fetchButtons(scheduler, preferences)
                val uiState = NotifyData.game.uiState.collectAsState()
                val timerState = timerViewModel.uiState.collectAsState()

                LaunchedEffect(NotifyData.timer.uiState.collectAsState().value) {
                    timerViewModel.updateTimer()
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
                    Text("Next Restock", fontSize = 16.sp)
                    Text(NotifyData.timer.formatTimer(timerState.value), fontSize = 20.sp)
                }

                val views = NotifyData.game.getData(uiState.value, NotifyData.game.favorites, gameItems)
                val categories = listOf<String>("Seeds", "Gears", "Eggs")
                categories.forEach { category ->
                    Text(
                        category,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(15.dp, 0.dp).graphicsLayer { alpha = 0.8f })
                    val items = when(category)
                    {
                        "Seeds" -> views.seeds;
                        "Gears" -> views.gears;
                        "Eggs" -> views.eggs;
                        else -> listOf();
                    }
                    items.forEach { view ->
                        displayShopView(view, editMode, NotifyData.game.favorites)
                    }
                }
            }
        }
    }

    @Composable
    @Preview
    fun testView()
    {
        val view = ShopDataView("Carrot", 12, Color(0xFFF98230), "🥕")
        displayShopView(view, remember { mutableStateOf(false) }, remember { mutableStateOf(setOf<String>()) })
    }

    @Composable
    fun fetchButtons(scheduler: NotifyAlarmManager, preferences: NotifyDataStore)
    {
        Button(onClick = {
            if(!NotifyState.isNotifyRunning.value)
                scheduler.schedule(0, NotifyData.game.favorites.value)
            else
                scheduler.cancel()
            //Intent(applicationContext, NotificationService::class.java).also {
            //    it.action = if(!ServiceState.isServiceRunning.value) NotificationService.Actions.START.toString() else NotificationService.Actions.STOP.toString()

            //    startService(it)
            //}
        },
            colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().height(80.dp).padding(5.dp).clip(
                RoundedCornerShape(25.dp)
            )) {
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center)
            {
                val text = if(!NotifyState.isNotifyRunning.value) "Fetch" else "Stop"
                Text(text, modifier = Modifier.padding(12.dp))
            }
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun expandedAppBar(modifier: Modifier = Modifier, height: Int, editable: MutableState<Boolean>)
    {
        val insets = WindowInsets.statusBars.asPaddingValues()
        val padding = insets.calculateTopPadding()
        var progress = (height / TOP_BAR_HEIGHT.toFloat()).coerceIn(0.0f, 1.0f)
        progress = 2.0f * (progress - 0.5f)
        val heightAnimated by animateDpAsState(height.dp, label = "Animated height")
        val title = if(editable.value) "Notify me on" else "Stock"

        TopAppBar(
            modifier = modifier.height(heightAnimated).padding(top = padding),
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(1.0f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(title, fontSize = 40.sp, modifier = Modifier.graphicsLayer { alpha = progress })
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

                        if(!editable.value && NotifyState.isNotifyRunning.value)
                        {
                            NotifyData.game.saveFavorites(preferences)
                            scheduler.cancel()
                            val time = NotifyData.timer.getTime(NotifyData.game.uiState.value.updatedAt)
                            scheduler.schedule(time, NotifyData.game.favorites.value)
                        }
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
