package ng.commu.ui.boards

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ng.commu.R
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import io.noties.markwon.Markwon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ng.commu.data.model.ImageUploadResponse
import ng.commu.viewmodel.BoardsViewModel
import ng.commu.utils.Constants
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    boardSlug: String,
    viewModel: BoardsViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var communityType by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImage by remember { mutableStateOf<ImageUploadResponse?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Edit, 1 = Preview
    var showHelpDialog by remember { mutableStateOf(false) }
    var expandedCommunityType by remember { mutableStateOf(false) }

    val isPromoBoard = boardSlug == "promo"

    // Helper function to parse hashtags - handles comma/space separation and # prefix
    fun parseHashtags(input: String): List<String> {
        return input
            .replace(",", " ")
            .split(" ")
            .map { it.trim().trimStart('#') }
            .filter { it.isNotEmpty() }
    }

    // Auto-sync community type with hashtags (only for promo board)
    LaunchedEffect(communityType) {
        if (!isPromoBoard) return@LaunchedEffect

        val communityTypeLabels = Constants.COMMUNITY_TYPE_LABELS.values.toSet()
        val currentHashtags = parseHashtags(hashtags)

        // Remove any existing community type hashtags
        val filtered = currentHashtags.filter { it !in communityTypeLabels }

        // Add the selected community type if one is selected
        val updated = if (communityType != null) {
            filtered + communityType!!
        } else {
            filtered
        }

        hashtags = updated.joinToString(", ")
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val markwon = remember { Markwon.create(context) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Upload the image
            scope.launch {
                isUploading = true
                errorMessage = null

                try {
                    // Create a temporary file from URI
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
                    val outputStream = FileOutputStream(tempFile)

                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Create multipart body
                    val requestBody = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    val filePart = MultipartBody.Part.createFormData(
                        "file",
                        tempFile.name,
                        requestBody
                    )

                    // Upload
                    val result = withContext(Dispatchers.IO) {
                        viewModel.uploadImage(filePart)
                    }

                    result.onSuccess { response ->
                        uploadedImage = response
                    }.onFailure { error ->
                        errorMessage = error.message ?: "Failed to upload image"
                        selectedImageUri = null
                    }

                    // Clean up temp file
                    tempFile.delete()
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to process image"
                    selectedImageUri = null
                }

                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_post_title_kr)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            isCreating = true
                            errorMessage = null

                            val hashtagList = parseHashtags(hashtags)
                                .takeIf { it.isNotEmpty() }

                            viewModel.createPost(
                                boardSlug = boardSlug,
                                title = title,
                                content = content,
                                imageId = uploadedImage?.id,
                                hashtags = hashtagList,
                                onSuccess = { _ ->
                                    isCreating = false
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    isCreating = false
                                    errorMessage = error
                                }
                            )
                        },
                        enabled = title.isNotBlank() && content.isNotBlank() && !isCreating && !isUploading
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.create_post_post_kr))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            errorMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.create_post_title_label_kr)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating && !isUploading
            )

            // Community Type dropdown (only for promo board)
            if (isPromoBoard) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedCommunityType,
                        onExpandedChange = { expandedCommunityType = !expandedCommunityType }
                    ) {
                        OutlinedTextField(
                            value = communityType ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.community_type_label)) },
                            placeholder = { Text(stringResource(R.string.community_type_placeholder)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCommunityType) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            enabled = !isCreating && !isUploading
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCommunityType,
                            onDismissRequest = { expandedCommunityType = false }
                        ) {
                            // Option to clear selection
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.community_type_placeholder)) },
                                onClick = {
                                    communityType = null
                                    expandedCommunityType = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                            // Community type options
                            Constants.COMMUNITY_TYPE_LABELS.values.forEach { label ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        communityType = label
                                        expandedCommunityType = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.community_type_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Content section with integrated tabs
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tabs with help button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.weight(1f)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.editor_tab_edit)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.editor_tab_preview)) }
                        )
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.editor_markdown_help))
                    }
                }

                // Content area - integrated with tabs
                if (selectedTab == 0) {
                    // Edit mode
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text(stringResource(R.string.create_post_content_placeholder_kr)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        enabled = !isCreating && !isUploading,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                } else {
                    // Preview mode
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        if (content.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.editor_preview_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            AndroidView(
                                factory = { context ->
                                    TextView(context).apply {
                                        setPadding(16, 16, 16, 16)
                                    }
                                },
                                update = { textView ->
                                    markwon.setMarkdown(textView, content)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = hashtags,
                onValueChange = { hashtags = it },
                label = { Text(stringResource(R.string.create_post_hashtags_label_kr)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.create_post_hashtags_placeholder_kr)) },
                enabled = !isCreating && !isUploading
            )

            // Image upload section
            if (selectedImageUri != null && uploadedImage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "선택한 이미지",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            contentScale = ContentScale.Fit
                        )

                        IconButton(
                            onClick = {
                                selectedImageUri = null
                                uploadedImage = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "이미지 제거",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else if (isUploading) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating && !isUploading
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_post_add_image_kr))
                }
            }
        }
    }

    // Show help dialog
    if (showHelpDialog) {
        MarkdownHelpDialog(onDismiss = { showHelpDialog = false })
    }
}
