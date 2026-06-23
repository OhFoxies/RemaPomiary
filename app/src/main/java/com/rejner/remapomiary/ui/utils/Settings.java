package com.rejner.remapomiary.ui.utils;

public class Settings {
//    Nazwa rozdzielni z WLZ mieszkań - pomiar izolacji mieszkania
    public final static String mainBoardName = "WLZ - mieszkania";
    public final static String measurementDone = "Pomiar gotowy ✅";
    public final static String measurementNotReady = "Pomiar niewykonany ❌";
//    Nazwa pokoju w którym są pętle zwarcia wszystkich lokali
    public final static String mainRoomName = "Lokale";
    public final static String installationTypeTNS = "TN-S";
    public final static String installationTypeTNC = "TN-C";
    public final static String installation1f = "1f";
    public final static String installation3f = "3f";
//    Brak uwag uznawane przez generator protokołów
    public final static String noNotes = "brak uwag";
//    brak bolca, używane przez generator skutuje nie wyświetleniem pomiaru różnicówki z danego gniazdka

    public final static String noGroundingBolt = "nie podłączony bolec";
//    zepsute, nie wyświetla pomiaru z różnicówki i nie pokazuje pomiaru gniazdka
public final static String flatNoAccess = "Brak dostępu";
    public final static String brokenOutlet = "zepsute";
    public final static String flatGotAccess = "Gotowe";
// W roomactivity i jego adapterach dla części wspólnej tworzony jest pokoj Lokale, w któym pomiary nie są wyświetlane od razu tylko są ładowane dopiero na życzenie użytkownika. Chcę taki sam system (ładowanie w tle, anulowanie, pasek ładowania) dla pomiarów izolacji WLZ Mieszkań dla BoardCommonSpace
}
