package com.ivangames.chessbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boardSize = 8

    // Доска: null = пусто, иначе символ фигуры
    private var board: Array<Array<Char?>> = Array(boardSize) { Array(boardSize) { null } }

    // Выбранная фигура
    private var selectedRow = -1
    private var selectedCol = -1

    // Возможные ходы от выбранной фигуры
    private var possibleMoves: MutableList<Pair<Int, Int>> = mutableListOf()

    // Чей ход: true = белые, false = чёрные
    private var whiteTurn = true

    // Размеры
    private var cellSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f

    // Краски
    private val lightCellPaint = Paint().apply {
        color = Color.parseColor("#F0D9B5")
        style = Paint.Style.FILL
    }
    private val darkCellPaint = Paint().apply {
        color = Color.parseColor("#B58863")
        style = Paint.Style.FILL
    }
    private val selectedPaint = Paint().apply {
        color = Color.parseColor("#FFEB3B")
        style = Paint.Style.FILL
        alpha = 180
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
        setShadowLayer(4f, 0f, 2f, Color.parseColor("#66000000"))
    }
    private val blackPiecePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 0f, 2f, Color.parseColor("#66000000"))
    }

    // Колбэк — сообщить Activity о смене хода
    var onTurnChanged: ((Boolean) -> Unit)? = null

    init {
        setupInitialPosition()
    }

    private fun setupInitialPosition() {
        board[0][0] = 'r'; board[0][1] = 'n'; board[0][2] = 'b'; board[0][3] = 'q'
        board[0][4] = 'k'; board[0][5] = 'b'; board[0][6] = 'n'; board[0][7] = 'r'
        for (col in 0 until 8) board[1][col] = 'p'
        for (col in 0 until 8) board[6][col] = 'P'
        board[7][0] = 'R'; board[7][1] = 'N'; board[7][2] = 'B'; board[7][3] = 'Q'
        board[7][4] = 'K'; board[7][5] = 'B'; board[7][6] = 'N'; board[7][7] = 'R'
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = minOf(w, h).toFloat() * 0.95f
        cellSize = size / boardSize
        boardLeft = (w - cellSize * boardSize) / 2f
        boardTop = (h - cellSize * boardSize) / 2f
    }

    // ============ ЛОГИКА ДВИЖЕНИЯ ============

    // Проверка: своя ли это фигура
    private fun isOwnPiece(piece: Char?, whiteTurn: Boolean): Boolean {
        if (piece == null) return false
        return if (whiteTurn) piece.isUpperCase() else piece.isLowerCase()
    }

    // Проверка: враг ли это
    private fun isEnemyPiece(piece: Char?, whiteTurn: Boolean): Boolean {
        if (piece == null) return false
        return if (whiteTurn) piece.isLowerCase() else piece.isUpperCase()
    }

    // Проверка: в пределах доски
    private fun inBounds(row: Int, col: Int): Boolean =
        row in 0 until boardSize && col in 0 until boardSize

    // Получить возможные ходы для фигуры
    private fun getMovesFor(row: Int, col: Int): MutableList<Pair<Int, Int>> {
        val piece = board[row][col] ?: return mutableListOf()
        val moves = mutableListOf<Pair<Int, Int>>()

        when (piece.uppercaseChar()) {
            'R' -> addLineMoves(moves, row, col, piece, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
            'B' -> addLineMoves(moves, row, col, piece, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
            'Q' -> addLineMoves(moves, row, col, piece,
                listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1))
            'K' -> addKingMoves(moves, row, col, piece)
            'N' -> addKnightMoves(moves, row, col, piece)
            'P' -> addPawnMoves(moves, row, col, piece)
        }
        return moves
    }

    // Ладья, слон, ферзь — движение по линиям
    private fun addLineMoves(
        moves: MutableList<Pair<Int, Int>>,
        row: Int, col: Int, piece: Char,
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

    // Король — по 1 клетке во все стороны
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

    // Конь — буквой Г
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

    // Пешка — вперёд, двойной ход с начала, взятие по диагонали
    private fun addPawnMoves(moves: MutableList<Pair<Int, Int>>, row: Int, col: Int, piece: Char) {
        val white = piece.isUpperCase()
        val dir = if (white) -1 else 1  // белые идут вверх, чёрные — вниз
        val startRow = if (white) 6 else 1

        // 1 клетка вперёд
        if (inBounds(row + dir, col) && board[row + dir][col] == null) {
            moves.add((row + dir) to col)
            // 2 клетки вперёд с начальной позиции
            if (row == startRow && board[row + 2 * dir][col] == null) {
                moves.add((row + 2 * dir) to col)
            }
        }
        // Взятие по диагонали
        for (dc in listOf(-1, 1)) {
            val r = row + dir
            val c = col + dc
            if (inBounds(r, c)) {
                val target = board[r][c]
                if (target != null && isEnemyPiece(target, white)) {
                    moves.add(r to c)
                }
            }
        }
    }
    // ============ РИСОВАНИЕ ============

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.parseColor("#1A1A1A"))

        // Клетки
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val left = boardLeft + col * cellSize
                val top = boardTop + row * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                val paint = if ((row + col) % 2 == 0) lightCellPaint else darkCellPaint
                canvas.drawRect(left, top, right, bottom, paint)

                // Подсветка выбранной клетки
                if (row == selectedRow && col == selectedCol) {
                    canvas.drawRect(left, top, right, bottom, selectedPaint)
                }
            }
        }

        // Возможные ходы — точки
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
                val piece = board[row][col] ?: continue
                drawPiece(canvas, row, col, piece)
            }
        }

        // Рамка
        canvas.drawRect(
            boardLeft, boardTop,
            boardLeft + cellSize * boardSize,
            boardTop + cellSize * boardSize,
            borderPaint
        )
    }

    private fun drawPiece(canvas: Canvas, row: Int, col: Int, piece: Char) {
        val symbol = getPieceSymbol(piece)
        val isWhite = piece.isUpperCase()

        val paint = if (isWhite) whitePiecePaint else blackPiecePaint
        paint.textSize = cellSize * 0.75f

        val cx = boardLeft + col * cellSize + cellSize / 2f
        val cy = boardTop + row * cellSize + cellSize / 2f -
                (paint.descent() + paint.ascent()) / 2f

        canvas.drawText(symbol, cx, cy, paint)
    }

    private fun getPieceSymbol(piece: Char): String {
        return when (piece) {
            'K' -> "\u2654"
            'Q' -> "\u2655"
            'R' -> "\u2656"
            'B' -> "\u2657"
            'N' -> "\u2658"
            'P' -> "\u2659"
            'k' -> "\u265A"
            'q' -> "\u265B"
            'r' -> "\u265C"
            'b' -> "\u265D"
            'n' -> "\u265E"
            'p' -> "\u265F"
            else -> ""
        }
    }

    // ============ ОБРАБОТКА ТАПА ============

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true

        val col = ((event.x - boardLeft) / cellSize).toInt()
        val row = ((event.y - boardTop) / cellSize).toInt()

        if (!inBounds(row, col)) return true

        val piece = board[row][col]

        // Если что-то уже выбрано
        if (selectedRow != -1) {
            // Тапнули по клетке из возможных ходов — делаем ход
            if (possibleMoves.contains(row to col)) {
                makeMove(selectedRow, selectedCol, row, col)
                selectedRow = -1
                selectedCol = -1
                possibleMoves.clear()
                invalidate()
                return true
            }
            // Тапнули по своей фигуре — перевыбираем
            if (isOwnPiece(piece, whiteTurn)) {
                selectedRow = row
                selectedCol = col
                possibleMoves = getMovesFor(row, col)
                invalidate()
                return true
            }
            // Тапнули по пустой клетке / врагу, но ход невозможен — снимаем выделение
            selectedRow = -1
            selectedCol = -1
            possibleMoves.clear()
            invalidate()
            return true
        }

        // Ничего не выбрано — выбираем свою фигуру
        if (isOwnPiece(piece, whiteTurn)) {
            selectedRow = row
            selectedCol = col
            possibleMoves = getMovesFor(row, col)
            invalidate()
        }

        return true
    }

    // Делаем ход
    private fun makeMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val piece = board[fromRow][fromCol] ?: return

        // Перемещаем фигуру
        board[toRow][toCol] = piece
        board[fromRow][fromCol] = null

        // Превращение пешки в ферзя (упрощённо)
        if (piece == 'P' && toRow == 0) board[toRow][toCol] = 'Q'
        if (piece == 'p' && toRow == 7) board[toRow][toCol] = 'q'

        // Меняем ход
        whiteTurn = !whiteTurn
        onTurnChanged?.invoke(whiteTurn)
    }
}
