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

public class TableFor1f {
    private final AppDatabase db;

    public TableFor1f(AppDatabase db) {
        this.db = db;
    }

    public PdfPTable createMeasurementTableFor1f(List<Circuit> circuits, Flat flat) throws DocumentException {
        PdfPTable table;
        String[] headers;

        boolean isTNC = flat.type.equals(Settings.installationTypeTNC);

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

    public PdfPTable createMeasurementTableFor1fCommonSpace(List<BoardCommonSpace> boards, String flatType) throws DocumentException {
        boolean hasAny1fCircuits = false;
        for (BoardCommonSpace board : boards) {
            if (board.type.equals(flatType) && !db.circuitCommonSpaceDao().getCircuitsForBoardSync1f(board.id).isEmpty()) {
                hasAny1fCircuits = true;
                break;
            }
        }

        if (!hasAny1fCircuits) {
            return null;
        }

        PdfPTable table;
        String[] headers;

        // Używamy flatType do ustalenia liczby kolumn dla całej tabeli
        boolean isTNC = flatType.equals(Settings.installationTypeTNC);

        int columns = isTNC ? 5 : 7;
        table = new PdfPTable(columns);
        table.setWidthPercentage(100);

        if (isTNC) {
            table.setWidths(new float[]{2f, 6f, 2f, 2f, 4f});
            headers = new String[]{"Lp.", "Nazwa obwodu", "R(L-N)", "R(W)", "Ocena pomiaru"};
        } else {
            table.setWidths(new float[]{2f, 6f, 2f, 2f, 2f, 2f, 4f});
            headers = new String[]{"Lp.", "Nazwa obwodu", "R(L-PE)", "R(L-N)", "R(N-PE)", "R(W)", "Ocena pomiaru"};
        }

        // --- Dodanie nagłówków TYLKO RAZ ---
        for (String h : headers) {
            PdfPCell cell = TableHeaders.createHeader(h);
            table.addCell(cell);
        }

        // --- Pętla po rozdzielniach ---
        for (BoardCommonSpace board : boards) {
            if (!board.type.equals(flatType)) {
                continue;
            }

            // 1. Pobranie obwodów 1f dla tej konkretnej rozdzielni
            List<CircuitCommonSpace> circuits = db.circuitCommonSpaceDao().getCircuitsForBoardSync1f(board.id);
            if (circuits.isEmpty()) {
                continue;
            }

            // 2. Wiersz z nazwą rozdzielni (zachowuje się jak nazwa pokoju w RCD)
            PdfPCell boardCell = new PdfPCell(new Phrase(board.name, ProFonts.fontNormalBold));
            boardCell.setColspan(columns); // Łączy wszystkie kolumny
            boardCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            boardCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            boardCell.setPaddingBottom(5f);
            boardCell.setPaddingTop(5f);
            table.addCell(boardCell);

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


            int index = 1;
            for (CircuitCommonSpace c : circuits) {
                List<String> values = new ArrayList<>();

                if (isTNC) {
                    if (c.notes.contains(Settings.flatNoAccess)) {
                        values.add(Integer.toString(index));
                        values.add(c.name);
                        values.add("-");
                        values.add("-");
                        values.add(Settings.flatNoAccess);
                    } else {
                        values.add(Integer.toString(index));
                        values.add(c.name);
                        values.add(">2");
                        values.add("1");
                        values.add("Pozytywna");
                    }

                } else {
                    if (c.notes.contains(Settings.flatNoAccess)) {
                        values.add(Integer.toString(index));
                        values.add(c.name);
                        values.add("-");
                        values.add("-");
                        values.add("-");
                        values.add("-");
                        values.add(Settings.flatNoAccess);
                    } else {
                        values.add(Integer.toString(index));
                        values.add(c.name);
                        values.add(">2");
                        values.add(">2");
                        values.add(">2");
                        values.add("1");
                        values.add("Pozytywna");
                    }

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
