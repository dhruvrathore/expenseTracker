package com.expensetracker

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.domain.ExpenseRepository
import com.expensetracker.domain.MonthView
import com.expensetracker.sms.ParsedTransaction
import com.expensetracker.sms.SmsNotifier
import com.expensetracker.sms.SmsTransactionBus
import com.expensetracker.ui.AddTransactionScreen
import com.expensetracker.ui.CategoryAlertDialog
import com.expensetracker.ui.CategoryLimitsScreen
import com.expensetracker.ui.ChartsScreen
import com.expensetracker.ui.ExpenseViewModel
import com.expensetracker.ui.ExpenseViewModelFactory
import com.expensetracker.ui.HistoryMonthScreen
import com.expensetracker.ui.HistoryScreen
import com.expensetracker.ui.HomeScreen
import com.expensetracker.ui.IncomeScreen
import com.expensetracker.ui.SavingsScreen
import com.expensetracker.ui.SmsConfirmSheet
import com.expensetracker.ui.TransactionsScreen
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.util.buildTransactionsCsv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth

private fun daysInMonth(month: String): Int =
    runCatching { YearMonth.parse(month).lengthOfMonth() }.getOrDefault(31)

private object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val CATEGORIES = "categories"
    const val INCOME = "income"
    const val SAVINGS = "savings"
    const val HISTORY = "history"
    const val HISTORY_MONTH = "history/{month}"
    const val TRANSACTIONS = "transactions"
    const val CHARTS = "charts"
    const val EDIT = "edit/{id}"
    fun historyMonth(month: String) = "history/$month"
    fun edit(id: Long) = "edit/$id"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as ExpenseApp).repository

        // A tap on the "transaction detected" notification launches us with the parsed fields.
        publishSmsTransaction(intent)

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpenseTrackerApp(repository = repository)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        publishSmsTransaction(intent)
    }

    /** Reconstructs a [ParsedTransaction] from a notification-tap intent and hands it to the UI. */
    private fun publishSmsTransaction(intent: Intent?) {
        if (intent?.action != SmsNotifier.ACTION_CONFIRM_SMS_TXN) return
        val amount = intent.getDoubleExtra(SmsNotifier.EXTRA_AMOUNT, 0.0)
        if (amount <= 0.0) return
        SmsTransactionBus.post(
            ParsedTransaction(
                amount = amount,
                merchant = intent.getStringExtra(SmsNotifier.EXTRA_MERCHANT) ?: "Unknown",
                isDebit = intent.getBooleanExtra(SmsNotifier.EXTRA_IS_DEBIT, true),
                smsTimestamp = intent.getLongExtra(SmsNotifier.EXTRA_TIMESTAMP, 0L),
                isSavingsTransfer = intent.getBooleanExtra(SmsNotifier.EXTRA_IS_SAVINGS, false)
            )
        )
    }
}

@Composable
private fun ExpenseTrackerApp(repository: ExpenseRepository) {
    val navController = rememberNavController()
    val viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModelFactory(repository))
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categoryState.collectAsState()
    val income by viewModel.income.collectAsState()
    val savingsEntries by viewModel.savingsEntries.collectAsState()
    val historyMonths by viewModel.historyMonths.collectAsState()
    val alert by viewModel.pendingAlert.collectAsState()
    val suggestions by viewModel.descriptionSuggestions.collectAsState()
    val pendingSms by SmsTransactionBus.pending.collectAsState()

    RequestSmsPermissions()

    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route

    val downloadCsv = rememberCsvDownloader(context = context, scope = scope)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                DrawerItem(Icons.Filled.Home, "Home", route == Routes.HOME) {
                    closeDrawer()
                    if (route != Routes.HOME) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                }
                DrawerItem(Icons.Filled.ShoppingCart, "Transactions", route == Routes.TRANSACTIONS) {
                    closeDrawer()
                    if (route != Routes.TRANSACTIONS) navController.navigate(Routes.TRANSACTIONS)
                }
                DrawerItem(Icons.Filled.PieChart, "Charts", route == Routes.CHARTS) {
                    closeDrawer()
                    if (route != Routes.CHARTS) navController.navigate(Routes.CHARTS)
                }
                DrawerItem(Icons.AutoMirrored.Filled.List, "Category limits", route == Routes.CATEGORIES) {
                    closeDrawer()
                    if (route != Routes.CATEGORIES) navController.navigate(Routes.CATEGORIES)
                }
                DrawerItem(Icons.Filled.AccountBalanceWallet, "Income", route == Routes.INCOME) {
                    closeDrawer()
                    if (route != Routes.INCOME) navController.navigate(Routes.INCOME)
                }
                DrawerItem(Icons.Filled.Savings, "Savings and investments", route == Routes.SAVINGS) {
                    closeDrawer()
                    if (route != Routes.SAVINGS) navController.navigate(Routes.SAVINGS)
                }
                DrawerItem(Icons.Filled.DateRange, "History", route == Routes.HISTORY) {
                    closeDrawer()
                    if (route != Routes.HISTORY) navController.navigate(Routes.HISTORY)
                }
                DrawerItem(Icons.Filled.Download, "Export data", selected = false) {
                    closeDrawer()
                    scope.launch {
                        val csv = viewModel.exportTransactionsCsv()
                        downloadCsv("expense_history.csv", csv)
                    }
                }
            }
        }
    ) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = state,
                    onAddClick = { navController.navigate(Routes.ADD) },
                    onOpenDrawer = openDrawer,
                    onSetLimit = viewModel::setMonthlyLimit
                )
            }
            composable(Routes.CATEGORIES) {
                CategoryLimitsScreen(
                    categories = categories,
                    onOpenDrawer = openDrawer,
                    onSetCategoryLimit = viewModel::setCategoryLimit
                )
            }
            composable(Routes.INCOME) {
                IncomeScreen(
                    income = income,
                    categories = categories,
                    onOpenDrawer = openDrawer,
                    onSetIncome = viewModel::setIncome
                )
            }
            composable(Routes.SAVINGS) {
                SavingsScreen(
                    savingsEntries = savingsEntries,
                    income = income,
                    onOpenDrawer = openDrawer,
                    onAddSavings = viewModel::addSavings,
                    onDeleteSavingsEntry = viewModel::deleteSavingsEntry
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    months = historyMonths,
                    onOpenDrawer = openDrawer,
                    onMonthClick = { month -> navController.navigate(Routes.historyMonth(month)) }
                )
            }
            historyMonthDestination(
                viewModel,
                onBack = { navController.popBackStack() },
                onExport = { month, view ->
                    val csv = buildTransactionsCsv(view.transactions, view.savingsEntries)
                    downloadCsv("expense_$month.csv", csv)
                }
            )
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(
                    transactions = state.transactions,
                    onOpenDrawer = openDrawer,
                    onEdit = { txn -> navController.navigate(Routes.edit(txn.id)) },
                    onDelete = viewModel::deleteTransaction,
                    onClearMonth = viewModel::clearCurrentMonthTransactions
                )
            }
            composable(Routes.CHARTS) {
                ChartsScreen(
                    categories = categories,
                    transactions = state.transactions,
                    daysInMonth = daysInMonth(viewModel.currentMonth),
                    onOpenDrawer = openDrawer
                )
            }
            composable(Routes.EDIT) { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull()
                val txn = state.transactions.firstOrNull { it.id == id }
                if (txn == null) {
                    navController.popBackStack()
                } else {
                    AddTransactionScreen(
                        title = "Edit transaction",
                        initialAmount = txn.amount.toString().removeSuffix(".0"),
                        initialDescription = txn.description,
                        initialCategory = txn.category,
                        initialTag = txn.tag.orEmpty(),
                        suggestions = suggestions,
                        onBack = { navController.popBackStack() },
                        onSave = { amount, description, category, tag ->
                            viewModel.updateTransaction(txn.id, amount, description, category, tag)
                        }
                    )
                }
            }
            composable(Routes.ADD) {
                AddTransactionScreen(
                    suggestions = suggestions,
                    onBack = { navController.popBackStack() },
                    onSave = viewModel::addTransaction
                )
            }
        }

        // Rendered above every screen so add- and edit-triggered alerts always surface.
        alert?.let { CategoryAlertDialog(it, onDismiss = viewModel::clearAlert) }

        // A transaction detected in an SMS (real-time or via notification tap) awaiting confirmation.
        pendingSms?.let { txn ->
            SmsConfirmSheet(
                transaction = txn,
                suggestions = suggestions,
                onSave = viewModel::addTransaction,
                onSaveSavings = viewModel::addSavingsFromSms,
                onDismiss = SmsTransactionBus::clear
            )
        }
    }
}

/** Asks for SMS (and, on Android 13+, notification) permissions once when the app first opens. */
@Composable
private fun RequestSmsPermissions() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* If denied, the receiver simply stays dormant; the app remains fully usable. */ }

    LaunchedEffect(Unit) {
        val wanted = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
    }
}

/**
 * Returns a function that downloads a CSV export: saves it to the public Downloads folder, then
 * opens the share sheet on the same file (e.g. to send it straight to Claude). On Android 9 and
 * below, writing to the public Downloads folder requires [Manifest.permission.WRITE_EXTERNAL_STORAGE],
 * so this requests it first if needed.
 */
@Composable
private fun rememberCsvDownloader(context: Context, scope: CoroutineScope): (fileName: String, csv: String) -> Unit {
    var pendingExport by remember { mutableStateOf<Pair<String, String>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val (fileName, csv) = pendingExport ?: return@rememberLauncherForActivityResult
        pendingExport = null
        if (granted) {
            scope.launch { saveAndShareCsv(context, csv, fileName) }
        } else {
            Toast.makeText(context, "Storage permission is required to save the export", Toast.LENGTH_LONG).show()
        }
    }
    return { fileName, csv ->
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingExport = fileName to csv
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            scope.launch { saveAndShareCsv(context, csv, fileName) }
        }
    }
}

/** Saves [csv] as [fileName] in the public Downloads folder, then opens the share sheet on it. */
private suspend fun saveAndShareCsv(context: Context, csv: String, fileName: String) {
    val result = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create download entry")
                resolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                    ?: error("Could not open output stream")
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                file.writeText(csv)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        }
    }

    val uri = result.getOrNull()
    if (uri == null) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save export", Toast.LENGTH_LONG).show()
        }
        return
    }

    withContext(Dispatchers.Main) {
        Toast.makeText(context, "Saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export spending history"))
    }
}

private fun NavGraphBuilder.historyMonthDestination(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
    onExport: (String, MonthView) -> Unit
) {
    composable(Routes.HISTORY_MONTH) { entry ->
        val month = entry.arguments?.getString("month").orEmpty()
        val view by remember(month) { viewModel.observeMonth(month) }
            .collectAsState(initial = null)
        HistoryMonthScreen(
            month = month,
            view = view,
            onBack = onBack,
            onExport = { view?.let { onExport(month, it) } }
        )
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}
