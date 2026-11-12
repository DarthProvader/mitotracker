# Android first, in Kotlin, with a portable core

The panel's official app is Kotlin Multiplatform with Compose and the Kable BLE library, so
writing this one in the same language and against the same BLE library keeps the two
directly comparable and lets protocol knowledge move between them without translation.
Kotlin also needs nothing beyond the Android SDK and JDK 17, whereas Flutter would add a
fresh SDK and a second language for no gain.

This is a plain Android app rather than a Kotlin Multiplatform one, because iOS cannot be
built without a Mac and KMP would slow every iteration for a payoff that is not yet needed. To keep that door open, the protocol, domain and persistence code
stay in modules that touch no Android APIs, so moving them into a KMP `commonMain` later is
mechanical rather than a rewrite.
