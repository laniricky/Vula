package com.vula.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import com.vula.app.global.ui.ripples.RipplesScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.vula.app.core.ui.OnboardingScreen
import com.vula.app.global.ui.discover.DiscoverScreen
import com.vula.app.global.ui.feed.FeedScreen
import com.vula.app.global.ui.post.CommentScreen
import com.vula.app.global.ui.post.CreatePostScreen
import com.vula.app.global.ui.profile.EditProfileScreen
import com.vula.app.global.ui.profile.ProfileScreen
// SearchScreen replaced by DiscoverScreen
import com.vula.app.global.ui.story.StoryViewerScreen
import com.vula.app.local.ui.LocalModeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// ─── Route Definitions ────────────────────────────────────────────────────────

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector?,
    val outlinedIcon: ImageVector? = null
) {
    object Onboarding  : Screen("onboarding",            "Welcome",  null)
    object Login       : Screen("login",                 "Login",    null)
    object Register    : Screen("register",              "Register", null)
    object Feed        : Screen("feed",         "Home",     Icons.Filled.Home,          Icons.Outlined.Home)
    object Local       : Screen("local",        "Local",   Icons.Filled.Wifi,          null)
    object Ripples     : Screen("ripples",      "Ripples", null)
    object CreatePost  : Screen("create_post",  "Camera",  Icons.Filled.CameraAlt,     null)
    object CreateStory : Screen("create_story", "Story",   null)
    object Chat        : Screen("chat",         "Activity",Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Profile     : Screen("profile",      "Profile", Icons.Filled.Person,        Icons.Outlined.Person)
    object Settings    : Screen("settings",     "Settings",null)
    object EditProfile : Screen("edit_profile", "Edit",    null)
    object StoryViewer : Screen("story/{index}","Story",   null) {
        fun createRoute(index: Int) = "story/$index"
    }
    object Search      : Screen("search",       "Discover",Icons.Filled.Explore,       Icons.Outlined.Explore)
    object Comments    : Screen("comments/{postId}", "Comments", null) {
        fun createRoute(postId: String) = "comments/$postId"
    }
    object UserProfile : Screen("user/{userId}", "Profile", null) {
        fun createRoute(userId: String) = "user/$userId"
    }
    object Conversation: Screen("conversation/{roomId}", "Chat", null) {
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
    object Contacts    : Screen("contacts", "Contacts", null)
}

val bottomNavScreens = listOf(
    Screen.Feed, Screen.Search, Screen.CreatePost, Screen.Chat, Screen.Profile
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

    // ── True floating pill: content fills the entire screen; pill overlays on top ──
    Box(modifier = Modifier.fillMaxSize()) {

        VulaNavGraph(
            navController    = navController,
            currentUserId    = currentUser?.id,
            chatViewModel    = chatViewModel,
            startDestination = startDest,
            modifier         = Modifier.fillMaxSize()   // edge-to-edge — no scaffold padding
        )

        // Pill slides up from the bottom when entering a nav-bar screen
        AnimatedVisibility(
            visible  = showBottomBar,
            enter    = slideInVertically { it } + fadeIn(),
            exit     = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()    // respect gesture-nav bar on Android 10+
        ) {
            VulaBottomBar(
                navController = navController,
                currentRoute  = currentRoute,
                chatUnread    = unreadCount
            )
        }
    }
}

// ─── Bottom Bar — pill shape, 5 icons, dot indicator ──────────────────────────

@Composable
fun VulaBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    chatUnread: Int
) {
    // Sleek Midnight-Cyan gradient for the camera button
    val cameraGradient = Brush.linearGradient(
        listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
    )

    Box(
        modifier         = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier        = Modifier
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .height(60.dp),
            shape           = CircleShape,
            shadowElevation = 28.dp,
            tonalElevation  = 0.dp,
            color           = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border          = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    val scale by animateFloatAsState(
                        targetValue   = if (isSelected) 1.18f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium
                        ),
                        label = "tab_scale_${screen.route}"
                    )

                    Box(
                        modifier         = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                indication       = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Feed.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (screen) {
                            Screen.CreatePost -> {
                                // ╌╌ Gradient camera button ╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
                                val cameraGradient = Brush.linearGradient(
                                    listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                                )
                                Box(
                                    modifier         = Modifier
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(cameraGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector        = Icons.Filled.CameraAlt,
                                        contentDescription = "Camera",
                                        tint               = Color.White,
                                        modifier           = Modifier.size(22.dp)
                                    )
                                }
                            }
                            else -> {
                                // ╌╌ Regular tab with filled/outline swap + badge ╌╌╌╌╌╌╌╌╌╌
                                val displayIcon = if (isSelected)
                                    screen.icon else (screen.outlinedIcon ?: screen.icon)
                                val iconTint = if (isSelected)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

                                BadgedBox(
                                    badge = {
                                        if (screen == Screen.Chat && chatUnread > 0) {
                                            Badge {
                                                Text(
                                                    text     = if (chatUnread > 9) "9+" else chatUnread.toString(),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    displayIcon?.let {
                                        Icon(
                                            imageVector        = it,
                                            contentDescription = screen.title,
                                            tint               = iconTint,
                                            modifier           = Modifier
                                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                                .size(24.dp)
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
}

// ─── Nav Graph ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VulaNavGraph(
    navController: NavHostController,
    currentUserId: String?,
    chatViewModel: ChatViewModel,
    startDestination: String,
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
                    onNavigateToRipples  = { navController.navigate(Screen.Ripples.route) },
                    onDmReplyToPost = { post ->
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
                    }
                )
            }
        }

        // Story Viewer
        composable(Screen.StoryViewer.route) { backStackEntry ->
            val indexStr = backStackEntry.arguments?.getString("index") ?: "0"
            val index    = indexStr.toIntOrNull() ?: 0
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Feed.route)
            }
            val feedViewModel: com.vula.app.global.ui.feed.FeedViewModel = hiltViewModel(parentEntry)
            val stories by feedViewModel.stories.collectAsState()

            StoryViewerScreen(
                stories       = stories,
                initialIndex  = index,
                currentUserId = currentUserId ?: "",
                onDismiss     = { navController.popBackStack() },
                onReplyToStory = { authorUserId, message ->
                    chatViewModel.createDirectChat(authorUserId) { roomId ->
                        if (roomId != null) {
                            chatViewModel.sendMessage(roomId, message)
                            navController.navigate(
                                Screen.Conversation.createRoute(roomId = roomId)
                            )
                        }
                    }
                }
            )
        }

        // Local Mode
        composable(Screen.Local.route) {
            LocalModeScreen(onMenuClick = null)
        }

        // Ripples
        composable(Screen.Ripples.route) {
            currentUserId?.let { uid ->
                RipplesScreen(
                    currentUserId        = uid,
                    onNavigateToProfile  = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                    onNavigateToComments = { postId -> navController.navigate(Screen.Comments.createRoute(postId)) },
                    onNavigateToCreatePost = { navController.navigate(Screen.CreatePost.route) }
                )
            }
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
                onBackClick    = { navController.popBackStack() }
            )
        }

        // Chat list  (mapped to Heart / Favorites tab)
        composable(Screen.Chat.route) {
            ChatListScreen(
                onChatClick          = { roomId ->
                    navController.navigate(Screen.Conversation.createRoute(roomId))
                },
                onUserClick          = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onNavigateToContacts = { navController.navigate(Screen.Contacts.route) },
                onMenuClick          = null,
                viewModel            = chatViewModel
            )
        }

        // Contacts
        composable(Screen.Contacts.route) {
            ContactsScreen(
                onBackClick    = { navController.popBackStack() },
                onContactClick = { userId, richStatus, contactName ->
                    chatViewModel.createDirectChat(userId) { roomId ->
                        if (roomId != null) {
                            navController.navigate(
                                Screen.Conversation.createRoute(
                                    roomId       = roomId,
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
            route     = Screen.Conversation.routeWithArgs,
            arguments = listOf(
                navArgument("roomId")       { type = NavType.StringType },
                navArgument("replyContext") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("contactName")  { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val roomId      = backStackEntry.arguments?.getString("roomId") ?: ""
            val rawCtx      = backStackEntry.arguments?.getString("replyContext")
            val rawName     = backStackEntry.arguments?.getString("contactName")
            val replyCtx    = rawCtx?.let  { java.net.URLDecoder.decode(it, "UTF-8") }
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
            val authVM: AuthViewModel = hiltViewModel()
            ProfileScreen(
                userId                   = null,
                onEditProfileClick       = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToConversation = { roomId ->
                    navController.navigate(Screen.Conversation.createRoute(roomId))
                },
                onMenuClick = null,
                onLogoutClick = {
                    authVM.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            val authVM: AuthViewModel = hiltViewModel()
            com.vula.app.global.ui.settings.SettingsScreen(
                onBackClick   = { navController.popBackStack() },
                onLogoutClick = {
                    authVM.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Edit Profile
        composable(Screen.EditProfile.route) {
            EditProfileScreen(onBackClick = { navController.popBackStack() })
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

        // Discover (replaces Search)
        composable(Screen.Search.route) {
            currentUserId?.let { uid ->
                DiscoverScreen(
                    currentUserId = uid,
                    onUserClick   = { userId ->
                        navController.navigate(Screen.UserProfile.createRoute(userId))
                    },
                    onPostClick   = { postId ->
                        navController.navigate(Screen.Comments.createRoute(postId))
                    },
                    onCreatePost  = {
                        navController.navigate(Screen.CreatePost.route)
                    }
                )
            }
        }
    }
    } // end SharedTransitionLayout
}
