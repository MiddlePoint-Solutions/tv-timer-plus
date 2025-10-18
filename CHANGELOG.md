# Changelog

All notable changes to this project will be documented in this file.

## [1.2.0]

### Added
- An in-app review prompt has been added to gather user feedback.
- A new app selection screen has been introduced.

### Changed
- The timer state is now observed for a better user experience.
- The timer overlay is now shown in the final minute of the timer.
- ViewModel logic has been reorganized and now tracks the selected app.

## [1.1.1]

### Fixed
- Sleep not working on some TV devices.

## [1.1.0]

### Added
- Users can now create and save their own custom time options.
- Time options can be deleted by long-pressing the item.
- A back-press will cancel the delete operation.
- A little easter egg when you long-press the "Custom" button.

### Changed
- UI events are now handled in dedicated ViewModels for each screen.
- Added smooth transitions for deleting time options.

## [1.0.0] - Initial Release

### Added
- Select from a predefined list of times to set a sleep timer for your TV.
