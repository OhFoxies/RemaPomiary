package com.rejner.remapomiary.generator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.room.Insert;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.rejner.remapomiary.data.dao.CircuitDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatFullData;
import com.rejner.remapomiary.generator.constants.ProFonts;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ProtocolGenerator {

    private static final String TAG = "ProtocolGenerator";

    private final Context context;
    private Document document;
    private PdfWriter writer;
    private OutputStream outputStream;
    AppDatabase db;

    public ProtocolGenerator(Context context) {
        this.context = context;
        db = AppDatabase.getDatabase(context.getApplicationContext());

    }

    public Uri generate(String fileName, int blockId) {
        try {
            String protocolNumber = "PROT/" + blockId + "/" + new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());


            Uri fileUri = createPdfFileInDownloads(fileName);
            if (fileUri == null) {
                throw new IOException("Nie udało się utworzyć URI dla pliku PDF.");
            }

            document.open();
//            document.add();

            List<FlatFullData> flats = db.flatDao().getFlatsSync(blockId);
            BlockFullData blockFullData1 = db.blockDao().getBlockById(blockId);
            Paragraph p = new Paragraph("BLOK " + blockFullData1.block.number, ProFonts.large);
            document.add(p);
            for (FlatFullData flat : flats) {
                document.newPage();
                addHeader();
                addTitleSection(protocolNumber, flat.flat.type, flat.flat.hasRCD);
                BlockFullData blockFullData = db.blockDao().getBlockById(blockId);

                String clientData = blockFullData.client.name + "\n" +
                        "ul. " + blockFullData.client.street + ", " + blockFullData.client.postal_code + " " + blockFullData.client.city;
                String objectData = "Budynek wielorodzinny" + "\n" + "ul. " + blockFullData.block.street + " " + blockFullData.block.number + ", " + blockFullData.block.postal_code + blockFullData.block.city + "\n" + "LOKAL: " + flat.flat.number + "\n" + "Napięcie znamionowe: 230V/400V";
                addDetailsTable(clientData, objectData);
                addData(flat.flat.type, flat.flat.creation_date);

                Paragraph title1 = new Paragraph("Wyniki z pomiarów rezystancji izolacji instalacji " + flat.flat.type, ProFonts.fontNormal);
                title1.setAlignment(Element.ALIGN_LEFT);
                title1.setSpacingAfter(5f);
                document.add(title1);
                PdfPTable table = createMeasurementTable(ProFonts.medium, ProFonts.medium, flat.flat);
                document.add(table);
                PdfPTable table2 = createRCDTable(flat);
                document.add(table2);
                addFooter("Strona 1/2");
            }

            return fileUri;

        } catch (Exception e) {
            Log.e(TAG, "Błąd podczas generowania PDF", e);
            return null;
        } finally {
            closeDocument();
        }
    }
    public PdfPTable createRCDTable(Flat flat) {
        PdfPTable table = new PdfPTable(11);

        String[] headers = {"lp.", "Badany punkt", "Wyłącznik RCD", "Typ", "IΔn [mA]", "la [mA]", "ta [ms]", "t rcd [ms]", "Ub [V]", "UI [V]", "Ocena"};
        for (String header : headers) {
            if (header.contains("[")) {
                int start = header.indexOf("[");
                int end = header.indexOf("]");
                String main = header.substring(0, start -1);
                String sub = header.substring(start, end);
                Chunk mainChunk = new Chunk(main, ProFonts.medium);
                Chunk subChunk = new Chunk(sub, ProFonts.medium);
                Phrase phrase = new Phrase();
                phrase.add(mainChunk);
                phrase.add(subChunk);
                PdfPCell cell = new PdfPCell(phrase);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPaddingBottom(5f);
                cell.setPaddingTop(5f);
                table.addCell(cell);
            }
        }
        return table;

    }
    public PdfPTable createMeasurementTable(Font fontHeader, Font fontCell, Flat flat) throws DocumentException {
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

        // --- Nagłówki ---
        for (String h : headers) {
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
                    unit = new Chunk("\n[MΩ]", fontHeader);
                } else {
                    unit = new Chunk("\n[GΩ]", fontHeader);
                }
                phrase = new Phrase();
                phrase.add(mainChunk);
                phrase.add(subChunk);
                phrase.add(unit);
            } else {
                phrase = new Phrase(h, fontHeader);
            }

            PdfPCell cell = new PdfPCell(phrase);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPaddingBottom(5f);
            cell.setPaddingTop(5f);
            table.addCell(cell);
        }

        // --- Dane ---
        List<Circuit> circuits = db.circuitDao().getCircuitsForFlatSync(flat.id);
        int index = 1;

        for (Circuit c : circuits) {
            List<String> values = new ArrayList<>();

            if (isTNC) {
                // dokładnie 10 kolumn
                values.add(Integer.toString(index)); // 1
                values.add(c.name); // 2
                for (int i = 0; i < 6; i++) values.add("-"); // 3-9
                values.add("1"); // 10
                values.add("Pozytywna"); // 10
            } else {
                // dokładnie 14 kolumn
                values.add(Integer.toString(index));
                values.add(c.name);
                for (int i = 0; i < 10; i++) values.add("-");
                values.add("1");
                values.add("Pozytywna");
            }

            if (isTNC) {
                switch (c.type) {
                    case "L1":
                        values.set(5, ">2");
                        break;
                    case "L2":
                        values.set(6, ">2");
                        break;
                    case "L3":
                        values.set(7, ">2");
                        break;
                    case "3f":
                        for (int i = 2; i <= 7; i++) values.set(i, ">2");
                        break;
                }
            } else {
                switch (c.type) {
                    case "L1":
                        values.set(5, ">2");
                        values.set(8, ">2");
                        values.set(11, ">2");
                        break;
                    case "L2":
                        values.set(6, ">2");
                        values.set(9, ">2");
                        values.set(11, ">2");

                        break;
                    case "L3":
                        values.set(7, ">2");
                        values.set(10, ">2");
                        values.set(11, ">2");

                        break;
                    case "3f":
                        for (int i = 2; i <= 11; i++) values.set(i, ">2");
                        break;
                }
            }
            for (String v : values) {
                table.addCell(createCell(v));
            }

            index++;
        }

        return table;
    }


    private PdfPCell createCell(String data) {
        PdfPCell cell = new PdfPCell(new Phrase(data, ProFonts.medium));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(5f);
        cell.setPaddingTop(5f);
        return cell;
    }

    private void generateIsolation() {

    }

    private void addData(String type, Date date) throws DocumentException {
        float titleSpacing = 8f;
        float titleSpacingA = 2f;

        Paragraph measurementsCon = new Paragraph("3. Warunki pomiarów", ProFonts.fontNormalBold);
        measurementsCon.setAlignment(Element.ALIGN_LEFT);
        measurementsCon.setSpacingAfter(titleSpacingA);
        measurementsCon.setSpacingBefore(titleSpacing);
        float indentation = 15f;

        Paragraph con1 = new Paragraph("Układ sieci: " + type, ProFonts.fontNormal);
        con1.setAlignment(Element.ALIGN_LEFT);
        con1.setIndentationLeft(indentation);


        Paragraph con2 = new Paragraph("Napięcie względem ziemi Uo = 230 [V]", ProFonts.fontNormal);
        con2.setAlignment(Element.ALIGN_LEFT);
        con2.setIndentationLeft(indentation);

        Paragraph con3 = new Paragraph("Napięcie probiercze: 500 [V]", ProFonts.fontNormal);
        con3.setAlignment(Element.ALIGN_LEFT);
        con3.setIndentationLeft(indentation);

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
        Paragraph device = new Paragraph("1. Sonel MPI 540, Miernik instalacji elektrycznych, EK 0076", ProFonts.fontNormal);
        device.setAlignment(Element.ALIGN_LEFT);
        device.setIndentationLeft(indentation);

        Paragraph me = new Paragraph("6. Wynik pomiarów", ProFonts.fontNormalBold);
        me.setSpacingBefore(titleSpacing);
        me.setSpacingAfter(titleSpacing);
        me.setAlignment(Element.ALIGN_LEFT);

        document.add(measurementsCon);
        document.add(con1);
        document.add(con2);
        document.add(con3);
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

        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/Protokoły";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        }

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            outputStream = resolver.openOutputStream(uri);
            if (outputStream != null) {
                document = new Document();
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
        Paragraph companyName = new Paragraph("PHU Rema", ProFonts.fontBold14);
        companyName.setAlignment(Element.ALIGN_LEFT);
        document.add(companyName);

        Paragraph companyDetails = new Paragraph("ul. Przykładowa 12, 00-123 Warszawa\n" +
                "NIP: 123-456-78-90\n" +
                "Tel: 123 456 789", ProFonts.fontNormal);
        companyDetails.setAlignment(Element.ALIGN_LEFT);
        companyDetails.setSpacingAfter(10f);
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
        Paragraph title = new Paragraph("PROTOKÓŁ NR " + protocolNumber, ProFonts.fontBold14);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Z badań okresowych", ProFonts.fontNormal);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingBefore(10f);
        subtitle.setSpacingAfter(20f);
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
//        table.setSpacingAfter(20f);
        table.setWidths(new float[]{2f, 5f});

        table.addCell(createLabelCell("1. Zleceniodawca"));
        table.addCell(createValueCell(clientData));

        table.addCell(createLabelCell("2. Obiekt"));
        table.addCell(createValueCell(objectData));

        document.add(table);
    }

    private PdfPCell createLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, ProFonts.fontNormalBold));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPaddingBottom(12f);
        return cell;
    }

    private PdfPCell createValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, ProFonts.fontNormalBold));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPaddingBottom(12f);
        return cell;
    }


    private void addFooter(String footerText) {
        PdfContentByte cb = writer.getDirectContent();
        Rectangle pageSize = document.getPageSize();
        float yLine = 40f;

        // Rysowanie linii
        cb.setLineWidth(1f);
        cb.moveTo(document.left(), yLine);
        cb.lineTo(pageSize.getRight() - document.rightMargin(), yLine);
        cb.stroke();

        // Wstawienie podanego tekstu
        ColumnText.showTextAligned(
                cb,
                Element.ALIGN_RIGHT,
                new Phrase(footerText, ProFonts.fontNormal), // Użycie podanego Stringa
                pageSize.getRight() - document.rightMargin(),
                yLine - 12,
                0
        );
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
