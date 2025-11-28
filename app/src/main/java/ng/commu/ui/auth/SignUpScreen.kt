package ng.commu.ui.auth

import android.Manifest
import android.os.Build
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import ng.commu.R
import ng.commu.viewmodel.AuthState
import ng.commu.viewmodel.AuthViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    authState: AuthState,
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    var loginName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var policyAgreed by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val passwordMinLengthError = stringResource(R.string.auth_password_min_length)
    val passwordsNotMatchError = stringResource(R.string.auth_passwords_not_match)
    val policyAgreementRequiredError = stringResource(R.string.auth_policy_agreement_required)

    // Request notification permission on Android 13+
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    // Navigate to profile when authentication is successful
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            // Request notification permission immediately after successful signup
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (notificationPermissionState?.status?.isGranted == false) {
                    notificationPermissionState.launchPermissionRequest()
                }
            }
            onSignUpSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.auth_sign_up),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = loginName,
            onValueChange = { newValue ->
                // Only allow alphanumeric characters and underscores
                if (newValue.all { it.isLetterOrDigit() || it == '_' }) {
                    loginName = newValue
                }
            },
            label = { Text(stringResource(R.string.auth_login_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.auth_password_confirm)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = policyAgreed,
                onCheckedChange = { policyAgreed = it },
                modifier = Modifier.padding(0.dp)
            )
            val annotatedString = buildAnnotatedString {
                append(stringResource(R.string.auth_policy_agreement_prefix))
                pushStringAnnotation(tag = "privacy", annotation = "https://commu.ng/privacy")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append(stringResource(R.string.auth_privacy_policy))
                }
                pop()
                append(" ${stringResource(R.string.auth_and)} ")
                pushStringAnnotation(tag = "guidelines", annotation = "https://commu.ng/policy")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append(stringResource(R.string.auth_community_guidelines))
                }
                pop()
                append(stringResource(R.string.auth_policy_agreement_suffix))
            }
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        annotatedString.getStringAnnotations(tag = "privacy", start = 0, end = annotatedString.length)
                            .firstOrNull()?.let { annotation ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                            }
                        annotatedString.getStringAnnotations(tag = "guidelines", start = 0, end = annotatedString.length)
                            .firstOrNull()?.let { annotation ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item)))
                            }
                    }
            )
        }

        if (validationError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = validationError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = authState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                validationError = null

                if (!policyAgreed) {
                    validationError = policyAgreementRequiredError
                    return@Button
                }

                if (password.length < 8) {
                    validationError = passwordMinLengthError
                    return@Button
                }

                if (password != confirmPassword) {
                    validationError = passwordsNotMatchError
                    return@Button
                }

                authViewModel.signup(loginName, password)
            },
            enabled = authState !is AuthState.Loading && loginName.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && policyAgreed,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.auth_sign_up))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onNavigateToLogin,
            enabled = authState !is AuthState.Loading
        ) {
            Row {
                Text(
                    text = stringResource(R.string.auth_already_have_account),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.auth_sign_in),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
