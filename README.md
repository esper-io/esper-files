# esper-files
## Esper Files Application

The Esper Files App can be pushed to any esper managed device and it will expose all the files from a particular folder.
 - Internal Storage Path: **storage/emulated/0/esperfiles**
 - External Storage Path: **ESPER_EXTERNAL/esperfiles**

Any files pushed from Content Management to the above mentioned folder will be visible in this app.

### * Customer Problem:
Customer wanted to push some files/videos to be used by their end user but do not want to give access to the entire Files and folders of the system.  Also they wanted to push these files using the CMS of esper.

### * Current Solution:
We don’t have. We ask them to enable system files/folder and push the content over there. Also enable that file app in the template to make it available by default.
Proposed Solution:
Creating a small esper files apk that can be installed as part of template or later on and act as  a Folder in the multi app mode. This app will read the specific folder from the device and just only display the content pushed from the console.

### * Salient Features:
* Ability to push any files to the device without giving access to the default android files/folder apps.
* Search Functionality Added
* In-built PDF Reader
* Can parse most type of file formats (Depends on the device as well)
* No change in the DPC/ Esper Console UI for now.
* Esper Files folder available right in the multi app mode.
* Ability to push content via Esper console CMS to the device.
* Available right away to provide it to the customer
* Folder Support Added
* Internal and External Storage Support
* Android 11 Support - Needs Testing
