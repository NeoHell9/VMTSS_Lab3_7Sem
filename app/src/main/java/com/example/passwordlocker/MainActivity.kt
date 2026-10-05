package com.example.passwordlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.passwordlocker.ui.EntryEditScreen
import com.example.passwordlocker.ui.EntryListScreen
import com.example.passwordlocker.ui.EntryViewModel
import com.example.passwordlocker.ui.UnlockScreen
import com.example.passwordlocker.ui.theme.PasswordLockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PasswordLockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val vm: EntryViewModel = viewModel()

                    NavHost(navController = navController, startDestination = "unlock") {
                        composable("unlock") {
                            UnlockScreen(vm = vm, onUnlocked = {
                                navController.navigate("list") {
                                    popUpTo("unlock") { inclusive = true }
                                }
                            })
                        }
                        composable("list") {
                            EntryListScreen(
                                vm = vm,
                                onAddEntry = { navController.navigate("edit/null") },
                                onEditEntry = { id -> navController.navigate("edit/$id") }
                            )
                        }
                        composable("edit/{id}") { backStackEntry ->
                            val idStr = backStackEntry.arguments?.getString("id")
                            val entryId = if (idStr == "null") null else idStr?.toLongOrNull()
                            EntryEditScreen(
                                vm = vm,
                                entryId = entryId,                 // ⬅️ передаём ID, не объект
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}