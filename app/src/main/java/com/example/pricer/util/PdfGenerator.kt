package com.example.pricer.util

// Android framework imports
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.util.Log
// AndroidX imports
import androidx.core.graphics.withTranslation

// App-specific data models
import com.example.pricer.data.model.MultiplierType
import com.example.pricer.data.model.Quote
import com.example.pricer.data.model.QuoteItem

// Java IO/Util imports
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private const val TAG = "PdfGenerator"

    // --- Page Constants ---
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val MARGIN = 40f

    // Derived constants for convenience
    private const val CONTENT_WIDTH = PAGE_WIDTH - (2 * MARGIN)
    const val FOOTER_AREA_HEIGHT = 100f // Reserve space for totals + footer

    // --- TextPaint Definitions ---
     internal val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 20f; color = Color.BLACK; textAlign = Paint.Align.CENTER }
    internal val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 11f; color = Color.BLACK }
    internal val bodyTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textSize = 10f; color = Color.DKGRAY; textAlign = Paint.Align.LEFT }
    internal val bodyRightAlignPaint = TextPaint(bodyTextPaint).apply { textAlign = Paint.Align.RIGHT }
    internal val bodyBoldTextPaint = TextPaint(bodyTextPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.BLACK }
    internal val bodyBoldRightAlignPaint = TextPaint(bodyBoldTextPaint).apply { textAlign = Paint.Align.RIGHT }
    internal val linePaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 0.5f }

    // Helper result class for item portions
    // Helper data class for pre-calculating item layout
     data class ItemLayoutInfo(
        val spannable: SpannableString,
        val staticLayout: StaticLayout,
        val itemTextHeight: Float,
        val rowHeight: Float, // Includes padding/divider needed BELOW the content
        val baselineYAdjust: Float // Adjustment needed from rowStartY for numeric baselines
    )

    // --- Main PDF Generation Function (Handles Multiple Pages) ---
    fun generateQuotePdf(context: Context, quote: Quote, targetUri: Uri): Boolean {
        val pdfDocument = PdfDocument()
        val itemsToDraw = quote.items.toMutableList()
        var pageNumber = 1
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null // Use nullable Canvas
        var yPos = MARGIN
        var finalYPosOnLastItemPage = 0f

        try {
            // Start the first page
            currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = currentPage.canvas
            yPos = drawHeader(canvas, quote, MARGIN); yPos += 10f // Draw header on first page
            yPos = drawTableHeader(canvas, yPos) // Draw table header

            // --- Loop through items and handle pages ---
            var itemIndex = 0 // Use index to avoid concurrent modification issues
            while (itemIndex < itemsToDraw.size) {
                val item = itemsToDraw[itemIndex]

                // Pre-calculate item layout and needed height
                val layoutInfo = calculateItemLayoutInfo(item)
                val totalRowNeeded = layoutInfo.rowHeight // This now includes divider spacing

                // Check if item fits on CURRENT page
                val pageContentBottom = PAGE_HEIGHT - MARGIN - FOOTER_AREA_HEIGHT
                if (yPos + totalRowNeeded > pageContentBottom) {
                    // Doesn't fit: Finish current page, Start new one
                    pdfDocument.finishPage(currentPage)
                    pageNumber++
                    Log.d(TAG, "Starting page $pageNumber for item '${item.product.name}'")
                    currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = currentPage.canvas
                    yPos = MARGIN // Reset Y for new page
                    yPos = drawTableHeader(canvas, yPos) // Redraw table header

                    // Sanity check: If item STILL doesn't fit, it's too tall overall
                    if (yPos + totalRowNeeded > pageContentBottom) {
                        throw IllegalStateException("Item '${item.product.name}' ($totalRowNeeded) too tall for page.")
                    }
                }

                // --- Draw the item (guaranteed to fit) ---
                drawSingleItemRow(canvas!!, item, layoutInfo, yPos)

                // --- Advance Y pos for the next item ---
                yPos += totalRowNeeded

                // Move to the next item index
                itemIndex++

            } // End while loop for items

            // --- All items are drawn, now handle totals/footer ---
            finalYPosOnLastItemPage = yPos // Store the final Y on the last page items were on
// Check if totals fit on the current page
            if (finalYPosOnLastItemPage + FOOTER_AREA_HEIGHT > PAGE_HEIGHT - MARGIN) {
                // Doesn't fit, finish current page and start new page for totals
                Log.d(TAG,"Not enough space on page $pageNumber, finishing it.")
                pdfDocument.finishPage(currentPage!!)
                pageNumber++
                Log.d(TAG,"Starting new page $pageNumber for totals/footer.")
                currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = currentPage.canvas
                yPos = MARGIN + 20f // Reset Y for totals page
            } else {
                // Totals fit on current page, continue from finalYPosOnLastItemPage
                canvas = currentPage!!.canvas // Ensure we have the correct canvas reference
                yPos = finalYPosOnLastItemPage
                Log.d(TAG,"Drawing totals on existing page $pageNumber at y=$yPos")
            }

            // Draw Totals and Footer on the determined page
            canvas!!.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, linePaint.apply{ strokeWidth = 0.8f }); yPos += 10f
            yPos = drawTotals(canvas, quote, yPos); yPos += 30f
            bodyTextPaint.textAlign = Paint.Align.CENTER; canvas.drawText("Thank you!", (PAGE_WIDTH / 2f), yPos, bodyTextPaint); bodyTextPaint.textAlign = Paint.Align.LEFT

            // Finish the last page (which contains totals)
            pdfDocument.finishPage(currentPage!!)


            // --- Save Document ---
            context.contentResolver.openFileDescriptor(targetUri,"w")?.use{pfd->FileOutputStream(pfd.fileDescriptor).use{s->pdfDocument.writeTo(s)}}?:throw IOException("FD null")
            Log.i(TAG, "PDF saved: ${targetUri.path} (${pdfDocument.pages.size} pages)")
            return true

        } catch (e: Exception) { Log.e(TAG, "PDF Gen Error: ${quote.id}", e); return false }
        finally { pdfDocument.close() }
    } // End generateQuotePdf


    // --- Helper: Draws Header --- (Internal access)
    internal fun drawHeader(canvas: Canvas, quote: Quote, startY: Float): Float {
        var yPos=startY; canvas.drawText("Quote", (PAGE_WIDTH/2f), yPos, titlePaint); yPos+=30f
        val dateStr=SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date()); canvas.drawText("Date: $dateStr",PAGE_WIDTH-MARGIN,yPos, bodyRightAlignPaint)
        if(quote.companyName.isNotBlank()){ bodyBoldTextPaint.textAlign=Paint.Align.LEFT; canvas.drawText(quote.companyName,MARGIN,yPos, bodyBoldTextPaint) }
        yPos+=15f; bodyTextPaint.textAlign=Paint.Align.LEFT; canvas.drawText("Quote For:", MARGIN,yPos, bodyTextPaint)
        yPos+=12f; bodyBoldTextPaint.textAlign=Paint.Align.LEFT
        canvas.drawText(quote.customerName,MARGIN+10f,yPos, bodyBoldTextPaint); yPos+=15f; if(quote.customMessage.isNotBlank()){ bodyTextPaint.textAlign=Paint.Align.LEFT
            val ml=StaticLayout.Builder.obtain(quote.customMessage,0,quote.customMessage.length, bodyTextPaint,CONTENT_WIDTH.toInt()).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            canvas.withTranslation(MARGIN,yPos){ml.draw(this)}; yPos+=ml.height+10f; }
        canvas.drawLine(MARGIN,yPos,PAGE_WIDTH-MARGIN,yPos,linePaint.apply{strokeWidth=0.5f}); yPos+=10f; return yPos;

    }

    // --- Helper: Draws Table Header Only --- (Private access)
     fun drawTableHeader(canvas: Canvas, startY: Float): Float {
        var yPos = startY; val qtyColWidth=40f; val unitPriceColWidth=70f; val lineTotalColWidth=75f; val interColSpacing=8f; val lineTotalColX=PAGE_WIDTH-MARGIN-lineTotalColWidth; val unitPriceColX=lineTotalColX-interColSpacing-unitPriceColWidth; val qtyColX=unitPriceColX-interColSpacing-qtyColWidth; val itemColX=MARGIN;
        headerPaint.textAlign = Paint.Align.LEFT; canvas.drawText("Item / Description", itemColX, yPos, headerPaint); headerPaint.textAlign = Paint.Align.CENTER; canvas.drawText("Qty", qtyColX+qtyColWidth/2, yPos, headerPaint); headerPaint.textAlign = Paint.Align.RIGHT; canvas.drawText("Unit Price", unitPriceColX+unitPriceColWidth, yPos, headerPaint); canvas.drawText("Line Total", lineTotalColX+lineTotalColWidth, yPos, headerPaint);
        yPos += headerPaint.descent()-headerPaint.ascent()+5f; canvas.drawLine(MARGIN, yPos, PAGE_WIDTH-MARGIN, yPos, linePaint.apply{strokeWidth=0.75f}); yPos += 15f; return yPos;
    }

    // --- Helper: Draws Totals Section --- (Internal access)
    internal fun drawTotals(canvas: Canvas, quote: Quote, startY: Float): Float {
        var yPos=startY; val valueX=PAGE_WIDTH-MARGIN
        val labelX=valueX-120f
        val labelPaint=TextPaint(bodyTextPaint).apply{textAlign=Paint.Align.RIGHT}
        val boldLabelPaint=TextPaint(bodyBoldTextPaint).apply{textAlign=Paint.Align.RIGHT}
        val valuePaint=TextPaint(bodyRightAlignPaint)
        val boldValuePaint=TextPaint(bodyBoldRightAlignPaint)
        // --- UPDATED DRAW CALLS ---
        // Subtotal (Before Discount)
        canvas.drawText("Subtotal:", labelX, yPos, labelPaint)
        canvas.drawText(formatCurrency(quote.subtotalBeforeDiscount), valueX, yPos, valuePaint); yPos += 15f // Use subtotalBeforeDiscount

        // Discount (If applicable)
        if (quote.totalDiscountAmount > 0) {
            canvas.drawText("Discount (${formatPercentage(quote.globalDiscountRate)}):", labelX, yPos, labelPaint)
            canvas.drawText("-${formatCurrency(quote.totalDiscountAmount)}", valueX, yPos, valuePaint); yPos += 15f // Use totalDiscountAmount

            // Optional: Show Subtotal After Discount
            canvas.drawText("Subtotal (After Disc.):", labelX, yPos, labelPaint) // Use normal label paint
            canvas.drawText(formatCurrency(quote.subtotalAfterDiscount), valueX, yPos, valuePaint); yPos += 15f // Use subtotalAfterDiscount
        }

        // Tax (Calculated on subtotalAfterDiscount)
        canvas.drawText("Tax (${formatPercentage(quote.taxRate)}):", labelX, yPos, labelPaint)
        canvas.drawText(formatCurrency(quote.taxAmount), valueX, yPos, valuePaint); yPos += 15f // Use taxAmount

        // Grand Total (Based on subtotalAfterDiscount + taxAmount)
        canvas.drawText("Grand Total:", labelX, yPos, boldLabelPaint) // Use bold label paint
        canvas.drawText(formatCurrency(quote.grandTotal), valueX, yPos, boldValuePaint); yPos += 15f // Use grandTotal
        // --- END UPDATED DRAW CALLS ---
        return yPos;


    }


    // --- Helper: Calculates layout info and total needed row height for an item --- (Private)
    fun calculateItemLayoutInfo(item: QuoteItem): ItemLayoutInfo {
        val qtyColWidth=40f; val unitPriceColWidth=70f; val lineTotalColWidth=75f; val interColSpacing=8f
        val lineTotalColX=PAGE_WIDTH-MARGIN-lineTotalColWidth
        val unitPriceColX=lineTotalColX-interColSpacing-unitPriceColWidth; val qtyColX=unitPriceColX-interColSpacing-qtyColWidth
        val itemColWidth=qtyColX-interColSpacing-MARGIN
        if(itemColWidth <= 0) throw IllegalStateException("Invalid column width in calculateItemHeight")

        // Prepare text (using simplified build logic)
        val namePart = item.product.name
        var finalStringToDraw = namePart

        // Add description only ONCE
        if (item.product.description.isNotBlank()) {
            finalStringToDraw += "\n${item.product.description}"
        }

        // Add multipliers if any
        if (item.appliedMultipliers.isNotEmpty()) {
            val multipliersString = item.appliedMultipliers.joinToString("\n") { m->
                val v=when(m.type){
                    MultiplierType.PERCENTAGE->formatPercentage(m.appliedValue)
                    else->"${formatCurrency(m.appliedValue)}/unit"
                }
                "  + ${m.name} ($v)"
            }
            finalStringToDraw += "\n${multipliersString}"
        }

        // Apply bold span
        val spannable = SpannableString(finalStringToDraw)
        if (namePart.isNotEmpty()) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), 0, namePart.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }

        // Create StaticLayout to measure height
        bodyTextPaint.textAlign = Paint.Align.LEFT
        val layout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, bodyTextPaint, itemColWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        val itemTextHeight = layout.height.toFloat()

        // Calculate total row height needed (including divider spacing below)
        val rowHeightBase = itemTextHeight.coerceAtLeast(bodyTextPaint.textSize * 1.5f)
        val dividerPadding = 5f; val lineThickness = 1.0f; val belowDividerPadding = 5f
        val totalRowHeightNeeded = rowHeightBase + dividerPadding + lineThickness + belowDividerPadding

        // Add a safety check to prevent unreasonably large heights
        val maxReasonableHeight = PAGE_HEIGHT / 99f// No item should take more than half a page
        val safeRowHeight = totalRowHeightNeeded.coerceAtMost(maxReasonableHeight)

        // Calculate baseline adjustment relative to row start
        val baselineYAdjust = -bodyTextPaint.ascent() // Baseline relative to top = 0 - ascent

        return ItemLayoutInfo(spannable, layout, itemTextHeight, safeRowHeight, baselineYAdjust)
    }


    // --- Helper: Draws ONLY the content for a SINGLE item row --- (Private)
    fun drawSingleItemRow(
        canvas: Canvas,
        item: QuoteItem,
        layoutInfo: ItemLayoutInfo, // Pass pre-calculated info
        rowStartY: Float
    ) {
        // Define Column Geometry AGAIN (Or pass it into this function)
        val qtyColWidth=40f; val unitPriceColWidth=70f; val lineTotalColWidth=75f
        val interColSpacing=8f; val lineTotalColX=PAGE_WIDTH-MARGIN-lineTotalColWidth
        val unitPriceColX=lineTotalColX-interColSpacing-unitPriceColWidth; val qtyColX=unitPriceColX-interColSpacing-qtyColWidth
        val itemColX=MARGIN

        // Draw Item StaticLayout using pre-built layout
        canvas.withTranslation(itemColX, rowStartY) { layoutInfo.staticLayout.draw(this) }

        // Draw Numeric Columns using calculated baseline adjustment
        val textBaselineY = rowStartY + layoutInfo.baselineYAdjust
        bodyTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(item.quantity.toString(), qtyColX + qtyColWidth/2, textBaselineY, bodyTextPaint)
        canvas.drawText(formatCurrency(item.product.basePrice), unitPriceColX+unitPriceColWidth, textBaselineY, bodyRightAlignPaint)
        canvas.drawText(formatCurrency(item.lineTotalBeforeDiscount), lineTotalColX+lineTotalColWidth, textBaselineY, bodyBoldRightAlignPaint)

        // Draw Divider Line Below
        val dividerPadding = 5f; val lineThickness = 1.0f
        val rowHeightBase = layoutInfo.itemTextHeight.coerceAtLeast(bodyTextPaint.textSize*1.5f)
        val dividerY = rowStartY + rowHeightBase + dividerPadding
        canvas.drawLine(MARGIN, dividerY, PAGE_WIDTH-MARGIN, dividerY, linePaint.apply{strokeWidth=lineThickness})
    }

    // REMOVED original drawItemTable - functionality is in generateQuotePdf and helpers now
    // REMOVED drawItemTablePortion - functionality is in generateQuotePdf and helpers now

} // End PdfGenerator object