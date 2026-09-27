package com.ivangames.chessbox

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Размер доски
    private val boardSize = 8

    // Текущая позиция
    // null = пустая клетка
    // 'P' = белая пешка, 'p' = чёрная пешка
    // 'R' = белая ладья, 'r' = чёрная
    // 'N' = белый конь, 'n' = чёрный
    // 'B' = белый слон, 'b' = чёрный
    // 'Q' = белый ферзь, 'q' = чёрный
    // 'K' = белый король, 'k' = чёрный
    private var board: Array<Array<Char?>> = Array(boardSize) { Array(boardSize) { null } }

    // Выбранная клетка (для подсветки)
    private var selectedRow = -1
    private var selectedCol = -1

    // Размеры и координаты
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
    private val highlightPaint = Paint().apply {
        color = Color.parseColor("#FFFF66")
        style = Paint.Style.FILL
        alpha = 180
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

    init {
        setupInitialPosition()
    }

    // Начальная расстановка фигур
    private fun setupInitialPosition() {
        // Чёрные фигуры (сверху)
        board[0][0] = 'r'; board[0][1] = 'n'; board[0][2] = 'b'; board[0][3] = 'q'
        board[0][4] = 'k'; board[0][5] = 'b'; board[0][6] = 'n'; board[0][7] = 'r'
        for (col in 0 until 8) board[1][col] = 'p'

        // Белые фигуры (снизу)
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
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Фон
        canvas.drawColor(Color.parseColor("#1A1A1A"))

        // Рисуем клетки
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val left = boardLeft + col * cellSize
                val top = boardTop + row * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                // Цвет клетки
                val paint = if ((row + col) % 2 == 0) lightCellPaint else darkCellPaint
                canvas.drawRect(left, top, right, bottom, paint)

                // Подсветка выбранной клетки
                if (row == selectedRow && col == selectedCol) {
                    canvas.drawRect(left, top, right, bottom, highlightPaint)
                }
            }
        }

        // Рисуем фигуры
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val piece = board[row][col] ?: continue
                drawPiece(canvas, row, col, piece)
            }
        }

        // Рамка доски
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

    // Преобразование символа в Unicode-фигуру
    private fun getPieceSymbol(piece: Char): String {
        return when (piece) {
            'K' -> "\u2654" // ♔ белый король
            'Q' -> "\u2655" // ♕ белый ферзь
            'R' -> "\u2656" // ♖ белая ладья
            'B' -> "\u2657" // ♗ белый слон
            'N' -> "\u2658" // ♘ белый конь
            'P' -> "\u2659" // ♙ белая пешка
            'k' -> "\u265A" // ♚ чёрный король
            'q' -> "\u265B" // ♛ чёрный ферзь
            'r' -> "\u265C" // ♜ чёрная ладья
            'b' -> "\u265D" // ♝ чёрный слон
            'n' -> "\u265E" // ♞ чёрный конь
            'p' -> "\u265F" // ♟ чёрная пешка
            else -> ""
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val col = ((event.x - boardLeft) / cellSize).toInt()
            val row = ((event.y - boardTop) / cellSize).toInt()

            if (row in 0 until boardSize && col in 0 until boardSize) {
                if (selectedRow == row && selectedCol == col) {
                    // Повторный тап по той же клетке — снимаем выделение
                    selectedRow = -1
                    selectedCol = -1
                } else {
                    // Выделяем клетку
                    selectedRow = row
                    selectedCol = col
                }
                invalidate()
            }
        }
        return true
    }
}
