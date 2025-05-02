package com.example.thryftapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfExportHelper(private val context: Context) {

    fun createTransactionPdf(transactions: List<Transaction>): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint = Paint()
        paint.textSize = 14f

        var y = 40
        canvas.drawText("Transaction Statement", 200f, y.toFloat(), paint)
        y += 30

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        transactions.forEachIndexed { index, transaction ->
            if (y > 800) {
                document.finishPage(page)
                val newPage = document.startPage(pageInfo)
                y = 40
                canvas.drawText("Page ${index + 1}", 10f, y.toFloat(), paint)
                y += 30
            }

            val line = "${dateFormat.format(transaction.date)} - ${transaction.description} - R${"%.2f".format(transaction.amount)} - ${transaction.type}"
            canvas.drawText(line, 40f, y.toFloat(), paint)
            y += 20
        }

        document.finishPage(page)

        val fileName = "transactions_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        document.writeTo(FileOutputStream(file))
        document.close()

        return file
    }
}
