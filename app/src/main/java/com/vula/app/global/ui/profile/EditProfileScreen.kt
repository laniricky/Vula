package com.vula.app.global.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving = uiState is EditProfileUiState.Saving

    val avatarPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let { viewModel.onPhotoSelected(it) }
    }
    val bannerPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let { viewModel.onBannerSelected(it) }
    }

    LaunchedEffect(uiState) {
        if (uiState is EditProfileUiState.SaveSuccess) onBackClick()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.weight(1f))
            // Save pill
            Surface(
                shape  = CircleShape,
                color  = if (isSaving) MaterialTheme.colorScheme.surfaceVariant
                         else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable(enabled = !isSaving) { viewModel.saveProfile() }
            ) {
                Box(
                    modifier         = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            "Save",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 14.sp,
                            color      = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Live profile preview ──────────────────────────────────────────
            ProfilePreviewHero(
                displayName  = viewModel.displayName,
                username     = viewModel.username,
                avatarModel  = viewModel.pendingPhotoUri ?: viewModel.currentImageUrl,
                bannerModel  = viewModel.pendingBannerUri ?: viewModel.currentBannerUrl,
                onBannerClick = {
                    bannerPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                },
                onAvatarClick = {
                    avatarPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }
            )

            Spacer(Modifier.height(28.dp))

            // ── Form ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {

                ProfileField(
                    label         = "NAME",
                    value         = viewModel.displayName,
                    onValueChange = { viewModel.displayName = it },
                    placeholder   = "Your name",
                    singleLine    = true
                )

                UsernameField(
                    value         = viewModel.username,
                    onValueChange = {
                        viewModel.username = it.lowercase()
                            .filter { c -> c.isLetterOrDigit() || c == '_' }
                    }
                )

                BioField(
                    value         = viewModel.bio,
                    onValueChange = { if (it.length <= 150) viewModel.bio = it }
                )

                ProfileField(
                    label         = "STATUS",
                    value         = viewModel.richStatus,
                    onValueChange = { if (it.length <= 60) viewModel.richStatus = it },
                    placeholder   = "What are you up to?",
                    singleLine    = true
                )

                ProfileField(
                    label         = "WEBSITE",
                    value         = viewModel.website,
                    onValueChange = { viewModel.website = it },
                    placeholder   = "yoursite.com",
                    singleLine    = true
                )

                PrivacyRow(
                    isPrivate = viewModel.isPrivate,
                    onToggle  = { viewModel.isPrivate = it }
                )

                if (uiState is EditProfileUiState.Error) {
                    Text(
                        text  = (uiState as EditProfileUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

// ─── Live Preview Hero ────────────────────────────────────────────────────────

@Composable
private fun ProfilePreviewHero(
    displayName  : String,
    username     : String,
    avatarModel  : Any?,
    bannerModel  : Any?,
    onBannerClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val bannerH  = 180.dp
    val avatarSz = 92.dp
    val overlap  = 46.dp      // avatar half-height overlap into banner

    // Outer box: banner height + avatar overflow
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerH + overlap)
    ) {
        // Banner
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .height(bannerH)
                .align(Alignment.TopCenter)
                .clickable(onClick = onBannerClick),
            contentAlignment = Alignment.Center
        ) {
            if (bannerModel != null) {
                AsyncImage(
                    model              = bannerModel,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                )
                Text(
                    "Tap to add a cover photo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Bottom gradient fade into background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
        }

        // Avatar — centered, straddles banner bottom edge
        Box(
            modifier         = Modifier
                .align(Alignment.BottomCenter)
                .size(avatarSz)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onAvatarClick),
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
                    text  = displayName.take(1).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Name + handle live preview below hero
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text(
            text       = displayName.ifEmpty { "Your Name" },
            fontWeight = FontWeight.Bold,
            fontSize   = 20.sp,
            color      = if (displayName.isEmpty())
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.onSurface
        )
        if (username.isNotEmpty()) {
            Text(
                text  = "@$username",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Plain underline field ────────────────────────────────────────────────────

@Composable
private fun ProfileField(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String = "",
    singleLine   : Boolean = true,
    minLines     : Int     = 1
) {
    var focused by remember { mutableStateOf(false) }
    val lineColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "line_$label"
    )
    val labelColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "lbl_$label"
    )

    Column {
        Text(
            label,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color         = labelColor
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            singleLine    = singleLine,
            minLines      = minLines,
            textStyle     = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
            modifier      = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Column {
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                            )
                        }
                        inner()
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(lineColor)
                    )
                }
            }
        )
    }
}

// ─── @handle field with availability dot ─────────────────────────────────────

@Composable
private fun UsernameField(
    value        : String,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val lineColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "line_handle"
    )
    val labelColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "lbl_handle"
    )

    Column {
        Text(
            "HANDLE",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color         = labelColor
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "@",
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                color      = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(2.dp))
            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                singleLine    = true,
                textStyle     = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                modifier      = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                "username",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                            )
                        }
                        inner()
                    }
                }
            )
            // Availability dot — no text, just a colored dot
            AnimatedVisibility(
                visible = value.length >= 3,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(lineColor))
    }
}

// ─── Bio field with focus-only ring counter ───────────────────────────────────

@Composable
private fun BioField(
    value        : String,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val lineColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "line_bio"
    )
    val labelColor by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "lbl_bio"
    )

    Column {
        Text(
            "BIO",
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color         = labelColor
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                singleLine    = false,
                minLines      = 1,
                maxLines      = 4,
                textStyle     = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                modifier      = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                "Something about you…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                            )
                        }
                        inner()
                    }
                }
            )
            // Ring counter — only visible when this field is focused
            AnimatedVisibility(
                visible = focused,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                val fraction = value.length / 150f
                CircularProgressIndicator(
                    progress  = { fraction },
                    modifier  = Modifier.padding(start = 10.dp, bottom = 2.dp).size(22.dp),
                    color     = if (fraction > 0.9f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                    trackColor  = MaterialTheme.colorScheme.outlineVariant,
                    strokeWidth = 2.dp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(lineColor))
    }
}

// ─── Privacy switch row ───────────────────────────────────────────────────────

@Composable
private fun PrivacyRow(isPrivate: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).paddingFromBaseline(bottom = 0.dp)) {
            Text(
                "PRIVATE ACCOUNT",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color         = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isPrivate) "Only approved followers see your posts"
                else "Anyone can follow and see your posts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked        = isPrivate,
            onCheckedChange = onToggle,
            modifier       = Modifier.padding(start = 16.dp),
            colors         = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
