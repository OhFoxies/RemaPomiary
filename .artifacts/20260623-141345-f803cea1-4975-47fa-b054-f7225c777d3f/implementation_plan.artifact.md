# Implementation Plan - Adding Photos to Protocol

This plan outlines the changes to `ProtocolGenerator.java` to include photo pages at the end of each flat's report. Photos will be sourced from `FlatPhoto` entries (for boards and notes) and `OutletMeasurement` entries (for measurements).

## Proposed Changes

### [Generator]

#### [ProtocolGenerator.java](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/java/com/rejner/remapomiary/generator/ProtocolGenerator.java)

- Add a new private method `addPhotoPages(Flat flat)` that will:
    - Create a new page.
    - Add a title "Dokumentacja fotograficzna".
    - Fetch and add photos from `FlatPhoto` where `type = 0` (Rozdzielnia).
    - Fetch and add photos from `FlatPhoto` where `type = 1` (Uwagi/Usterki).
    - Fetch and add photos from `OutletMeasurement` for all rooms in the flat.
    - Layout photos (e.g., 2 per page) with proper captions.
- Capions format:
    - **Boards**: "Rozdzielnia - [Board Name]" or "Zdjęcie rozdzielni" (if name unavailable).
    - **Notes**: "Uwaga/Usterka - [Description]".
    - **Measurements**: "Pomiar: [Room Name] - [Appliance Name]".
- Update the main loop in `generate()` to call `addPhotoPages(flat.flat)` after `createEndSummary(next, flat.flat)`.
- **Note**: `FlatPhotoDao` needs to be accessed sync. Since `ProtocolGenerator` uses `AppDatabase`, I can call `db.flatPhotoDao().getPhotosByFlatAndTypeSync(...)`.

### [Data Access]

#### [FlatPhotoDao.java](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/java/com/rejner/remapomiary/data/dao/FlatPhotoDao.java)
- Add a synchronous query:
```java
@Query("SELECT * FROM flat_photos WHERE flat_id = :flatId AND type = :type ORDER BY id ASC")
List<FlatPhoto> getPhotosByFlatAndTypeSync(int flatId, int type);
```

#### [OutletMeasurementDao.java](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/java/com/rejner/remapomiary/data/dao/OutletMeasurementDao.java)
- Add a synchronous query to get all measurements with photos for a flat:
```java
@Query("SELECT om.* FROM outletMeasurement om " +
       "INNER JOIN room r ON om.roomId = r.id " +
       "WHERE r.flatId = :flatId AND om.photo_path IS NOT NULL AND om.photo_path != ''")
List<OutletMeasurement> getMeasurementsWithPhotosForFlatSync(int flatId);
```

## Verification Plan

### Automated Tests
- I cannot run full Android instrumented tests, but I can verify the logic by analyzing the generated code and ensuring database queries are correct.

### Manual Verification
- Deploy the app to the device.
- Add some photos in `BoardActivity`, `BoardCommonSpace`, `RoomActivity`, and `NotesActivity`.
- Generate a protocol for the block.
- Verify that the PDF contains additional pages at the end of each flat section with correctly labeled photos.
- Check both common spaces and regular flats.
