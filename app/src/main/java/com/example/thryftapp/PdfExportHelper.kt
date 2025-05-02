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
        val document = PdfDocument() //create pdf document
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() //define page size
        val page = document.startPage(pageInfo) //start page

        val canvas: Canvas = page.canvas //get canvas
        val paint = Paint() //init paint
        paint.textSize = 14f //set font size

        var y = 40 //initial y position
        canvas.drawText("Transaction Statement", 200f, y.toFloat(), paint) //title
        y += 30

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) //format date

        transactions.forEachIndexed { index, transaction ->
            if (y > 800) {
                document.finishPage(page) //finish current page
                val newPage = document.startPage(pageInfo) //start new page
                y = 40
                canvas.drawText("Page ${index + 1}", 10f, y.toFloat(), paint) //page number
                y += 30
            }

            val line = "${dateFormat.format(transaction.date)} - ${transaction.description} - R${"%.2f".format(transaction.amount)} - ${transaction.type}" //format line
            canvas.drawText(line, 40f, y.toFloat(), paint) //draw line
            y += 20
        }

        document.finishPage(page) //finish final page

        val fileName = "transactions_${System.currentTimeMillis()}.pdf" //generate file name
        val file = File(context.getExternalFilesDir(null), fileName) //create file
        document.writeTo(FileOutputStream(file)) //write to file
        document.close() //close document

        return file //return file
    }
}
