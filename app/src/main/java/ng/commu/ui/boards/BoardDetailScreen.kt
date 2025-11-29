package ng.commu.ui.boards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ng.commu.R
import ng.commu.data.model.Board
import ng.commu.data.model.Post
import ng.commu.utils.formatRelativeTime
import ng.commu.viewmodel.BoardsViewModel
import ng.commu.viewmodel.HashtagsUiState
import ng.commu.viewmodel.PostsUiState
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BoardDetailScreen(
    board: Board,
    boardSlug: String,
    isAuthenticated: Boolean,
    onNavigateBack: (() -> Unit)? = null,
    onPostClick: (Post) -> Unit,
    onCreatePost: ((String) -> Unit)? = null,
    viewModel: BoardsViewModel = hiltViewModel()
) {
	val postsState by viewModel.postsState.collectAsState()
	val selectedBoard by viewModel.selectedBoard.collectAsState()
	val listState = rememberLazyListState()

	// Only select board if it's not already selected or is different
	LaunchedEffect(board.id) {
		if (selectedBoard?.id != board.id) {
			viewModel.selectBoard(board)
		}
	}

    // Detect when user scrolls to bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMorePosts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(board.name ?: "") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAuthenticated && onCreatePost != null) {
                FloatingActionButton(
                    onClick = { onCreatePost(boardSlug) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "새 게시물 작성"
                    )
                }
            }
        }
    ) { padding ->
        when (val state = postsState) {
            is PostsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is PostsUiState.Success -> {
                val selectedHashtags by viewModel.selectedHashtags.collectAsState()
                val hashtagsState by viewModel.hashtagsState.collectAsState()
                val isLoadingContent by viewModel.isLoadingBoardContent.collectAsState()
                val isRefreshing by viewModel.isRefreshing.collectAsState()
                var isFirstLoad by remember { mutableStateOf(true) }

                // Scroll to top when hashtags change (skip first load)
                LaunchedEffect(selectedHashtags) {
                    if (!isFirstLoad && listState.firstVisibleItemIndex > 0) {
                        listState.scrollToItem(0)
                    }
                    isFirstLoad = false
                }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshPosts() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Popular hashtags section
                    when (val hashtags = hashtagsState) {
                        is HashtagsUiState.Success -> {
                            if (hashtags.hashtags.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.hashtag_popular),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        FlowRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            hashtags.hashtags.forEach { hashtag ->
                                                val isSelected = selectedHashtags.contains(hashtag)
                                                Surface(
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = "#$hashtag",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .clickable {
                                                                val newHashtags = selectedHashtags.toMutableList()
                                                                if (isSelected) {
                                                                    newHashtags.remove(hashtag)
                                                                } else {
                                                                    newHashtags.add(hashtag)
                                                                }
                                                                viewModel.setHashtags(newHashtags)
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {}
                    }

                    // Selected hashtags filter section
                    if (selectedHashtags.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.hashtag_filters),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    TextButton(onClick = { viewModel.setHashtags(emptyList()) }) {
                                        Text(stringResource(R.string.action_clear_all))
                                    }
                                }

                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    selectedHashtags.forEach { hashtag ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .clickable {
                                                        val newHashtags = selectedHashtags.toMutableList()
                                                        newHashtags.remove(hashtag)
                                                        viewModel.setHashtags(newHashtags)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "#$hashtag",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove filter",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Posts list
                    if (state.posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingContent) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(
                                        text = stringResource(R.string.empty_no_posts),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.posts) { post ->
                            PostItem(
                                post = post,
                                selectedHashtags = selectedHashtags,
                                onPostClick = { onPostClick(post) },
                                onHashtagClick = { hashtag ->
                                    val newHashtags = selectedHashtags.toMutableList()
                                    if (newHashtags.contains(hashtag)) {
                                        newHashtags.remove(hashtag)
                                    } else {
                                        newHashtags.add(hashtag)
                                    }
                                    viewModel.setHashtags(newHashtags)
                                }
                            )
                        }

                        if (state.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
            }
            is PostsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.selectBoard(board) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostItem(
    post: Post,
    selectedHashtags: List<String>,
    onPostClick: () -> Unit,
    onHashtagClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPostClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(24.dp)
            ) {
                if (post.author.avatarUrl != null) {
                    AsyncImage(
                        model = post.author.avatarUrl,
                        contentDescription = "User avatar",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User avatar",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = post.author.loginName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatRelativeTime(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (post.image != null) {
                AsyncImage(
                    model = post.image.url,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                post.hashtags.forEach { hashtag ->
                    val isSelected = selectedHashtags.contains(hashtag.tag)
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.2f else 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "#${hashtag.tag}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onHashtagClick(hashtag.tag) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
