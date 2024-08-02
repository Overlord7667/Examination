package com.betelgeuse.corp.examination.documents_build

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.betelgeuse.corp.examination.adapters.PhotoAdapter
import com.betelgeuse.corp.examination.dao.PhotoEntity
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STOnOff1
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger

class DocumentHelper(private val context: Context, private val adapter: PhotoAdapter) {

    fun collectDataForExport(): List<String> {
        val dataToExport = mutableListOf<String>()
        val photos = adapter.getItems()

        for (photo in photos) {
            val comment = photo.comment ?: ""
            dataToExport.add(comment)
        }

        return dataToExport
    }

    fun copyFileToInternalStorage(uri: Uri): Uri? {
        val destinationPath = File(context.filesDir, "images")
        if (!destinationPath.exists()) {
            destinationPath.mkdirs()
        }

        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(destinationPath, fileName)

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", destinationFile)
            Log.d("DocumentHelper", "File URI: $fileUri")
            fileUri
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun addDataToDocument(doc: XWPFDocument, dataToExport: List<String>, photos: List<PhotoEntity>) {
        // Установить альбомную ориентацию и размер страницы A4
        val section = doc.document.body.addNewSectPr()
        val pageSize = section.addNewPgSz()
        pageSize.orient = STPageOrientation.LANDSCAPE
        pageSize.w = BigInteger.valueOf(16840)
        pageSize.h = BigInteger.valueOf(11906)

        // Установить поля страницы
        val pageMar = section.addNewPgMar()
        pageMar.left = BigInteger.valueOf(1440)
        pageMar.right = BigInteger.valueOf(1440)
        pageMar.top = BigInteger.valueOf(1800)
        pageMar.bottom = BigInteger.valueOf(480)

        // Создать таблицу
        val table = doc.createTable()

        // Удалить первую пустую строку
        if (table.numberOfRows > 0) {
            table.removeRow(0)
        }

        // Установить ширину таблицы и выравнивание
        val tableCT = table.ctTbl
        val tablePr = tableCT.tblPr ?: tableCT.addNewTblPr()
        val tableWidth = tablePr.addNewTblW()
        tableWidth.w = BigInteger.valueOf(16840 - 2 * 1440)
        tableWidth.type = STTblWidth.DXA

        // Установить ширину каждой колонки
        val columnWidths = listOf(2948, 2665, 4026, 1588, 9014, 3799, 3799)
        val cttblgrid = tableCT.tblGrid ?: tableCT.addNewTblGrid()
        cttblgrid.gridColList.clear()
        columnWidths.forEach { width ->
            cttblgrid.addNewGridCol().w = BigInteger.valueOf(width.toLong())
        }

        // Создание заголовка таблицы (шапка)
        val headerRow = table.createRow()
        val headers = listOf("Комната", "Объект", "Дефект", "Вид работ", "Норматив", "Фото 1", "Фото 2")
        headers.forEachIndexed { index, header ->
            val cell = if (index < headerRow.tableCells.size) headerRow.getCell(index) else headerRow.createCell()
            val para = cell.paragraphs[0]
            para.runs.forEach { para.removeRun(0) }

            val run = para.createRun()
            run.isBold = true
            run.fontFamily = "Times New Roman"
            run.fontSize = 12
            run.setText(header)
            para.alignment = ParagraphAlignment.CENTER

            val cellCT = cell.ctTc
            val tcPr = cellCT.addNewTcPr()
            val shd = tcPr.addNewShd()
            shd.fill = "D3D3D3"
            shd.setVal(STShd.CLEAR)

            val cttc = headerRow.ctRow
            val trPr = cttc.addNewTrPr()
            val trHeight = trPr.addNewTrHeight()
            trHeight.`val` = BigInteger.valueOf(700)
        }

        val cttc = headerRow.ctRow
        val trPr = cttc.addNewTrPr()
        trPr.addNewTblHeader().`val` = STOnOff1.ON

        // Добавить строки с данными
        for (photo in photos) {
            val typeRoom = photo.typeRoom ?: ""
            val buildObject = photo.buildObject ?: ""
            val comment = photo.comment ?: ""
            val typeWork = photo.typeWork ?: ""
            val buildSpText = photo.buildSpText ?: ""

            val row = table.createRow()
            listOf(typeRoom, buildObject, comment).forEachIndexed { index, text ->
                val cell = if (index < row.tableCells.size) row.getCell(index) else row.createCell()
                val para = cell.paragraphs[0]
                para.runs.forEach { para.removeRun(0) }

                val run = para.createRun()
                run.fontFamily = "Times New Roman"
                run.fontSize = 12
                run.setText(text)
            }

            // Центрируем текст в ячейке с данными typeWork
            val typeWorkCell = if (row.tableCells.size > 3) row.getCell(3) else row.createCell()
            val typeWorkPara = typeWorkCell.paragraphs[0]
            typeWorkPara.runs.forEach { typeWorkPara.removeRun(0) }
            val typeWorkRun = typeWorkPara.createRun()
            typeWorkRun.fontFamily = "Times New Roman"
            typeWorkRun.fontSize = 12
            typeWorkRun.setText(typeWork)
            typeWorkPara.alignment = ParagraphAlignment.CENTER

            // Ячейка с данными buildSpText
            val buildSpTextCell = if (row.tableCells.size > 4) row.getCell(4) else row.createCell()
            val buildSpTextPara = buildSpTextCell.paragraphs[0]
            buildSpTextPara.runs.forEach { buildSpTextPara.removeRun(0) }
            val buildSpTextRun = buildSpTextPara.createRun()
            buildSpTextRun.fontFamily = "Times New Roman"
            buildSpTextRun.fontSize = 12
            buildSpTextRun.setText(buildSpText)

            // Ячейка с первым изображением
            val firstImageUri = photo.imageUri
            val firstImageCell = if (row.tableCells.size > 5) row.getCell(5) else row.createCell()
            if (firstImageUri != null) {
                addImageToCell(firstImageCell, firstImageUri)
            }

            // Ячейка со вторым изображением
            val secondImageUri = photo.imageUri2
            val secondImageCell = if (row.tableCells.size > 6) row.getCell(6) else row.createCell()
            if (secondImageUri != null) {
                addImageToCell(secondImageCell, secondImageUri)
            }

            for (i in 0 until row.tableCells.size) {
                val cell = row.getCell(i)
                if (i < columnWidths.size) {
                    val cellWidth = columnWidths[i]
                    cell.ctTc.addNewTcPr().addNewTcW().w = BigInteger.valueOf(cellWidth.toLong())
                }
            }
        }
    }

    private fun addImageToCell(cell: XWPFTableCell, imageUri: String) {
        try {
            val uri = Uri.parse(imageUri)
            val localUri = copyFileToInternalStorage(uri) ?: return
            val inputStream = context.contentResolver.openInputStream(localUri)
            if (inputStream != null) {
                // Добавление изображения
                val imageParagraph = cell.addParagraph()
                val imageRun = imageParagraph.createRun()
                val pictureType = when (imageUri.substringAfterLast('.').lowercase()) {
                    "jpeg", "jpg" -> XWPFDocument.PICTURE_TYPE_JPEG
                    "png" -> XWPFDocument.PICTURE_TYPE_PNG
                    else -> XWPFDocument.PICTURE_TYPE_JPEG
                }
                val imageFileName = localUri.lastPathSegment
                imageRun.addPicture(inputStream, pictureType, imageFileName, Units.toEMU(140.0), Units.toEMU(140.0))
                inputStream.close()

                // Центрирование изображения
                imageParagraph.alignment = ParagraphAlignment.CENTER

                // Добавление отступа после изображения
                val spacingParagraph = cell.addParagraph()
                val spacingRun = spacingParagraph.createRun()
                spacingRun.addCarriageReturn() // Создание нового параграфа для отступа
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("DocumentHelper", "Exception: ${e.message}")
        }
    }
}