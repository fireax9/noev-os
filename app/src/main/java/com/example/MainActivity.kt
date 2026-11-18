package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.CordlyRepository
import com.example.ui.auth.AuthScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.ui.cordly.AppAppearanceStore
import com.example.ui.cordly.AppLanguageManager
import com.example.ui.cordly.LocalAppStrings
import com.example.ui.cordly.LocalOnLanguageSelected
import com.example.ui.cordly.LocalSelectedLanguageCode
import com.example.ui.cordly.CordlyHomeScreen
import com.example.ui.cordly.CordlyViewModel
import com.example.ui.theme.CordlyTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var selectedLanguageCode by remember {
                mutableStateOf(AppAppearanceStore.loadLanguageCode(context))
            }
            val appStrings = remember(selectedLanguageCode) {
                AppLanguageManager.stringsFor(selectedLanguageCode)
            }
            val layoutDirection = remember(selectedLanguageCode) {
                AppLanguageManager.layoutDirectionFor(selectedLanguageCode)
            }

            CompositionLocalProvider(
                LocalAppStrings provides appStrings,
                LocalSelectedLanguageCode provides selectedLanguageCode,
                LocalOnLanguageSelected provides { newCode ->
                    selectedLanguageCode = AppAppearanceStore.saveLanguageCode(context, newCode)
                },
                LocalLayoutDirection provides layoutDirection
            ) {
                CordlyTheme {
                    CordlyAppNavigation()
                }
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun CordlyAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val authFlow = remember(auth) { auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: CordlyViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        CordlyViewModel(
                            repository = CordlyRepository(db, auth),
                            currentUserId = uid
                        )
                    }
                }
            )
            CordlyHomeScreen(viewModel = viewModel)
        }
    }
}
