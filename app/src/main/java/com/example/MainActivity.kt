package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audio.MisbahaAudioEngine
import com.example.auth.AuthScreen
import com.example.auth.signOut
import com.example.billing.BillingManager
import com.example.data.MisbahaRepository
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.screens.AdhkarScreen
import com.example.ui.screens.MisbahaScreen
import com.example.ui.screens.StoreScreen
import com.example.ui.theme.GharsMisbahaTheme
import com.example.viewmodel.MisbahaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable data object Misbaha : AppRoute
    @Serializable data object Adhkar : AppRoute
    @Serializable data object Store : AppRoute
}

private data class NavTab(
    val route: AppRoute,
    val labelAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Force RTL layout direction for natural Arabic Misbaha experience
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                GharsAppRoot()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun GharsAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        GharsMisbahaTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedAppContent(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "مستخدم غرس",
                userEmail = user.email,
            )
        }
    }
}

@Composable
private fun AuthenticatedAppContent(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MisbahaViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MisbahaViewModel(
                    repository = MisbahaRepository(db),
                    currentUserId = currentUserId,
                    currentUserDisplayName = userDisplayName,
                    audioEngine = MisbahaAudioEngine(),
                    billingManager = BillingManager(app),
                )
            }
        }
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val adhkarState by viewModel.adhkarState.collectAsStateWithLifecycle()
    val dailyLogsState by viewModel.dailyLogsState.collectAsStateWithLifecycle()
    val purchasesState by viewModel.purchasesState.collectAsStateWithLifecycle()
    val optimistic by viewModel.optimistic.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val sandboxPackage by viewModel.sandboxCheckoutPackage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val activeProfile = (profileState as? UiState.Success)?.data ?: UserProfile(
        userId = currentUserId,
        displayName = userDisplayName,
    )

    GharsMisbahaTheme(selectedThemeId = activeProfile.selectedThemeId) {
        when (val pState = profileState) {
            UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("جارٍ مزامنة مسبحتك وغرسك...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(pState.message, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { signOut(credentialManager, scope) }) {
                            Text("إعادة تسجيل الدخول")
                        }
                    }
                }
            }

            is UiState.Success -> {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val tabs = remember {
                    listOf(
                        NavTab(
                            route = AppRoute.Misbaha,
                            labelAr = "المسبحة",
                            selectedIcon = Icons.Rounded.RadioButtonChecked,
                            unselectedIcon = Icons.Outlined.RadioButtonChecked,
                            testTag = "nav_tab_misbaha",
                        ),
                        NavTab(
                            route = AppRoute.Adhkar,
                            labelAr = "الأذكار",
                            selectedIcon = Icons.Rounded.FormatListBulleted,
                            unselectedIcon = Icons.Outlined.FormatListBulleted,
                            testTag = "nav_tab_adhkar",
                        ),
                        NavTab(
                            route = AppRoute.Store,
                            labelAr = "المتجر",
                            selectedIcon = Icons.Rounded.Storefront,
                            unselectedIcon = Icons.Outlined.Storefront,
                            testTag = "nav_tab_store",
                        ),
                    )
                }

                val navigateToTab: (AppRoute) -> Unit = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                val adhkar = (adhkarState as? UiState.Success)?.data ?: emptyList()
                val dailyLogs = (dailyLogsState as? UiState.Success)?.data ?: emptyList()
                val purchases = (purchasesState as? UiState.Success)?.data ?: emptyList()

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 700.dp
                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar {
                                    tabs.forEach { tab ->
                                        val selected = currentDestination?.hasRoute(tab.route::class) == true
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateToTab(tab.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.labelAr,
                                                )
                                            },
                                            label = { Text(tab.labelAr) },
                                            modifier = Modifier.testTag(tab.testTag),
                                        )
                                    }
                                }
                            }
                        },
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail {
                                    tabs.forEach { tab ->
                                        val selected = currentDestination?.hasRoute(tab.route::class) == true
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateToTab(tab.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.labelAr,
                                                )
                                            },
                                            label = { Text(tab.labelAr) },
                                            modifier = Modifier.testTag(tab.testTag),
                                        )
                                    }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = AppRoute.Misbaha,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            ) {
                                composable<AppRoute.Misbaha> {
                                    MisbahaScreen(
                                        profile = activeProfile,
                                        adhkar = adhkar,
                                        optimistic = optimistic,
                                        isSyncing = isSyncing,
                                        userEmail = userEmail,
                                        onTapMisbaha = { viewModel.onTasbihTap() },
                                        onResetSession = { viewModel.resetSessionCounter() },
                                        onSelectDhikr = { viewModel.selectActiveDhikr(it) },
                                        onSaveAudioPreferences = { enabled, volume, tone, haptic ->
                                            viewModel.updateAudioPreferences(enabled, volume, tone, haptic)
                                        },
                                        onPreviewTone = { tone, volume ->
                                            viewModel.previewSound(tone, volume)
                                        },
                                        onSelectBeadHex = { viewModel.setCustomBeadHex(it) },
                                        onOpenStore = { navigateToTab(AppRoute.Store) },
                                        onSignOut = {
                                            viewModel.flushPendingSync(forceImmediate = true)
                                            signOut(credentialManager, scope)
                                        },
                                    )
                                }

                                composable<AppRoute.Adhkar> {
                                    AdhkarScreen(
                                        profile = activeProfile,
                                        adhkar = adhkar,
                                        dailyLogs = dailyLogs,
                                        onSelectAndOpenMisbaha = { dhikrId ->
                                            viewModel.selectActiveDhikr(dhikrId)
                                            navigateToTab(AppRoute.Misbaha)
                                        },
                                        onAddCustomDhikr = { phrase, subtitle, goal, hex ->
                                            viewModel.addCustomDhikr(phrase, subtitle, goal, hex)
                                        },
                                        onUpdateDhikr = { id, phrase, subtitle, goal, hex ->
                                            viewModel.updateDhikr(id, phrase, subtitle, goal, hex)
                                        },
                                        onResetDhikrToday = { viewModel.resetDhikrToday(it) },
                                        onDeleteDhikr = { viewModel.deleteDhikr(it) },
                                    )
                                }

                                composable<AppRoute.Store> {
                                    StoreScreen(
                                        profile = activeProfile,
                                        purchases = purchases,
                                        sandboxCheckoutPackage = sandboxPackage,
                                        onStoreItemClick = { viewModel.equipOrUnlockStoreItem(it) },
                                        onPurchasePackageClick = { activity, pkg ->
                                            viewModel.startPackagePurchase(activity, pkg)
                                        },
                                        onConfirmSandboxPurchase = { viewModel.confirmSandboxPurchase(it) },
                                        onDismissSandboxCheckout = { viewModel.dismissSandboxCheckout() },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
