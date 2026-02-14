This file contains a list of issues that were found in testing.
Any time you read the file, check which points are not yet created as tasks (TaskCreate) and set them up for development.

# Issues

1. The map does not load. Is it missing an API key?
2. Searching an address does not work; is it missing a search button, or is this related to the map not showing anything at all?
3. ~~The user can manually start and stop the tracking, but no events appear in the list of recordings.~~ **FIXED** - Added `WHERE endTime IS NOT NULL` filter to entries query to show only completed tracking sessions.
4. All the settings in the settings page cannot be changed. Tapping a value shows some animation, but no update form/dialog opens.
