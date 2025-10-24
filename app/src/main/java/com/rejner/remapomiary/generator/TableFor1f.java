package com.rejner.remapomiary.generator;

import static com.rejner.remapomiary.generator.helpers.CellGenerator.createCell;

import androidx.annotation.NonNull;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.generator.constants.ProFonts;
import com.rejner.remapomiary.generator.helpers.TableHeaders;

import java.util.ArrayList;
import java.util.List;

public class TableFor1f {
    private final AppDatabase db;

    public TableFor1f(AppDatabase db) {
        this.db = db;
    }

    public PdfPTable createMeasurementTableFor1f(List<Circuit> circuits, Flat flat) throws DocumentException {
        PdfPTable table;
        String[] headers;

        boolean isTNC = flat.type.equals("TN-C");

        if (isTNC) {
            table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 6f, 2f, 2f, 4f});
            headers = new String[]{
                    "Lp.", "Nazwa obwodu",
                    "R(L-N)",
                    "R(W)", "Ocena pomiaru"
            };
        } else {
            table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 6f, 2f, 2f, 2f, 2f, 4f});
            headers = new String[]{
                    "Lp.", "Nazwa obwodu",
                    "R(L-PE)", "R(L-N)", "R(N-PE)",
                    "R(W)", "Ocena pomiaru"
            };
        }

        // --- Nagłówki ---
        for (String h : headers) {
            PdfPCell cell = TableHeaders.createHeader(h);

            table.addCell(cell);
        }

        int index = 1;
        for (Circuit c : circuits) {
            List<String> values = new ArrayList<>();

            if (isTNC) {
                values.add(Integer.toString(index));
                values.add(c.name);
                values.add(">2");
                values.add("1");
                values.add("Pozytywna");
            } else {
                values.add(Integer.toString(index));
                values.add(c.name);
                values.add(">2");
                values.add(">2");
                values.add(">2");
                values.add("1");
                values.add("Pozytywna");
            }
            for (String v : values) {
                table.addCell(createCell(v));
            }

            index++;
        }

        return table;
    }
}
