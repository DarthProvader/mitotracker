# The panel is the source of truth for sessions

A **Panel** can be operated from its own touchscreen, accepts only one connected client at a
time, and Android drops Bluetooth connections when the phone sleeps — so an app that treats
its own button presses as the record of what happened will be wrong regularly and silently.
Instead the app asks `DEVICE_STATUS` whenever it connects and reconciles its record with
what the hardware reports, which also means **Sessions** started from the touchscreen are
picked up rather than lost.

While disconnected the app infers the ending, because it set the **Countdown** itself and
the Panel switches off on its own when the countdown expires.

## Consequences

An indefinite Countdown of zero cannot be inferred, so a Session started that way stays open
until the app next connects. Either avoid offering zero, or mark such Sessions as
unconfirmed until they are reconciled.

Only the `running` flag in the status response is trustworthy. The countdown and remaining
fields keep their last values after a stop, so they must not be used to decide whether the
light is on.

A Session discovered on reconnect has no **Preset** behind it and therefore no **Area**. It
is recorded anyway and left for the user to label, rather than being dropped or guessed at.
