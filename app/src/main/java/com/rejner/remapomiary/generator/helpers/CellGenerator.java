package com.rejner.remapomiary.generator.helpers;

import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.rejner.remapomiary.generator.constants.ProFonts;

public class CellGenerator {

    private CellGenerator() {
    }

    public static PdfPCell createLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, ProFonts.fontNormalBold));

        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPaddingBottom(12f);
        return cell;
    }

    public static PdfPCell createValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, ProFonts.fontNormalBold));

        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPaddingBottom(12f);
        return cell;
    }

    public static PdfPCell createCell(String data) {
        PdfPCell cell = new PdfPCell(new Phrase(data, ProFonts.medium));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(5f);
        cell.setPaddingTop(5f);
        return cell;
    }
}
