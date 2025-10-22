package com.rejner.remapomiary.generator;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.generator.constants.BreakerB;
import com.rejner.remapomiary.generator.constants.BreakerC;
import com.rejner.remapomiary.generator.constants.BreakerGg;
import com.rejner.remapomiary.generator.constants.ProFonts;
import com.rejner.remapomiary.generator.helpers.CellGenerator;

import java.util.ArrayList;
import java.util.List;

public class OmTable {
    AppDatabase db;
    private final ArrayList<String> mistakes;
    private int grade = 0;

    public OmTable(AppDatabase db) {
        this.db = db;
        mistakes = new ArrayList<>();
    }

    public int getGrade() {
        return grade;
    }

    public ArrayList<String> getMistakes() {
        return mistakes;
    }

    public PdfPTable createOmTable(Flat flat) throws DocumentException {
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 6f, 6f, 2f, 2f, 2f, 2f, 2f, 4f});

        String[] headers = {"Lp.", "Badany punkt", "Wyłącznik", "Typ", "In [A]", "Ia [A]", "Zs [Ω]", "Za [Ω]", "Ocena"};
        for (String header : headers) {
            Phrase phrase = new Phrase();
            if (header.contains("[")) {
                int start = header.indexOf("[");
                int end = header.indexOf("]");
                String main = header.substring(0, start).trim();
                String sub = header.substring(start, end + 1);
                phrase.add(new Chunk(main + "\n", ProFonts.medium));
                phrase.add(new Chunk(sub, ProFonts.medium));
            } else {
                phrase.add(new Chunk(header, ProFonts.medium));
            }

            PdfPCell cell = new PdfPCell(phrase);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPaddingTop(5f);
            cell.setPaddingBottom(5f);
            table.addCell(cell);
        }

        List<RoomInFlat> rooms = db.roomDao().getRoomsForFlatSync(flat.id);
        int index = 1;

        for (RoomInFlat room : rooms) {
            List<OutletMeasurement> oms = db.outletMeasurementDao().getMeasurementsForRoomSync(room.id);
            boolean found = false;
            if (!oms.isEmpty()) {
                for (OutletMeasurement om : oms) {
                    if (om.ohms != null && om.ohms != 0.0) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                continue;
            }

            PdfPCell cell = new PdfPCell(new Phrase(room.name, ProFonts.fontNormalBold));
            cell.setColspan(9);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPaddingBottom(5f);
            cell.setPaddingTop(5f);
            table.addCell(cell);


            for (OutletMeasurement om : oms) {
                List<String> values = new ArrayList<>();
                values.add(Integer.toString(index)); // 1
                values.add(om.appliance); // 2

                if (om.switchName.isEmpty()) {
                    values.add("-"); // 3
                    mistakes.add("Mieszkanie: " + flat.number + " błąd danych pętla zwarcia brak nazwy wyłącznika");
                } else {
                    values.add(om.switchName);
                }

                values.add(om.breakerType);
                values.add(Double.toString(om.amps));
                double ia = 0.0;
                double za = 0.0;

                if (om.breakerType.equals("B")) {
                    BreakerB breaker = new BreakerB(om.amps);
                    ia = breaker.ia;
                    za = breaker.za;
                }

                if (om.breakerType.equals("C")) {
                    BreakerC breaker = new BreakerC(om.amps);
                    ia = breaker.ia;
                    za = breaker.za;
                }

                if (om.breakerType.equals("gG")) {
                    BreakerGg breaker = new BreakerGg(om.amps);
                    ia = breaker.ia;
                    za = breaker.za;
                }
                values.add(Double.toString(ia).replace(".", ","));
                if (om.ohms != null &&  om.ohms != 0.0) {
                    values.add(Double.toString(om.ohms).replace(".", ","));
                } else {
                    continue;
                }
                values.add(Double.toString(za).replace(".", ","));

                boolean good = om.ohms < za;
                if (good) {
                    if (om.note.equals("brak uwag")) {
                        values.add("Pozytywna");
                    } else {
                        values.add(om.note);
                        grade = 1;
                    }
                } else {
                    values.add("Negatywna");
                    grade = 2;
                }

                index++;
                for (String v : values) {
                    table.addCell(CellGenerator.createCell(v));
                }
            }
        }
        return table;
    }
}
