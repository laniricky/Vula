package com.vula.app.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vula.app.auth.ui.AuthViewModel
import com.vula.app.auth.ui.LoginScreen
import com.vula.app.auth.ui.RegisterScreen
import com.vula.app.chat.ui.ChatListScreen
import com.vula.app.chat.ui.ChatViewModel
import com.vula.app.chat.ui.ConversationScreen
import com.vula.app.contacts.ui.ContactsScreen
import com.vula.app.core.data.PreferencesDataStore
import com.vula.app.core.ui.OnboardingScreen
import com.vula.app.global.ui.feed.FeedScreen
import com.vula.app.global.ui.post.CommentScreen
import com.vula.app.global.ui.post.CreatePostScreen
import com.vula.app.global.ui.profile.EditProfileScreen
import com.vula.app.global.ui.profile.ProfileScreen
import com.vula.app.global.ui.search.SearchScreen
import com.vula.app.global.ui.story.StoryViewerScreen
import com.vula.app.local.ui.LocalModeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// ─── Route Definitions ────────────────────────────────────────────────────────

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object Onboarding  : Screen("onboarding",           "Welcome",    null)
    object Login       : Screen("login",                "Login",      null)
    object Register    : Screen("register",             "Register",   null)
    object Feed        : Screen("feed",                 "Global",     Icons.Filled.Home)
    object Local       : Screen("local",                "Local Mode", Icons.Filled.Wifi)
    object CreatePost  : Screen("create_post",          "Post",       Icons.Filled.AddCircle)
    object CreateStory : Screen("create_story",         "Story",      null)
    object Chat        : Screen("chat",                 "Chat",       Icons.AutoMirrored.Filled.Chat)
    object Profile     : Screen("profile",              "Profile",    Icons.Filled.Person)
    object Settings    : Screen("settings",             "Settings",   null)
    object EditProfile : Screen("edit_profile",         "Edit",       null)
    object StoryViewer : Screen("story/{index}",        "Story",      null) {
        fun createRoute(index: Int) = "story/$index"
    }
    object Search      : Screen("search",               "Search",     null)
    object Comments    : Screen("comments/{postId}",    "Comments",   null) {
        fun createRoute(postId: String) = "comments/$postId"
    }
    object UserProfile : Screen("user/{userId}",        "Profile",    null) {
        fun createRoute(userId: String) = "user/$userId"
    }
    object Conversation: Screen("conversation/{roomId}","Chat",       null) {
        fun createRoute(roomId: String, replyContext: String? = null, contactName: String? = null): String {
            var route = "conversation/$roomId"
            val params = buildList {
                if (replyContext != null) add("replyContext=${java.net.URLEncoder.encode(replyContext, "UTF-8")}")
                if (contactName != null) add("contactName=${java.net.URLEncoder.encode(contactName, "UTF-8")}")
            }
            if (params.isNotEmpty()) route += "?${params.joinToString("&")}"
            return route
        }
        val routeWithArgs = "conversation/{roomId}?replyContext={replyContext}&contactName={contactName}"
    }
    object Contacts    : Screen("contacts",             "Contacts",   null)
}

val bottomNavScreens = listOf(
    Screen.Feed, Screen.CreatePost, Screen.Chat, Screen.Profile
)

// ─── Root App ─────────────────────────────────────────────────────────────────

@Composable
fun VulaApp(
    authViewModel: AuthViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel  = hiltViewModel()
) {
    val navController      = rememberNavController()
    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentRoute       = navBackStackEntry?.destination?.route
    val currentUser        by authViewModel.currentUser.collectAsState(initial = null)
    val unreadCount        by chatViewModel.unreadCount.collectAsState()
    // Collect login state from DataStore-backed Flow
    val isLoggedIn         by authViewModel.isUserLoggedIn.collectAsState(initial = false)

    val startDest = remember {
        val loggedIn = runBlocking { authViewModel.isUserLoggedIn.first() }
        if (loggedIn) Screen.Feed.route else Screen.Login.route
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null &&
            currentRoute != Screen.Login.route &&
            currentRoute != Screen.Register.route &&
            currentRoute != Screen.Onboarding.route &&
            currentRoute != null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                    modifier = Modifier.padding(androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp))
                )
            }
        }
    ) {
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
                openDrawer       = { coroutineScope.launch { drawerState.open() } },
                modifier         = Modifier.padding(innerPadding)
            )
        }
    }
}

// ─── Bottom Bar with animated tab scaling ─────────────────────────────────────

@Composable
fun VulaBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    chatUnread: Int
) {
    Surface(
        modifier        = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        shape           = MaterialTheme.shapes.extraLarge,
        shadowElevation = 8.dp,
        color           = MaterialTheme.colorScheme.surface
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier       = Modifier.padding(horizontal = 8.dp)
        ) {
            bottomNavScreens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                // Animated scale for selected tab
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.18f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    label = "tab_scale"
                )
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
                            if (screen == Screen.Chat && chatUnread > 0) {
                                BadgedBox(badge = {
                                    Badge { Text(if (chatUnread > 9) "9+" else "$chatUnread") }
                                }) {
                                    Icon(icon, contentDescription = screen.title,
                                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
                                }
                            } else {
                                Icon(icon, contentDescription = screen.title,
                                    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VulaNavGraph(
    navController: NavHostController,
    currentUserId: String?,
    chatViewModel: ChatViewModel,
    startDestination: String,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    SharedTransitionLayout {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier
    ) {

        // Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

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

        // Feed
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
                    onNavigateToSearch   = { navController.navigate(Screen.Search.route) },
                    onNavigateToStory    = { index ->
                        navController.navigate(Screen.StoryViewer.createRoute(index))
                    },
                    onNavigateToCreateStory = {
                        navController.navigate(Screen.CreateStory.route)
                    },
                    onDmReplyToPost = { post ->
                        // Open a DM with the post author, pre-filling caption as reply context
                        chatViewModel.createDirectChat(post.authorId) { roomId ->
                            if (roomId != null) {
                                navController.navigate(
                                    Screen.Conversation.createRoute(
                                        roomId       = roomId,
                                        replyContext = post.caption.ifBlank { "📸 Post" }
                                    )
                                )
                            }
                        }
                    },
                    onMenuClick          = openDrawer
                )
            }
        }

        // Story Viewer
        composable(Screen.StoryViewer.route) { backStackEntry ->
            val indexStr = backStackEntry.arguments?.getString("index") ?: "0"
            val index = indexStr.toIntOrNull() ?: 0
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Feed.route)
            }
            val feedViewModel: com.vula.app.global.ui.feed.FeedViewModel = hiltViewModel(parentEntry)
            val stories by feedViewModel.stories.collectAsState()

            StoryViewerScreen(
                stories = stories,
                initialIndex = index,
                currentUserId = currentUserId ?: "",
                onDismiss = { navController.popBackStack() },
                onReplyToStory = { authorUserId, message ->
                    // Create or find DM room, send the message, navigate
                    chatViewModel.createDirectChat(authorUserId) { roomId ->
                        if (roomId != null) {
                            chatViewModel.sendMessage(roomId, message)
                            navController.navigate(
                                Screen.Conversation.createRoute(
                                    roomId = roomId,
                                    replyContext = null
                                )
                            )
                        }
                    }
                }
            )
        }

        // Local Mode
        composable(Screen.Local.route) {
            LocalModeScreen(
                onMenuClick = openDrawer
            )
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

        // Create Story
        composable(Screen.CreateStory.route) {
            com.vula.app.global.ui.story.CreateStoryScreen(
                onStoryCreated = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Chat list
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
                onMenuClick  = openDrawer,
                viewModel    = chatViewModel
            )
        }

        // Contacts List
        composable(Screen.Contacts.route) {
            ContactsScreen(
                onBackClick    = { navController.popBackStack() },
                onContactClick = { userId, richStatus, contactName ->
                    chatViewModel.createDirectChat(userId) { roomId ->
                        if (roomId != null) {
                            navController.navigate(
                                Screen.Conversation.createRoute(
                                    roomId      = roomId,
                                    replyContext = richStatus,
                                    contactName  = contactName
                                )
                            )
                        }
                    }
                }
            )
        }

        // Conversation
        composable(
            route = Screen.Conversation.routeWithArgs,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("replyContext") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("contactName") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val roomId      = backStackEntry.arguments?.getString("roomId") ?: ""
            val rawCtx      = backStackEntry.arguments?.getString("replyContext")
            val rawName     = backStackEntry.arguments?.getString("contactName")
            val replyCtx    = rawCtx?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            val contactName = rawName?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            ConversationScreen(
                chatRoomId              = roomId,
                onBackClick             = { navController.popBackStack() },
                replyContext            = replyCtx,
                contactName             = contactName,
                sharedTransitionScope   = this@SharedTransitionLayout,
                animatedVisibilityScope = this@composable,
                viewModel               = chatViewModel
            )
        }

        // Own Profile
        composable(Screen.Profile.route) {
            ProfileScreen(
                userId                = null,
                onLogoutClick         = {}, // Removed from here
                onEditProfileClick    = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToConversation = { roomId ->
                    navController.navigate(Screen.Conversation.createRoute(roomId))
                },
                onMenuClick           = openDrawer
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            com.vula.app.global.ui.settings.SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Edit Profile
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Another user's profile
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
    } // end SharedTransitionLayout
}
