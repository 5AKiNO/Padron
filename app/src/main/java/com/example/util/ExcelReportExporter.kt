package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.Voter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExcelFormat(val extension: String, val mimeType: String, val displayName: String) {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel Moderno (.xlsx)"),
    XLS("xls", "application/vnd.ms-excel", "Excel 97-2003 (.xls)"),
    CSV("csv", "text/csv", "CSV Delimitado (.csv)")
}

object ExcelReportExporter {

    private val COLUMNS = listOf(
        "CÉDULA" to 16.0,
        "NOMBRE COMPLETO" to 32.0,
        "LUGAR DE VOTACIÓN" to 30.0,
        "MESA" to 12.0,
        "N° ORDEN" to 12.0,
        "ESTADO VOTO" to 16.0,
        "TELÉFONO" to 18.0,
        "DIRECCIÓN" to 28.0,
        "CIUDAD / ZONA" to 20.0,
        "NOTAS / OBS" to 26.0,
        "ID REGISTRO" to 12.0,
        "ÚLTIMA ACTUALIZACIÓN" to 22.0
    )

    fun generateVotersSpreadsheet(
        context: Context,
        reportTitle: String,
        subtitle: String = "",
        voters: List<Voter>,
        format: ExcelFormat = ExcelFormat.XLSX
    ): Pair<Uri?, String>? {
        return when (format) {
            ExcelFormat.XLSX -> generateXlsx(context, reportTitle, subtitle, voters)?.let { Pair(it, format.mimeType) }
            ExcelFormat.XLS -> generateXls(context, reportTitle, subtitle, voters)?.let { Pair(it, format.mimeType) }
            ExcelFormat.CSV -> generateCsv(context, reportTitle, subtitle, voters)?.let { Pair(it, format.mimeType) }
        }
    }

    /**
     * Backward-compatible helper for existing callers
     */
    fun generateVotersExcel(
        context: Context,
        reportTitle: String,
        voters: List<Voter>
    ): Uri? {
        return generateXlsx(context, reportTitle, "Padrón Electoral Oficial", voters)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. GENERATE NATIVE .XLSX (OpenXML ZIP Standard)
    // ─────────────────────────────────────────────────────────────────────────────
    fun generateXlsx(
        context: Context,
        reportTitle: String,
        subtitle: String,
        voters: List<Voter>
    ): Uri? {
        val fileName = "padron_votantes_${System.currentTimeMillis()}.xlsx"
        val file = File(context.cacheDir, fileName)

        return try {
            val fos = FileOutputStream(file)
            val zos = ZipOutputStream(fos)

            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent()
            zos.write(contentTypesXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()
            zos.write(relsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()
            zos.write(wbRelsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Votantes" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent()
            zos.write(workbookXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. xl/styles.xml
            zos.putNextEntry(ZipEntry("xl/styles.xml"))
            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="11"/><name val="Calibri"/><color rgb="FF1E293B"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/><color rgb="FFFFFFFF"/></font>
    <font><b/><sz val="14"/><name val="Calibri"/><color rgb="FFB91C1C"/></font>
    <font><i/><sz val="10"/><name val="Calibri"/><color rgb="FF64748B"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFB91C1C"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/><bgColor indexed="64"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFE2E8F0"/></left>
      <right style="thin"><color rgb="FFE2E8F0"/></right>
      <top style="thin"><color rgb="FFE2E8F0"/></top>
      <bottom style="thin"><color rgb="FFE2E8F0"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="6">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>""".trimIndent()
            zos.write(stylesXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 6. xl/worksheets/sheet1.xml
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date())

            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

            // Column Widths
            sb.append("<cols>")
            for ((idx, col) in COLUMNS.withIndex()) {
                sb.append("""<col min="${idx + 1}" max="${idx + 1}" width="${col.second}" customWidth="1"/>""")
            }
            sb.append("</cols>")

            sb.append("<sheetData>")

            // Title Row
            val titleText = escapeXml(reportTitle.uppercase())
            sb.append("""<row r="1" ht="26" customHeight="1">""")
            sb.append("""<c r="A1" s="3" t="inlineStr"><is><t>$titleText</t></is></c>""")
            sb.append("</row>")

            // Subtitle & Date Row
            val sub = if (subtitle.isNotBlank()) escapeXml(subtitle) else "Reporte de Padrón Electoral"
            val totalVoted = voters.count { it.voted }
            val metaInfo = "$sub | Generado: $dateStr | Total: ${voters.size} | Votaron: $totalVoted | Pendientes: ${voters.size - totalVoted}"
            sb.append("""<row r="2" ht="20" customHeight="1">""")
            sb.append("""<c r="A2" s="4" t="inlineStr"><is><t>$metaInfo</t></is></c>""")
            sb.append("</row>")

            // Empty row
            sb.append("""<row r="3" ht="10" customHeight="1"/>""")

            // Header Row (Row 4)
            sb.append("""<row r="4" ht="26" customHeight="1">""")
            val colLetters = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L")
            for ((idx, col) in COLUMNS.withIndex()) {
                val ref = "${colLetters[idx]}4"
                sb.append("""<c r="$ref" s="1" t="inlineStr"><is><t>${escapeXml(col.first)}</t></is></c>""")
            }
            sb.append("</row>")

            // Data Rows (Row 5+)
            var rowIndex = 5
            for (v in voters) {
                val rowStyle = if (rowIndex % 2 == 0) 5 else 2
                val dateUpdate = if (v.updatedAt > 0) dateFormat.format(Date(v.updatedAt)) else "-"
                val rowData = listOf(
                    v.cedula,
                    v.fullName,
                    v.votingPlace.ifBlank { "-" },
                    v.tableNumber.ifBlank { "-" },
                    v.orderNumber.ifBlank { "-" },
                    if (v.voted) "VOTANTE" else "PENDIENTE",
                    v.phone.ifBlank { "-" },
                    v.address.ifBlank { "-" },
                    v.cityOrZone.ifBlank { "-" },
                    v.notes.ifBlank { "-" },
                    v.id.toString(),
                    dateUpdate
                )

                sb.append("""<row r="$rowIndex" ht="20" customHeight="1">""")
                for ((colIdx, value) in rowData.withIndex()) {
                    val ref = "${colLetters[colIdx]}$rowIndex"
                    sb.append("""<c r="$ref" s="$rowStyle" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
                }
                sb.append("</row>")
                rowIndex++
            }

            sb.append("</sheetData>")
            sb.append("</worksheet>")

            zos.write(sb.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.finish()
            zos.close()
            fos.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. GENERATE SPREADSHEETML .XLS (Compatible with all Excel 97-2024 versions)
    // ─────────────────────────────────────────────────────────────────────────────
    fun generateXls(
        context: Context,
        reportTitle: String,
        subtitle: String,
        voters: List<Voter>
    ): Uri? {
        val fileName = "padron_votantes_${System.currentTimeMillis()}.xls"
        val file = File(context.cacheDir, fileName)

        return try {
            val fos = FileOutputStream(file)
            val writer = OutputStreamWriter(fos, Charsets.UTF_8)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date())

            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
            sb.append("""<?mso-application progid="Excel.Sheet"?>""").append("\n")
            sb.append("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center"/>
   <Borders/>
   <Font ss:FontName="Calibri" ss:Size="11" ss:Color="#1E293B"/>
   <Interior/>
   <NumberFormat/>
   <Protection/>
  </Style>
  <Style ss:ID="TitleStyle">
   <Font ss:FontName="Calibri" ss:Size="14" ss:Color="#B91C1C" ss:Bold="1"/>
   <Alignment ss:Vertical="Center"/>
  </Style>
  <Style ss:ID="SubtitleStyle">
   <Font ss:FontName="Calibri" ss:Size="10" ss:Color="#64748B" ss:Italic="1"/>
   <Alignment ss:Vertical="Center"/>
  </Style>
  <Style ss:ID="HeaderStyle">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
   </Borders>
   <Font ss:FontName="Calibri" ss:Size="11" ss:Color="#FFFFFF" ss:Bold="1"/>
   <Interior ss:Color="#B91C1C" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="DataStyleEven">
   <Alignment ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Calibri" ss:Size="10" ss:Color="#1E293B"/>
   <Interior ss:Color="#F8FAFC" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="DataStyleOdd">
   <Alignment ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Calibri" ss:Size="10" ss:Color="#1E293B"/>
  </Style>
 </Styles>
 <Worksheet ss:Name="Padrón de Votantes">
  <Table>
""")

            // Column Widths in points
            for (col in COLUMNS) {
                sb.append("""   <Column ss:AutoFitWidth="0" ss:Width="${col.second * 7.5}"/>""").append("\n")
            }

            // Title
            val titleText = escapeXml(reportTitle.uppercase())
            sb.append("""   <Row ss:Height="26">
    <Cell ss:StyleID="TitleStyle"><Data ss:Type="String">$titleText</Data></Cell>
   </Row>""").append("\n")

            // Subtitle
            val sub = if (subtitle.isNotBlank()) escapeXml(subtitle) else "Padrón Electoral Oficial"
            val totalVoted = voters.count { it.voted }
            val metaInfo = "$sub | Fecha: $dateStr | Total: ${voters.size} | Votantes: $totalVoted | Pendientes: ${voters.size - totalVoted}"
            sb.append("""   <Row ss:Height="18">
    <Cell ss:StyleID="SubtitleStyle"><Data ss:Type="String">$metaInfo</Data></Cell>
   </Row>""").append("\n")

            // Empty row
            sb.append("""   <Row ss:Height="8"/>""").append("\n")

            // Headers
            sb.append("""   <Row ss:Height="24">""").append("\n")
            for (col in COLUMNS) {
                sb.append("""    <Cell ss:StyleID="HeaderStyle"><Data ss:Type="String">${escapeXml(col.first)}</Data></Cell>""").append("\n")
            }
            sb.append("""   </Row>""").append("\n")

            // Rows
            for ((index, v) in voters.withIndex()) {
                val styleId = if (index % 2 == 0) "DataStyleEven" else "DataStyleOdd"
                val dateUpdate = if (v.updatedAt > 0) dateFormat.format(Date(v.updatedAt)) else "-"
                val rowData = listOf(
                    v.cedula,
                    v.fullName,
                    v.votingPlace.ifBlank { "-" },
                    v.tableNumber.ifBlank { "-" },
                    v.orderNumber.ifBlank { "-" },
                    if (v.voted) "VOTANTE" else "PENDIENTE",
                    v.phone.ifBlank { "-" },
                    v.address.ifBlank { "-" },
                    v.cityOrZone.ifBlank { "-" },
                    v.notes.ifBlank { "-" },
                    v.id.toString(),
                    dateUpdate
                )

                sb.append("""   <Row ss:Height="18">""").append("\n")
                for (value in rowData) {
                    sb.append("""    <Cell ss:StyleID="$styleId"><Data ss:Type="String">${escapeXml(value)}</Data></Cell>""").append("\n")
                }
                sb.append("""   </Row>""").append("\n")
            }

            sb.append("""  </Table>
 </Worksheet>
</Workbook>""")

            writer.write(sb.toString())
            writer.flush()
            writer.close()
            fos.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. GENERATE CSV (UTF-8 with BOM)
    // ─────────────────────────────────────────────────────────────────────────────
    fun generateCsv(
        context: Context,
        reportTitle: String,
        subtitle: String,
        voters: List<Voter>
    ): Uri? {
        val fileName = "padron_votantes_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        return try {
            val fos = FileOutputStream(file)
            // UTF-8 BOM
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = OutputStreamWriter(fos, Charsets.UTF_8)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date())

            writer.write("# ${reportTitle.replace(";", " ")}\n")
            if (subtitle.isNotBlank()) writer.write("# ${subtitle.replace(";", " ")}\n")
            writer.write("# Fecha: $dateStr | Total: ${voters.size}\n\n")

            // Headers
            writer.write(COLUMNS.joinToString(";") { cleanForCsv(it.first) } + "\n")

            for (v in voters) {
                val dateUpdate = if (v.updatedAt > 0) dateFormat.format(Date(v.updatedAt)) else "-"
                val line = listOf(
                    cleanForCsv(v.cedula),
                    cleanForCsv(v.fullName),
                    cleanForCsv(v.votingPlace),
                    cleanForCsv(v.tableNumber),
                    cleanForCsv(v.orderNumber),
                    if (v.voted) "VOTANTE" else "PENDIENTE",
                    cleanForCsv(v.phone),
                    cleanForCsv(v.address),
                    cleanForCsv(v.cityOrZone),
                    cleanForCsv(v.notes),
                    v.id.toString(),
                    dateUpdate
                ).joinToString(";")

                writer.write(line + "\n")
            }

            writer.flush()
            writer.close()
            fos.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun cleanForCsv(text: String): String {
        val sanitized = text.replace(";", " ").replace("\n", " ").trim()
        return if (sanitized.contains("\"") || sanitized.contains(",")) {
            "\"" + sanitized.replace("\"", "\"\"") + "\""
        } else {
            sanitized
        }
    }
}
