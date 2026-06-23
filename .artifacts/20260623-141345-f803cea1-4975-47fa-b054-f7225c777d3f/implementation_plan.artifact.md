# Implementation Plan - UI Refactoring for BoardCommonSpace and RoomActivity

Refactor `BoardCommonSpace` activity to move the "add board" form to the bottom of the screen (matching `RoomActivity` style) and add a "Scroll to Top" button to both `BoardCommonSpace` and `RoomActivity`.

## Proposed Changes

### [Layouts]

#### [activity_board_common_space.xml](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/res/layout/activity_board_common_space.xml)
- Overhaul layout to include an `AppBarLayout` for navigation buttons.
- Wrap `RecyclerView` in a `CoordinatorLayout`.
- Add the "Add Board" form at the bottom (Spinner + EditText + Button).
- Add the `scrollToTopButton` FloatingActionButton.

#### [activity_room.xml](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/res/layout/activity_room.xml)
- Add the `scrollToTopButton` FloatingActionButton inside the existing `CoordinatorLayout`.

#### [DELETE] [headers_commonspace_board.xml](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/res/layout/headers_commonspace_board.xml)
- This layout is no longer needed as its content is moved to the activity layout.

---

### [Adapters]

#### [DELETE] [BoardHeaderAdapter.java](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/java/com/rejner/remapomiary/adapters/BoardHeaderAdapter.java)
- Removed as the functionality is moved to the Activity.

---

### [Activities]

#### [BoardCommonSpace.java](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/java/com/rejner/remapomiary/ui/activities/BoardCommonSpace.java)
- Remove `BoardHeaderAdapter` integration.
- Initialize and set up new UI elements (bottom form and navigation buttons).
- Implement board addition logic.
- Implement scroll-to-top logic.
- Update `setupRecyclerView` to use only `BoardAdapter`.

#### [RoomActivity.java](file:///C:/Users/Wojtek/AndroidStudioProjects/RemaPomiary/app/src/main/java/com/rejner/remapomiary/ui/activities/RoomActivity.java)
- Initialize `scrollToTopButton`.
- Implement scroll-to-top logic using a scroll listener on `roomRecyclerView`.

## Verification Plan

### Manual Verification
- **BoardCommonSpace Activity**:
    - Verify the "Add Board" form is at the bottom and stays there.
    - Verify navigation buttons (Summary, Board, Loop) are at the top and scroll with the list.
    - Verify adding a board works as before (with "inne" and "Rozdzielnia -" options).
    - Verify "Scroll to Top" button appears after scrolling down and works when clicked.
- **RoomActivity**:
    - Verify "Scroll to Top" button appears after scrolling down and works when clicked.
    - Verify it doesn't interfere with the existing "Add Room" form at the bottom.
