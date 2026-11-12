# The connection belongs to the app, and remembered panels are settings

The first version held the **Panel** in a view model belonging to the control screen. On the phone
this failed in the obvious way: an **Exposure** lasts ten minutes, and ten minutes is exactly long
enough for somebody to lock the phone or open something else, after which the screen came back with
no panel and the only way forward was another scan. The **Panel** kept running regardless, because
it is the source of truth (ADR 0003) — the app had simply lost sight of it.

Three separate things were wrong, and it is worth keeping them apart.

The connection was owned by a screen. It is now owned by the application, in a `PanelConnection`
that lives as long as the process. A screen asks it questions and gives it orders; it does not hold
the link.

Nothing noticed a link going down. Kable reports it, and we were not listening, so a panel that hung
up looked exactly like one that was connected but quiet. `BluetoothPanel` now watches the peripheral's
own state, and `PanelConnection` reconnects with a widening delay — one, two, five, ten, fifteen
seconds — giving up after eight attempts, because a panel switched off at the wall will not answer
and scanning at it all afternoon costs the battery for nothing.

Nothing kept the process alive. A foreground service now runs while a panel is held, typed
`connectedDevice`, and its notification is where the remaining time can be read once the phone is in
a pocket. It also carries a Stop button, which is the fastest way to switch the light off and
something the vendor app cannot do at all.

## Remembering panels

A panel's id is its Bluetooth address, and Kable will open a peripheral straight from one, so a
remembered panel reconnects **without scanning at all** — a second or two instead of the fifteen a
scan needs, and it works when the panel is not advertising loudly enough to be found.

These are kept in the app's preferences as JSON rather than in the session database. ADR 0005 chose
SQLDelight for **history** because the questions asked of history are aggregations; a remembered
panel is the opposite of that — a three-line list, read whole, never counted. It is a setting, and
it stays one even once the database exists.

## Consequences

No screen may own a panel again, which also means the control screen has to survive being handed a
panel that is already connected, already lit, and part way through a countdown — as it now does.

The retry ladder is a guess. Nobody has measured how the ML4 behaves when the phone sleeps for an
hour, or whether it drops the link at a predictable interval. The numbers are deliberately in one
place so they can be corrected once that is known.

Reconnecting by address has only been proved for a panel that has been seen at least once on this
phone. Whether an address stays valid across a panel power cycle is untested; ML4 addresses look
static — the Bluetooth name contains the address — but that is an observation, not a guarantee.
