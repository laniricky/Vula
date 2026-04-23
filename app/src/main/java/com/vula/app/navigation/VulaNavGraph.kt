package com.vula.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.vula.app.chat.ui.ChatViewModel
import com.vula.app.chat.ui.ConversationScreen
import com.vula.app.contacts.ui.ContactsScreen
import com.vula.app.global.ui.feed.FeedScreen
import com.vula.app.global.ui.post.CommentScreen
import com.vula.app.global.ui.post.CreatePostScreen
import com.vula.app.global.ui.profile.ProfileScreen
import com.vula.app.global.ui.search.SearchScreen
import com.vula.app.local.ui.LocalModeScreen

// ─── Route Definitions ────────────────────────────────────────────────────────

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object Login        : Screen("login",               "Login",      null)
    object Register     : Screen("register",            "Register",   null)
    object Feed         : Screen("feed",                "Global",     Icons.Filled.Home)
    object Local        : Screen("local",               "Local Mode", Icons.Filled.Wifi)
    object CreatePost   : Screen("create_post",         "Post",       Icons.Filled.AddCircle)
    object Chat         : Screen("chat",                "Chat",       Icons.Filled.Chat)
    object Profile      : Screen("profile",             "Profile",    Icons.Filled.Person)
    object Search       : Screen("search",              "Search",     null)
    object Comments     : Screen("comments/{postId}",   "Comments",   null) {
        fun createRoute(postId: String) = "comments/$postId"
    }
    object UserProfile  : Screen("user/{userId}",       "Profile",    null) {
        fun createRoute(userId: String) = "user/$userId"
    }
    object Conversation : Screen("conversation/{roomId}", "Chat",     null) {
        fun createRoute(roomId: String) = "conversation/$roomId"
    }
    object Contacts     : Screen("contacts",            "Contacts",   null)
}

val bottomNavScreens = listOf(
    Screen.Feed, Screen.CreatePost, Screen.Chat, Screen.Profile
)

// ─── Root App ─────────────────────────────────────────────────────────────────

@Composable
fun VulaApp(
    authViewModel: AuthViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel  = hiltViewModel()   // activity-scoped for unread count
) {
    val navController    = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute     = navBackStackEntry?.destination?.route
    val currentUser      by authViewModel.currentUser.collectAsState(initial = null)
    val unreadCount      by chatViewModel.unreadCount.collectAsState()

    // Determine initial route synchronously to avoid flashing the login screen
    val startDest = remember {
        if (authViewModel.isUserLoggedIn) Screen.Feed.route else Screen.Login.route
    }

    // Still navigate if the user logs out later during runtime
    LaunchedEffect(currentUser) {
        if (currentUser == null && 
            currentRoute != Screen.Login.route && 
            currentRoute != Screen.Register.route && 
            currentRoute != null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                VulaBottomBar(
                    navController  = navController,
                    currentRoute   = currentRoute,
                    chatUnread     = unreadCount
                )
            }
        }
    ) { innerPadding ->
        VulaNavGraph(
            navController    = navController,
            currentUserId    = currentUser?.id,
            chatViewModel    = chatViewModel,
            startDestination = startDest,
            modifier         = Modifier.padding(innerPadding)
        )
    }
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────────

@Composable
fun VulaBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    chatUnread: Int
) {
    Surface(
        modifier      = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        shape         = MaterialTheme.shapes.extraLarge,
        shadowElevation = 8.dp,
        color         = MaterialTheme.colorScheme.surface
    ) {
        NavigationBar(
            containerColor  = Color.Transparent,
            tonalElevation  = 0.dp,
            modifier        = Modifier.padding(horizontal = 8.dp)
        ) {
            bottomNavScreens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                NavigationBarItem(
                    selected = isSelected,
                    onClick  = {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Feed.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    },
                    icon = {
                        screen.icon?.let { icon ->
                            // Show badge on Chat tab when there are unread items
                            if (screen == Screen.Chat && chatUnread > 0) {
                                BadgedBox(badge = {
                                    Badge {
                                        Text(
                                            if (chatUnread > 9) "9+" else "$chatUnread"
                                        )
                                    }
                                }) {
                                    Icon(icon, contentDescription = screen.title)
                                }
                            } else {
                                Icon(icon, contentDescription = screen.title)
                            }
                        }
                    },
                    label           = if (isSelected) { { Text(screen.title) } } else null,
                    alwaysShowLabel = false,
                    colors          = NavigationBarItemDefaults.colors(
                        selectedIconColor   = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        selectedTextColor   = MaterialTheme.colorScheme.primary,
                        indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

// ─── Nav Graph ────────────────────────────────────────────────────────────────

@Composable
fun VulaNavGraph(
    navController: NavHostController,
    currentUserId: String?,
    chatViewModel: ChatViewModel,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier
    ) {

        // Auth
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Feed – with Search icon + comment / profile nav
        composable(Screen.Feed.route) {
            currentUserId?.let { uid ->
                FeedScreen(
                    currentUserId        = uid,
                    onNavigateToProfile  = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    },
                    onNavigateToComments = { postId ->
                        navController.navigate(Screen.Comments.createRoute(postId))
                    },
                    onNavigateToSearch   = { navController.navigate(Screen.Search.route) }
                )
            }
        }

        // Local Mode
        composable(Screen.Local.route) {
            LocalModeScreen()
        }

        // Create Post
        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                onPostCreated = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Feed.route) { inclusive = true }
                    }
                }
            )
        }

        // Chat list – pass shared chatViewModel so unread state is consistent
        composable(Screen.Chat.route) {
            ChatListScreen(
                onChatClick  = { roomId ->
                    navController.navigate(Screen.Conversation.createRoute(roomId))
                },
                onUserClick  = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.Contacts.route)
                },
                viewModel    = chatViewModel
            )
        }

        // Contacts List
        composable(Screen.Contacts.route) {
            ContactsScreen(
                onBackClick = { navController.popBackStack() },
                onContactClick = { contact ->
                    // For now, just navigate back. Future: open invite or start chat if matched
                    navController.popBackStack()
                }
            )
        }

        // Conversation
        composable(Screen.Conversation.route) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            ConversationScreen(
                chatRoomId  = roomId,
                onBackClick = { navController.popBackStack() },
                viewModel   = chatViewModel
            )
        }

        // Own Profile
        composable(Screen.Profile.route) {
            ProfileScreen(
                userId                   = null,
                onLogoutClick            = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToConversation = { roomId ->
                    navController.navigate(Screen.Conversation.createRoute(roomId))
                }
            )
        }

        // Another user's profile (navigated-to from feed, chat, search)
        composable(Screen.UserProfile.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId                   = userId,
                onBackClick              = { navController.popBackStack() },
                onNavigateToConversation = { roomId ->
                    navController.navigate(Screen.Conversation.createRoute(roomId))
                }
            )
        }

        // Comments
        composable(Screen.Comments.route) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            CommentScreen(
                postId      = postId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Search
        composable(Screen.Search.route) {
            SearchScreen(
                onUserClick = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
