package com.vula.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vula.app.auth.ui.AuthViewModel
import com.vula.app.auth.ui.LoginScreen
import com.vula.app.auth.ui.RegisterScreen
import com.vula.app.chat.ui.ChatListScreen
import com.vula.app.chat.ui.ConversationScreen
import com.vula.app.global.ui.feed.FeedScreen
import com.vula.app.global.ui.post.CreatePostScreen
import com.vula.app.global.ui.profile.ProfileScreen
import com.vula.app.local.ui.LocalModeScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object Login : Screen("login", "Login", null)
    object Register : Screen("register", "Register", null)
    object Feed : Screen("feed", "Global", Icons.Filled.Home)
    object Local : Screen("local", "Local Mode", Icons.Filled.Wifi)
    object CreatePost : Screen("create_post", "Post", Icons.Filled.AddCircle)
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
}

val bottomNavScreens = listOf(
    Screen.Feed,
    Screen.Local,
    Screen.CreatePost,
    Screen.Chat,
    Screen.Profile
)

@Composable
fun VulaApp(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)

    LaunchedEffect(currentUser) {
        if (currentUser != null && (currentRoute == Screen.Login.route || currentRoute == Screen.Register.route || currentRoute == null)) {
            navController.navigate(Screen.Feed.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (currentUser == null && currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                VulaBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        VulaNavGraph(
            navController = navController,
            currentUserId = currentUser?.id,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun VulaBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        bottomNavScreens.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Feed.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    screen.icon?.let {
                        Icon(imageVector = it, contentDescription = screen.title)
                    }
                },
                label = { Text(screen.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun VulaNavGraph(navController: NavHostController, currentUserId: String?, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) { popUpTo(Screen.Login.route) { inclusive = true } } }
            )
        }
        composable(Screen.Feed.route) {
            currentUserId?.let {
                FeedScreen(currentUserId = it)
            }
        }
        composable(Screen.Local.route) {
            LocalModeScreen()
        }
        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                onPostCreated = { navController.navigate(Screen.Feed.route) { popUpTo(Screen.Feed.route) { inclusive = true } } }
            )
        }
        composable(Screen.Chat.route) {
            ChatListScreen(
                onChatClick = { roomId -> navController.navigate("conversation/$roomId") }
            )
        }
        composable("conversation/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            ConversationScreen(
                chatRoomId = roomId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogoutClick = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }
            )
        }
    }
}
