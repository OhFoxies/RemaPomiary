package com.rejner.remapomiary.generator.helpers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class FooterPageEventManual extends PdfPageEventHelper {

    private BaseFont bf;
    private Font font;
    private int manualPageNumber = 1;
    private int maxPages = 1;

    public FooterPageEventManual(BaseFont bf) {
        this.bf = bf;
        this.font = new Font(bf, 10);
    }

    public void setManualPageNumber(int pageNumber) {
        this.manualPageNumber = pageNumber;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        float pageWidth = document.getPageSize().getWidth();
        float yLine = document.bottomMargin() - 10;

        cb.setLineWidth(1f);
        cb.moveTo(document.leftMargin(), yLine);
        cb.lineTo(pageWidth - document.rightMargin(), yLine);
        cb.stroke();

        String pageText = "Strona " + manualPageNumber + "/" + maxPages;
        ColumnText.showTextAligned(
                cb,
                Element.ALIGN_RIGHT,
                new Phrase(pageText, font),
                pageWidth - document.rightMargin(),
                yLine - 12,
                0
        );
    }
}
