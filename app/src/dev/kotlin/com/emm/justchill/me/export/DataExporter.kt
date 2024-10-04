package com.emm.justchill.me.export

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.emm.justchill.me.daily.domain.Daily
import com.emm.justchill.me.daily.domain.DailyRepository
import com.emm.justchill.me.driver.domain.Driver
import com.emm.justchill.me.driver.domain.DriverRepository
import com.emm.justchill.me.loan.domain.Loan
import com.emm.justchill.me.loan.domain.LoanRepository
import com.emm.justchill.me.payment.domain.Payment
import com.emm.justchill.me.payment.domain.PaymentRepository
import com.emm.justchill.me.payment.domain.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataExporter(
    private val context: Context,
    private val driverRepository: DriverRepository,
    private val dailyRepository: DailyRepository,
    private val loanRepository: LoanRepository,
    private val paymentRepository: PaymentRepository,
) {

    private val defaultFileName = "xxx.json"

    suspend fun export() = withContext(Dispatchers.Default) {
        val jsonString: String = generateJsonFromSqlDelight()

        val target: Uri = targetUri()

        val existingFileUri: Uri? = existingFileUri(target)
        existingFileUri?.let {
            context.contentResolver.delete(it, null, null)
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, defaultFileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uriOfFileCreated: Uri? = context.contentResolver.insert(target, values)

        uriOfFileCreated?.let {
            try {
                context.contentResolver.openOutputStream(it).use { outputStream ->
                    outputStream?.write(jsonString.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun existingFileUri(target: Uri): Uri? {
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(defaultFileName)

        context.contentResolver.query(
            target,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return ContentUris.withAppendedId(target, id)
            }
        }
        return null
    }

    private fun targetUri(): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {

        MediaStore.Files.getContentUri("external")
    }

    private suspend fun generateJsonFromSqlDelight(): String {

        val drivers: List<DriverModel> = drivers()
        val payments: List<PaymentModel> = payments()
        val loans: List<LoanModel> = loans()
        val dailies: List<DailyModel> = dailies()

        val exportModel = ExportModel(
            drivers = drivers,
            payments = payments,
            loans = loans,
            daily = dailies,
        )

        val networkJson = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        return networkJson.encodeToString(exportModel)
    }

    private suspend fun dailies() = dailyRepository.all().firstOrNull().orEmpty().map {
        DailyModel(
            dailyId = it.dailyId,
            amount = it.amount,
            dailyDate = it.dailyDate,
            driverId = it.driverId,
        )
    }

    private suspend fun loans() = loanRepository.all().firstOrNull().orEmpty().map {
        LoanModel(
            loanId = it.loanId,
            amount = it.amount,
            amountWithInterest = it.amountWithInterest,
            interest = it.interest,
            startDate = it.startDate,
            duration = it.duration,
            status = it.status,
            driverId = it.driverId,
        )
    }

    private suspend fun payments() = paymentRepository.all().firstOrNull().orEmpty().map {
        PaymentModel(
            paymentId = it.paymentId,
            loanId = it.loanId,
            dueDate = it.dueDate,
            amount = it.amount,
            status = it.status.name
        )
    }

    private suspend fun drivers() = driverRepository.all().firstOrNull().orEmpty().map {
        DriverModel(
            driverId = it.driverId,
            name = it.name
        )
    }

    suspend fun import(json: String): Unit = withContext(Dispatchers.Default) {
        val networkJson = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
        val exportModel = networkJson.decodeFromString<ExportModel>(json)
        insertDrivers(exportModel)
        insertDailies(exportModel)
        insertLoans(exportModel)
        insertPayments(exportModel)
    }

    private suspend fun insertPayments(exportModel: ExportModel) {
        val payments: List<Payment> = exportModel.payments.map {
            Payment(
                paymentId = it.paymentId,
                loanId = it.loanId,
                dueDate = it.dueDate,
                amount = it.amount,
                status = PaymentStatus.valueOf(it.status)
            )
        }
        paymentRepository.addAll(payments)
    }

    private suspend fun insertLoans(exportModel: ExportModel) {
        exportModel.loans.forEach {
            val newLoan = Loan(
                loanId = it.loanId,
                amount = it.amount,
                amountWithInterest = it.amountWithInterest,
                interest = it.interest,
                startDate = it.startDate,
                duration = it.duration,
                status = it.status,
                driverId = it.driverId
            )
            loanRepository.add(newLoan)
        }
    }

    private suspend fun insertDailies(exportModel: ExportModel) {
        exportModel.daily.forEach {
            val newDaily = Daily(
                dailyId = it.dailyId,
                amount = it.amount,
                dailyDate = it.dailyDate,
                driverId = it.driverId
            )
            dailyRepository.insert(newDaily)
        }
    }

    private suspend fun insertDrivers(exportModel: ExportModel) {
        exportModel.drivers.forEach {
            val newDriver = Driver(driverId = it.driverId, name = it.name)
            driverRepository.insert(newDriver)
        }
    }
}