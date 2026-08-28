package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.Voter

object ShareUtils {

    fun formatSingleVoter(voter: Voter): String {
        val sb = StringBuilder()
        sb.append("📋 *DATOS DEL VOTANTE*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("👤 *NOMBRE:* ${voter.fullName}\n")
        if (voter.orderNumber.isNotBlank()) {
            sb.append("🔢 *ORDEN:* ${voter.orderNumber}\n")
        }
        sb.append("🆔 *CÉDULA:* ${voter.cedula}\n")
        sb.append("🏛️ *LOCAL:* ${voter.votingPlace.ifBlank { "Sin asignar" }}\n")
        if (voter.tableNumber.isNotBlank()) {
            sb.append("🗳️ *MESA:* ${voter.tableNumber}\n")
        }
        if (voter.address.isNotBlank()) {
            sb.append("📍 *DIRECCIÓN:* ${voter.address}\n")
        }
        if (voter.phone.isNotBlank()) {
            sb.append("📞 *TELÉFONO:* ${voter.phone}\n")
        }
        val status = if (voter.voted) "✅ VOTANTE" else "⏳ PENDIENTE"
        sb.append("📌 *ESTADO:* $status\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("_Sistema de Padrón Electoral 2026_")
        return sb.toString()
    }

    fun formatVotersForWhatsApp(
        title: String,
        voters: List<Voter>
    ): String {
        val sb = StringBuilder()
        sb.append("📋 *${title.uppercase()}*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        voters.forEachIndexed { index, voter ->
            sb.append("*${index + 1}. ${voter.fullName}*\n")
            if (voter.orderNumber.isNotBlank()) {
                sb.append("  • *ORDEN:* ${voter.orderNumber}\n")
            }
            sb.append("  • *CÉDULA:* ${voter.cedula}\n")
            sb.append("  • *LOCAL:* ${voter.votingPlace.ifBlank { "Sin asignar" }}\n")
            if (voter.tableNumber.isNotBlank()) {
                sb.append("  • *MESA:* ${voter.tableNumber}\n")
            }
            val status = if (voter.voted) "✅ VOTANTE" else "⏳ PENDIENTE"
            sb.append("  • *ESTADO:* $status\n")
            if (index < voters.size - 1) {
                sb.append("────────────────────\n")
            }
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📊 *Total de Votantes:* ${voters.size}\n")
        val votedCount = voters.count { it.voted }
        sb.append("✅ *Votantes:* $votedCount | ⏳ *Pendientes:* ${voters.size - votedCount}\n")
        sb.append("🗳️ _Enviado desde Buscador de Votantes_")
        return sb.toString()
    }

    fun shareViaWhatsApp(context: Context, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(sendIntent)
        } catch (e: ActivityNotFoundException) {
            // If WhatsApp is not installed, open system share chooser
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(genericIntent, "Compartir Votantes")
            try {
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Toast.makeText(context, "No se pudo compartir la información", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareGenericText(context: Context, text: String, title: String = "Compartir Votantes") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo compartir", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdfFile(context: Context, uri: android.net.Uri, title: String = "Compartir Reporte PDF") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo compartir el archivo PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdfFile(context: Context, uri: android.net.Uri) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(viewIntent, "Abrir Documento PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró una aplicación para abrir PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareSpreadsheetFile(
        context: Context,
        uri: android.net.Uri,
        mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        title: String = "Compartir Planilla Excel"
    ) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo compartir la planilla Excel", Toast.LENGTH_SHORT).show()
        }
    }

    fun openSpreadsheetFile(
        context: Context,
        uri: android.net.Uri,
        mimeType: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(viewIntent, "Abrir Planilla Excel"))
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró una aplicación para abrir Excel", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareVcfFile(
        context: Context,
        uri: android.net.Uri,
        title: String = "Compartir Contactos (.vcf)"
    ) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/x-vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo compartir el archivo de contactos", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareVcfFile(
        context: Context,
        uri: android.net.Uri,
        count: Int,
        title: String = "Compartir Contactos (.vcf)"
    ) {
        shareVcfFile(context, uri, "$title ($count contactos)")
    }

    fun openVcfFile(context: Context, uri: android.net.Uri) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/x-vcard")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(viewIntent, "Importar a Contactos del Teléfono"))
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró una aplicación de contactos para abrir el archivo", Toast.LENGTH_SHORT).show()
        }
    }
}
