# Android Permissions Demo

This project shows how two completely separated apps (signed with completely different signatures) can exchange data in a protected manner.

There are two approaches demonstrated:

1. One app, "System 1", renders some data and launches another app, "System 2" with it. The "System 2" is protecting it's dedicated entry point with a permission which the Android system enforces. The user is the ultimate authority to grant this permission and the Android system delegates the request as per standard permissions behaviour (a permissions system dialog).
2. One app, "System 1", renders some data and stores it in an internal repository, exposing protected access to it through a ContentProvider. Another app, "System 2", then uses this ContentProvider to extract the data. Again, "System 1" is protecting access to it's ContentProvider by requiring a custom read permission, which is handled as per standard patterns as described above.

