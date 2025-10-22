package com.rejner.remapomiary.generator;

import static com.rejner.remapomiary.generator.helpers.CellGenerator.createCell;

import androidx.annotation.NonNull;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.generator.constants.ProFonts;
import com.rejner.remapomiary.generator.helpers.TableHeaders;

import java.util.ArrayList;
import java.util.List;

public class TableFor3f {

    public PdfPTable createMeasurementTableFor3f(List<Circuit> circuits, Flat flat) throws DocumentException {
        PdfPTable table;
        String[] headers;

        boolean isTNC = flat.type.equals("TN-C");

        if (isTNC) {
            table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 6f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 4f});
            headers = new String[]{
                    "Lp.", "Nazwa obwodu",
                    "R(L1-L2)", "R(L2-L3)", "R(L3-L1)",
                    "R(L1-N)", "R(L2-N)", "R(L3-N)",
                    "R(W)", "Ocena pomiaru"
            };
        } else {
            table = new PdfPTable(14);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 6f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 4f});
            headers = new String[]{
                    "Lp.", "Nazwa obwodu",
                    "R(L1-L2)", "R(L2-L3)", "R(L3-L1)",
                    "R(L1-PE)", "R(L2-PE)", "R(L3-PE)",
                    "R(L1-N)", "R(L2-N)", "R(L3-N)",
                    "R(N-PE)", "R(W)", "Ocena pomiaru"
            };
        }

        for (String h : headers) {
            PdfPCell cell = TableHeaders.createHeader(h);
            table.addCell(cell);
        }

        int index = 1;

        for (Circuit c : circuits) {
            List<String> values = new ArrayList<>();

            values.add(Integer.toString(index));
            values.add(c.name); // 2
            if (isTNC) {
                for (int i = 0; i < 6; i++) values.add(">2");
            } else {
                for (int i = 0; i < 10; i++) values.add(">2");
            }
            values.add("1");
            values.add("Pozytywna");

            for (String v : values) {
                table.addCell(createCell(v));
            }

            index++;
        }
        return table;
    }


}
