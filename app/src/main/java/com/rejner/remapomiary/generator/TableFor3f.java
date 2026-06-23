package com.rejner.remapomiary.generator;

import static com.rejner.remapomiary.generator.helpers.CellGenerator.createCell;

import androidx.annotation.NonNull;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.BoardCommonSpace;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.generator.constants.ProFonts;
import com.rejner.remapomiary.generator.helpers.TableHeaders;
import com.rejner.remapomiary.ui.utils.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TableFor3f {
    private final AppDatabase db;

    // Konstruktor przyjmujący bazę danych (analogicznie do TableFor1f)
    public TableFor3f(AppDatabase db) {
        this.db = db;
    }
    public PdfPTable createMeasurementTableFor3f(List<Circuit> circuits, Flat flat) throws DocumentException {
        PdfPTable table;
        String[] headers;

        boolean isTNC = flat.type.equals(Settings.installationTypeTNC);

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
    public PdfPTable createMeasurementTableFor3fCommonSpace(List<BoardCommonSpace> boards, String flatType) throws DocumentException {
        // Krok A: Sprawdzamy, czy w ogóle istnieją jakiekolwiek obwody 3f w przekazanych rozdzielniach o odpowiednim typie
        boolean hasAny3fCircuits = false;
        for (BoardCommonSpace board : boards) {
            if (board.type.equals(flatType) && !db.circuitCommonSpaceDao().getCircuitsForBoardSync3f(board.id).isEmpty()) {
                hasAny3fCircuits = true;
                break;
            }
        }

        // Jeśli nie ma żadnych obwodów 3f o tym typie, zwracamy null
        if (!hasAny3fCircuits) {
            return null;
        }



        PdfPTable table;
        String[] headers;
        boolean isTNC = flatType.equals(Settings.installationTypeTNC);
        int columns = isTNC ? 10 : 14;

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

        // --- Nagłówki dodawane tylko RAZ na początku tabeli ---
        for (String h : headers) {
            table.addCell(TableHeaders.createHeader(h));
        }

        // --- Pętla grupująca po rozdzielniach ---
        for (BoardCommonSpace board : boards) {
            if (!board.type.equals(flatType)) {
                continue;
            }

            List<CircuitCommonSpace> circuits = db.circuitCommonSpaceDao().getCircuitsForBoardSync3f(board.id);
            if (board.name.equals(Settings.mainBoardName)) {
                Collections.sort(circuits,
                        Comparator
                                .comparing((CircuitCommonSpace c) -> Settings.flatNoAccess.equals(c.notes))
                                .thenComparingInt(c -> {
                                    try {
                                        String cleanedNumber = c.name.replaceAll("\\D+", "");
                                        return Integer.parseInt(cleanedNumber);
                                    } catch (NumberFormatException e) {
                                        return 0;
                                    }
                                })
                );
            }
            // Jeśli ta konkretna rozdzielnia nie ma obwodów 3f, pomijamy nagłówek sekcji dla niej
            if (circuits.isEmpty()) {
                continue;
            }

            // Nagłówek sekcji rozdzielni (środek, pogrubiony, tło LIGHT_GRAY)
            PdfPCell boardCell = new PdfPCell(new Phrase( board.name, ProFonts.fontNormalBold));
            boardCell.setColspan(columns);
            boardCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            boardCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            boardCell.setPaddingBottom(5f);
            boardCell.setPaddingTop(5f);
            table.addCell(boardCell);

            int index = 1;
            for (CircuitCommonSpace c : circuits) {
                List<String> values = new ArrayList<>();

                values.add(Integer.toString(index));
                values.add(c.name);
                if (c.notes.contains(Settings.flatNoAccess)) {
                    if (isTNC) {
                        for (int i = 0; i < 6; i++) values.add("-");
                    } else {
                        for (int i = 0; i < 10; i++) values.add("-");
                    }
                    values.add("-");
                    values.add(Settings.flatNoAccess);
                } else {
                    if (isTNC) {
                        for (int i = 0; i < 6; i++) values.add(">2");
                    } else {
                        for (int i = 0; i < 10; i++) values.add(">2");
                    }
                    values.add("1");
                    values.add("Pozytywna");
                }


                for (String v : values) {
                    table.addCell(createCell(v));
                }
                index++;
            }
        }

        return table;
    }

}
