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

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import java.util.ArrayList;
import java.util.List;

public class FlatPageNumberEvent extends PdfPageEventHelper {

    private final List<PdfTemplate> templates = new ArrayList<>();
    private int currentPageForFlat = 0;
    private final Font footerFont;
    private String flat;
    private PdfContentByte cb;
    private boolean ignore;
    private Document document;

    private boolean isBlankPage = false;

    public FlatPageNumberEvent(Font footerFont, boolean ignore, Document document) {
        this.footerFont = footerFont;
        this.document = document;
        this.ignore = ignore;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        cb = writer.getDirectContent();
    }

    @Override
    public void onStartPage(PdfWriter writer, Document document) {
        if (isBlankPage || writer.getPageNumber() == 2) {
            return;
        }
        currentPageForFlat++;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (isBlankPage) {
            return;
        }

        if ((writer.getPageNumber() == 1 || writer.getPageNumber() == 2) && ignore) {
            return;
        }

        Rectangle pageSize = document.getPageSize();
        float yLine = 40f;
        float footerY = yLine - 12;
        float rightX = pageSize.getRight() - document.rightMargin();

        cb.setLineWidth(1f);
        cb.moveTo(document.left(), yLine);
        cb.lineTo(rightX, yLine);
        cb.stroke();

        String pageXText = flat + " | Strona " + (currentPageForFlat + 1) + "/"; // tutaj +1 bo zakładamy start od 0, ale to zależy od Twojej logiki

        if (footerFont.getBaseFont() != null) {
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
    }


    private void fillTemplates() {
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


    public void startNewFlat(String flat_, PdfWriter writer) {
        fillTemplates();

        int pagesInCurrentFlat = currentPageForFlat;

        document.newPage();

        if (pagesInCurrentFlat % 2 != 0) {
            this.isBlankPage = true;
            writer.setPageEmpty(false);
            document.newPage();
            this.isBlankPage = false;
        }

        templates.clear();
        flat = flat_;

        currentPageForFlat = 0;
    }


    public void finishDocument(PdfWriter writer) {
        fillTemplates();

        int pagesInCurrentFlat = currentPageForFlat;

        if (pagesInCurrentFlat % 2 != 0) {
            document.newPage();
            this.isBlankPage = true;
            writer.setPageEmpty(false);
            document.newPage();
            this.isBlankPage = false;
        }
    }
}
