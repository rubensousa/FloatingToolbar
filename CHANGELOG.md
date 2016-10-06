# Changelog

### 1.2.0 (06/10/2016)
- Added floatingAutoHide attribute
- Updated support library to 24.2.1

### 1.1.1 (20/08/2016)
- Fixed dp to pixels conversion
- Updated support library to 24.2.0
- Copied the new floating action button behavior.

### 1.1.0 (04/08/2016)
- Add MorphListener to receive morph animation events.
- Fix issue that caused the FAB's position to be incorrect when the app window is resized.
- Fix issue that caused the FAB's position to be incorrect when the FloatingToolbar is attached to an AppBarLayout on API < 21
- Fix issue with animation on API < 21
- Added sources and javadoc to the build
- Update support library to 24.1.1

### 1.0.0 (02/07/2016)
- Improved behavior with SnackBar
- Improved animation on APIs below 21
- Improved animation duration on larger devices
- Update support library to 24.0.0
- Add getMenu() method
- Add detach methods to disable events
- Add floatingHandleFabClick attribute to handle fab click automatically (defaults to true)
- Add floatingToastOnLongClick attribute to show a toast with the MenuItem's title (defaults to true)
- Add support to anchored FAB

### 0.3 (29/04/2016)
- Improved behavior with SnackBar (Still needs some fixes though)
- Improved morph animation
- Throw IllegalStateException if FAB is not attached and show() or hide() are called

### 0.2 (26/04/2016)
- Renamed attributes
