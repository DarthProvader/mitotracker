# Support both panel generations, and record brightness per channel

The app was developed against an ML4 with two **Channels**, but Mito Light's current range
is the 5.0 series with six. Supporting both is therefore worth real effort, and the storage half of it costs nothing: ADR 0002
already commits to recording each Channel's **Brightness** as a fact, so keeping that as a
channel-to-brightness mapping rather than two red/near-infrared fields avoids migrating
session history later.

The behavioural half does cost something. Six-channel panels use a different command to set
brightness (`0x54` with light modes rather than `0x41` per channel), have built-in modes and
a different minimum brightness. That code is written from the vendor app's own source, but
unlike the ML4 path it is **unverified against hardware** — no 5.0 was available to test on.

## Consequences

Capability is selected from the Bluetooth name prefix, exactly as the vendor app does it, and
the six-channel path is kept strictly separate so it cannot regress the tested two-channel
one. Until a 5.0 has been driven successfully, treat that path as a plausible implementation
rather than a working feature.
