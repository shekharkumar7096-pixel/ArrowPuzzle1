package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameSoundManager
import com.example.data.GameRepository
import com.example.data.HintSpendOutcome
import com.example.data.MilestoneDefinition
import com.example.data.ads.RewardedAdController
import com.example.data.ads.RewardedOfferType
import com.example.data.local.ArrowPuzzleDatabase
import com.example.data.local.DailyCompletionEntity
import com.example.data.local.LevelProgressEntity
import com.example.data.local.PlayerPreferences
import com.example.data.local.PreferencesManager
import com.example.game.ArrowTile
import com.example.game.GameMoveSnapshot
import com.example.game.LevelCompletionSummary
import com.example.game.LevelGenerator
import com.example.game.LevelSpec
import com.example.game.MoveResult
import com.example.game.PuzzleEngine
import com.example.ui.theme.GameThemePalette
import com.example.ui.theme.GameThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedFeedback(
    val arrowId: Int,
    val blockerId: Int,
    val message: String,
    val nonce: Long = System.nanoTime()
)

data class ExitingArrow(
    val tile: ArrowTile,
    val key: Long = System.nanoTime()
)

data class GameplayUiState(
    val level: LevelSpec = LevelGenerator.generateLevel(1),
    val arrows: List<ArrowTile> = level.arrows,
    val exitingArrows: List<ExitingArrow> = emptyList(),
    val blockedArrowIds: Set<Int> = emptySet(),
    val movesCount: Int = 0,
    val mistakesCount: Int = 0,
    val heartsRemaining: Int = 3,
    val undoStack: List<GameMoveSnapshot> = emptyList(),
    val hintedArrowId: Int? = null,
    val blockedFeedback: BlockedFeedback? = null,
    val showPathGuide: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleting: Boolean = false,
    val showInsufficientCoinsDialog: Boolean = false,
    val comboCount: Int = 0
) {
    val remainingCount: Int get() = arrows.size
    val canUndo: Boolean get() = undoStack.isNotEmpty() && !isCompleting
    val isOutOfHearts: Boolean get() = heartsRemaining <= 0 && !isCompleting
    val currentProjectedStars: Int get() = PuzzleEngine.calculateStars(mistakesCount)
}

data class AppUiState(
    val preferences: PlayerPreferences = PlayerPreferences(),
    val levelProgress: Map<Int, LevelProgressEntity> = emptyMap(),
    val dailyCompletions: Map<String, DailyCompletionEntity> = emptyMap(),
    val activeTheme: GameThemePalette = GameThemes.ClassicBlue,
    val dailyStreak: Int = 0,
    val totalStars: Int = 0,
    val completedLevelsCount: Int = 0,
    val isDailyRewardAvailableToday: Boolean = true,
    val isTodayDailyChallengeDone: Boolean = false,
    val milestones: List<MilestoneDefinition> = emptyList()
)

sealed interface RewardedAdUiState {
    data object Idle : RewardedAdUiState
    data class Loading(val offer: RewardedOfferType) : RewardedAdUiState
    data class Error(val offer: RewardedOfferType, val message: String) : RewardedAdUiState
}

sealed interface GameNavEvent {
    data class OpenLevelComplete(val summary: LevelCompletionSummary) : GameNavEvent
    data class ShowToast(val message: String) : GameNavEvent
}

class ArrowPuzzleViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ArrowPuzzleDatabase.getInstance(application)
    private val prefsManager = PreferencesManager(application)
    val repository = GameRepository(database.gameDao(), prefsManager)
    val soundManager = GameSoundManager(application)
    val rewardedAdController = RewardedAdController(repository.adManager)

    val appState: StateFlow<AppUiState> = combine(
        repository.preferences,
        repository.levelProgressMap,
        repository.dailyCompletionsMap
    ) { prefs, progressMap, dailyMap ->
        soundManager.soundEnabled = prefs.soundEnabled
        soundManager.musicEnabled = prefs.musicEnabled
        soundManager.vibrationEnabled = prefs.vibrationEnabled

        val today = GameRepository.todayDateKey()
        val completedCount = progressMap.values.count { it.isCompleted }
        val totalStars = progressMap.values.sumOf { it.stars }
        AppUiState(
            preferences = prefs,
            levelProgress = progressMap,
            dailyCompletions = dailyMap,
            activeTheme = GameThemes.byId(prefs.selectedThemeId),
            dailyStreak = GameRepository.calculateDailyStreak(dailyMap),
            totalStars = totalStars,
            completedLevelsCount = completedCount,
            isDailyRewardAvailableToday = prefs.lastDailyRewardDate != today,
            isTodayDailyChallengeDone = dailyMap.containsKey(today),
            milestones = GameRepository.buildMilestones(
                completedLevelsCount = completedCount,
                totalStars = totalStars,
                dailyCompletedCount = dailyMap.size,
                claimedIds = prefs.claimedMilestoneIds
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppUiState()
    )

    private val _gameplayState = MutableStateFlow(GameplayUiState())
    val gameplayState: StateFlow<GameplayUiState> = _gameplayState.asStateFlow()

    private val _lastCompletion = MutableStateFlow<LevelCompletionSummary?>(null)
    val lastCompletion: StateFlow<LevelCompletionSummary?> = _lastCompletion.asStateFlow()

    private val _activeRewardOffer = MutableStateFlow<RewardedOfferType?>(null)
    val activeRewardOffer: StateFlow<RewardedOfferType?> = _activeRewardOffer.asStateFlow()

    private val _rewardedAdState = MutableStateFlow<RewardedAdUiState>(RewardedAdUiState.Idle)
    val rewardedAdState: StateFlow<RewardedAdUiState> = _rewardedAdState.asStateFlow()

    private val _navEvents = MutableSharedFlow<GameNavEvent>(extraBufferCapacity = 4)
    val navEvents: SharedFlow<GameNavEvent> = _navEvents.asSharedFlow()

    fun initializeAds(context: Context) {
        rewardedAdController.initializeAndPreload(context)
    }

    fun startLevel(levelNumber: Int) {
        val spec = repository.getLevel(levelNumber)
        loadLevelSpec(spec)
    }

    fun startDailyChallenge(dateKey: String = GameRepository.todayDateKey()) {
        val spec = repository.getDailyLevel(dateKey)
        loadLevelSpec(spec)
    }

    private fun loadLevelSpec(spec: LevelSpec) {
        _gameplayState.value = GameplayUiState(
            level = spec,
            arrows = spec.arrows,
            exitingArrows = emptyList(),
            blockedArrowIds = emptySet(),
            movesCount = 0,
            mistakesCount = 0,
            heartsRemaining = 3,
            undoStack = emptyList(),
            hintedArrowId = null,
            blockedFeedback = null,
            showPathGuide = false,
            isPaused = false,
            isCompleting = false,
            showInsufficientCoinsDialog = false,
            comboCount = 0
        )
    }

    fun onArrowTapped(arrowId: Int) {
        val state = _gameplayState.value
        if (state.isPaused || state.isCompleting || state.isOutOfHearts) return

        val result = PuzzleEngine.evaluateTap(
            arrows = state.arrows,
            rows = state.level.rows,
            cols = state.level.cols,
            targetId = arrowId
        ) ?: return

        when (result) {
            is MoveResult.Cleared -> {
                val snapshot = GameMoveSnapshot(
                    arrows = state.arrows,
                    movesCount = state.movesCount,
                    mistakesCount = state.mistakesCount,
                    heartsRemaining = state.heartsRemaining,
                    blockedArrowIds = state.blockedArrowIds
                )
                val updatedArrows = state.arrows.filter { it.id != result.arrow.id }
                val exiting = ExitingArrow(tile = result.arrow)
                val nextCombo = state.comboCount + 1

                soundManager.playSlidePop(pitchStep = nextCombo)
                soundManager.vibrateLight()

                _gameplayState.update { current ->
                    current.copy(
                        arrows = updatedArrows,
                        exitingArrows = current.exitingArrows + exiting,
                        blockedArrowIds = current.blockedArrowIds - result.arrow.id,
                        movesCount = current.movesCount + 1,
                        undoStack = (current.undoStack + snapshot).takeLast(50),
                        hintedArrowId = if (current.hintedArrowId == result.arrow.id) null else current.hintedArrowId,
                        blockedFeedback = null,
                        isCompleting = result.isBoardComplete,
                        comboCount = nextCombo
                    )
                }

                viewModelScope.launch {
                    delay(400L)
                    _gameplayState.update { current ->
                        current.copy(exitingArrows = current.exitingArrows.filterNot { it.key == exiting.key })
                    }
                }

                if (result.isBoardComplete) {
                    handleLevelCompleted()
                }
            }

            is MoveResult.Blocked -> {
                soundManager.playBlockedThud()
                val feedback = BlockedFeedback(
                    arrowId = result.arrow.id,
                    blockerId = result.blocker.id,
                    message = result.reason
                )
                _gameplayState.update { current ->
                    current.copy(
                        mistakesCount = current.mistakesCount + 1,
                        heartsRemaining = (current.heartsRemaining - 1).coerceAtLeast(0),
                        blockedArrowIds = current.blockedArrowIds + result.arrow.id,
                        blockedFeedback = feedback,
                        comboCount = 0
                    )
                }
            }
        }
    }

    fun refillHeartsWithCoins() {
        viewModelScope.launch {
            if (repository.spendCoinsForRefill(50)) {
                soundManager.playCoinCollect()
                _gameplayState.update {
                    it.copy(heartsRemaining = 3, blockedArrowIds = emptySet(), blockedFeedback = null)
                }
            } else {
                soundManager.playBlockedThud()
                _navEvents.tryEmit(GameNavEvent.ShowToast("Need 50 coins to refill hearts, or use Restart!"))
            }
        }
    }

    private fun handleLevelCompleted() {
        viewModelScope.launch {
            delay(440L)
            val state = _gameplayState.value
            val stars = PuzzleEngine.calculateStars(state.mistakesCount)
            val score = PuzzleEngine.calculateScore(
                difficulty = state.level.difficulty,
                arrowCount = state.level.arrows.size,
                mistakes = state.mistakesCount,
                stars = stars
            )
            val isReplay = !state.level.isDaily && repository.isLevelAlreadyCompleted(state.level.levelNumber)
            val coinsEarned = PuzzleEngine.calculateCoinsEarned(
                difficulty = state.level.difficulty,
                stars = stars,
                isDaily = state.level.isDaily,
                isReplay = isReplay
            )

            if (state.level.isDaily && state.level.dailyDateKey != null) {
                repository.recordDailyCompletion(
                    dateKey = state.level.dailyDateKey,
                    stars = stars,
                    moves = state.movesCount,
                    score = score,
                    coinsEarned = coinsEarned
                )
            } else {
                repository.recordLevelCompletion(
                    levelNumber = state.level.levelNumber,
                    stars = stars,
                    moves = state.movesCount,
                    score = score,
                    coinsEarned = coinsEarned
                )
                repository.adManager.shouldShowPostLevelInterstitial(state.level.levelNumber)
            }

            soundManager.playWinFanfare()

            val summary = LevelCompletionSummary(
                levelNumber = state.level.levelNumber,
                isDaily = state.level.isDaily,
                dailyDateKey = state.level.dailyDateKey,
                stars = stars,
                coinsEarned = coinsEarned,
                score = score,
                moves = state.movesCount,
                mistakes = state.mistakesCount,
                initialLevel = state.level,
                bonusClaimed = false
            )
            _lastCompletion.value = summary
            _navEvents.tryEmit(GameNavEvent.OpenLevelComplete(summary))
        }
    }

    fun onUndoClicked() {
        val state = _gameplayState.value
        if (!state.canUndo) return
        val lastSnapshot = state.undoStack.last()
        soundManager.playUndoTick()
        _gameplayState.update { current ->
            current.copy(
                arrows = lastSnapshot.arrows,
                movesCount = lastSnapshot.movesCount,
                mistakesCount = lastSnapshot.mistakesCount,
                heartsRemaining = lastSnapshot.heartsRemaining,
                blockedArrowIds = lastSnapshot.blockedArrowIds,
                undoStack = current.undoStack.dropLast(1),
                hintedArrowId = null,
                blockedFeedback = null,
                comboCount = 0
            )
        }
    }

    fun onRestartClicked() {
        soundManager.playButtonClick()
        loadLevelSpec(_gameplayState.value.level)
    }

    fun onHintClicked() {
        val state = _gameplayState.value
        if (state.isPaused || state.isCompleting || state.arrows.isEmpty()) return

        val existingHint = state.hintedArrowId?.let { id -> state.arrows.firstOrNull { it.id == id } }
        if (existingHint != null && PuzzleEngine.canClear(state.arrows, state.level.rows, state.level.cols, existingHint)) {
            soundManager.playHintSparkle()
            return
        }

        val hintTile = PuzzleEngine.findHintArrow(state.arrows, state.level.rows, state.level.cols) ?: return

        viewModelScope.launch {
            when (repository.consumeHint(GameRepository.HINT_COST_COINS)) {
                is HintSpendOutcome.UsedFreeHint,
                is HintSpendOutcome.PaidWithCoins -> {
                    soundManager.playHintSparkle()
                    _gameplayState.update {
                        it.copy(
                            hintedArrowId = hintTile.id,
                            blockedFeedback = null,
                            showInsufficientCoinsDialog = false
                        )
                    }
                }

                is HintSpendOutcome.InsufficientCoins -> {
                    soundManager.playBlockedThud()
                    _gameplayState.update { it.copy(showInsufficientCoinsDialog = true) }
                }
            }
        }
    }

    fun dismissInsufficientCoinsDialog() {
        _gameplayState.update { it.copy(showInsufficientCoinsDialog = false) }
    }

    fun togglePathGuide() {
        soundManager.playButtonClick()
        _gameplayState.update { it.copy(showPathGuide = !it.showPathGuide) }
    }

    fun setPaused(paused: Boolean) {
        soundManager.playButtonClick()
        _gameplayState.update { it.copy(isPaused = paused) }
    }

    fun toggleSound() {
        val next = !appState.value.preferences.soundEnabled
        viewModelScope.launch {
            repository.setSoundEnabled(next)
            if (next) soundManager.playButtonClick()
        }
    }

    fun toggleMusic() {
        val next = !appState.value.preferences.musicEnabled
        soundManager.playButtonClick()
        viewModelScope.launch { repository.setMusicEnabled(next) }
    }

    fun toggleVibration() {
        val next = !appState.value.preferences.vibrationEnabled
        soundManager.playButtonClick()
        viewModelScope.launch { repository.setVibrationEnabled(next) }
    }

    fun setLanguage(code: String) {
        soundManager.playButtonClick()
        viewModelScope.launch { repository.setLanguageCode(code) }
    }

    fun onThemeClicked(theme: GameThemePalette) {
        val prefs = appState.value.preferences
        viewModelScope.launch {
            if (theme.id in prefs.unlockedThemeIds) {
                soundManager.playButtonClick()
                repository.selectTheme(theme.id)
            } else {
                val unlocked = repository.unlockOrSelectTheme(theme.id, theme.priceCoins)
                if (unlocked) {
                    soundManager.playCoinCollect()
                    _navEvents.tryEmit(GameNavEvent.ShowToast("${theme.name} theme unlocked!"))
                } else {
                    soundManager.playBlockedThud()
                    _navEvents.tryEmit(
                        GameNavEvent.ShowToast("Need ${theme.priceCoins - prefs.coins} more coins to unlock ${theme.name}.")
                    )
                }
            }
        }
    }

    fun claimDailyLoginReward() {
        viewModelScope.launch {
            val awarded = repository.claimDailyLoginReward()
            if (awarded != null) {
                soundManager.playCoinCollect()
                _navEvents.tryEmit(GameNavEvent.ShowToast("+$awarded Daily Reward coins claimed!"))
            } else {
                _navEvents.tryEmit(GameNavEvent.ShowToast("Today's daily reward is already claimed!"))
            }
        }
    }

    fun claimMilestone(milestone: MilestoneDefinition) {
        if (!milestone.isReadyToClaim) return
        viewModelScope.launch {
            if (repository.claimMilestone(milestone.id, milestone.rewardCoins)) {
                soundManager.playCoinCollect()
                _navEvents.tryEmit(GameNavEvent.ShowToast("+${milestone.rewardCoins} coins claimed!"))
            }
        }
    }

    fun startRewardedOffer(offer: RewardedOfferType) {
        soundManager.playButtonClick()
        _gameplayState.update { it.copy(showInsufficientCoinsDialog = false) }
        _activeRewardOffer.value = offer
    }

    fun launchRewardedAd(activity: Activity?, offer: RewardedOfferType) {
        if (activity == null) {
            _rewardedAdState.value = RewardedAdUiState.Error(offer, "Host activity unavailable.")
            return
        }
        rewardedAdController.loadAndShowRewardedAd(
            activity = activity,
            offer = offer,
            onLoadingStarted = {
                _rewardedAdState.value = RewardedAdUiState.Loading(offer)
            },
            onAdOpened = {
                _rewardedAdState.value = RewardedAdUiState.Idle
            },
            onRewardEarned = {
                completeRewardedOffer(offer)
            },
            onDismissedWithoutReward = {
                _activeRewardOffer.value = null
                _rewardedAdState.value = RewardedAdUiState.Idle
                _navEvents.tryEmit(GameNavEvent.ShowToast("Finish watching the ad to claim ${offer.rewardSummary}."))
            },
            onFailed = { errorMsg ->
                _rewardedAdState.value = RewardedAdUiState.Error(offer, errorMsg)
            }
        )
    }

    fun cancelRewardedOffer() {
        _activeRewardOffer.value = null
        _rewardedAdState.value = RewardedAdUiState.Idle
    }

    fun completeRewardedOffer(offer: RewardedOfferType) {
        _activeRewardOffer.value = null
        _rewardedAdState.value = RewardedAdUiState.Idle
        viewModelScope.launch {
            when (offer) {
                RewardedOfferType.EXTRA_HINT -> {
                    repository.addFreeHint(1)
                    soundManager.playHintSparkle()
                    _navEvents.tryEmit(GameNavEvent.ShowToast("+1 Free Hint added!"))
                }

                RewardedOfferType.BONUS_COINS -> {
                    repository.addBonusCoins(100)
                    soundManager.playCoinCollect()
                    _navEvents.tryEmit(GameNavEvent.ShowToast("+100 Bonus Coins added!"))
                }

                RewardedOfferType.DOUBLE_LEVEL_COINS -> {
                    val summary = _lastCompletion.value
                    if (summary != null && !summary.bonusClaimed) {
                        repository.addBonusCoins(summary.coinsEarned)
                        soundManager.playCoinCollect()
                        _lastCompletion.value = summary.copy(
                            coinsEarned = summary.coinsEarned * 2,
                            bonusClaimed = true
                        )
                        _navEvents.tryEmit(GameNavEvent.ShowToast("Coins doubled!"))
                    }
                }
            }
        }
    }

    fun resetProgress() {
        soundManager.playButtonClick()
        viewModelScope.launch {
            repository.resetAllProgress()
            loadLevelSpec(repository.getLevel(1))
            _navEvents.tryEmit(GameNavEvent.ShowToast("Progress reset to Level 1."))
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
