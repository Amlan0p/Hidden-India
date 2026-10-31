package `in`.hiddenindia.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.hiddenindia.app.data.AiRepository
import `in`.hiddenindia.app.data.ChatHistoryRepository
import `in`.hiddenindia.app.data.ChatMessage
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.data.DestinationRepository
import `in`.hiddenindia.app.data.GeoRepository
import `in`.hiddenindia.app.data.GeneratedItinerary
import `in`.hiddenindia.app.data.LocalConversation
import `in`.hiddenindia.app.data.MessageSender
import `in`.hiddenindia.app.data.SupportedLanguage
import `in`.hiddenindia.app.data.toAiChatHistory
import `in`.hiddenindia.app.data.toGeneratedItinerary
import `in`.hiddenindia.app.data.OfflineImageCache
import `in`.hiddenindia.app.data.OfflineTileCache
import `in`.hiddenindia.app.network.ApiResult
import `in`.hiddenindia.app.network.NearestEmergencyResponse
import `in`.hiddenindia.app.network.TranslateResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DestinationRepository(application)
    private val aiRepository = AiRepository(application)
    private val chatHistoryRepository = ChatHistoryRepository(application)

    private val _allDestinations = MutableStateFlow<List<Destination>>(emptyList())
    val allDestinations: StateFlow<List<Destination>> = _allDestinations

    private val _allLanguages = MutableStateFlow<List<SupportedLanguage>>(emptyList())
    val allLanguages: StateFlow<List<SupportedLanguage>> = _allLanguages

    val searchQuery = MutableStateFlow("")
    val selectedState = MutableStateFlow("All")
    val selectedCategory = MutableStateFlow("All")
    val selectedRegion = MutableStateFlow("All")

    private val _downloadedPackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedPackIds: StateFlow<Set<String>> = _downloadedPackIds

    // Tracks destinations whose offline bundle (images + map tiles) are actively being fetched.
    private val _downloadingPackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingPackIds: StateFlow<Set<String>> = _downloadingPackIds

    // Backward compatibility for existing references
    val tilesDownloadingIds: StateFlow<Set<String>> = _downloadingPackIds

    // Port of web OfflineContext's isOfflineMode: a simulated "strict offline" toggle used
    // to demo/test what the app looks like with zero network (see OfflineVaultScreen).
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode
    fun setOfflineMode(enabled: Boolean) { _isOfflineMode.value = enabled }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    /**
     * Synchronizes and verifies all downloaded offline packs, ensuring images and map
     * tiles for every saved location are downloaded and up to date.
     */
    fun syncAllPacks() {
        viewModelScope.launch {
            if (_isOfflineMode.value) {
                // When Strict Offline Mode is active in the Offline Tab, internet network calls are blocked.
                // Perform local storage check without making any network calls.
                _isSyncing.value = true
                kotlinx.coroutines.delay(400)
                _downloadedPackIds.value = repository.getDownloadedPackIds()
                _isSyncing.value = false
                return@launch
            }
            _isSyncing.value = true
            try {
                val packIds = repository.getDownloadedPackIds()
                val context = getApplication<Application>().applicationContext
                val dests = _allDestinations.value.filter { packIds.contains(it.id) }
                for (dest in dests) {
                    _downloadingPackIds.value = _downloadingPackIds.value + dest.id
                    try {
                        val imgJob = async {
                            OfflineImageCache.downloadImagesForDestination(context, dest)
                        }
                        val tileJob = async {
                            val bbox = dest.osmBoundingBox
                            if (bbox != null && bbox.size >= 4) {
                                val tiles = OfflineTileCache.computeTileList(bbox)
                                OfflineTileCache.downloadTiles(context, tiles)
                            } else 0 to 0
                        }
                        imgJob.await()
                        tileJob.await()
                    } finally {
                        _downloadingPackIds.value = _downloadingPackIds.value - dest.id
                    }
                }
            } finally {
                _downloadedPackIds.value = repository.getDownloadedPackIds()
                _isSyncing.value = false
            }
        }
    }

    fun getDownloadTimestamp(destinationId: String): Long = repository.getDownloadTimestamp(destinationId)

    // NOTE: favorites and saved itineraries are backend-synced account data as of
    // Stage 4 and live in AuthViewModel (favoriteIds / savedItineraries there),
    // not here. This ViewModel keeps local-device state (destinations,
    // languages, search/filter state, offline pack downloads) plus the real
    // backend AI features added in Stage 5 (chat, itinerary generation,
    // translation) via AiRepository — no local/fake AI remains.

    val selectedDestination = MutableStateFlow<Destination?>(null)
    val currentItinerary = MutableStateFlow<GeneratedItinerary?>(null)

    private val _isGeneratingTrip = MutableStateFlow(false)
    val isGeneratingTrip: StateFlow<Boolean> = _isGeneratingTrip

    private val _itineraryError = MutableStateFlow<String?>(null)
    val itineraryError: StateFlow<String?> = _itineraryError

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AI,
                text = WELCOME_MESSAGE_TEXT
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError

    private val _chatHistory = MutableStateFlow<List<LocalConversation>>(emptyList())
    val chatHistory: StateFlow<List<LocalConversation>> = _chatHistory

    private val _isChatHistoryLoading = MutableStateFlow(false)
    val isChatHistoryLoading: StateFlow<Boolean> = _isChatHistoryLoading

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating

    private val _translationResult = MutableStateFlow<TranslateResponse?>(null)
    val translationResult: StateFlow<TranslateResponse?> = _translationResult

    private val _translationError = MutableStateFlow<String?>(null)
    val translationError: StateFlow<String?> = _translationError

    val filteredDestinations: StateFlow<List<Destination>> = combine(
        _allDestinations,
        searchQuery,
        selectedState,
        selectedCategory,
        selectedRegion
    ) { destinations, query, state, category, region ->
        val q = query.trim().lowercase()
        val filtered = destinations.filter { dest ->
            val matchesQuery = q.isEmpty() ||
                dest.name.lowercase().contains(q) ||
                dest.state.lowercase().contains(q) ||
                dest.region.lowercase().contains(q) ||
                dest.category.lowercase().contains(q) ||
                dest.places.any { it.name.lowercase().contains(q) } ||
                dest.description.lowercase().contains(q)
            val matchesState = state == "All" || dest.state.equals(state, ignoreCase = true)
            val matchesCategory = category == "All" ||
                dest.category.equals(category, ignoreCase = true) ||
                dest.secondaryCategories.any { it.equals(category, ignoreCase = true) }
            val matchesRegion = region == "All" || dest.region.equals(region, ignoreCase = true)
            matchesQuery && matchesState && matchesCategory && matchesRegion
        }
        if (q.isEmpty()) {
            filtered
        } else {
            filtered.sortedWith(
                compareBy(
                    // 1. Destination name starts with query (e.g. typing 'a' puts 'Abhaneri' first)
                    { dest -> if (dest.name.lowercase().startsWith(q)) 0 else 1 },
                    // 2. Any word in destination name starts with query
                    { dest -> if (dest.name.lowercase().split(" ").any { it.startsWith(q) }) 0 else 1 },
                    // 3. State starts with query
                    { dest -> if (dest.state.lowercase().startsWith(q)) 0 else 1 },
                    // 4. Any attraction starts with query
                    { dest -> if (dest.places.any { it.name.lowercase().startsWith(q) }) 0 else 1 },
                    // 5. Name contains query
                    { dest -> if (dest.name.lowercase().contains(q)) 0 else 1 },
                    // 6. Alphabetical
                    { dest -> dest.name }
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInitialData()
        loadChatHistory()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            val destinations = repository.loadDestinations()
            _allDestinations.value = destinations
            _allLanguages.value = repository.loadLanguages()
            _downloadedPackIds.value = repository.getDownloadedPackIds()
            _isLoading.value = false

            // If an active conversation was restored, reconcile its selected destination
            val activeId = _currentConversationId.value
            if (activeId != null && selectedDestination.value == null) {
                val activeConv = _chatHistory.value.find { it.id == activeId }
                if (activeConv?.destinationId != null) {
                    selectedDestination.value = destinations.find { it.id == activeConv.destinationId }
                }
            }
        }
    }

    fun toggleDownloadPack(destId: String) {
        val updated = repository.toggleDownloadPack(destId)
        val current = _downloadedPackIds.value.toMutableSet()
        if (updated) current.add(destId) else current.remove(destId)
        _downloadedPackIds.value = current

        // Download or remove both the destination imagery (cover photo, places, food, experiences)
        // and OSM map tiles covering this destination.
        val destination = _allDestinations.value.find { it.id == destId } ?: return
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch {
            if (updated) {
                _downloadingPackIds.value = _downloadingPackIds.value + destId
                try {
                    val imgJob = async {
                        OfflineImageCache.downloadImagesForDestination(context, destination)
                    }
                    val tileJob = async {
                        val bbox = destination.osmBoundingBox
                        if (bbox != null && bbox.size >= 4) {
                            val tiles = OfflineTileCache.computeTileList(bbox)
                            OfflineTileCache.downloadTiles(context, tiles)
                        } else 0 to 0
                    }
                    imgJob.await()
                    tileJob.await()
                } finally {
                    _downloadingPackIds.value = _downloadingPackIds.value - destId
                }
            } else {
                // Delete cached map tiles
                destination.osmBoundingBox?.let { bbox ->
                    if (bbox.size >= 4) {
                        val tiles = OfflineTileCache.computeTileList(bbox)
                        OfflineTileCache.deleteCachedTiles(context, tiles)
                    }
                }
                // Delete cached images that are not referenced by other remaining downloaded destinations
                val remaining = _allDestinations.value.filter {
                    _downloadedPackIds.value.contains(it.id) && it.id != destId
                }
                OfflineImageCache.deleteCachedImagesForDestination(context, destination, remaining)
            }
        }
    }

    fun selectDestinationById(id: String) {
        val found = _allDestinations.value.find { it.id.equals(id, ignoreCase = true) }
        selectedDestination.value = found
    }

    /**
     * Calls the real backend AI trip planner (POST /api/ai/plan-trip). No local
     * fallback pretending to be AI: on failure, currentItinerary is left
     * untouched and itineraryError carries a user-facing message instead.
     */
    fun generateItinerary(destination: Destination, days: Int, budget: String, interests: List<String>) {
        viewModelScope.launch {
            _isGeneratingTrip.value = true
            _itineraryError.value = null
            currentItinerary.value = null // clear any previous plan while a new one is generated

            when (val result = aiRepository.planTrip(
                destinationId = destination.id,
                days = days,
                budgetStyle = budget,
                interests = interests
            )) {
                is ApiResult.Success -> {
                    currentItinerary.value = result.data.toGeneratedItinerary().copy(requestedDays = days)
                    // Not persisted here: saving a generated itinerary to the user's
                    // account is an explicit action (see ItineraryResultScreen's Save
                    // button / AuthViewModel.saveItineraryToAccount), not automatic.
                }
                is ApiResult.Failure -> {
                    _itineraryError.value = result.error.message
                }
            }
            _isGeneratingTrip.value = false
        }
    }

    /**
     * Calls the real backend AI guide (POST /api/ai/ask-place) for the currently
     * selected destination (or pan-India, if none is selected). No keyword
     * matching, no local fallback, no artificial delay — a failure surfaces
     * through chatError rather than a fake answer, and the user's own message
     * stays in the transcript so they can retry.
     */
    companion object {
        const val WELCOME_MESSAGE_TEXT =
            "Namaste! I am your AI Travel Companion for Hidden India. Ask me anything about folklore, secret trails, authentic food, or respectful travel etiquette across India's lesser-known regions."
    }

    private fun createWelcomeMessage() = ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = MessageSender.AI,
        text = WELCOME_MESSAGE_TEXT
    )

    fun loadChatHistory() {
        _isChatHistoryLoading.value = true
        val conversations = chatHistoryRepository.getConversations()
        _chatHistory.value = conversations
        val activeId = chatHistoryRepository.getActiveConversationId()
        if (activeId != null) {
            val activeConv = conversations.find { it.id == activeId }
            if (activeConv != null && activeConv.messages.isNotEmpty()) {
                _currentConversationId.value = activeConv.id
                _chatMessages.value = activeConv.messages
                if (activeConv.destinationId != null) {
                    selectedDestination.value = _allDestinations.value.find { it.id == activeConv.destinationId }
                }
            }
        }
        _isChatHistoryLoading.value = false
    }

    private fun persistCurrentConversation(messages: List<ChatMessage>) {
        val convId = _currentConversationId.value ?: UUID.randomUUID().toString().also {
            _currentConversationId.value = it
        }
        chatHistoryRepository.setActiveConversationId(convId)

        val existing = chatHistoryRepository.getConversation(convId)
        val firstUserMsg = messages.firstOrNull { it.sender == MessageSender.USER }
        val title = existing?.title ?: firstUserMsg?.text?.let { ChatHistoryRepository.generateTitle(it) } ?: "Trip Discussion"

        val currentDest = selectedDestination.value
        val destinationId = existing?.destinationId ?: currentDest?.id
        val destinationName = existing?.destinationName ?: (currentDest?.let { "${it.name}, ${it.state}" } ?: "All India")

        val conversation = LocalConversation(
            id = convId,
            title = title,
            destinationId = destinationId,
            destinationName = destinationName,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messages = messages
        )
        chatHistoryRepository.saveConversation(conversation)
        _chatHistory.value = chatHistoryRepository.getConversations()
    }

    private fun saveCurrentConversationIfEligible() {
        val msgs = _chatMessages.value
        val hasUserMsg = msgs.any { it.sender == MessageSender.USER }
        val hasAiResponse = msgs.any { it.sender == MessageSender.AI && it.text.isNotBlank() && it.text != WELCOME_MESSAGE_TEXT }
        if (hasUserMsg && hasAiResponse) {
            persistCurrentConversation(msgs)
        }
    }

    fun startNewChat() {
        saveCurrentConversationIfEligible()
        _currentConversationId.value = null
        chatHistoryRepository.setActiveConversationId(null)
        _chatMessages.value = listOf(createWelcomeMessage())
        _chatError.value = null
    }

    fun selectConversation(conversationId: String) {
        saveCurrentConversationIfEligible()
        val conv = chatHistoryRepository.getConversation(conversationId) ?: return
        _currentConversationId.value = conv.id
        _chatMessages.value = conv.messages
        _chatError.value = null
        if (conv.destinationId != null) {
            selectedDestination.value = _allDestinations.value.find { it.id == conv.destinationId }
        } else {
            selectedDestination.value = null
        }
        chatHistoryRepository.setActiveConversationId(conv.id)
    }

    fun deleteConversation(conversationId: String) {
        chatHistoryRepository.deleteConversation(conversationId)
        _chatHistory.value = chatHistoryRepository.getConversations()
        if (_currentConversationId.value == conversationId) {
            _currentConversationId.value = null
            _chatMessages.value = listOf(createWelcomeMessage())
            _chatError.value = null
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank() || _isChatLoading.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = text.trim()
        )
        val historyBeforeThisMessage = _chatMessages.value
        _chatMessages.value = historyBeforeThisMessage + userMsg
        _chatError.value = null
        _isChatLoading.value = true

        viewModelScope.launch {
            when (val result = aiRepository.askPlace(
                destinationId = selectedDestination.value?.id,
                question = text.trim(),
                chatHistory = historyBeforeThisMessage.toAiChatHistory()
            )) {
                is ApiResult.Success -> {
                    val aiMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text = result.data.answer
                    )
                    val updated = _chatMessages.value + aiMsg
                    _chatMessages.value = updated
                    persistCurrentConversation(updated)
                }
                is ApiResult.Failure -> {
                    _chatError.value = result.error.message
                }
            }
            _isChatLoading.value = false
        }
    }

    /**
     * Calls the real backend AI translator (POST /api/ai/translate). No
     * hardcoded phrasebook fallback, no arbitrary delay.
     */
    fun translate(text: String, targetLang: String, sourceLang: String = "English") {
        if (text.isBlank() || _isTranslating.value) return
        _isTranslating.value = true
        _translationError.value = null
        _translationResult.value = null

        viewModelScope.launch {
            when (val result = aiRepository.translate(text.trim(), sourceLang, targetLang)) {
                is ApiResult.Success -> {
                    _translationResult.value = result.data
                }
                is ApiResult.Failure -> {
                    _translationError.value = result.error.message
                }
            }
            _isTranslating.value = false
        }
    }

    fun clearTranslation() {
        _translationResult.value = null
        _translationError.value = null
    }

    private val geoRepository = GeoRepository(application)
    private val _nearestEmergency = MutableStateFlow<NearestEmergencyResponse?>(null)
    val nearestEmergency: StateFlow<NearestEmergencyResponse?> = _nearestEmergency
    private val _isEmergencyLoading = MutableStateFlow(false)
    val isEmergencyLoading: StateFlow<Boolean> = _isEmergencyLoading
    private val _emergencyError = MutableStateFlow<String?>(null)
    val emergencyError: StateFlow<String?> = _emergencyError

    /**
     * Looks up the nearest verified hospital and police station via GeoRepository
     * (GET /api/geo/nearest-emergency) with OpenStreetMap coordinates.
     */
    suspend fun getNearestEmergencyFacilities(lat: Double, lng: Double): ApiResult<NearestEmergencyResponse> {
        _isEmergencyLoading.value = true
        _emergencyError.value = null
        val result = geoRepository.getNearestEmergencyFacilities(lat, lng)
        when (result) {
            is ApiResult.Success -> {
                _nearestEmergency.value = result.data
                _emergencyError.value = null
            }
            is ApiResult.Failure -> {
                _emergencyError.value = result.error.message
            }
        }
        _isEmergencyLoading.value = false
        return result
    }
}

