package com.rejner.remapomiary.generator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatFullData;
import com.rejner.remapomiary.data.entities.ProtocolNumber;
import com.rejner.remapomiary.generator.constants.ProFonts;
import com.rejner.remapomiary.generator.helpers.CellGenerator;
import com.rejner.remapomiary.generator.helpers.FlatPageNumberEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ProtocolGenerator {

    private static final String TAG = "ProtocolGenerator";

    private final Context context;
    private Document document;
    private PdfWriter writer;
    private OutputStream outputStream;
    private float titleSpacing = 8f;
    private float titleSpacingA = 2f;
    private float indentation = 15f;
    private ArrayList<String> grade0Flats = new ArrayList<>();
    private ArrayList<String> grade1Flats = new ArrayList<>();
    private ArrayList<String> grade2Flats = new ArrayList<>();
    private Font normalFont = ProFonts.mediumNotBold;
    private Font subscriptFont = ProFonts.small;
    private int totalFlats = 0;
    private int generatedFlats = 0;
    float subscriptOffset = 1;
    private ArrayList<String> skippedFlats = new ArrayList<>();
    AppDatabase db;
    int omGrade = 0;
    int rcdIsGood = 1;
    ArrayList<String> rcdMistakes = new ArrayList<>();
    ArrayList<String> omMistakes = new ArrayList<>();


    public ProtocolGenerator(Context context) {
        this.context = context;
        db = AppDatabase.getDatabase(context.getApplicationContext());

    }

    public Uri generate(String fileName, int blockId) {
        try {
            Uri fileUri = createPdfFileInDownloads(fileName);
            if (fileUri == null) {
                throw new IOException("Nie udało się utworzyć URI dla pliku PDF.");
            }
            FlatPageNumberEvent pageEvent = new FlatPageNumberEvent(ProFonts.fontNormal);
            writer.setPageEvent(pageEvent);
            document.open();

            List<FlatFullData> flats = db.flatDao().getFlatsSync(blockId);

            BlockFullData blockFullData1 = db.blockDao().getBlockById(blockId);
            Paragraph p = new Paragraph("BLOK " + blockFullData1.block.number, ProFonts.large);
            p.setAlignment(Element.ALIGN_CENTER);
            float pageHeight = document.getPageSize().getHeight();
            float textHeight = p.getLeading(); // wysokość wiersza tekstu
            float yPosition = (pageHeight / 2) - (textHeight / 2);

            PdfContentByte canvas = writer.getDirectContent();
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase("BLOK " + blockFullData1.block.number, ProFonts.large),
                    document.getPageSize().getWidth() / 2,  // środek w poziomie
                    yPosition,                              // środek w pionie
                    0);

            Collections.sort(flats, Comparator.comparingInt(f -> {
                try {
                    String cleanedNumber = f.flat.number.replaceAll("\\s+", "");
                    return Integer.parseInt(cleanedNumber);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }));

            List<ProtocolNumber> protocolNumber = db.protocolNumberDao().getAllProtocols();
            if (protocolNumber.isEmpty()) {
                ProtocolNumber newProtocolNumber = new ProtocolNumber();
                newProtocolNumber.number = 1;
                newProtocolNumber.creationDate = new Date();
                db.protocolNumberDao().insert(newProtocolNumber);
                protocolNumber.add(newProtocolNumber);
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(protocolNumber.get(0).creationDate);

            int creationYear = cal.get(Calendar.YEAR);

            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (creationYear != currentYear) {
                protocolNumber.get(0).number = 1;
                protocolNumber.get(0).creationDate = new Date();
                db.protocolNumberDao().update(protocolNumber.get(0));
            }
            totalFlats = flats.size();

            int currentProtocolNumber = db.protocolNumberDao().getCurrentNumber();
            for (FlatFullData flat : flats) {
                if (flat.flat.status.contains("niewykonany")) {
                    skippedFlats.add("Mieszkanie " + blockFullData1.block.number + "/" + flat.flat.number);
                    continue;
                }
                String endNotes = "";
                document.newPage();
                pageEvent.startNewFlat("Mieszkanie " + blockFullData1.block.number + "/" + flat.flat.number);

                addHeader();
                String protocolNumberTitle = "Protokół nr w/" + currentProtocolNumber + "/" + new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());

                addTitleSection(protocolNumberTitle, flat.flat.type, flat.flat.hasRCD);
                BlockFullData blockFullData = db.blockDao().getBlockById(blockId);

                String clientData = blockFullData.client.name + "\n" +
                        "ul. " + blockFullData.client.street + ", " + blockFullData.client.postal_code + " " + blockFullData.client.city;
                String objectData = "Budynek wielorodzinny" + "\n" + "ul. " + blockFullData.block.street + " " + blockFullData.block.number + ", " + blockFullData.block.postal_code + " " + blockFullData.block.city + "\n" + "LOKAL: " + flat.flat.number + "\n" + "Napięcie znamionowe: 230V/400V";
                addDetailsTable(clientData, objectData);
                addData(flat.flat.type, flat.flat.creation_date);


                List<Circuit> circuits3f = db.circuitDao().getCircuitsForFlatSync3f(flat.flat.id);
                if (!circuits3f.isEmpty()) {
                    Paragraph circuitsFor3fTitle = new Paragraph("Wyniki z pomiarów rezystancji izolacji instalacji - obwody 3f " + flat.flat.type, ProFonts.fontNormalBold);
                    circuitsFor3fTitle.setAlignment(Element.ALIGN_LEFT);
                    circuitsFor3fTitle.setSpacingAfter(5f);
                    document.add(circuitsFor3fTitle);

                    TableFor3f tableFor3fGenerator = new TableFor3f();

                    PdfPTable table = tableFor3fGenerator.createMeasurementTableFor3f(circuits3f, flat.flat);
                    document.add(table);
                    table.setSpacingAfter(5f);

                    if (flat.flat.type.equals("TN-S")) {
                        Paragraph f3Legend = new Paragraph();

                        f3Legend.setFont(normalFont); // Ustaw domyślną czcionkę dla paragrafu

                        // R L1-L2
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL1L2 = new Chunk("L1-L2", subscriptFont);
                        subL1L2.setTextRise(-subscriptOffset);
                        f3Legend.add(subL1L2);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L1 i L2, ", normalFont));

                        // R L2-L3
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL2L3 = new Chunk("L2-L3", subscriptFont);
                        subL2L3.setTextRise(-subscriptOffset);
                        f3Legend.add(subL2L3);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L2 i L3, ", normalFont));

                        // R L3-L1
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL3L1 = new Chunk("L3-L1", subscriptFont);
                        subL3L1.setTextRise(-subscriptOffset);
                        f3Legend.add(subL3L1);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L3 i L1, ", normalFont));

                        // R L1-PE
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL1PE = new Chunk("L1-PE", subscriptFont);
                        subL1PE.setTextRise(-subscriptOffset);
                        f3Legend.add(subL1PE);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L1 i PE, ", normalFont));

                        // R L2-PE
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL2PE = new Chunk("L2-PE", subscriptFont);
                        subL2PE.setTextRise(-subscriptOffset);
                        f3Legend.add(subL2PE);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L2 i PE, ", normalFont));

                        // R L3-PE
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL3PE = new Chunk("L3-PE", subscriptFont);
                        subL3PE.setTextRise(-subscriptOffset);
                        f3Legend.add(subL3PE);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L3 i PE, ", normalFont));

                        // R L1-N
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL1N = new Chunk("L1-N", subscriptFont);
                        subL1N.setTextRise(-subscriptOffset);
                        f3Legend.add(subL1N);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L1 i N, ", normalFont));

                        // R L2-N
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL2N = new Chunk("L2-N", subscriptFont);
                        subL2N.setTextRise(-subscriptOffset);
                        f3Legend.add(subL2N);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L2 i N, ", normalFont));

                        // R L3-N
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL3N = new Chunk("L3-N", subscriptFont);
                        subL3N.setTextRise(-subscriptOffset);
                        f3Legend.add(subL3N);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L3 i N, ", normalFont));

                        // R N-PE
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subNPE = new Chunk("N-PE", subscriptFont);
                        subNPE.setTextRise(-subscriptOffset);
                        f3Legend.add(subNPE);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami N i PE, ", normalFont));

                        // Rw
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subW = new Chunk("w", subscriptFont);
                        subW.setTextRise(-subscriptOffset);
                        f3Legend.add(subW);
                        f3Legend.add(new Chunk(": wartość rezystancji wymagane", normalFont));

                        // Ustawienia końcowe i dodanie do dokumentu
                        f3Legend.setSpacingAfter(15f);
                        document.add(f3Legend);
                    } else {
                        Paragraph f3Legend = new Paragraph();
                        f3Legend.setFont(normalFont); // Ustaw domyślną czcionkę

                        // R L1-L2
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL1L2 = new Chunk("L1-L2", subscriptFont);
                        subL1L2.setTextRise(-subscriptOffset);
                        f3Legend.add(subL1L2);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L1 i L2, ", normalFont));

                        // R L2-L3
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL2L3 = new Chunk("L2-L3", subscriptFont);
                        subL2L3.setTextRise(-subscriptOffset);
                        f3Legend.add(subL2L3);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L2 i L3, ", normalFont));

                        // R L3-L1
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL3L1 = new Chunk("L3-L1", subscriptFont);
                        subL3L1.setTextRise(-subscriptOffset);
                        f3Legend.add(subL3L1);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L3 i L1, ", normalFont));

                        // R L1-N
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL1N = new Chunk("L1-N", subscriptFont);
                        subL1N.setTextRise(-subscriptOffset);
                        f3Legend.add(subL1N);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L1 i N, ", normalFont));

                        // R L2-N
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL2N = new Chunk("L2-N", subscriptFont);
                        subL2N.setTextRise(-subscriptOffset);
                        f3Legend.add(subL2N);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L2 i N, ", normalFont));

                        // R L3-N
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subL3N = new Chunk("L3-N", subscriptFont);
                        subL3N.setTextRise(-subscriptOffset);
                        f3Legend.add(subL3N);
                        f3Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L3 i N, ", normalFont));

                        // Rw
                        f3Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subW = new Chunk("W", subscriptFont);
                        subW.setTextRise(-subscriptOffset);
                        f3Legend.add(subW);
                        f3Legend.add(new Chunk(": wartość rezystancji wymagane", normalFont));

                        // Ustawienia końcowe i dodanie do dokumentu
                        f3Legend.setSpacingAfter(15f);
                        document.add(f3Legend);
                    }

                }

                List<Circuit> circuits = db.circuitDao().getCircuitsForFlatSync(flat.flat.id);
                if (!circuits.isEmpty()) {
                    Paragraph circuitsFor1fTitle = new Paragraph("Wyniki z pomiarów rezystancji izolacji instalacji - obwody 1f " + flat.flat.type, ProFonts.fontNormalBold);
                    circuitsFor1fTitle.setAlignment(Element.ALIGN_LEFT);
                    circuitsFor1fTitle.setSpacingAfter(5f);
                    document.add(circuitsFor1fTitle);

                    TableFor1f tableFor1fGenerator = new TableFor1f(db);

                    PdfPTable tableFor1f = tableFor1fGenerator.createMeasurementTableFor1f(circuits, flat.flat);
                    document.add(tableFor1f);
                    tableFor1f.setSpacingAfter(5f);

                    if(flat.flat.type.equals("TN-S")) {
                        Paragraph f1Legend = new Paragraph();
                        f1Legend.setFont(normalFont);

                        f1Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subLPE = new Chunk("L-PE", subscriptFont);
                        subLPE.setTextRise(-subscriptOffset);
                        f1Legend.add(subLPE);
                        f1Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L i PE, ", normalFont));

                        f1Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subLN = new Chunk("L-N", subscriptFont);
                        subLN.setTextRise(-subscriptOffset);
                        f1Legend.add(subLN);
                        f1Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L i N, ", normalFont));


                        f1Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subNPE = new Chunk("N-PE", subscriptFont);
                        subNPE.setTextRise(-subscriptOffset);
                        f1Legend.add(subNPE);
                        f1Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami N i PE, ", normalFont));


                        f1Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subRw = new Chunk("W", subscriptFont);
                        subRw.setTextRise(-subscriptOffset);
                        f1Legend.add(subRw);
                        f1Legend.add(new Chunk(": wartość rezystancji wymagane.", normalFont));

                        f1Legend.setSpacingAfter(15f);
                        document.add(f1Legend);
                    } else {
                        Paragraph f1Legend = new Paragraph();
                        f1Legend.setFont(normalFont);

                        f1Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subLN = new Chunk("L-N", subscriptFont);
                        subLN.setTextRise(-subscriptOffset);
                        f1Legend.add(subLN);
                        f1Legend.add(new Chunk(": zmierzona rezystancja izolacji pomiędzy obwodami L i N, ", normalFont));


                        f1Legend.add(new Chunk("R", ProFonts.medium));
                        Chunk subRw = new Chunk("W", subscriptFont);
                        subRw.setTextRise(-subscriptOffset);
                        f1Legend.add(subRw);
                        f1Legend.add(new Chunk(": wartość rezystancji wymagane.", normalFont));

                        f1Legend.setSpacingAfter(15f);
                        document.add(f1Legend);
                    }

                }


                if (flat.flat.hasRCD == 1) {
                    Paragraph RCDTitle = new Paragraph("Wyniki z badania wyłączników różnicowoprądowych ", ProFonts.fontNormalBold);
                    RCDTitle.setAlignment(Element.ALIGN_LEFT);
                    RCDTitle.setSpacingAfter(5f);
                    document.add(RCDTitle);

                    RCDTable rcdTableGenerator = new RCDTable(db);

                    PdfPTable rcdTable = rcdTableGenerator.createRCDTable(flat.flat);
                    document.add(rcdTable);
                    rcdTable.setSpacingAfter(5f);
                    rcdIsGood = rcdTableGenerator.getRcdIsGood();
                    if (!rcdTableGenerator.getMistakes().isEmpty()) {
                        rcdMistakes.addAll(rcdTableGenerator.getMistakes());
                    }
                    if (!rcdTableGenerator.getRcdNotes().isEmpty()) {
                        endNotes += rcdTableGenerator.getRcdNotes() + "\n";
                    }

                    Paragraph rcdLegend = new Paragraph("Typ: charakterystyka bezpiecznika, I∆n [mA]: różnicowy prąd wyłączający, " +
                            "Ia [mA]: prąd powodujący wyłączenie RCD, " +
                            "t rcd [ms]: zmierzony czas wyłączenia RCD", ProFonts.mediumNotBold);
                    rcdLegend.setSpacingAfter(15f);
                    document.add(rcdLegend);

                }

                if (!circuits.isEmpty()) {
                    Paragraph omTableTitle = new Paragraph("Wynik pomiarów skuteczności samoczynnego wyłączenia", ProFonts.fontNormalBold);
                    omTableTitle.setAlignment(Element.ALIGN_LEFT);
                    omTableTitle.setSpacingAfter(5f);
                    document.add(omTableTitle);

                    OmTable omTableGenerator = new OmTable(db);

                    PdfPTable omTable = omTableGenerator.createOmTable(flat.flat);
                    document.add(omTable);
                    omTable.setSpacingAfter(5f);
                    omGrade = omTableGenerator.getGrade();
                    if (!omTableGenerator.getMistakes().isEmpty()) {
                        omMistakes.addAll(omTableGenerator.getMistakes());
                    }
                    Paragraph omLegend = new Paragraph("Typ: charakterystyka bezpiecznika, In [A]: prąd nominalny bezpiecznika, " +
                            "Ia [A]: prąd powodujący wyzwolenie bezpiecznika, " +
                            "Zs [Ω]: zmierzona impedancja pętli zwarciowej, " +
                            "Za [Ω]: wartość wymagana impedancji pętli zwarciowej: Za = (Uo/Ia)", ProFonts.mediumNotBold);
                    omLegend.setSpacingAfter(15f);
                    document.add(omLegend);

                }
                if (!flat.flat.notes.isEmpty()) {
                    endNotes += flat.flat.circuitNotes;
                }
                int next = 7;
                if (!endNotes.isEmpty()) {
                    createNotes(endNotes);
                    next = 8;
                }
                generatedFlats++;
                createGrade(next, flat.flat);
                next ++;
                currentProtocolNumber++;
                createEndSummary(next, flat.flat);
                db.protocolNumberDao().incrementNum();

            }

            document.newPage();
            pageEvent.finishDocument();
            writer.setPageEvent(null);
            addSummary(blockFullData1.block);
            document.close();


            return fileUri;

        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas generowania PDF", e);
            return null;
        } finally {
            closeDocument();
        }
    }
    public void createEndSummary(int next, Flat flat)  throws DocumentException {

        Paragraph nextTitle = new Paragraph(next + ". Data następnego badania", ProFonts.fontNormalBold);
        nextTitle.setSpacingBefore(titleSpacing);
        nextTitle.setSpacingAfter(titleSpacingA);
        nextTitle.setAlignment(Element.ALIGN_LEFT);
        Date now = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.YEAR, 5);
        Date datePlus5Years = cal.getTime();
        Paragraph nextDesc = new Paragraph("Nie później niż: " + formatMonthAndYear(datePlus5Years), ProFonts.fontNormal);
        nextDesc.setAlignment(Element.ALIGN_LEFT);
        nextDesc.setIndentationLeft(indentation);
        next ++;
        document.add(nextTitle);
        document.add(nextDesc);



        Paragraph whoDidTitle = new Paragraph(next + ". Wykonawcy pomiarów:", ProFonts.fontNormalBold);
        whoDidTitle.setSpacingBefore(titleSpacing);
        whoDidTitle.setSpacingAfter(titleSpacingA);
        whoDidTitle.setAlignment(Element.ALIGN_LEFT);
        document.add(whoDidTitle);
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});

        Paragraph p1 = new Paragraph();
        p1.add(new Chunk("Wykonał: ", ProFonts.fontNormal));
        p1.add(new Chunk("Paweł Rejner\n", ProFonts.fontNormalBold));
        p1.add(new Chunk("Zaświadczenie kwalifikacyjne nr E/405/2131/21\n", ProFonts.fontNormal));
        p1.add(new Chunk("Zaświadczenie kwalifikacyjne nr D/405/2132/21", ProFonts.fontNormal));

        PdfPCell cell1 = new PdfPCell(p1);
        cell1.setPaddingLeft(15f);
        cell1.setBorder(PdfPCell.NO_BORDER); // brak obramowania
        cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell1.setHorizontalAlignment(Element.ALIGN_LEFT);

        Paragraph p2 = new Paragraph();
        p2.add(new Chunk("Sprawdził: ", ProFonts.fontNormal));
        p2.add(new Chunk("Marek Rejner\n", ProFonts.fontNormalBold));
        p2.add(new Chunk("Zaświadczenie kwalifikacyjne nr E/180/21/23\n", ProFonts.fontNormal));
        p2.add(new Chunk("Zaświadczenie kwalifikacyjne nr D/180/25/23", ProFonts.fontNormal));

        PdfPCell cell2 = new PdfPCell(p2);
        cell2.setBorder(PdfPCell.NO_BORDER);
        cell2.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell2.setHorizontalAlignment(Element.ALIGN_LEFT);

        table.addCell(cell1);
        table.addCell(cell2);

        document.add(table);


    }
    private void createNotes(String endNotes) throws DocumentException {


        Paragraph notesTitle = new Paragraph("7. Uwagi i wnioski", ProFonts.fontNormalBold);
        notesTitle.setSpacingBefore(titleSpacing);
        notesTitle.setSpacingAfter(titleSpacingA);
        notesTitle.setAlignment(Element.ALIGN_LEFT);

        Paragraph notes = new Paragraph(endNotes, ProFonts.fontNormal);
        notes.setAlignment(Element.ALIGN_LEFT);
        notes.setIndentationLeft(indentation);

        document.add(notesTitle);
        document.add(notes);
    }

    private void createGrade(int next, Flat flat) throws DocumentException {


        Paragraph gradeTitle = new Paragraph(next + ". Orzeczenie", ProFonts.fontNormalBold);
        gradeTitle.setSpacingBefore(titleSpacing);
        gradeTitle.setSpacingAfter(titleSpacingA);
        gradeTitle.setAlignment(Element.ALIGN_LEFT);

        String finalGrade;
        boolean shouldSetGradeTo1 = db.flatDao().shouldSetGradeToOneSync(flat.id);

        if (flat.gradeByUser == 1 && flat.grade == 2) {
            omGrade = 2;
        }

        if ((shouldSetGradeTo1 && flat.gradeByUser == 0) || rcdIsGood == 0) {
            finalGrade = "Instalacja dopuszczona do użytku po usunięciu usterek.";
        } else {
            finalGrade = "Instalacja dopuszczona do użytku.";
        }

        if (omGrade == 2) {
            finalGrade = "Instalacja niedopuszczona do użytku.";
        }

        if (finalGrade.equals("Instalacja dopuszczona do użytku po usunięciu usterek.")) {
            grade1Flats.add(flat.number);
        }
        if (finalGrade.equals("Instalacja dopuszczona do użytku.")) {
            grade0Flats.add(flat.number);

        }
        if (finalGrade.equals("Instalacja niedopuszczona do użytku.")) {
            grade2Flats.add(flat.number);

        }
        Paragraph gradeDesc = new Paragraph(finalGrade, ProFonts.fontNormal);
        gradeDesc.setAlignment(Element.ALIGN_LEFT);
        gradeDesc.setIndentationLeft(indentation);

        document.add(gradeTitle);
        document.add(gradeDesc);
    }

    private void addSummary(Block block) throws DocumentException {
        document.newPage();
        Paragraph title = new Paragraph("Podsumowanie", ProFonts.fontBold14);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15f);
        document.add(title);



        Paragraph flatsNumtitle = new Paragraph("Informacje o mieszkaniach:", ProFonts.fontNormalBold);
        flatsNumtitle.setAlignment(Element.ALIGN_LEFT);
        flatsNumtitle.setSpacingAfter(titleSpacingA);
        flatsNumtitle.setSpacingBefore(titleSpacing);
        document.add(flatsNumtitle);

        Paragraph flatsNumDesc = new Paragraph("Łącznie mieszkań: " + totalFlats, ProFonts.fontNormal);
        flatsNumDesc.setAlignment(Element.ALIGN_LEFT);
        flatsNumDesc.setIndentationLeft(indentation);
        document.add(flatsNumDesc);


        Paragraph flatsNumDesc2 = new Paragraph("Wygenerowano protokoły dla mieszkań: " + generatedFlats + " (" + generatedFlats + "/" + totalFlats + ")", ProFonts.fontNormal);
        flatsNumDesc2.setAlignment(Element.ALIGN_LEFT);
        flatsNumDesc2.setIndentationLeft(indentation);
        document.add(flatsNumDesc2);


        Paragraph flatsNumDesc3 = new Paragraph("Pominięte mieszkania (nikt nie otwarł): " + (totalFlats - generatedFlats), ProFonts.fontNormal);
        flatsNumDesc3.setAlignment(Element.ALIGN_LEFT);
        flatsNumDesc3.setIndentationLeft(indentation);
        document.add(flatsNumDesc3);

        int index = 1;
        for (String s : skippedFlats) {
            Paragraph skippedFlat = new Paragraph(index + ". " + s, ProFonts.fontNormal);
            skippedFlat.setAlignment(Element.ALIGN_LEFT);
            skippedFlat.setIndentationLeft(25f);
            document.add(skippedFlat);
            index++;
        }

        if (!rcdMistakes.isEmpty() || !omMistakes.isEmpty()) {
            Paragraph genInfo = new Paragraph("Informacje o generowaniu:", ProFonts.fontNormalBold);
            genInfo.setAlignment(Element.ALIGN_LEFT);
            genInfo.setSpacingAfter(titleSpacingA);
            genInfo.setSpacingBefore(titleSpacing);
            document.add(genInfo);

            if (!rcdMistakes.isEmpty()) {
                Paragraph genInfoDesc = new Paragraph("Błędy różnicówek: ", ProFonts.fontNormal);
                genInfoDesc.setAlignment(Element.ALIGN_LEFT);
                genInfoDesc.setIndentationLeft(indentation);
                document.add(genInfoDesc);
                for (String mistake : rcdMistakes) {
                    Paragraph mistakeDesc = new Paragraph("- " + mistake, ProFonts.fontNormal);
                    mistakeDesc.setAlignment(Element.ALIGN_LEFT);
                    mistakeDesc.setIndentationLeft(25f);
                    document.add(mistakeDesc);
                }
            }

            if (!omMistakes.isEmpty()) {
                Paragraph genInfoDesc = new Paragraph("Błędy pętli zwarcia: ", ProFonts.fontNormal);
                genInfoDesc.setAlignment(Element.ALIGN_LEFT);
                genInfoDesc.setIndentationLeft(indentation);
                document.add(genInfoDesc);
                for (String mistake : omMistakes) {
                    Paragraph mistakeDesc = new Paragraph("- " + mistake, ProFonts.fontNormal);
                    mistakeDesc.setAlignment(Element.ALIGN_LEFT);
                    mistakeDesc.setIndentationLeft(25f);
                    document.add(mistakeDesc);
                }
            }

        }

        Paragraph statsTitle = new Paragraph("Statystyki pomiarów:", ProFonts.fontNormalBold);
        statsTitle.setAlignment(Element.ALIGN_LEFT);
        statsTitle.setSpacingAfter(titleSpacingA);
        statsTitle.setSpacingBefore(titleSpacing);
        document.add(statsTitle);


        Paragraph s1 = new Paragraph("Mieszkania z orzeczeniem `Instalacja niedopuszczona do użytku` (" + grade2Flats.size() + "):", ProFonts.fontNormal);
        s1.setAlignment(Element.ALIGN_LEFT);
        s1.setIndentationLeft(indentation);
        document.add(s1);
        index = 1;
        for (String g2 : grade2Flats) {
            Paragraph gradedFlat = new Paragraph(index + ". Mieszkanie " + block.number + "/" + g2, ProFonts.fontNormal);
            gradedFlat.setAlignment(Element.ALIGN_LEFT);
            gradedFlat.setIndentationLeft(25f);
            document.add(gradedFlat);
            index++;
        }

        Paragraph s3 = new Paragraph("Mieszkania z orzeczeniem `Instalacja dopuszczona do użytku` (" + grade0Flats.size() + "):", ProFonts.fontNormal);
        s3.setAlignment(Element.ALIGN_LEFT);
        s3.setIndentationLeft(indentation);
        document.add(s3);

        index = 1;
        for (String g0 : grade0Flats) {
            Paragraph gradedFlat = new Paragraph(index + ". Mieszkanie " + block.number + "/" + g0, ProFonts.fontNormal);
            gradedFlat.setAlignment(Element.ALIGN_LEFT);
            gradedFlat.setIndentationLeft(25f);
            document.add(gradedFlat);
            index++;
        }

        Paragraph s5 = new Paragraph("Mieszkania z orzeczeniem `Instalacja dopuszczona do użytku po usunięciu usterek` (" + grade1Flats.size() + "):", ProFonts.fontNormal);
        s5.setAlignment(Element.ALIGN_LEFT);
        s5.setIndentationLeft(indentation);
        document.add(s5);
        index = 1;
        for (String g1 : grade1Flats) {
            Paragraph gradedFlat = new Paragraph(index + ". Mieszkanie " + block.number + "/" + g1, ProFonts.fontNormal);
            gradedFlat.setAlignment(Element.ALIGN_LEFT);
            gradedFlat.setIndentationLeft(25f);
            document.add(gradedFlat);
            index++;
        }


    }

    private void addData(String type, Date date) throws DocumentException {


        Paragraph measurementsCon = new Paragraph("3. Warunki pomiarów", ProFonts.fontNormalBold);
        measurementsCon.setAlignment(Element.ALIGN_LEFT);
        measurementsCon.setSpacingAfter(titleSpacingA);
        measurementsCon.setSpacingBefore(titleSpacing);


        Paragraph con1 = new Paragraph("układ sieci: " + type + ", napięcie względem ziemi Uo = 230 [V], " + "napięcie probiercze: 500 [V]", ProFonts.fontNormal);
        con1.setAlignment(Element.ALIGN_LEFT);
        con1.setIndentationLeft(indentation);

        Paragraph meDate = new Paragraph("4. Data badania", ProFonts.fontNormalBold);
        meDate.setSpacingBefore(titleSpacing);
        meDate.setSpacingAfter(titleSpacingA);
        meDate.setAlignment(Element.ALIGN_LEFT);
        Paragraph meDate1 = new Paragraph(formatMonthAndYear(date), ProFonts.fontNormal);
        meDate1.setAlignment(Element.ALIGN_LEFT);
        meDate1.setIndentationLeft(indentation);

        Paragraph deviceTitle = new Paragraph("5. Przyrządy pomiarowe", ProFonts.fontNormalBold);
        deviceTitle.setSpacingBefore(titleSpacing);
        deviceTitle.setSpacingAfter(titleSpacingA);
        deviceTitle.setAlignment(Element.ALIGN_LEFT);
        Paragraph device = new Paragraph("1. Sonel MPI 540, Miernik instalacji elektrycznych, AH 5137", ProFonts.fontNormal);
        device.setAlignment(Element.ALIGN_LEFT);
        device.setIndentationLeft(indentation);

        Paragraph me = new Paragraph("6. Wynik pomiarów", ProFonts.fontNormalBold);
        me.setSpacingBefore(titleSpacing);
        me.setSpacingAfter(titleSpacing);
        me.setAlignment(Element.ALIGN_LEFT);

        document.add(measurementsCon);
        document.add(con1);
        document.add(meDate);
        document.add(meDate1);
        document.add(deviceTitle);
        document.add(device);
        document.add(me);

    }

    private Uri createPdfFileInDownloads(String fileName) throws IOException, DocumentException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        String currentYear = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());

        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/RemaPomiary/protokoły/" + currentYear;
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            outputStream = resolver.openOutputStream(uri);
            if (outputStream != null) {
                document = new Document(PageSize.A4, 36, 36, 50, 50);
                writer = PdfWriter.getInstance(document, outputStream);
            }
        }
        return uri;
    }

    public String formatMonthAndYear(Date date) {
        if (date == null) return "";

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String[] months = {
                "styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec",
                "lipiec", "sierpień", "wrzesień", "październik", "listopad", "grudzień"
        };

        int monthIndex = cal.get(Calendar.MONTH); // 0-based
        int year = cal.get(Calendar.YEAR);

        return months[monthIndex] + " " + year;
    }


    private void addHeader() throws DocumentException {
        Chunk phuChunk = new Chunk("PHU ", ProFonts.logoNormal);

// Chunk dla REMA z inną czcionką
        Chunk remaChunk = new Chunk("REMA", ProFonts.logo);

// Tworzymy Paragraph i dodajemy oba Chunki
        Paragraph companyName = new Paragraph();
        companyName.add(phuChunk);
        companyName.add(remaChunk);

        companyName.setSpacingAfter(5f);
        companyName.setAlignment(Element.ALIGN_LEFT);

        document.add(companyName);

        Paragraph companyDetails = new Paragraph ("Marek Rejner\n" + "ul. Wyzwolenia 10A/2, 41-907 Bytom\n" +
                "NIP: 626-101-54-81\n" +
                "Tel: 601-411-391",
                ProFonts.fontNormal);
        companyDetails.setAlignment(Element.ALIGN_LEFT);
        companyDetails.setSpacingAfter(5f);
        document.add(companyDetails);

        LineSeparator line = new LineSeparator(1f, 100f, null, Element.ALIGN_CENTER, 0);
        document.add(new Chunk(line));

        String currentDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        Paragraph dateParagraph = new Paragraph("Data dokumentu: " + currentDate, ProFonts.fontNormal);
        dateParagraph.setAlignment(Element.ALIGN_RIGHT);
        dateParagraph.setSpacingBefore(1f);
        dateParagraph.setSpacingAfter(15f);
        document.add(dateParagraph);
    }


    private void addTitleSection(String protocolNumber, String type, int hasRCD) throws DocumentException {
        Paragraph title = new Paragraph(protocolNumber, ProFonts.fontBold14);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Z badań okresowych", ProFonts.fontNormal);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingBefore(5f);
        subtitle.setSpacingAfter(15f);
        document.add(subtitle);

        Paragraph desc1 = new Paragraph("Wynik z pomiarów rezystancji izolacji instalacji " + type, ProFonts.fontNormal);
        desc1.setAlignment(Element.ALIGN_LEFT);
        document.add(desc1);

        Paragraph desc2 = new Paragraph("Wynik z pomiarów skuteczności samoczynnego wyłączenia", ProFonts.fontNormal);
        desc2.setAlignment(Element.ALIGN_LEFT);

        if (hasRCD == 1) {
            Paragraph desc3 = new Paragraph("Wynik z badania wyłączników różnicowoprądowych", ProFonts.fontNormal);
            desc3.setAlignment(Element.ALIGN_LEFT);
            desc3.setSpacingAfter(25f);
            document.add(desc2);
            document.add(desc3);
        } else {
            desc2.setSpacingAfter(25f);
            document.add(desc2);
        }
    }


    private void addDetailsTable(String clientData, String objectData) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 5f});

        table.addCell(CellGenerator.createValueCell("1. Zleceniodawca"));
        table.addCell(CellGenerator.createValueCell(clientData));

        table.addCell(CellGenerator.createValueCell("2. Obiekt"));
        table.addCell(CellGenerator.createValueCell(objectData));

        document.add(table);
    }

    private void closeDocument() {
        if (document != null && document.isOpen()) {
            document.close();
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Błąd podczas zamykania OutputStream", e);
            }
        }
    }
}
