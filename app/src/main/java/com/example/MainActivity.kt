package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.signOutUser
import com.example.data.AppScreen
import com.example.data.UiState
import com.example.data.WorkedDefaults
import com.example.data.WorkedFormatters
import com.example.data.WorkedRepository
import com.example.ui.WorkedDashboardState
import com.example.ui.WorkedViewModel
import com.example.ui.screens.ActivityLedgerScreen
import com.example.ui.screens.AdminSettingsScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.ColleaguesScreen
import com.example.ui.screens.DailyTasksScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KeyDatesScreen
import com.example.ui.screens.MissedPunchDialog
import com.example.ui.screens.MyProfileScreen
import com.example.ui.screens.OutOnWorkScreen
import com.example.ui.screens.QuickLeaveDialog
import com.example.ui.theme.WorkedTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = true
            }
        }
        WorkedTheme(darkTheme = false) {
            AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
        }
    } else {
        val uid = user.uid
        key(uid) {
            WorkedAppShell(
                currentUserId = uid,
                userDisplayName = user.displayName ?: "Admin",
                userEmail = user.email
            )
        }
    }
}

private data class BottomNavTab(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val PrimaryTabs = listOf(
    BottomNavTab(AppScreen.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    BottomNavTab(AppScreen.EXPENSES, "Expenses", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_expenses"),
    BottomNavTab(AppScreen.FINANCE, "Finance", Icons.Rounded.MonetizationOn, Icons.Outlined.MonetizationOn, "nav_finance"),
    BottomNavTab(AppScreen.MORE, "More", Icons.Rounded.MoreHoriz, Icons.Outlined.MoreHoriz, "nav_more")
)

@Composable
fun WorkedAppShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: WorkedViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                WorkedViewModel(
                    repository = WorkedRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName
                )
            }
        }
    )

    val uiState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val yearMonth by viewModel.calendarYearMonth.collectAsStateWithLifecycle()
    val isPunchBusy by viewModel.isPunchBusy.collectAsStateWithLifecycle()
    val isBreakBusy by viewModel.isBreakBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var activeScreen by remember { mutableStateOf(AppScreen.HOME) }
    var showMissedPunchDialog by remember { mutableStateOf(false) }
    var showQuickLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle Android Back press on all sub-screens gracefully
    BackHandler(enabled = activeScreen != AppScreen.HOME) {
        activeScreen = AppScreen.HOME
    }

    val dashboard = (uiState as? UiState.Success)?.data
    val isDark = dashboard?.profile?.darkMode == true

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            val hasDarkTopHeader = activeScreen == AppScreen.HOME || activeScreen == AppScreen.PROFILE
            controller.isAppearanceLightStatusBars = if (hasDarkTopHeader) false else !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    WorkedTheme(darkTheme = isDark) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            PrimaryTabs.forEach { tab ->
                                val selected = activeScreen == tab.screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { activeScreen = tab.screen },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            PrimaryTabs.forEach { tab ->
                                val selected = activeScreen == tab.screen
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { activeScreen = tab.screen },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 640.dp)
                        ) {
                            when (val current = uiState) {
                                is UiState.Loading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                    }
                                }
                                is UiState.Error -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.large,
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(
                                                text = "Sync Error: ${current.message}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(20.dp)
                                            )
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    WorkedActiveScreenHost(
                                        screen = activeScreen,
                                        state = current.data,
                                        yearMonth = yearMonth,
                                        userEmail = userEmail,
                                        isDark = isDark,
                                        isPunchBusy = isPunchBusy,
                                        isBreakBusy = isBreakBusy,
                                        viewModel = viewModel,
                                        onNavigate = { activeScreen = it },
                                        onOpenMissedPunch = { showMissedPunchDialog = true },
                                        onOpenQuickLeave = { showQuickLeaveDialog = true },
                                        onSignOut = {
                                            signOutUser(credentialManager, scope)
                                        },
                                        snackbarHostState = snackbarHostState
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Shared Global Dialogs (Missed Punch & Quick Leave)
            if (showMissedPunchDialog && dashboard != null) {
                val todayKey = WorkedFormatters.todayDateKey()
                MissedPunchDialog(
                    initialDate = todayKey,
                    existing = dashboard.attendance[todayKey],
                    onDismiss = { showMissedPunchDialog = false },
                    onSave = { d, inT, outT, note ->
                        viewModel.saveMissedPunch(d, inT, outT, note, dashboard.profile.shiftStart)
                        showMissedPunchDialog = false
                    }
                )
            }

            if (showQuickLeaveDialog) {
                QuickLeaveDialog(
                    leaveTypes = WorkedDefaults.leaveTypes,
                    onDismiss = { showQuickLeaveDialog = false },
                    onSave = { type, from, to, note ->
                        viewModel.saveQuickLeave(type, from, to, note)
                        showQuickLeaveDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkedActiveScreenHost(
    screen: AppScreen,
    state: WorkedDashboardState,
    yearMonth: Pair<Int, Int>,
    userEmail: String?,
    isDark: Boolean,
    isPunchBusy: Boolean,
    isBreakBusy: Boolean,
    viewModel: WorkedViewModel,
    onNavigate: (AppScreen) -> Unit,
    onOpenMissedPunch: () -> Unit,
    onOpenQuickLeave: () -> Unit,
    onSignOut: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val showQuickSnackbar: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    when (screen) {
        AppScreen.HOME -> HomeScreen(
            state = state,
            isDark = isDark,
            isPunchBusy = isPunchBusy,
            isBreakBusy = isBreakBusy,
            onNavigate = onNavigate,
            onToggleDarkMode = { viewModel.toggleDarkMode(isDark) },
            onPunchToggle = {
                viewModel.handlePunchToggle(state.profile, state.attendance[WorkedFormatters.todayDateKey()])
            },
            onBreakToggle = { viewModel.handleBreakToggle(state.profile) },
            onOpenMissedPunch = onOpenMissedPunch,
            onOpenQuickLeave = onOpenQuickLeave
        )
        AppScreen.ATTENDANCE -> AttendanceScreen(
            state = state,
            yearMonth = yearMonth,
            leaveTypes = WorkedDefaults.leaveTypes,
            onBack = { onNavigate(AppScreen.HOME) },
            onChangeMonth = viewModel::changeCalendarMonth,
            onOpenMissedPunch = onOpenMissedPunch,
            onNavigate = onNavigate,
            onSaveDay = viewModel::saveDayEditor,
            onClearDay = viewModel::clearDayRecord
        )
        AppScreen.OUT_ON_WORK -> OutOnWorkScreen(
            onBack = { onNavigate(AppScreen.HOME) },
            onSaved = { onNavigate(AppScreen.RECORDS) },
            onSubmit = viewModel::saveOutOnWork
        )
        AppScreen.RECORDS -> ActivityLedgerScreen(
            state = state,
            isDark = isDark,
            onBack = { onNavigate(AppScreen.HOME) },
            onNavigate = onNavigate,
            onOpenQuickLeave = onOpenQuickLeave
        )
        AppScreen.EXPENSES -> ExpensesScreen(
            state = state,
            categories = WorkedDefaults.expenseCategories,
            isDark = isDark,
            onAddExpense = viewModel::addExpense,
            onToggleRecovered = viewModel::toggleExpenseRecovered,
            onDeleteExpense = viewModel::deleteExpense
        )
        AppScreen.FINANCE -> FinanceScreen(
            state = state,
            isDark = isDark,
            onAddFinance = viewModel::addFinance,
            onToggleStatus = viewModel::toggleFinanceStatus,
            onDeleteFinance = viewModel::deleteFinance
        )
        AppScreen.REMINDERS -> KeyDatesScreen(
            state = state,
            categories = WorkedDefaults.reminderCategories,
            isDark = isDark,
            onBack = { onNavigate(AppScreen.HOME) },
            onSaveReminder = viewModel::saveReminder,
            onDeleteReminder = viewModel::deleteReminder
        )
        AppScreen.DAILY_TASKS -> DailyTasksScreen(
            state = state,
            onBack = { onNavigate(AppScreen.HOME) },
            onAddTask = viewModel::addTask,
            onToggleTask = viewModel::toggleTask,
            onDeleteTask = viewModel::deleteTask
        )
        AppScreen.COLLEAGUES -> ColleaguesScreen(
            state = state,
            onBack = { onNavigate(AppScreen.HOME) },
            onCopiedToast = showQuickSnackbar,
            onSaveColleague = viewModel::saveColleague,
            onDeleteColleague = viewModel::deleteColleague
        )
        AppScreen.PROFILE -> MyProfileScreen(
            state = state,
            onBack = { onNavigate(AppScreen.HOME) },
            onCopiedToast = showQuickSnackbar,
            onUpdateQuickPhoto = viewModel::updateProfilePhoto,
            onSaveProfile = viewModel::saveMyProfile
        )
        AppScreen.MORE -> AdminSettingsScreen(
            state = state,
            userEmail = userEmail,
            isDark = isDark,
            onNavigate = onNavigate,
            onToggleDarkMode = { viewModel.toggleDarkMode(isDark) },
            onSavePreferences = viewModel::saveOfficePreferences,
            onExportJson = { viewModel.buildBackupJson(state) },
            onSignOut = onSignOut
        )
    }
}
