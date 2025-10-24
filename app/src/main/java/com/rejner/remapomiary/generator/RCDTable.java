package com.rejner.remapomiary.generator;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.generator.constants.Constants;
import com.rejner.remapomiary.generator.constants.ProFonts;
import com.rejner.remapomiary.generator.helpers.CellGenerator;
import com.rejner.remapomiary.generator.helpers.RandomNumber;

import java.util.ArrayList;
import java.util.List;

public class RCDTable {
    private final AppDatabase db;
    private final ArrayList<String> mistakes;
    private String rcdNotes = "";
    private int rcdIsGood = 1;

    public int getRcdIsGood() {
        return rcdIsGood;
    }

    public String getRcdNotes() {
        return rcdNotes;
    }

    public RCDTable(AppDatabase db) {
        this.db = db;
        mistakes = new ArrayList<>();
    }

    public ArrayList<String> getMistakes() {
        return mistakes;
    }

    public PdfPTable createRCDTable(Flat flat) throws DocumentException {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 6f, 6f, 2f, 2f, 2f, 2f, 4f});

        String[] headers = {"Lp.", "Badany punkt", "Wyłącznik RCD", "Typ", "IΔn [mA]", "la [mA]", "t rcd [ms]", "Ocena"};
        for (String header : headers) {
            Phrase phrase = new Phrase();
            if (header.contains("[")) {
                int start = header.indexOf("[");
                int end = header.indexOf("]");
                String main = header.substring(0, start).trim();
                String sub = header.substring(start, end + 1);
                phrase.add(new Chunk(main + "\n", ProFonts.medium));
                phrase.add(new Chunk(sub, ProFonts.mediumNotBold));
            } else {
                phrase.add(new Chunk(header, ProFonts.medium));
            }

            PdfPCell cell = new PdfPCell(phrase);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPaddingTop(5f);
            cell.setPaddingBottom(5f);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);

            table.addCell(cell);
        }

        List<RoomInFlat> rooms = db.roomDao().getRoomsForFlatSync(flat.id);
        int index = 1;
        boolean first = true;

        RCD rcd;
        List<RCD> rcds = db.rcdDao().getRcdsForFlatSync(flat.id);
        if (rcds.isEmpty()) {
            rcd = new RCD();
            rcd.isGood = 1;
            rcd.time1 = 0;
            rcd.time2 = 0;
            rcd.name = "BŁĄD DANYCH";
            rcd.type = "BŁĄD DANYCH";
            mistakes.add(flat.number + " błąd danych RCD (wejdź do mieszkania i przejdź do róznicówka)");
        } else {
            rcd = rcds.get(0);
            if (rcd.notes != null && !rcd.notes.isEmpty()) {
                rcdNotes += rcd.notes;
            }
        }

        for (RoomInFlat room : rooms) {
            List<OutletMeasurement> oms = db.outletMeasurementDao().getMeasurementsForRoomSync(room.id);

            boolean founDataInRoom = false;
            if (!oms.isEmpty()) {
                for (OutletMeasurement om : oms ) {
                    if (om.ohms != null && om.ohms != 0.0) {
                        founDataInRoom = true;
                        break;
                    }
                }
            }
            if (!founDataInRoom) {
                continue;
            }


//            Nazwa pokoju
            PdfPCell cell = new PdfPCell(new Phrase(room.name, ProFonts.fontNormalBold));
            cell.setColspan(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPaddingBottom(5f);
            cell.setPaddingTop(5f);
            table.addCell(cell);

//            Różnicówki


            for (OutletMeasurement om : oms) {
                if (om.ohms == null || om.ohms == 0.0) {
                    continue;
                }
                List<String> values = new ArrayList<>();

//                    lp
                values.add(Integer.toString(index));
//                    badany punkt
                values.add(om.appliance);
//                    Model rcd
                if (rcd.name == null || rcd.type.isEmpty()) {
                    values.add("-");
                    mistakes.add("Mieszkanie: " + flat.number + " błąd danych RCD (nazwa)");
                } else {
                    values.add(rcd.name);
                }
//                    typ rcd
                values.add(rcd.type); //4
//                    stała
                values.add(String.valueOf(Constants.rcdIdeltaN)); //5

                if (rcd.isGood == 0) {
                    rcdIsGood = 0;
                    values.add("-"); //6
                    values.add("-"); //9
                    values.add("Negatywna"); //10
                } else if (om.note.equals("nie podłączony bolec") || om.note.equals("zepsute")) {
                    values.add("-"); //6
                    values.add("-"); //9
                    values.add(om.note); //10

                }
                else {
                    if (first && rcd.time2 != 0 && rcd.time1 != 0) {
                        values.add(Integer.toString(rcd.time2)); //6
                        values.add(Integer.toString(rcd.time1)); //9
                        first = false;
                    } else {
                        values.add(Integer.toString(RandomNumber.randomInt(19, 25))); //6
                        values.add(Integer.toString(RandomNumber.randomInt(19, 25))); //9
                    }
                    values.add("Pozytywna"); //10
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
