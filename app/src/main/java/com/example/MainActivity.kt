package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.engine3d.actions.ActionManager
import com.example.engine3d.actions.InteractionSystem
import com.example.engine3d.controller.ExpandedContainerConfig
import com.example.engine3d.controller.GamepadHandler
import com.example.engine3d.controller.HudPreferences
import com.example.engine3d.importer.BatchImportManager
import com.example.engine3d.importer.CustomModelManager
import com.example.engine3d.npc.NpcManager
import com.example.engine3d.physics.PhysicsEngine
import com.example.engine3d.renderer.EngineSettings
import com.example.engine3d.terrain.TerrainMesh
import com.example.engine3d.ui.EngineScreen
import com.example.engine3d.ui.LobbyScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppNavigationScreen {
    LOBBY,
    ENGINE_WORLD
}

class MainActivity : ComponentActivity() {
    val gamepadHandler = GamepadHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val context = LocalContext.current
                    var currentScreen by remember { mutableStateOf(AppNavigationScreen.LOBBY) }

                    // Dynamically set screen orientation: Portrait in Lobby, Sensor Landscape in Gameplay
                    DisposableEffect(currentScreen) {
                        requestedOrientation = when (currentScreen) {
                            AppNavigationScreen.LOBBY -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            AppNavigationScreen.ENGINE_WORLD -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                        onDispose {}
                    }

                    // Shared Engine Singletons across Lobby & In-Game World
                    val settings = remember { EngineSettings().apply { loadFromPrefs(context) } }
                    val terrainMesh = remember { TerrainMesh() }
                    val customModelManager = remember {
                        CustomModelManager(context).also { mgr ->
                            mgr.activeCustomTerrainMesh?.let { mesh ->
                                terrainMesh.setCustomMesh(mesh)
                            }
                        }
                    }
                    val physicsEngine = remember { PhysicsEngine(terrainMesh.heightQuery) }
                    val actionManager = remember { ActionManager(physicsEngine) }
                    val npcManager = remember { NpcManager(terrainMesh.heightQuery) }
                    val interactionSystem = remember { InteractionSystem() }
                    val hudPreferences = remember { HudPreferences(context) }
                    val batchImportManager = remember {
                        BatchImportManager(
                            context = context,
                            customModelManager = customModelManager,
                            terrainMesh = terrainMesh,
                            actionManager = actionManager,
                            npcManager = npcManager,
                            barrierManager = physicsEngine.barrierManager
                        )
                    }

                    var expandedContainerConfig by remember {
                        mutableStateOf(ExpandedContainerConfig())
                    }

                    when (currentScreen) {
                        AppNavigationScreen.LOBBY -> {
                            LobbyScreen(
                                settings = settings,
                                terrainMesh = terrainMesh,
                                customModelManager = customModelManager,
                                batchImportManager = batchImportManager,
                                actionManager = actionManager,
                                npcManager = npcManager,
                                barrierManager = physicsEngine.barrierManager,
                                hudPreferences = hudPreferences,
                                expandedContainerConfig = expandedContainerConfig,
                                onExpandedConfigChanged = { expandedContainerConfig = it },
                                onEnterGame = {
                                    currentScreen = AppNavigationScreen.ENGINE_WORLD
                                },
                                interactionSystem = interactionSystem,
                                physicsEngine = physicsEngine
                            )
                        }
                        AppNavigationScreen.ENGINE_WORLD -> {
                            EngineScreen(
                                settings = settings,
                                terrainMesh = terrainMesh,
                                customModelManager = customModelManager,
                                npcManager = npcManager,
                                physicsEngine = physicsEngine,
                                actionManager = actionManager,
                                interactionSystem = interactionSystem,
                                expandedContainerConfig = expandedContainerConfig,
                                gamepadHandler = gamepadHandler,
                                onExpandedConfigChanged = { expandedContainerConfig = it },
                                onBackToLobby = {
                                    currentScreen = AppNavigationScreen.LOBBY
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (gamepadHandler.handleKeyDown(event.keyCode, event)) {
                return true
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (gamepadHandler.handleKeyUp(event.keyCode, event)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (gamepadHandler.handleGenericMotion(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }
}
