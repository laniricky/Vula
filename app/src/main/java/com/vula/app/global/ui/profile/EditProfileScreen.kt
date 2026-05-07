package com.vula.app.global.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.onPhotoSelected(it) } }

    val bannerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.onBannerSelected(it) } }

    LaunchedEffect(uiState) {
        if (uiState is EditProfileUiState.SaveSuccess) onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = uiState !is EditProfileUiState.Saving
                    ) {
                        if (uiState is EditProfileUiState.Saving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                "Save",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Banner + Avatar Hero ──────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {

                // Banner photo (16:7 aspect ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            bannerPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bannerModel: Any? = viewModel.pendingBannerUri ?: viewModel.currentBannerUrl
                    if (bannerModel != null) {
                        AsyncImage(
                            model            = bannerModel,
                            contentDescription = "Banner",
                            modifier         = Modifier.fillMaxSize(),
                            contentScale     = ContentScale.Crop
                        )
                        // Dim overlay so camera icon is legible
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                    } else {
                        // Default gradient placeholder
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    )
                                )
                            )
                        )
                    }
                    // Centre camera badge
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change banner",
                            tint     = Color.White,
                            modifier = Modifier.padding(10.dp).size(22.dp)
                        )
                    }
                }

                // Avatar: overlaps the banner bottom edge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp)
                        .offset(y = 42.dp)
                ) {
                    val avatarModel: Any? = viewModel.pendingPhotoUri ?: viewModel.currentImageUrl
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .border(3.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarModel != null) {
                            AsyncImage(
                                model              = avatarModel,
                                contentDescription = "Avatar",
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text  = viewModel.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Camera badge on avatar
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).size(28.dp),
                        shape    = CircleShape,
                        color    = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onPrimary,
                            modifier           = Modifier.padding(5.dp)
                        )
                    }
                }
            }

            // Space for avatar overflow
            Spacer(modifier = Modifier.height(54.dp))

            // ── Section cards ─────────────────────────────────────────────────
            Column(
                modifier  = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Identity card ─────────────────────────────────────────────
                ProfileCard(title = "Identity", icon = Icons.Default.Person) {

                    ProfileTextField(
                        value         = viewModel.displayName,
                        onValueChange = { viewModel.displayName = it },
                        label         = "Display Name",
                        singleLine    = true
                    )

                    ProfileDivider()

                    // @username row with live hint
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "@",
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary,
                            fontSize   = 16.sp
                        )
                        BasicTextField(
                            value         = viewModel.username,
                            onValueChange = {
                                viewModel.username = it.lowercase()
                                    .filter { c -> c.isLetterOrDigit() || c == '_' }
                            },
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier      = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (viewModel.username.isEmpty()) {
                                        Text(
                                            "username",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        if (viewModel.username.length >= 3) {
                            Text(
                                "✓ available",
                                fontSize   = 11.sp,
                                color      = Color(0xFF4CAF50),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    ProfileDivider()

                    ProfileTextField(
                        value         = viewModel.bio,
                        onValueChange = { if (it.length <= 150) viewModel.bio = it },
                        label         = "Bio",
                        singleLine    = false,
                        minLines      = 2,
                        supportingText = "${viewModel.bio.length} / 150"
                    )
                }

                // ── Presence card ─────────────────────────────────────────────
                ProfileCard(title = "Presence", icon = Icons.Default.Mood) {

                    // Rich Status
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape  = CircleShape,
                            color  = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val statusEmoji = viewModel.richStatus
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { s ->
                                        val cp = s.codePointAt(0)
                                        if (cp > 127) s.take(2) else null
                                    } ?: "🟢"
                                Text(statusEmoji, fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value         = viewModel.richStatus,
                            onValueChange = { if (it.length <= 60) viewModel.richStatus = it },
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier      = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (viewModel.richStatus.isEmpty()) {
                                        Text(
                                            "Set a status…",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }

                    ProfileDivider()

                    // Website link
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value         = viewModel.website,
                            onValueChange = { viewModel.website = it },
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier      = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (viewModel.website.isEmpty()) {
                                        Text(
                                            "Website link",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }

                // ── Account / Privacy card ────────────────────────────────────
                ProfileCard(title = "Account", icon = Icons.Default.Security) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Account Type",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = !viewModel.isPrivate,
                                onClick  = { viewModel.isPrivate = false },
                                label    = { Text("Public") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Public, null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape  = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor  = MaterialTheme.colorScheme.onBackground,
                                    selectedLabelColor      = MaterialTheme.colorScheme.background,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.background
                                )
                            )
                            FilterChip(
                                selected = viewModel.isPrivate,
                                onClick  = { viewModel.isPrivate = true },
                                label    = { Text("Private") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock, null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape  = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor  = MaterialTheme.colorScheme.onBackground,
                                    selectedLabelColor      = MaterialTheme.colorScheme.background,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.background
                                )
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = if (!viewModel.isPrivate)
                                "Anyone can follow you and see your posts"
                            else
                                "Only approved followers can see your posts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Error message
                if (uiState is EditProfileUiState.Error) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text     = (uiState as EditProfileUiState.Error).message,
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ─── Section card shell ───────────────────────────────────────────────────────

@Composable
private fun ProfileCard(
    title   : String,
    icon    : ImageVector,
    content : @Composable ColumnScope.() -> Unit
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card header
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            content()
        }
    }
}

// ─── Underline-only labelled text field ──────────────────────────────────────

@Composable
private fun ProfileTextField(
    value         : String,
    onValueChange : (String) -> Unit,
    label         : String,
    singleLine    : Boolean = true,
    minLines      : Int     = 1,
    supportingText: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            singleLine    = singleLine,
            minLines      = minLines,
            textStyle     = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
            modifier      = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            label,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    inner()
                }
            }
        )
        supportingText?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Subtle section divider ───────────────────────────────────────────────────

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
}
