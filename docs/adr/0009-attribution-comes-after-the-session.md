# 9. Attribution comes after the session, not before it

Date: 2026-07-31

## Status

Accepted. Supersedes the pre-start flow described in [0008](0008-a-session-may-have-no-exposure.md),
which stands otherwise.

## Context

The first build put Target Area between the start button and the panel. Pressing "Start therapy" did
not start anything: it pushed a screen, and the light came on only after an Area had been chosen and
confirmed there. Presets went the other way and did everything in one tap — brightness, countdown,
areas and start — so the same screen was a light switch, a channel mixer and a preset picker at once,
and the one thing the app was opened for cost an extra screen.

That ordering came from wanting good data. Dose is the feature the vendor app has no answer to, and
dose needs to know which Area received the light, so asking first guarantees an answer. But it also
made naming a body part the price of turning a lamp on, which is the wrong trade for a device somebody
uses for ten minutes before breakfast.

There is also a case that the pre-start question cannot express at all, and that the app's own
vocabulary was written for: turning around halfway through. One Session, two Exposures.

## Decision

**The round start button lights the panel immediately**, with whatever the panel is already set to. No
screen in between.

**Attribution happens at the end**, in the session summary that already existed for confirming what was
recorded. It becomes the primary place a run is credited, not a second chance. Dismissing it without
choosing anything is a supported outcome and leaves the Session unattributed — ambient light, counting
towards no dose and no streak.

**Configuration moves to its own screen.** Channel brightness, countdown length and Preset selection
live behind the card that reports them. Choosing a Preset there loads it — channels, countdown and the
Areas it is for — and hands back to the start button rather than starting the panel itself, so the app
has exactly one place where the light comes on.

**A run the app starts is not "unclaimed".** That word now means only what it was coined for: a panel
found already lit, started from its own touchscreen, whose minutes belong to nobody. Starting from the
app with no Area named is not a mystery to be solved mid-run; the summary will ask.

**Turning around splits the Session by the clock, not down the middle.** The tracker keeps the run as a
list of legs — a set of Areas and the moment the light was pointed at them — and each leg lasts until
the next one begins. A turn seven minutes into ten is seven and three. Correcting a run *after* it has
finished still divides it evenly, because by then nobody knows when the turn happened; splitting by real
time is only possible while the app is watching it happen.

**A panel set to nothing gets a first run rather than a dead button.** If every channel reads zero per
cent, pressing start would light nothing, so the app applies the catalog's explicit first-run Preset —
and says which one, on the screen, before the button is pressed. It is explicit rather than whichever
routine happens to be listed first, so reordering the catalog cannot change what the button does. A
countdown of zero is not treated the same way:
holding the light on until it is stopped is a real setting with its own word for it.

## Consequences

The start screen becomes a switch: a countdown, a round button, and one line reporting what the button
will do. That is a better screen and a smaller one.

Dose data gets slightly worse in the case where somebody starts the panel and dismisses the summary
without answering. This is deliberate: an unattributed Session is an honest record of what the hardware
did, and the model has always allowed it. The pre-start question guaranteed an answer by making the
answer compulsory, which is a different thing from making it accurate.

Presets carry their Areas, so a run started from a Preset arrives at the summary already attributed and
only needs confirming — which is the good path, and the one worth making easy.

The legs are held in memory by the tracker, which is application-scoped for the same reason the
connection is ([0007](0007-the-connection-belongs-to-the-app-not-a-screen.md)). A turn survives the
screen being closed; it does not survive the process being killed, and neither does the session it
belongs to, which is the same limit history has until SQLDelight arrives
([0005](0005-sqldelight-for-history.md)).
