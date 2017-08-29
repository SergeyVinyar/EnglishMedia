**EnglishMedia** is an open-source Android application for learning English via listening podcasts in English.

All podcasts are divided into groups according to their complexity: beginning, intermediate and advanced.
The  application automatically checks for new episodes, remembers which episodes you've already listened to
and starts playback from the point you stopped earlier.

Its Google Play page is [here](https://play.google.com/store/apps/details?id=ru.vinyarsky.englishmedia).

EnglishMedia uses Firebase Analytics and Crash Reporting services thus you need *google-services.json* file
to build the source code. You can download this file from the Firebase console using your Firebase account.
Or just remove line 
*apply plugin: 'com.google.gms.google-services'*
in *app/build.gradle*. No Firebase functionality in this case obviously.

The list of podcasts is distributed withing the APK-file because we need to manually sort them according to their complexity.
Please have a look at */app/src/main/res/xml/podcasts.xml* if you want to add more of them. Directory */Utils* contains an utility
for auto-generating *podcasts.xml* using the list of RSS-feeds in *urls.txt*.
Be care: GUIDs in *podcasts.xml* are generated once and shouldn't be changed later because they are using as identifiers
in the SQLite-database.

Currently my TODO list is:
* Add video podcast support
* Add additional categories like IT, Music, Education, Travelling, History, etc.

Suggestions and pull-requests are welcome.

Have a nice day :)
