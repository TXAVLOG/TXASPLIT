/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAExportManager.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.export

import android.content.Context
import android.os.Environment
import ke.txasplit.vk.data.dao.TXABillDao
import ke.txasplit.vk.data.dao.TXAPaymentDao
import ke.txasplit.vk.data.dao.TXAMemberDao
import ke.txasplit.vk.data.entity.TXABillEntity
import ke.txasplit.vk.data.entity.TXAPaymentEntity
import ke.txasplit.vk.data.entity.TXAMemberEntity
import ke.txasplit.vk.txa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for exporting data to CSV and Excel formats
 * Provides detailed reports for bills and payments
 */
@Singleton
class TXAExportManager @Inject constructor(
    private val billDao: TXABillDao,
    private val paymentDao: TXAPaymentDao,
    private val memberDao: TXAMemberDao
) {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * Export bills to CSV format
     * @param context Application context
     * @param groupId Group ID (optional, if null exports all groups)
     * @param startDate Start date filter
     * @param endDate End date filter
     * @return File path of exported CSV
     */
    suspend fun exportBillsToCSV(
        context: Context,
        groupId: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bills = getBillsForExport(groupId, startDate, endDate)
            val fileName = "TXASplit_Bills_${fileDateFormat.format(Date())}.csv"
            val file = createExportFile(context, fileName)
            
            file.bufferedWriter().use { writer ->
                // Write CSV header
                writer.write("Bill ID,Group Name,Title,Description,Category,Total Amount,Paid Amount,Status,Due Date,Created By,Created At\n")
                
                // Write bill data
                for (bill in bills) {
                    val group = billDao.getGroupForBill(bill.id)
                    val groupName = group?.name ?: "Unknown"
                    
                    writer.write("${escapeCSV(bill.id)},")
                    writer.write("${escapeCSV(groupName)},")
                    writer.write("${escapeCSV(bill.title)},")
                    writer.write("${escapeCSV(bill.description)},")
                    writer.write("${bill.category},")
                    writer.write("${bill.totalAmount},")
                    writer.write("${bill.paidAmount},")
                    writer.write("${bill.status},")
                    writer.write("${dateFormat.format(Date(bill.dueDate))},")
                    writer.write("${bill.createdBy},")
                    writer.write("${dateFormat.format(Date(bill.createdAt))}\n")
                }
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export payments to CSV format
     * @param context Application context
     * @param groupId Group ID (optional)
     * @param startDate Start date filter
     * @param endDate End date filter
     * @return File path of exported CSV
     */
    suspend fun exportPaymentsToCSV(
        context: Context,
        groupId: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payments = getPaymentsForExport(groupId, startDate, endDate)
            val fileName = "TXASplit_Payments_${fileDateFormat.format(Date())}.csv"
            val file = createExportFile(context, fileName)
            
            file.bufferedWriter().use { writer ->
                // Write CSV header
                writer.write("Payment ID,Bill Title,Payer Name,Amount,Payment Method,Status,Verification Status,Payment Date,Notes,Created At\n")
                
                // Write payment data
                for (payment in payments) {
                    val bill = billDao.getBillById(payment.billId)
                    val billTitle = bill?.title ?: "Unknown"
                    val payerName = getMemberDisplayName(payment.payerMemberId)
                    
                    writer.write("${escapeCSV(payment.id)},")
                    writer.write("${escapeCSV(billTitle)},")
                    writer.write("${escapeCSV(payerName)},")
                    writer.write("${payment.amount},")
                    writer.write("${payment.paymentMethod},")
                    writer.write("${payment.status},")
                    writer.write("${payment.verificationStatus},")
                    writer.write("${dateFormat.format(Date(payment.paymentDate))},")
                    writer.write("${escapeCSV(payment.notes)},")
                    writer.write("${dateFormat.format(Date(payment.createdAt))}\n")
                }
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export comprehensive report to Excel format
     * @param context Application context
     * @param groupId Group ID (optional)
     * @param startDate Start date filter
     * @param endDate End date filter
     * @return File path of exported Excel
     */
    suspend fun exportReportToExcel(
        context: Context,
        groupId: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "TXASplit_Report_${fileDateFormat.format(Date())}.xlsx"
            val file = createExportFile(context, fileName)
            
            val workbook = XSSFWorkbook()
            
            // Create sheets
            createBillsSheet(workbook, groupId, startDate, endDate)
            createPaymentsSheet(workbook, groupId, startDate, endDate)
            createSummarySheet(workbook, groupId, startDate, endDate)
            
            // Write to file
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export group summary to Excel
     * @param context Application context
     * @param groupId Group ID
     * @return File path of exported Excel
     */
    suspend fun exportGroupSummaryToExcel(
        context: Context,
        groupId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "TXASplit_GroupSummary_${fileDateFormat.format(Date())}.xlsx"
            val file = createExportFile(context, fileName)
            
            val workbook = XSSFWorkbook()
            
            // Create group-specific sheets
            createGroupOverviewSheet(workbook, groupId)
            createGroupMembersSheet(workbook, groupId)
            createGroupBillsSheet(workbook, groupId)
            createGroupPaymentsSheet(workbook, groupId)
            
            // Write to file
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create bills sheet in Excel
     */
    private suspend fun createBillsSheet(
        workbook: Workbook,
        groupId: String?,
        startDate: Long?,
        endDate: Long?
    ) {
        val sheet = workbook.createSheet("Bills")
        
        // Create header style
        val headerStyle = createHeaderStyle(workbook)
        
        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf(
            "Bill ID", "Group Name", "Title", "Description", "Category",
            "Total Amount", "Paid Amount", "Remaining", "Status", "Due Date",
            "Created By", "Created At"
        )
        
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        // Add data rows
        val bills = getBillsForExport(groupId, startDate, endDate)
        bills.forEachIndexed { index, bill ->
            val row = sheet.createRow(index + 1)
            val group = billDao.getGroupForBill(bill.id)
            val groupName = group?.name ?: "Unknown"
            
            row.createCell(0).setCellValue(bill.id)
            row.createCell(1).setCellValue(groupName)
            row.createCell(2).setCellValue(bill.title)
            row.createCell(3).setCellValue(bill.description)
            row.createCell(4).setCellValue(bill.category.toString())
            row.createCell(5).setCellValue(bill.totalAmount.toDouble())
            row.createCell(6).setCellValue(bill.paidAmount.toDouble())
            row.createCell(7).setCellValue((bill.totalAmount - bill.paidAmount).toDouble())
            row.createCell(8).setCellValue(bill.status.toString())
            row.createCell(9).setCellValue(dateFormat.format(Date(bill.dueDate)))
            row.createCell(10).setCellValue(bill.createdBy)
            row.createCell(11).setCellValue(dateFormat.format(Date(bill.createdAt)))
        }
        
        // Auto-size columns
        for (i in headers.indices) {
            sheet.autoSizeColumn(i)
        }
    }
    
    /**
     * Create payments sheet in Excel
     */
    private suspend fun createPaymentsSheet(
        workbook: Workbook,
        groupId: String?,
        startDate: Long?,
        endDate: Long?
    ) {
        val sheet = workbook.createSheet("Payments")
        
        val headerStyle = createHeaderStyle(workbook)
        
        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = arrayOf(
            "Payment ID", "Bill Title", "Payer Name", "Amount",
            "Payment Method", "Status", "Verification Status", "Payment Date",
            "Verified By", "Notes", "Created At"
        )
        
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        // Add data rows
        val payments = getPaymentsForExport(groupId, startDate, endDate)
        payments.forEachIndexed { index, payment ->
            val row = sheet.createRow(index + 1)
            val bill = billDao.getBillById(payment.billId)
            val billTitle = bill?.title ?: "Unknown"
            val payerName = getMemberDisplayName(payment.payerMemberId)
            
            row.createCell(0).setCellValue(payment.id)
            row.createCell(1).setCellValue(billTitle)
            row.createCell(2).setCellValue(payerName)
            row.createCell(3).setCellValue(payment.amount.toDouble())
            row.createCell(4).setCellValue(payment.paymentMethod.toString())
            row.createCell(5).setCellValue(payment.status.toString())
            row.createCell(6).setCellValue(payment.verificationStatus.toString())
            row.createCell(7).setCellValue(dateFormat.format(Date(payment.paymentDate)))
            row.createCell(8).setCellValue(payment.verifiedBy ?: "")
            row.createCell(9).setCellValue(payment.notes)
            row.createCell(10).setCellValue(dateFormat.format(Date(payment.createdAt)))
        }
        
        // Auto-size columns
        for (i in headers.indices) {
            sheet.autoSizeColumn(i)
        }
    }
    
    /**
     * Create summary sheet in Excel
     */
    private suspend fun createSummarySheet(
        workbook: Workbook,
        groupId: String?,
        startDate: Long?,
        endDate: Long?
    ) {
        val sheet = workbook.createSheet("Summary")
        
        val headerStyle = createHeaderStyle(workbook)
        
        // Summary statistics
        val bills = getBillsForExport(groupId, startDate, endDate)
        val payments = getPaymentsForExport(groupId, startDate, endDate)
        
        val totalBills = bills.size
        val totalAmount = bills.sumOf { it.totalAmount }
        val totalPaid = bills.sumOf { it.paidAmount }
        val totalRemaining = totalAmount - totalPaid
        
        // Create summary rows
        var rowIndex = 0
        
        // Title
        val titleRow = sheet.createRow(rowIndex++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("TXASplit Report Summary")
        titleCell.cellStyle = createTitleStyle(workbook)
        
        rowIndex++ // Empty row
        
        // Statistics
        createSummaryRow(sheet, rowIndex++, "Total Bills", totalBills.toString(), headerStyle)
        createSummaryRow(sheet, rowIndex++, "Total Amount", formatCurrency(totalAmount), headerStyle)
        createSummaryRow(sheet, rowIndex++, "Total Paid", formatCurrency(totalPaid), headerStyle)
        createSummaryRow(sheet, rowIndex++, "Total Remaining", formatCurrency(totalRemaining), headerStyle)
        
        rowIndex++ // Empty row
        
        // Category breakdown
        val categoryBreakdown = bills.groupBy { it.category }
            .map { (category, categoryBills) ->
                category to categoryBills.sumOf { it.totalAmount }
            }
            .sortedByDescending { it.second }
        
        createSummaryRow(sheet, rowIndex++, "Category Breakdown", "", headerStyle)
        categoryBreakdown.forEach { (category, amount) ->
            createSummaryRow(sheet, rowIndex++, category.toString(), formatCurrency(amount), null)
        }
        
        // Auto-size columns
        sheet.autoSizeColumn(0)
        sheet.autoSizeColumn(1)
    }
    
    /**
     * Create group overview sheet
     */
    private suspend fun createGroupOverviewSheet(workbook: Workbook, groupId: String) {
        val sheet = workbook.createSheet("Group Overview")
        
        // Implementation for group-specific overview
        // This would include group statistics, member counts, etc.
    }
    
    /**
     * Create group members sheet
     */
    private suspend fun createGroupMembersSheet(workbook: Workbook, groupId: String) {
        val sheet = workbook.createSheet("Group Members")
        
        // Implementation for group members export
    }
    
    /**
     * Create group bills sheet
     */
    private suspend fun createGroupBillsSheet(workbook: Workbook, groupId: String) {
        val sheet = workbook.createSheet("Group Bills")
        
        // Implementation for group bills export
    }
    
    /**
     * Create group payments sheet
     */
    private suspend fun createGroupPaymentsSheet(workbook: Workbook, groupId: String) {
        val sheet = workbook.createSheet("Group Payments")
        
        // Implementation for group payments export
    }
    
    /**
     * Helper methods
     */
    private suspend fun getBillsForExport(
        groupId: String?,
        startDate: Long?,
        endDate: Long?
    ): List<TXABillEntity> {
        return if (groupId != null) {
            billDao.getBillsInGroup(groupId)
        } else if (startDate != null && endDate != null) {
            billDao.getBillsByDateRange(startDate, endDate)
        } else {
            billDao.getAllActiveBills()
        }
    }
    
    private suspend fun getPaymentsForExport(
        groupId: String?,
        startDate: Long?,
        endDate: Long?
    ): List<TXAPaymentEntity> {
        return if (startDate != null && endDate != null) {
            paymentDao.getPaymentsByDateRange(startDate, endDate)
        } else {
            emptyList() // Get all payments if no date range
        }
    }
    
    private suspend fun getMemberDisplayName(memberId: String): String {
        // This needs proper implementation with memberDao
        return "Member $memberId"
    }
    
    private fun createExportFile(context: Context, fileName: String): File {
        val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "TXASplit")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return File(downloadsDir, fileName)
    }
    
    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    private fun formatCurrency(amount: Long): String {
        return String.format("%,d VND", amount)
    }
    
    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val headerFont = workbook.createFont().apply {
            bold = true
            color = IndexedColors.BLACK.getIndex()
        }
        return workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.getIndex()
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(headerFont)
            setBorder(BorderStyle.THIN, IndexedColors.BLACK.getIndex())
        }
    }
    
    private fun createTitleStyle(workbook: Workbook): CellStyle {
        val titleFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 16
            color = IndexedColors.BLUE.getIndex()
        }
        return workbook.createCellStyle().apply {
            setFont(titleFont)
        }
    }
    
    private fun CellStyle.setBorder(borderStyle: BorderStyle, color: Short) {
        borderTop = borderStyle
        borderBottom = borderStyle
        borderLeft = borderStyle
        borderRight = borderStyle
        topBorderColor = color
        bottomBorderColor = color
        leftBorderColor = color
        rightBorderColor = color
    }
    
    private fun createSummaryRow(sheet: Sheet, rowIndex: Int, label: String, value: String, style: CellStyle?) {
        val row = sheet.createRow(rowIndex)
        val labelCell = row.createCell(0)
        labelCell.setCellValue(label)
        if (style != null) labelCell.cellStyle = style
        
        val valueCell = row.createCell(1)
        valueCell.setCellValue(value)
        if (style != null) valueCell.cellStyle = style
    }
}
