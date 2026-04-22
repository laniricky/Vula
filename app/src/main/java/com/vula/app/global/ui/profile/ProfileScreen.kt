package com.vula.app.global.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vula.app.core.ui.components.FullScreenLoading
import com.vula.app.core.ui.components.UserAvatar
import com.vula.app.core.ui.components.VulaTopBar

@Composable
fun ProfileScreen(
    userId: String? = null,
    onLogoutClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> FullScreenLoading()
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is ProfileUiState.Success -> {
                VulaTopBar(
                    title = state.user.username,
                    actions = {
                        if (state.isOwnProfile) {
                            IconButton(onClick = { 
                                viewModel.logout()
                                onLogoutClick()
                            }) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    UserAvatar(
                        imageUrl = state.user.profileImageUrl,
                        username = state.user.username,
                        size = 80.dp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.weight(1f)
                    ) {
                        ProfileStat(state.posts.size.toString(), "Posts")
                        ProfileStat(state.user.followersCount.toString(), "Followers")
                        ProfileStat(state.user.followingCount.toString(), "Following")
                    }
                }

                Text(
                    text = state.user.displayName,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (state.user.bio.isNotEmpty()) {
                    Text(
                        text = state.user.bio,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    if (state.isOwnProfile) {
                        OutlinedButton(
                            onClick = { /* TODO Edit Profile */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit Profile")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.toggleFollow(state.user.id, state.isFollowing) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (state.isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (state.isFollowing) "Following" else "Follow")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { /* TODO Message */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Message")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.posts) { post ->
                        if (post.imageUrl != null) {
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = "Post thumbnail",
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = post.caption.take(20),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
