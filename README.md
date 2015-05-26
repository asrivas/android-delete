# Google Drive Android API Delete Sample.

Google Drive Android API Delete Sample app demonstrates deleting of
DriveResources via the [Google Drive Android API][1] available in
[Google Play Services][2]. Only DriveResources accessible to an app and
can owned by the authenticated user can be deleted. For full API details
check [Google Drive Android API Reference][3].

### How deleting files in works.
Delete is permanent and it bypasses trash. The authenticated user must be the
owner of the resource. If the DriveResource being deleted is a DriveFolder then
The application must has access to all descendants. Delete is intended for
app data, for user data consider using Trash since it is recoverable while
Delete is not.

### Using the sample.
This sample (Simple Math) generates simple math problems and allows the user
to submit answers. Problems and answers are stored in app data. If the user
hits the reset button the saved problems are deleted.

[1]: https://developers.google.com/drive/android/intro
[2]: http://developer.android.com/google/play-services
[3]: https://developer.android.com/reference/com/google/android/gms/drive/package-summary.html
