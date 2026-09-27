package com.ivangames.chessbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boardSize = 8

    private var board: Array<Array<Char?>> = Array(boardSize) { Array(boardSize) { null } }

    private var selectedRow = -1
    private var selectedCol = -1
    private var possibleMoves: MutableList<Pair<Int, Int>> = mutableListOf()

    private var whiteTurn = true
    private var kingInCheck = false
    private var gameOver = false

    // ИИ
    var vsComputer = false
    var aiThinking = false

    // Сеть
    var networkMode = false
    var myTurnIsWhite = true

    // Рокировка
    private var whiteKingMoved = false
    private var blackKingMoved = false
    private var whiteRookLeftMoved = false
    private var whiteRookRightMoved = false
    private var blackRookLeftMoved = false
    private var blackRookRightMoved = false

    // Взятие на проходе
    private var enPassantRow = -1
    private var enPassantCol = -1

    // ============ АНИМАЦИИ ============

    // Анимация движения фигуры
    private var animPiece: Char? = null
    private var animFromRow = -1
    private var animFromCol = -1
    private var animToRow = -1
    private var animToCol = -1
    private var animProgress = 0f  // 0.0 → 1.0
    private var animActive = false

    // Последний ход (для подсветки)
    private var lastMoveFromRow = -1
    private var lastMoveFromCol = -1
    private var lastMoveToRow = -1
    private var lastMoveToCol = -1

    // Анимация шаха (пульсация)
    private var checkPulse = 0f
    private var checkPulseDirection = 1f

    // Анимация превращения пешки
    private var promoteRow = -1
    private var promoteCol = -1
    private var promoteAnimTimer = 0

    // ============ СЕТКА И КООРДИНАТЫ ============

    private var cellSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f

    // ============ КРАСКИ ============

    private val lightCellPaint = Paint().apply {
        color = Color.parseColor("#F0D9B5")
        style = Paint.Style.FILL
    }
    private val darkCellPaint = Paint().apply {
        color = Color.parseColor("#B58863")
        style = Paint.Style.FILL
    }
    private val lastMovePaint = Paint().apply {
        color = Color.parseColor("#80FFEB3B")
        style = Paint.Style.FILL
    }
    private val selectedPaint = Paint().apply {
        color = Color.parseColor("#80FF9800")
        style = Paint.Style.FILL
    }
    private val checkPaint = Paint().apply {
        color = Color.parseColor("#FF0000")
        style = Paint.Style.FILL
        alpha = 150
    }
    private val moveDotPaint = Paint().apply {
        color = Color.parseColor("#66AAAAAA")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val captureDotPaint = Paint().apply {
        color = Color.parseColor("#66FF4444")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val whitePiecePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(6f, 0f, 3f, Color.parseColor("#88000000"))
    }
    private val blackPiecePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(6f, 0f, 3f, Color.parseColor("#88000000"))
    }
    private val promoteFlashPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // ============ КОЛБЭКИ ============

    var onTurnChanged: ((Boolean) -> Unit)? = null
    var onCheck: (() -> Unit)? = null
    var onCheckmate: ((Boolean) -> Unit)? = null
    var onStalemate: (() -> Unit)? = null
    var onMoveMade: ((Int, Int, Int, Int) -> Unit)? = null

    init {
        setupInitialPosition()
    }

    fun setupInitialPosition() {
        for (row in 0 until boardSize) for (col in 0 until boardSize) board[row][col] = null
        board[0][0] = 'r'; board[0][1] = 'n'; board[0][2] = 'b'; board[0][3] = 'q'
        board[0][4] = 'k'; board[0][5] = 'b'; board[0][6] = 'n'; board[0][7] = 'r'
        for (col in 0 until 8) board[1][col] = 'p'
        for (col in 0 until 8) board[6][col] = 'P'
        board[7][0] = 'R'; board[7][1] = 'N'; board[7][2] = 'B'; board[7][3] = 'Q'
        board[7][4] = 'K'; board[7][5] = 'B'; board[7][6] = 'N'; board[7][7] = 'R'

        whiteTurn = true
        whiteKingMoved = false
        blackKingMoved = false
        whiteRookLeftMoved = false
        whiteRookRightMoved = false
        blackRookLeftMoved = false
        blackRookRightMoved = false
        enPassantRow = -1
        enPassantCol = -1
        kingInCheck = false
        selectedRow = -1
        selectedCol = -1
        possibleMoves.clear()
        aiThinking = false
        gameOver = false
        animActive = false
        lastMoveFromRow = -1
        lastMoveFromCol = -1
        lastMoveToRow = -1
        lastMoveToCol = -1
        promoteAnimTimer = 0
        history.clear()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = minOf(w, h).toFloat() * 0.95f
        cellSize = size / boardSize
        boardLeft = (w - cellSize * boardSize) / 2f
        boardTop = (h - cellSize * boardSize) / 2f
    }

    private fun isOwnPiece(piece: Char?, white: Boolean): Boolean {
        if (piece == null) return false
        return if (white) piece.isUpperCase() else piece.isLowerCase()
    }

    private fun isEnemyPiece(piece: Char?, white: Boolean): Boolean {
        if (piece == null) return false
        return if (white) piece.isLowerCase() else piece.isUpperCase()
    }

    private fun inBounds(row: Int, col: Int): Boolean =
        row in 0 until boardSize && col in 0 until boardSize

    private fun findKing(white: Boolean): Pair<Int, Int>? {
        val target = if (white) 'K' else 'k'
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] == target) return row to col
            }
        }
        return null
    }

    private fun isSquareAttackedBy(row: Int, col: Int, byWhite: Boolean): Boolean {
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                val piece = board[r][c] ?: continue
                if (piece.isUpperCase() != byWhite) continue
                if (attacks(r, c, piece, row, col)) return true
            }
        }
        return false
    }

    private fun attacks(r: Int, c: Int, piece: Char, tr: Int, tc: Int): Boolean {
        val white = piece.isUpperCase()
        val dr = tr - r
        val dc = tc - c

        return when (piece.uppercaseChar()) {
            'R' -> (dr == 0 || dc == 0) && (dr != 0 || dc != 0) && pathClear(r, c, tr, tc)
            'B' -> (Math.abs(dr) == Math.abs(dc)) && dr != 0 && pathClear(r, c, tr, tc)
            'Q' -> ((dr == 0 || dc == 0) || (Math.abs(dr) == Math.abs(dc))) && (dr != 0 || dc != 0) && pathClear(r, c, tr, tc)
            'N' -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2)
            'K' -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1 && (dr != 0 || dc != 0)
            'P' -> {
                val dir = if (white) -1 else 1
                dr == dir && Math.abs(dc) == 1
            }
            else -> false
        }
    }

    private fun pathClear(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val dr = Integer.signum(r2 - r1)
        val dc = Integer.signum(c2 - c1)
        var r = r1 + dr
        var c = c1 + dc
        while (r != r2 || c != c2) {
            if (board[r][c] != null) return false
            r += dr
            c += dc
        }
        return true
    }

    private val history = mutableListOf<MoveSnapshot>()

    data class MoveSnapshot(
        val board: Array<Array<Char?>>,
        val whiteTurn: Boolean,
        val whiteKingMoved: Boolean,
        val blackKingMoved: Boolean,
        val whiteRookLeftMoved: Boolean,
        val whiteRookRightMoved: Boolean,
        val blackRookLeftMoved: Boolean,
        val blackRookRightMoved: Boolean,
        val enPassantRow: Int,
        val enPassantCol: Int
    )
// ============ ГЕНЕРАЦИЯ ХОДОВ ============

private fun getMovesFor(row: Int, col: Int): MutableList<Pair<Int, Int>> {
    val piece = board[row][col] ?: return mutableListOf()
    val moves = mutableListOf<Pair<Int, Int>>()

    when (piece.uppercaseChar()) {
        'R' -> addLineMoves(moves, row, col, piece,
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
        'B' -> addLineMoves(moves, row, col, piece,
            listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
        'Q' -> addLineMoves(moves, row, col, piece,
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1))
        'K' -> {
            addKingMoves(moves, row, col, piece)
            addCastlingMoves(moves, row, col, piece)
        }
        'N' -> addKnightMoves(moves, row, col, piece)
        'P' -> addPawnMoves(moves, row, col, piece)
    }

    val white = piece.isUpperCase()
    val legalMoves = mutableListOf<Pair<Int, Int>>()
    for ((r, c) in moves) {
        val savedTarget = board[r][c]
        val savedFrom = board[row][col]
        board[r][c] = piece
        board[row][col] = null
        val kingPos = findKing(white)
        val stillSafe = if (kingPos == null) true
        else !isSquareAttackedBy(kingPos.first, kingPos.second, !white)
        board[r][c] = savedTarget
        board[row][col] = savedFrom
        if (stillSafe) legalMoves.add(r to c)
    }
    return legalMoves
}

private fun addLineMoves(
    moves: MutableList<Pair<Int, Int>>, row: Int, col: Int, piece: Char,
    directions: List<Pair<Int, Int>>
) {
    val white = piece.isUpperCase()
    for ((dr, dc) in directions) {
        var r = row + dr
        var c = col + dc
        while (inBounds(r, c)) {
            val target = board[r][c]
            if (target == null) {
                moves.add(r to c)
            } else {
                if (isEnemyPiece(target, white)) moves.add(r to c)
                break
            }
            r += dr
            c += dc
        }
    }
}

private fun addKingMoves(moves: MutableList<Pair<Int, Int>>, row: Int, col: Int, piece: Char) {
    val white = piece.isUpperCase()
    val dirs = listOf(
        -1 to -1, -1 to 0, -1 to 1,
        0 to -1, 0 to 1,
        1 to -1, 1 to 0, 1 to 1
    )
    for ((dr, dc) in dirs) {
        val r = row + dr
        val c = col + dc
        if (inBounds(r, c)) {
            val target = board[r][c]
            if (target == null || isEnemyPiece(target, white)) {
                moves.add(r to c)
            }
        }
    }
}

private fun addCastlingMoves(moves: MutableList<Pair<Int, Int>>, row: Int, col: Int, piece: Char) {
    val white = piece.isUpperCase()
    if (white && row == 7 && col == 4 && !whiteKingMoved) {
        if (!whiteRookRightMoved && board[7][5] == null && board[7][6] == null && board[7][7] == 'R'
            && !isSquareAttackedBy(7, 4, false) && !isSquareAttackedBy(7, 5, false) && !isSquareAttackedBy(7, 6, false)) {
            moves.add(7 to 6)
        }
        if (!whiteRookLeftMoved && board[7][1] == null && board[7][2] == null && board[7][3] == null && board[7][0] == 'R'
            && !isSquareAttackedBy(7, 4, false) && !isSquareAttackedBy(7, 3, false) && !isSquareAttackedBy(7, 2, false)) {
            moves.add(7 to 2)
        }
    }
    if (!white && row == 0 && col == 4 && !blackKingMoved) {
        if (!blackRookRightMoved && board[0][5] == null && board[0][6] == null && board[0][7] == 'r'
            && !isSquareAttackedBy(0, 4, true) && !isSquareAttackedBy(0, 5, true) && !isSquareAttackedBy(0, 6, true)) {
            moves.add(0 to 6)
        }
        if (!blackRookLeftMoved && board[0][1] == null && board[0][2] == null && board[0][3] == null && board[0][0] == 'r'
            && !isSquareAttackedBy(0, 4, true) && !isSquareAttackedBy(0, 3, true) && !isSquareAttackedBy(0, 2, true)) {
            moves.add(0 to 2)
        }
    }
}

private fun addKnightMoves(moves: MutableList<Pair<Int, Int>>, row: Int, col: Int, piece: Char) {
    val white = piece.isUpperCase()
    val jumps = listOf(
        -2 to -1, -2 to 1, -1 to -2, -1 to 2,
        1 to -2, 1 to 2, 2 to -1, 2 to 1
    )
    for ((dr, dc) in jumps) {
        val r = row + dr
        val c = col + dc
        if (inBounds(r, c)) {
            val target = board[r][c]
            if (target == null || isEnemyPiece(target, white)) {
                moves.add(r to c)
            }
        }
    }
}

private fun addPawnMoves(moves: MutableList<Pair<Int, Int>>, row: Int, col: Int, piece: Char) {
    val white = piece.isUpperCase()
    val dir = if (white) -1 else 1
    val startRow = if (white) 6 else 1

    if (inBounds(row + dir, col) && board[row + dir][col] == null) {
        moves.add((row + dir) to col)
        if (row == startRow && board[row + 2 * dir][col] == null) {
            moves.add((row + 2 * dir) to col)
        }
    }
    for (dc in listOf(-1, 1)) {
        val r = row + dir
        val c = col + dc
        if (inBounds(r, c)) {
            val target = board[r][c]
            if (target != null && isEnemyPiece(target, white)) {
                moves.add(r to c)
            } else if (target == null && r == enPassantRow && c == enPassantCol) {
                moves.add(r to c)
            }
        }
    }
}

// ============ ИИ ============

fun findBestMove(white: Boolean): Triple<Int, Int, Int>? {
    val allMoves = mutableListOf<MoveWithScore>()
    for (row in 0 until boardSize) {
        for (col in 0 until boardSize) {
            val piece = board[row][col] ?: continue
            if (piece.isUpperCase() != white) continue
            val moves = getMovesFor(row, col)
            for ((tr, tc) in moves) {
                val score = evaluateMove(row, col, tr, tc, white)
                allMoves.add(MoveWithScore(row, col, tr, tc, score))
            }
        }
    }
    if (allMoves.isEmpty()) return null
    val best = allMoves.maxByOrNull { it.score } ?: return null
    return Triple(best.fromRow, best.fromCol, best.toRow shl 8 or best.toCol)
}

private fun evaluateMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int, white: Boolean): Int {
    var score = 0
    val target = board[toRow][toCol]
    val piece = board[fromRow][fromCol] ?: return 0

    if (target != null) score += pieceValue(target) * 10
    if (piece.uppercaseChar() == 'P' && toCol != fromCol && target == null) {
        score += pieceValue(if (white) 'p' else 'P') * 10
    }
    if (piece.uppercaseChar() == 'P') {
        val advance = if (white) (fromRow - toRow) else (toRow - fromRow)
        score += advance * 5
        if ((white && toRow == 0) || (!white && toRow == 7)) score += 800
    }
    if (toRow in 3..4 && toCol in 3..4) score += 3
    if (piece.uppercaseChar() == 'K' && Math.abs(toCol - fromCol) == 2) score += 15

    val savedTarget = board[toRow][toCol]
    val savedFrom = board[fromRow][fromCol]
    board[toRow][toCol] = piece
    board[fromRow][fromCol] = null
    val enemyKing = findKing(!white)
    if (enemyKing != null && isSquareAttackedBy(enemyKing.first, enemyKing.second, white)) {
        score += 50
    }
    board[toRow][toCol] = savedTarget
    board[fromRow][fromCol] = savedFrom

    score += Random.nextInt(-2, 3)
    return score
}

private fun pieceValue(piece: Char): Int = when (piece.uppercaseChar()) {
    'P' -> 1; 'N' -> 3; 'B' -> 3; 'R' -> 5; 'Q' -> 9; 'K' -> 100; else -> 0
}

data class MoveWithScore(
    val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int,
    val score: Int
)

fun makeAIMove() {
    if (!vsComputer || whiteTurn) return
    aiThinking = true
    val move = findBestMove(false)
    if (move != null) {
        val fromRow = move.first
        val fromCol = move.second
        val packed = move.third
        val toRow = packed shr 8
        val toCol = packed and 0xFF
        saveHistory()
        startMoveAnimation(fromRow, fromCol, toRow, toCol)
        updateCheckState()
    }
    aiThinking = false
    invalidate()
}
    // ============ АНИМАЦИЯ ДВИЖЕНИЯ ============

    private fun startMoveAnimation(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val piece = board[fromRow][fromCol] ?: return
        animPiece = piece
        animFromRow = fromRow
        animFromCol = fromCol
        animToRow = toRow
        animToCol = toCol
        animProgress = 0f
        animActive = true
        invalidate()
    }

    private fun finishMoveAnimation() {
        if (!animActive) return
        val piece = animPiece ?: return
        val fromRow = animFromRow
        val fromCol = animFromCol
        val toRow = animToRow
        val toCol = animToCol
        val target = board[toRow][toCol]

        // Рокировка
        if (piece.uppercaseChar() == 'K' && Math.abs(toCol - fromCol) == 2) {
            if (toCol > fromCol) {
                board[toRow][toCol - 1] = board[fromRow][7]
                board[fromRow][7] = null
            } else {
                board[toRow][toCol + 1] = board[fromRow][0]
                board[fromRow][0] = null
            }
        }

        // Взятие на проходе
        if (piece.uppercaseChar() == 'P' && toCol != fromCol && target == null
            && toRow == enPassantRow && toCol == enPassantCol) {
            board[fromRow][toCol] = null
        }

        // Флаги движения
        if (piece == 'K') whiteKingMoved = true
        if (piece == 'k') blackKingMoved = true
        if (piece == 'R' && fromRow == 7 && fromCol == 0) whiteRookLeftMoved = true
        if (piece == 'R' && fromRow == 7 && fromCol == 7) whiteRookRightMoved = true
        if (piece == 'r' && fromRow == 0 && fromCol == 0) blackRookLeftMoved = true
        if (piece == 'r' && fromRow == 0 && fromCol == 7) blackRookRightMoved = true

        // En passant
        enPassantRow = -1
        enPassantCol = -1
        if (piece == 'P' && fromRow == 6 && toRow == 4) {
            enPassantRow = 5; enPassantCol = fromCol
        }
        if (piece == 'p' && fromRow == 1 && toRow == 3) {
            enPassantRow = 2; enPassantCol = fromCol
        }

        // Двигаем
        board[toRow][toCol] = piece
        board[fromRow][fromCol] = null

        // Превращение пешки
        if (piece == 'P' && toRow == 0) {
            board[toRow][toCol] = 'Q'
            promoteRow = toRow
            promoteCol = toCol
            promoteAnimTimer = 20  // 0.33 сек
        }
        if (piece == 'p' && toRow == 7) {
            board[toRow][toCol] = 'q'
            promoteRow = toRow
            promoteCol = toCol
            promoteAnimTimer = 20
        }

        // Запоминаем последний ход
        lastMoveFromRow = fromRow
        lastMoveFromCol = fromCol
        lastMoveToRow = toRow
        lastMoveToCol = toCol

        whiteTurn = !whiteTurn
        onTurnChanged?.invoke(whiteTurn)

        // Анимация завершена
        animActive = false
        animPiece = null
        updateCheckState()
        invalidate()
    }

    // ============ РИСОВАНИЕ ============

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.parseColor("#1A1A1A"))

        // Шахматная доска
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val left = boardLeft + col * cellSize
                val top = boardTop + row * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                val paint = if ((row + col) % 2 == 0) lightCellPaint else darkCellPaint
                canvas.drawRect(left, top, right, bottom, paint)

                // Подсветка последнего хода
                if ((row == lastMoveFromRow && col == lastMoveFromCol) ||
                    (row == lastMoveToRow && col == lastMoveToCol)) {
                    canvas.drawRect(left, top, right, bottom, lastMovePaint)
                }

                // Подсветка выбранной клетки
                if (row == selectedRow && col == selectedCol) {
                    canvas.drawRect(left, top, right, bottom, selectedPaint)
                }
            }
        }

        // Пульсация шаха
        if (kingInCheck) {
            val kingPos = findKing(whiteTurn)
            if (kingPos != null) {
                checkPulse += checkPulseDirection * 0.05f
                if (checkPulse >= 1f) { checkPulse = 1f; checkPulseDirection = -1f }
                if (checkPulse <= 0f) { checkPulse = 0f; checkPulseDirection = 1f }

                val left = boardLeft + kingPos.second * cellSize
                val top = boardTop + kingPos.first * cellSize

                val pulsePaint = Paint().apply {
                    color = Color.RED
                    style = Paint.Style.FILL
                    alpha = (60 + 100 * checkPulse).toInt()
                }
                canvas.drawRect(left, top, left + cellSize, top + cellSize, pulsePaint)
            }
        }

        // Точки возможных ходов
        for ((r, c) in possibleMoves) {
            val cx = boardLeft + c * cellSize + cellSize / 2f
            val cy = boardTop + r * cellSize + cellSize / 2f
            val isCapture = board[r][c] != null
            val paint = if (isCapture) captureDotPaint else moveDotPaint
            val radius = if (isCapture) cellSize * 0.42f else cellSize * 0.18f
            canvas.drawCircle(cx, cy, radius, paint)
        }

        // Фигуры
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                // Пропускаем начальную клетку анимируемой фигуры
                if (animActive && row == animFromRow && col == animFromCol) continue
                // Пропускаем конечную, если фигура уже там анимируется
                if (animActive && row == animToRow && col == animToCol) continue

                val piece = board[row][col] ?: continue
                drawPiece(canvas, row, col, piece, 0f, 0f)
            }
        }

        // Анимируемая фигура
        if (animActive && animPiece != null) {
            val startX = boardLeft + animFromCol * cellSize
            val startY = boardTop + animFromRow * cellSize
            val endX = boardLeft + animToCol * cellSize
            val endY = boardTop + animToRow * cellSize

            val x = startX + (endX - startX) * animProgress
            val y = startY + (endY - startY) * animProgress

            drawPieceAbsolute(canvas, animPiece!!, x, y)

            // Обновляем прогресс
            animProgress += 0.12f
            if (animProgress >= 1f) {
                animProgress = 1f
                finishMoveAnimation()
                return
            }
            invalidate()
        }

        // Анимация превращения пешки (белая вспышка)
        if (promoteAnimTimer > 0) {
            val left = boardLeft + promoteCol * cellSize
            val top = boardTop + promoteRow * cellSize
            val alpha = (promoteAnimTimer / 20f * 255).toInt().coerceIn(0, 255)
            promoteFlashPaint.alpha = alpha
            canvas.drawRect(left, top, left + cellSize, top + cellSize, promoteFlashPaint)
            promoteAnimTimer--
            invalidate()
        }

        // Рамка доски
        canvas.drawRect(
            boardLeft, boardTop,
            boardLeft + cellSize * boardSize,
            boardTop + cellSize * boardSize,
            borderPaint
        )
    }

    private fun drawPiece(canvas: Canvas, row: Int, col: Int, piece: Char, offsetX: Float, offsetY: Float) {
        val symbol = getPieceSymbol(piece)
        val isWhite = piece.isUpperCase()
        val paint = if (isWhite) whitePiecePaint else blackPiecePaint
        paint.textSize = cellSize * 0.75f
        val cx = boardLeft + col * cellSize + cellSize / 2f + offsetX
        val cy = boardTop + row * cellSize + cellSize / 2f -
                (paint.descent() + paint.ascent()) / 2f + offsetY
        canvas.drawText(symbol, cx, cy, paint)
    }

    private fun drawPieceAbsolute(canvas: Canvas, piece: Char, x: Float, y: Float) {
        val symbol = getPieceSymbol(piece)
        val isWhite = piece.isUpperCase()
        val paint = if (isWhite) whitePiecePaint else blackPiecePaint
        paint.textSize = cellSize * 0.75f
        val cx = x + cellSize / 2f
        val cy = y + cellSize / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(symbol, cx, cy, paint)
    }

    private fun getPieceSymbol(piece: Char): String {
        return when (piece) {
            'K' -> "\u2654"; 'Q' -> "\u2655"; 'R' -> "\u2656"
            'B' -> "\u2657"; 'N' -> "\u2658"; 'P' -> "\u2659"
            'k' -> "\u265A"; 'q' -> "\u265B"; 'r' -> "\u265C"
            'b' -> "\u265D"; 'n' -> "\u265E"; 'p' -> "\u265F"
            else -> ""
        }
    }

    // ============ ОБРАБОТКА ТАПА ============

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        if (aiThinking) return true
        if (gameOver) return true
        if (animActive) return true
        if (networkMode && whiteTurn != myTurnIsWhite) return true
        if (vsComputer && !whiteTurn) return true

        val col = ((event.x - boardLeft) / cellSize).toInt()
        val row = ((event.y - boardTop) / cellSize).toInt()

        if (!inBounds(row, col)) return true

        val piece = board[row][col]

        if (selectedRow != -1) {
            if (possibleMoves.contains(row to col)) {
                saveHistory()
                val isNetworkMove = networkMode
                val fromR = selectedRow
                val fromC = selectedCol
                selectedRow = -1
                selectedCol = -1
                possibleMoves.clear()

                startMoveAnimation(fromR, fromC, row, col)

                if (isNetworkMove) {
                    onMoveMade?.invoke(fromR, fromC, row, col)
                }

                if (vsComputer && !whiteTurn && !gameOver) {
                    postDelayed({ makeAIMove() }, 500)
                }
                return true
            }
            if (isOwnPiece(piece, whiteTurn)) {
                selectedRow = row
                selectedCol = col
                possibleMoves = getMovesFor(row, col)
                invalidate()
                return true
            }
            selectedRow = -1
            selectedCol = -1
            possibleMoves.clear()
            invalidate()
            return true
        }

        if (isOwnPiece(piece, whiteTurn)) {
            selectedRow = row
            selectedCol = col
            possibleMoves = getMovesFor(row, col)
            invalidate()
        }

        return true
    }

    // ============ СЕТЕВЫЕ МЕТОДЫ ============

    fun makeMoveFromNetwork(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        saveHistory()
        startMoveAnimation(fromRow, fromCol, toRow, toCol)
    }

    // ============ ШАХ / МАТ ============

    private fun updateCheckState() {
        val white = whiteTurn
        val kingPos = findKing(white)
        kingInCheck = if (kingPos == null) false
        else isSquareAttackedBy(kingPos.first, kingPos.second, !white)

        if (kingInCheck) onCheck?.invoke()

        if (!hasAnyLegalMove(white)) {
            gameOver = true
            if (kingInCheck) onCheckmate?.invoke(white)
            else onStalemate?.invoke()
        }
    }

    private fun hasAnyLegalMove(white: Boolean): Boolean {
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val piece = board[row][col] ?: continue
                if (piece.isUpperCase() != white) continue
                if (getMovesFor(row, col).isNotEmpty()) return true
            }
        }
        return false
    }

    // ============ ОТМЕНА / СБРОС ============

    private fun saveHistory() {
        history.add(
            MoveSnapshot(
                board = Array(boardSize) { r -> Array(boardSize) { c -> board[r][c] } },
                whiteTurn = whiteTurn,
                whiteKingMoved = whiteKingMoved,
                blackKingMoved = blackKingMoved,
                whiteRookLeftMoved = whiteRookLeftMoved,
                whiteRookRightMoved = whiteRookRightMoved,
                blackRookLeftMoved = blackRookLeftMoved,
                blackRookRightMoved = blackRookRightMoved,
                enPassantRow = enPassantRow,
                enPassantCol = enPassantCol
            )
        )
    }

    fun undoMove() {
        if (history.isEmpty()) return
        val snap = history.removeAt(history.size - 1)
        for (r in 0 until boardSize) for (c in 0 until boardSize) board[r][c] = snap.board[r][c]
        whiteTurn = snap.whiteTurn
        whiteKingMoved = snap.whiteKingMoved
        blackKingMoved = snap.blackKingMoved
        whiteRookLeftMoved = snap.whiteRookLeftMoved
        whiteRookRightMoved = snap.whiteRookRightMoved
        blackRookLeftMoved = snap.blackRookLeftMoved
        blackRookRightMoved = snap.blackRookRightMoved
        enPassantRow = snap.enPassantRow
        enPassantCol = snap.enPassantCol
        selectedRow = -1
        selectedCol = -1
        possibleMoves.clear()
        gameOver = false
        animActive = false
        lastMoveFromRow = -1
        lastMoveFromCol = -1
        lastMoveToRow = -1
        lastMoveToCol = -1
        updateCheckState()
        onTurnChanged?.invoke(whiteTurn)
        invalidate()
    }

    fun resetGame() {
        setupInitialPosition()
        onTurnChanged?.invoke(whiteTurn)
        invalidate()
    }
}
