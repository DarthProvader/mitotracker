# The panel is an interface, and one implementation is a simulator

A **Panel** accepts one connected client at a time and is not always within reach of the
machine the app is being written on, so building against the hardware directly would mean
the user interface can only be worked on while the panel is free, and a demonstration that
depends on a Bluetooth connection succeeding in an unfamiliar room can fail at the worst
moment.

So everything above the transport talks to a `Panel` interface, and there are two
implementations: one driving real hardware through Kable, and one simulating a panel in
memory — it accepts the same commands, counts its **Countdown** down in real time and
reports the same status. Which one is in use is switchable in settings.

## Consequences

No user interface or domain code may call Kable directly, or the simulator stops being a
faithful stand-in. The simulator is also the only way most automated tests can exercise
anything above the protocol layer, so it is production code and is kept honest against the
real panel's observed behaviour, including its refusal of a second client and its habit of
dropping the connection when the phone sleeps.
