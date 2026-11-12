# SQLDelight for session history

The vendor app stores its state with KStore — one serialised object graph in a file — and
keeps no history at all, which is precisely the gap this app fills. Matching that storage
choice would keep the two apps alike, but the questions this app is
built to answer ("how many minutes has this **Area** had today", "how many days in a row")
are aggregations, and computing those by deserialising a growing blob into memory gets worse
every day the user keeps using it.

We use SQLDelight rather than Room because ADR 0001 requires the core to stay free of
Android APIs, and SQLDelight's generated queries are plain Kotlin that moves into a KMP
`commonMain` untouched. Its SQL is also checked at compile time, so a broken query fails
the build rather than a user's history.
