package com.rejner.remapomiary.generator.helpers;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.rejner.remapomiary.generator.constants.ProFonts;

public class TableHeaders {
    private TableHeaders() {
    }

    public static PdfPCell createHeader(String h) {
        Phrase phrase;
        if (h.startsWith("R(")) {
            int start = h.indexOf('(');
            int end = h.indexOf(')');
            String main = h.substring(0, start);
            String sub = h.substring(start + 1, end);
            Chunk mainChunk = new Chunk(main, ProFonts.medium);
            Chunk subChunk = new Chunk(sub, ProFonts.small);
            subChunk.setTextRise(-1f);
            Chunk unit;
            if (sub.equals("W")) {
                unit = new Chunk("\n[MΩ]", ProFonts.mediumNotBold);
            } else {
                unit = new Chunk("\n[GΩ]", ProFonts.mediumNotBold);
            }
            phrase = new Phrase();
            phrase.add(mainChunk);
            phrase.add(subChunk);
            phrase.add(unit);
        } else {
            phrase = new Phrase(h, ProFonts.medium);
        }

        PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(5f);
        cell.setPaddingTop(5f);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return cell;
    }
}
