package com.example.session_kotlin.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.wearscorekeeperjava.core.MatchRules
import com.example.wearscorekeeperjava.core.ScoreEngine
import com.example.wearscorekeeperjava.core.SportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class SportOption { TENNIS, PADEL }

data class ScoreUiState(
    val leftPoints: Int = 0,
    val rightPoints: Int = 0,
    val leftGames: Int = 0,
    val rightGames: Int = 0,
    val leftSets: Int = 0,
    val rightSets: Int = 0,
    val serverLeft: Boolean = true,
    val inTieBreak: Boolean = false,
    val finished: Boolean = false,
    val leftName: String = "Left",
    val rightName: String = "Right",
    val sport: SportOption = SportOption.TENNIS,
    val setSummary: String = "",
    // NEW: live tie-break counters (only meaningful when inTieBreak = true)
    val tbLeft: Int = 0,
    val tbRight: Int = 0
)
// Put these inside ScoreViewModel (above init or below your data classes)

// Memento of the engine's mutable state
private data class EngineSnapshot(
    val leftPoints: Int,
    val rightPoints: Int,
    val leftGames: Int,
    val rightGames: Int,
    val leftSets: Int,
    val rightSets: Int,
    val isServerLeft: Boolean,
    val isInTieBreak: Boolean,
    val isFinished: Boolean,
    val tieBreakLeft: Int,
    val tieBreakRight: Int,
    val completedSets: List<com.example.wearscorekeeperjava.core.SetRecord>
)

private val undoStack = ArrayDeque<EngineSnapshot>()
private val maxUndo = 50 // cap memory


data class MatchRecord(
    val id: Long,
    val timestamp: Long,
    val sport: SportOption,
    val leftName: String,
    val rightName: String,
    // NEW: persist what we show (e.g., "6-0 7-6(5)")
    val setSummary: String,
    val leftSets: Int,
    val rightSets: Int,
    val leftGames: Int,
    val rightGames: Int
)

class ScoreViewModel(app: Application) : AndroidViewModel(app) {

    private var engine = ScoreEngine(MatchRules(SportType.TENNIS))
    private var leftName: String = "Left"
    private var rightName: String = "Right"
    private var currentSport: SportOption = SportOption.TENNIS

    private val _state = MutableStateFlow(mapState())
    val state: StateFlow<ScoreUiState> = _state

    // Persistent history
    private val _history = MutableStateFlow<List<MatchRecord>>(emptyList())
    val history: StateFlow<List<MatchRecord>> = _history

    // --- prefs ---
    private val prefs = app.getSharedPreferences("wear_scorekeeper_prefs", Context.MODE_PRIVATE)
    private val HISTORY_KEY = "history_json"

    init {
        _history.value = loadHistoryFromPrefs()
    }

    // ---- public API ----

    fun startNewMatch(sport: SportOption, left: String, right: String) {
        currentSport = sport
        leftName = left.ifBlank { "Left" }
        rightName = right.ifBlank { "Right" }
        engine = ScoreEngine(
            MatchRules(
                when (sport) {
                    SportOption.TENNIS -> SportType.TENNIS
                    SportOption.PADEL -> SportType.PADEL
                }
            )
        )
        resetInternal()
        undoStack.clear()
        emit()
    }

    fun addPointLeft() = updateAndMaybeSave { engine.pointLeft() }
    fun addPointRight() = updateAndMaybeSave { engine.pointRight() }
    fun toggleServer() = updateAndMaybeSave { engine.toggleServer() }

    fun reset() { // now acts as "Undo"
        if (undoStack.isNotEmpty()) {
            val last = undoStack.removeLast()
            restore(last)
            emit()
        }
        // else: nothing to undo; no-op
    }

    /** Manually end the match and save to history. */
    fun endMatchAndSave() {
        // allow undo of this as well
        if (undoStack.size >= maxUndo) undoStack.removeFirst()
        undoStack.addLast(snapshot())

        val s = engine.state
        if (!s.isFinished) {
            s.isFinished = true
            saveToHistoryAndPersist()
        }
        emit()
    }


    /** Optional: call to clear all saved history */
    fun clearHistory() {
        _history.value = emptyList()
        persistHistory()
    }

    // ---- internals ----

    private fun updateAndMaybeSave(block: () -> Unit) {
        // push current state for undo
        if (undoStack.size >= maxUndo) undoStack.removeFirst()
        undoStack.addLast(snapshot())

        val wasFinished = engine.state.isFinished
        block()
        val nowFinished = engine.state.isFinished
        if (!wasFinished && nowFinished) {
            // auto-save when match becomes finished
            saveToHistoryAndPersist()
        }
        emit()
    }


    private fun saveToHistoryAndPersist() {
        val s = engine.state
        val l = s.left
        val r = s.right
        val rec = MatchRecord(
            id = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            sport = currentSport,
            leftName = leftName,
            rightName = rightName,
            setSummary = formatSetsLine(), // NEW
            leftSets = l.sets,
            rightSets = r.sets,
            leftGames = l.games,
            rightGames = r.games
        )
        _history.value = listOf(rec) + _history.value // prepend newest
        persistHistory()
    }

    private fun resetInternal() {
        val s = engine.state
        s.left.points = 0; s.left.games = 0; s.left.sets = 0
        s.right.points = 0; s.right.games = 0; s.right.sets = 0
        s.isServerLeft = true
        s.isInTieBreak = false
        s.isFinished = false
        // If your engine stores completedSets in state, ensure it keeps/clears as your rules intend
        s.completedSets.clear()
        s.tieBreakLeft = 0
        s.tieBreakRight = 0
    }

    private fun emit() {
        _state.value = mapState()
    }

    // Build "6-0 7-6(5) 2-1" style line from engine.state
    private fun formatSetsLine(): String {
        val s = engine.state
        val parts = mutableListOf<String>()

        // Completed sets
        for (rec in s.completedSets) {
            if (rec.tieBreakLeft != null && rec.tieBreakRight != null) {
                val leftWon = rec.leftGames > rec.rightGames
                val loserTb = if (leftWon) rec.tieBreakRight else rec.tieBreakLeft
                parts += "${rec.leftGames}-${rec.rightGames}(${loserTb})"
            } else {
                parts += "${rec.leftGames}-${rec.rightGames}"
            }
        }

        // Ongoing set snapshot if any games in progress
        val gl = s.left.games
        val gr = s.right.games
        if (gl != 0 || gr != 0) {
            parts += "$gl-$gr"
        }

        return parts.joinToString(" ")
    }

    private fun mapState(): ScoreUiState {
        val s = engine.state
        val l = s.left
        val r = s.right
        return ScoreUiState(
            leftPoints = l.points,
            rightPoints = r.points,
            leftGames = l.games,
            rightGames = r.games,
            leftSets = l.sets,
            rightSets = r.sets,
            serverLeft = s.isServerLeft,
            inTieBreak = s.isInTieBreak,
            finished = s.isFinished,
            leftName = leftName,
            rightName = rightName,
            sport = currentSport,
            setSummary = formatSetsLine(),
            tbLeft = s.tieBreakLeft,      // NEW
            tbRight = s.tieBreakRight     // NEW
        )
    }


    // ---- persistence (JSON in SharedPreferences) ----

    private fun persistHistory() {
        val arr = JSONArray()
        for (rec in _history.value) {
            val o = JSONObject()
            o.put("id", rec.id)
            o.put("timestamp", rec.timestamp)
            o.put("sport", rec.sport.name)
            o.put("leftName", rec.leftName)
            o.put("rightName", rec.rightName)
            o.put("setSummary", rec.setSummary) // NEW
            o.put("leftSets", rec.leftSets)
            o.put("rightSets", rec.rightSets)
            o.put("leftGames", rec.leftGames)
            o.put("rightGames", rec.rightGames)
            arr.put(o)
        }
        prefs.edit().putString(HISTORY_KEY, arr.toString()).apply()
    }

    private fun loadHistoryFromPrefs(): List<MatchRecord> {
        val raw = prefs.getString(HISTORY_KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<MatchRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    MatchRecord(
                        id = o.optLong("id"),
                        timestamp = o.optLong("timestamp"),
                        sport = runCatching { SportOption.valueOf(o.optString("sport", "TENNIS")) }
                            .getOrDefault(SportOption.TENNIS),
                        leftName = o.optString("leftName", "Left"),
                        rightName = o.optString("rightName", "Right"),
                        setSummary = o.optString("setSummary", ""), // NEW
                        leftSets = o.optInt("leftSets"),
                        rightSets = o.optInt("rightSets"),
                        leftGames = o.optInt("leftGames"),
                        rightGames = o.optInt("rightGames")
                    )
                )
            }
            list
        } catch (t: Throwable) {
            emptyList()
        }
    }
    private fun snapshot(): EngineSnapshot {
        val s = engine.state
        // deep copy completedSets (new list, new record objects)
        val copied = s.completedSets.map {
            com.example.wearscorekeeperjava.core.SetRecord(
                it.leftGames, it.rightGames, it.tieBreakLeft, it.tieBreakRight
            )
        }
        return EngineSnapshot(
            leftPoints = s.left.points,
            rightPoints = s.right.points,
            leftGames = s.left.games,
            rightGames = s.right.games,
            leftSets = s.left.sets,
            rightSets = s.right.sets,
            isServerLeft = s.isServerLeft,
            isInTieBreak = s.isInTieBreak,
            isFinished = s.isFinished,
            tieBreakLeft = s.tieBreakLeft,
            tieBreakRight = s.tieBreakRight,
            completedSets = copied
        )
    }

    private fun restore(from: EngineSnapshot) {
        val s = engine.state
        s.left.points = from.leftPoints
        s.right.points = from.rightPoints
        s.left.games = from.leftGames
        s.right.games = from.rightGames
        s.left.sets = from.leftSets
        s.right.sets = from.rightSets
        s.isServerLeft = from.isServerLeft
        s.isInTieBreak = from.isInTieBreak
        s.isFinished = from.isFinished
        s.tieBreakLeft = from.tieBreakLeft
        s.tieBreakRight = from.tieBreakRight

        s.completedSets.clear()
        s.completedSets.addAll(from.completedSets.map {
            com.example.wearscorekeeperjava.core.SetRecord(
                it.leftGames, it.rightGames, it.tieBreakLeft, it.tieBreakRight
            )
        })
    }

}
