package ng.commu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ng.commu.data.model.*
import ng.commu.data.repository.BoardRepository
import javax.inject.Inject

sealed class BoardsUiState {
    data object Loading : BoardsUiState()
    data class Success(val boards: List<Board>) : BoardsUiState()
    data class Error(val message: String) : BoardsUiState()
}

sealed class PostsUiState {
    data object Loading : PostsUiState()
    data class Success(
        val posts: List<Post>,
        val nextCursor: String?,
        val hasMore: Boolean
    ) : PostsUiState()
    data class Error(val message: String) : PostsUiState()
}

sealed class PostDetailUiState {
    data object Loading : PostDetailUiState()
    data class Success(val post: Post) : PostDetailUiState()
    data class Error(val message: String) : PostDetailUiState()
}

sealed class HashtagsUiState {
    data object Loading : HashtagsUiState()
    data class Success(val hashtags: List<String>) : HashtagsUiState()
    data class Error(val message: String) : HashtagsUiState()
}

sealed class RepliesUiState {
    data object Loading : RepliesUiState()
    data class Success(
        val replies: List<BoardPostReply>,
        val nextCursor: String?,
        val hasMore: Boolean
    ) : RepliesUiState()
    data class Error(val message: String) : RepliesUiState()
}

@HiltViewModel
class BoardsViewModel @Inject constructor(
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val _boardsState = MutableStateFlow<BoardsUiState>(BoardsUiState.Loading)
    val boardsState: StateFlow<BoardsUiState> = _boardsState.asStateFlow()

    private val _postsState = MutableStateFlow<PostsUiState>(PostsUiState.Loading)
    val postsState: StateFlow<PostsUiState> = _postsState.asStateFlow()

    private val _postDetailState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val postDetailState: StateFlow<PostDetailUiState> = _postDetailState.asStateFlow()

    private val _selectedBoard = MutableStateFlow<Board?>(null)
    val selectedBoard: StateFlow<Board?> = _selectedBoard.asStateFlow()

    private val _selectedHashtags = MutableStateFlow<List<String>>(emptyList())
    val selectedHashtags: StateFlow<List<String>> = _selectedHashtags.asStateFlow()

    private val _hashtagsState = MutableStateFlow<HashtagsUiState>(HashtagsUiState.Loading)
    val hashtagsState: StateFlow<HashtagsUiState> = _hashtagsState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLoadingBoardContent = MutableStateFlow(false)
    val isLoadingBoardContent: StateFlow<Boolean> = _isLoadingBoardContent.asStateFlow()

    private val _repliesState = MutableStateFlow<RepliesUiState>(RepliesUiState.Loading)
    val repliesState: StateFlow<RepliesUiState> = _repliesState.asStateFlow()

    private val _isLoadingMoreReplies = MutableStateFlow(false)
    val isLoadingMoreReplies: StateFlow<Boolean> = _isLoadingMoreReplies.asStateFlow()

    private val _isCreatingReply = MutableStateFlow(false)
    val isCreatingReply: StateFlow<Boolean> = _isCreatingReply.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun loadBoards() {
        viewModelScope.launch {
            _boardsState.value = BoardsUiState.Loading
            boardRepository.getBoards()
                .onSuccess { boards ->
                    _boardsState.value = BoardsUiState.Success(boards)
                }
                .onFailure { error ->
                    _boardsState.value = BoardsUiState.Error(
                        error.message ?: "Failed to load boards"
                    )
                }
        }
    }

    fun loadBoard(boardSlug: String) {
        viewModelScope.launch {
            _isLoadingBoardContent.value = true
            _boardsState.value = BoardsUiState.Loading
            boardRepository.getBoard(boardSlug)
                .onSuccess { board ->
                    _boardsState.value = BoardsUiState.Success(listOf(board))
                    _selectedBoard.value = board
                    _selectedHashtags.value = emptyList()
                    // Use the boardSlug parameter instead of board.slug to avoid null issues
                    // Wait for both hashtags and posts to complete
                    val hashtagsJob = launch { loadBoardHashtags(boardSlug) }
                    val postsJob = launch { loadPosts(boardSlug, refresh = true) }
                    hashtagsJob.join()
                    postsJob.join()
                    _isLoadingBoardContent.value = false
                }
                .onFailure { error ->
                    _boardsState.value = BoardsUiState.Error(
                        error.message ?: "Failed to load board"
                    )
                    _isLoadingBoardContent.value = false
                }
        }
    }

    fun selectBoard(board: Board) {
        _selectedBoard.value = board
        _selectedHashtags.value = emptyList()
        board.slug?.let { slug ->
            loadBoardHashtags(slug)
            loadPosts(slug, refresh = true)
        }
    }

    fun loadBoardHashtags(boardSlug: String, showLoading: Boolean = true) {
        viewModelScope.launch {
            // Only show loading state if requested (not during refresh)
            if (showLoading) {
                _hashtagsState.value = HashtagsUiState.Loading
            }
            boardRepository.getBoardHashtags(boardSlug)
                .onSuccess { hashtags ->
                    _hashtagsState.value = HashtagsUiState.Success(hashtags)
                }
                .onFailure { error ->
                    _hashtagsState.value = HashtagsUiState.Error(
                        error.message ?: "Failed to load hashtags"
                    )
                }
        }
    }

    private suspend fun loadBoardHashtagsSuspend(boardSlug: String) {
        boardRepository.getBoardHashtags(boardSlug)
            .onSuccess { hashtags ->
                _hashtagsState.value = HashtagsUiState.Success(hashtags)
            }
            .onFailure { error ->
                _hashtagsState.value = HashtagsUiState.Error(
                    error.message ?: "Failed to load hashtags"
                )
            }
    }

    private suspend fun loadPostsSuspend(boardSlug: String) {
        val hashtags = _selectedHashtags.value
        boardRepository.getPosts(boardSlug, hashtags = hashtags.ifEmpty { null })
            .onSuccess { response ->
                _postsState.value = PostsUiState.Success(
                    posts = response.data,
                    nextCursor = response.pagination.nextCursor,
                    hasMore = response.pagination.hasMore
                )
            }
            .onFailure { error ->
                _postsState.value = PostsUiState.Error(
                    error.message ?: "Failed to load posts"
                )
            }
    }

    fun loadPosts(boardSlug: String, refresh: Boolean = false, cursor: String? = null) {
        viewModelScope.launch {
            if (!refresh) {
                _isLoadingMore.value = true
            } else {
                // Only show full loading state if there are no posts yet
                val currentState = _postsState.value
                if (currentState !is PostsUiState.Success || currentState.posts.isEmpty()) {
                    _postsState.value = PostsUiState.Loading
                }
            }

            val hashtags = if (_selectedHashtags.value.isEmpty()) null else _selectedHashtags.value

            boardRepository.getPosts(boardSlug, hashtags, cursor)
                .onSuccess { response ->
                    val currentPosts = if (refresh) {
                        emptyList()
                    } else {
                        (_postsState.value as? PostsUiState.Success)?.posts ?: emptyList()
                    }

                    _postsState.value = PostsUiState.Success(
                        posts = currentPosts + response.posts,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null
                    )
                    _isLoadingMore.value = false
                }
                .onFailure { error ->
                    if (refresh) {
                        _postsState.value = PostsUiState.Error(
                            error.message ?: "Failed to load posts"
                        )
                    }
                    _isLoadingMore.value = false
                }
        }
    }

    fun loadMorePosts() {
        val currentState = _postsState.value as? PostsUiState.Success ?: return
        val board = _selectedBoard.value ?: return
        val slug = board.slug ?: return

        if (currentState.hasMore && !_isLoadingMore.value) {
            loadPosts(slug, refresh = false, cursor = currentState.nextCursor)
        }
    }

    fun setHashtags(hashtags: List<String>) {
        _selectedHashtags.value = hashtags
        val board = _selectedBoard.value ?: return
        val slug = board.slug ?: return
        loadPosts(slug, refresh = true)
    }

    fun refreshPosts() {
        val board = _selectedBoard.value ?: return
        val slug = board.slug ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            loadBoardHashtagsSuspend(slug)
            loadPostsSuspend(slug)
            _isRefreshing.value = false
        }
    }

    fun loadPostDetail(boardSlug: String, postId: String) {
        viewModelScope.launch {
            _postDetailState.value = PostDetailUiState.Loading

            boardRepository.getPost(boardSlug, postId)
                .onSuccess { post ->
                    _postDetailState.value = PostDetailUiState.Success(post = post)
                }
                .onFailure { error ->
                    _postDetailState.value = PostDetailUiState.Error(
                        error.message ?: "Failed to load post"
                    )
                }
        }
    }

    fun loadReplies(boardSlug: String, postId: String, refresh: Boolean = false, cursor: String? = null) {
        viewModelScope.launch {
            if (!refresh) {
                _isLoadingMoreReplies.value = true
            } else if (_repliesState.value !is RepliesUiState.Success) {
                _repliesState.value = RepliesUiState.Loading
            }

            boardRepository.getReplies(boardSlug, postId, cursor)
                .onSuccess { response ->
                    val currentReplies = if (refresh) {
                        emptyList()
                    } else {
                        (_repliesState.value as? RepliesUiState.Success)?.replies ?: emptyList()
                    }

                    _repliesState.value = RepliesUiState.Success(
                        replies = currentReplies + response.replies,
                        nextCursor = response.nextCursor,
                        hasMore = response.hasMore
                    )
                    _isLoadingMoreReplies.value = false
                }
                .onFailure { error ->
                    if (refresh && _repliesState.value !is RepliesUiState.Success) {
                        _repliesState.value = RepliesUiState.Error(
                            error.message ?: "Failed to load replies"
                        )
                    }
                    _isLoadingMoreReplies.value = false
                }
        }
    }

    fun loadMoreReplies(boardSlug: String, postId: String) {
        val currentState = _repliesState.value as? RepliesUiState.Success ?: return

        if (currentState.hasMore && !_isLoadingMoreReplies.value) {
            loadReplies(boardSlug, postId, refresh = false, cursor = currentState.nextCursor)
        }
    }

    fun createReply(
        boardSlug: String,
        postId: String,
        content: String,
        inReplyToId: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isCreatingReply.value = true
            boardRepository.createReply(boardSlug, postId, content, inReplyToId)
                .onSuccess {
                    // Refresh replies to show the new reply
                    loadReplies(boardSlug, postId, refresh = true)
                    _isCreatingReply.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    _isCreatingReply.value = false
                    _repliesState.value = RepliesUiState.Error(
                        error.message ?: "Failed to create reply"
                    )
                }
        }
    }

    fun updateReply(
        boardSlug: String,
        postId: String,
        replyId: String,
        content: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            boardRepository.updateReply(boardSlug, postId, replyId, content)
                .onSuccess {
                    // Refresh replies to show the updated reply
                    loadReplies(boardSlug, postId, refresh = true)
                    onSuccess()
                }
                .onFailure { error ->
                    _repliesState.value = RepliesUiState.Error(
                        error.message ?: "Failed to update reply"
                    )
                }
        }
    }

    fun deleteReply(
        boardSlug: String,
        postId: String,
        replyId: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            boardRepository.deleteReply(boardSlug, postId, replyId)
                .onSuccess {
                    // Refresh replies to remove the deleted reply
                    loadReplies(boardSlug, postId, refresh = true)
                    onSuccess()
                }
                .onFailure { error ->
                    _repliesState.value = RepliesUiState.Error(
                        error.message ?: "Failed to delete reply"
                    )
                }
        }
    }

    fun deletePost(
        boardSlug: String,
        postId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            boardRepository.deletePost(boardSlug, postId)
                .onSuccess {
                    // Refresh the posts list to remove the deleted post
                    loadPosts(boardSlug, refresh = true)
                    onSuccess()
                }
                .onFailure { error ->
                    onError(error.message ?: "Failed to delete post")
                }
        }
    }

    suspend fun uploadImage(file: okhttp3.MultipartBody.Part): Result<ImageUploadResponse> {
        return boardRepository.uploadImage(file)
    }

    fun createPost(
        boardSlug: String,
        title: String,
        content: String,
        imageId: String? = null,
        hashtags: List<String>? = null,
        onSuccess: (Post) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            boardRepository.createPost(boardSlug, title, content, imageId, hashtags)
                .onSuccess { post ->
                    // Refresh posts to show the new post
                    loadPosts(boardSlug, refresh = true)
                    onSuccess(post)
                }
                .onFailure { error ->
                    onError(error.message ?: "Failed to create post")
                }
        }
    }

    fun reportPost(
        boardSlug: String,
        postId: String,
        reason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            boardRepository.reportPost(boardSlug, postId, reason)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    onError(error.message ?: "Failed to report post")
                }
        }
    }
}
