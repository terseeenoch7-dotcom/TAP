package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TapBlack
import com.example.ui.theme.TapDarkCharcoal
import com.example.ui.theme.TapGoldDark
import com.example.ui.theme.TapGoldLight
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapSurfaceElevated
import com.example.ui.theme.TapSurfaceVariant
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextPrimary
import com.example.ui.theme.TapTextSecondary

@Composable
fun AuthScreen(
    authState: AuthUiState,
    onGoogleSignInClick: () -> Unit,
    onEmailSignInClick: (String, String) -> Unit,
    onEmailSignUpClick: (String, String, String) -> Unit,
    onClearError: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Google, 1: Email/Password
    var isSignUpMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val isLoading = authState is AuthUiState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TapBlack,
                        Color(0xFF0F1523),
                        TapBlack
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Glowing Coin Emblem
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(elevation = 24.dp, shape = CircleShape, spotColor = TapGoldPrimary)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(listOf(TapGoldLight, TapGoldDark)),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(TapDarkCharcoal),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tap_coin),
                    contentDescription = "TAP Gold Coin Emblem",
                    modifier = Modifier.size(106.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TAP",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = TapGoldPrimary
            )

            Text(
                text = "ONLINE REWARDS & COIN TAPPING PLATFORM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TapTextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Method Selector Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TapSurfaceVariant,
                contentColor = TapGoldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TapGoldPrimary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, TapOutline, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onClearError()
                    },
                    text = {
                        Text(
                            text = "Google Sign-In",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) TapGoldPrimary else TapTextMuted,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onClearError()
                    },
                    text = {
                        Text(
                            text = "Email & Password",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) TapGoldPrimary else TapTextMuted,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error Message
            if (authState is AuthUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B151A)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78252E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = authState.message,
                        color = Color(0xFFFFB4AB),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            if (selectedTab == 0) {
                // GOOGLE SIGN-IN TAB
                Card(
                    colors = CardDefaults.cardColors(containerColor = TapSurfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FeatureRow(
                            icon = Icons.Default.MonetizationOn,
                            title = "Real-time Tapping Engine",
                            subtitle = "Earn virtual coins with tactile feedback & multipliers"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureRow(
                            icon = Icons.Default.Bolt,
                            title = "Energy & Power Boosters",
                            subtitle = "Upgrade Multitap, Capacity, & Turbo 2x speed"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureRow(
                            icon = Icons.Default.Shield,
                            title = "Firebase Realtime Sync",
                            subtitle = "Persistent verified balances & real withdrawal requests"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGoogleSignInClick,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapGoldPrimary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color(0xFF4285F4)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            } else {
                // EMAIL & PASSWORD TAB
                Card(
                    colors = CardDefaults.cardColors(containerColor = TapSurfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TapOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSignUpMode) "Create TAP Account" else "Welcome Back",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TapGoldPrimary
                        )

                        Text(
                            text = if (isSignUpMode) "Register with your email to start tapping" else "Sign in to access your coin balance",
                            fontSize = 12.sp,
                            color = TapTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TapGoldPrimary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TapGoldPrimary,
                                    unfocusedBorderColor = TapOutline,
                                    focusedLabelColor = TapGoldPrimary,
                                    unfocusedLabelColor = TapTextMuted,
                                    focusedTextColor = TapTextPrimary,
                                    unfocusedTextColor = TapTextPrimary
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = TapGoldPrimary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TapGoldPrimary,
                                unfocusedBorderColor = TapOutline,
                                focusedLabelColor = TapGoldPrimary,
                                unfocusedLabelColor = TapTextMuted,
                                focusedTextColor = TapTextPrimary,
                                unfocusedTextColor = TapTextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password (min 6 characters)") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TapGoldPrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                        tint = TapTextMuted
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TapGoldPrimary,
                                unfocusedBorderColor = TapOutline,
                                focusedLabelColor = TapGoldPrimary,
                                unfocusedLabelColor = TapTextMuted,
                                focusedTextColor = TapTextPrimary,
                                unfocusedTextColor = TapTextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isSignUpMode) {
                                        onEmailSignUpClick(email, password, username)
                                    } else {
                                        onEmailSignInClick(email, password)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (isSignUpMode) {
                                    onEmailSignUpClick(email, password, username)
                                } else {
                                    onEmailSignInClick(email, password)
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TapGoldPrimary,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("email_auth_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isSignUpMode) "Create Account" else "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle between Sign In and Sign Up
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.clickable {
                                isSignUpMode = !isSignUpMode
                                onClearError()
                            }
                        ) {
                            Text(
                                text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                                fontSize = 12.sp,
                                color = TapTextMuted
                            )
                            Text(
                                text = if (isSignUpMode) "Sign In" else "Create one",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TapGoldPrimary,
                                modifier = Modifier.testTag("toggle_auth_mode_button")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Protected by Firebase Realtime Database & Auth",
                fontSize = 11.sp,
                color = TapTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF26200A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TapGoldPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TapTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TapTextSecondary
            )
        }
    }
}
