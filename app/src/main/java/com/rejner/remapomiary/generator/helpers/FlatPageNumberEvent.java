package com.rejner.remapomiary.generator.helpers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.rejner.remapomiary.data.entities.Flat;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa pomocnicza (event handler) do zarządzania numeracją stron
 * specyficzną dla każdego mieszkania (format "Strona X / Y").
 * Używa PdfTemplate, aby móc wpisać całkowitą liczbę stron (Y)
 * po wygenerowaniu wszystkich stron dla danego mieszkania.
 */

public class FlatPageNumberEvent extends PdfPageEventHelper {

    private final List<PdfTemplate> templates = new ArrayList<>();
    private int currentPageForFlat = 0;
    private final Font footerFont;
    private String flat;
    private PdfContentByte cb;

    public FlatPageNumberEvent(Font footerFont) {
        this.footerFont = footerFont;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        cb = writer.getDirectContent();
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        currentPageForFlat++; // zwiększamy przy każdej nowej stronie
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (writer.getPageNumber() == 1) {
            return; // nic nie rysujemy
        }
        Rectangle pageSize = document.getPageSize();
        float yLine = 40f;
        float footerY = yLine - 12;
        float rightX = pageSize.getRight() - document.rightMargin();

        cb.setLineWidth(1f);
        cb.moveTo(document.left(), yLine);
        cb.lineTo(rightX, yLine);
        cb.stroke();

        String pageXText = flat + " | Strona " + (currentPageForFlat + 1) + "/";
        BaseFont baseFont = footerFont.getBaseFont();
        float totalWidthEstimate = baseFont.getWidthPoint("999", footerFont.getSize());
        PdfTemplate totalPagesTemplate = cb.createTemplate(totalWidthEstimate, footerFont.getSize() + 2);
        templates.add(totalPagesTemplate);

        ColumnText.showTextAligned(
                cb,
                Element.ALIGN_RIGHT,
                new Phrase(pageXText, footerFont),
                rightX - totalWidthEstimate,
                footerY,
                0
        );

        cb.addTemplate(
                totalPagesTemplate,
                rightX - totalWidthEstimate,
                footerY
        );
    }

    /** Finalizacja poprzedniego mieszkania */
    public void startNewFlat(String flat_) {
        if (!templates.isEmpty()) {
            String totalPages = String.valueOf(currentPageForFlat);
            for (PdfTemplate template : templates) {
                ColumnText.showTextAligned(
                        template,
                        Element.ALIGN_LEFT,
                        new Phrase(totalPages, footerFont),
                        0, 0, 0
                );
            }
        }

        templates.clear();
        flat = flat_;
        currentPageForFlat = 0; // reset — nowa numeracja od 1

    }

    /** Finalizacja ostatniego mieszkania */
    public void finishDocument() {
        if (!templates.isEmpty()) {
            String totalPages = String.valueOf(currentPageForFlat);
            for (PdfTemplate template : templates) {
                ColumnText.showTextAligned(
                        template,
                        Element.ALIGN_LEFT,
                        new Phrase(totalPages, footerFont),
                        0, 0, 0
                );
            }
        }
    }
}
