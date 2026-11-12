---
version: alpha
name: Mito Tracker
description: >
  A dark, instrument-like interface for driving a MITO LIGHT® red light therapy panel and keeping a
  record of what the light was used for. Built on Mito Light's own brand colours and typography,
  taken from their website stylesheets and their Android app.
colors:
  # The brand red. Identical in Mito Light's website CSS and in their app's own theme, so this is
  # not an interpretation — it is their colour.
  primary: "#F50537"
  primary-pressed: "#CF0000"
  # A lightened brand red, for text and small indicators where #F50537 on the dark background sits
  # just under the WCAG AA threshold. See Do's and Don'ts.
  primary-bright: "#FF5A6E"
  on-primary: "#FFFFFF"
  # Small text on saturated brand red needs to go darker, not lighter, to clear AA.
  on-primary-dark: "#0A0A0A"

  # The two light channels belong to one red-light panel. NIR is the deeper, quieter red.
  channel-red: "#EF202C"
  channel-nir: "#981B32"
  channel-nir-bright: "#D84A5F"

  background: "#141414"
  surface: "#282828"
  surface-high: "#3D3D3D"
  on-surface: "#F2F2F2"
  on-surface-muted: "#A8A8A8"
  outline: "#3D3D3D"
  scrim: "#00000054"

  success: "#24C02B"
  success-bright: "#4FE057"
  warning: "#F5A524"
  error: "#FF5A6E"

  # A four-step ramp for the exposure map, measuring how much light a day had rather than whether a
  # limit was passed: nothing, a short run, a full one, and a day with a lot of light in it.
  heat-none: "#232323"
  heat-light: "#5C0A1B"
  heat-full: "#A00A27"
  heat-over: "#F50537"

  # Depth. A card is a translucent gradient over the background, held together by a hairline of
  # white, so the red cast of the room shows through it. See Elevation & Depth.
  glass-top: "#B3282828"
  glass-bottom: "#661E1E1E"
  hairline: "#0DFFFFFF"
  hairline-bright: "#1AFFFFFF"
  # The deepest surface, under a pill or a segmented control sitting on a card. Not neutral: it
  # carries the same red cast as everything else.
  deep: "#1A0909"
  # The panel lighting the wall behind the interface, and the atmospheric washes inside cards.
  vignette: "#385C0A1B"
  glow-red: "#26EF202C"
  glow-amber: "#1AF5A524"
  glow-nir: "#14A98BFF"
typography:
  headline-lg:
    fontFamily: Archivo
    fontSize: 30px
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: 0.01em
  headline-md:
    fontFamily: Archivo
    fontSize: 26px
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: 0.01em
  headline-sm:
    fontFamily: Archivo
    fontSize: 20px
    fontWeight: 500
    lineHeight: 1.3
    letterSpacing: 0.01em
  title-md:
    fontFamily: Poppins
    fontSize: 18px
    fontWeight: 600
    lineHeight: 1.35
  title-sm:
    fontFamily: Poppins
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: 0.007em
  body-lg:
    fontFamily: Poppins
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.6
  body-md:
    fontFamily: Poppins
    fontSize: 15px
    fontWeight: 400
    lineHeight: 1.6
  body-sm:
    fontFamily: Poppins
    fontSize: 13px
    fontWeight: 400
    lineHeight: 1.5
  label-lg:
    fontFamily: Archivo
    fontSize: 20px
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: 0.02em
  label-md:
    fontFamily: Poppins
    fontSize: 12px
    fontWeight: 500
    lineHeight: 1.33
    letterSpacing: 0.03em
  label-sm:
    fontFamily: Poppins
    fontSize: 10px
    fontWeight: 500
    lineHeight: 1.6
    letterSpacing: 0.15em
  # Numbers that change while you watch them. Monospaced so a countdown does not shuffle its own
  # digits sideways every second.
  metric-lg:
    fontFamily: Roboto Mono
    fontSize: 44px
    fontWeight: 500
    lineHeight: 1
    letterSpacing: -0.02em
  metric-md:
    fontFamily: Roboto Mono
    fontSize: 20px
    fontWeight: 500
    lineHeight: 1.2
  metric-sm:
    fontFamily: Roboto Mono
    fontSize: 13px
    fontWeight: 400
    lineHeight: 1.2
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 24px
  xxl: 32px
  xxxl: 48px
  screen-margin: 20px
  card-padding: 20px
  gutter: 12px
rounded:
  none: 0px
  sm: 8px
  md: 12px
  lg: 16px
  xl: 24px
  full: 9999px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    borderColor: "{colors.hairline-bright}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.lg}"
    padding: 16px
    height: 60px
  button-primary-pressed:
    backgroundColor: "{colors.primary-pressed}"
    textColor: "{colors.on-primary}"
  button-primary-disabled:
    backgroundColor: "{colors.surface-high}"
    textColor: "{colors.on-surface-muted}"
  button-secondary:
    backgroundColor: "{colors.hairline}"
    textColor: "{colors.on-surface}"
    borderColor: "{colors.hairline-bright}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.lg}"
    padding: 16px
    height: 60px
  button-text:
    backgroundColor: "{colors.background}"
    textColor: "{colors.primary-bright}"
    typography: "{typography.title-sm}"
    rounded: "{rounded.full}"
    padding: 8px
  chip:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-surface-muted}"
    typography: "{typography.title-sm}"
    rounded: "{rounded.full}"
    padding: 12px
    height: 36px
  chip-selected:
    backgroundColor: "{colors.surface-high}"
    textColor: "{colors.on-surface}"
  card:
    backgroundColor: "{colors.glass-top}"
    textColor: "{colors.on-surface}"
    borderColor: "{colors.hairline}"
    rounded: "{rounded.xl}"
    padding: "{spacing.card-padding}"
  card-high:
    backgroundColor: "{colors.glass-top}"
    textColor: "{colors.on-surface}"
    borderColor: "{colors.hairline-bright}"
    rounded: "{rounded.xl}"
    padding: "{spacing.card-padding}"
  list-item:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "{spacing.lg}"
    height: 64px
  input-field:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.md}"
    padding: "{spacing.lg}"
    height: 52px
  slider-track:
    backgroundColor: "{colors.hairline-bright}"
    rounded: "{rounded.full}"
    height: 6px
  slider-thumb-red:
    backgroundColor: "{colors.channel-red}"
    borderColor: "{colors.on-primary}"
    rounded: "{rounded.full}"
    width: 24px
    height: 24px
  slider-thumb-nir:
    backgroundColor: "{colors.channel-nir}"
    borderColor: "{colors.on-primary}"
    rounded: "{rounded.full}"
    width: 24px
    height: 24px
  fader-vertical:
    backgroundColor: "{colors.deep}"
    borderColor: "{colors.hairline}"
    rounded: "{rounded.md}"
    height: 200px
  fader-vertical-fill-red:
    backgroundColor: "{colors.channel-red}"
    borderColor: "{colors.on-primary}"
  fader-vertical-fill-nir:
    backgroundColor: "{colors.channel-nir}"
    borderColor: "{colors.on-primary}"
  fader-step:
    backgroundColor: "{colors.hairline}"
    borderColor: "{colors.hairline-bright}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.md}"
    width: 56px
    height: 40px
  power-button:
    backgroundColor: "{colors.deep}"
    borderColor: "{colors.channel-red}"
    textColor: "{colors.channel-red}"
    rounded: "{rounded.full}"
    width: 132px
    height: 132px
  power-button-lit:
    backgroundColor: "{colors.deep}"
    borderColor: "{colors.primary-bright}"
    textColor: "{colors.primary-bright}"
  power-button-disabled:
    backgroundColor: "{colors.deep}"
    borderColor: "{colors.hairline}"
    textColor: "{colors.on-surface-muted}"
  countdown:
    textColor: "{colors.primary-bright}"
    typography: "{typography.metric-lg}"
  dose-bar:
    backgroundColor: "{colors.hairline-bright}"
    rounded: "{rounded.full}"
    height: 10px
  dose-bar-filled:
    backgroundColor: "{colors.primary}"
    rounded: "{rounded.full}"
    height: 10px
  dose-bar-over:
    backgroundColor: "{colors.warning}"
    rounded: "{rounded.full}"
    height: 10px
  streak-badge:
    backgroundColor: "{colors.surface-high}"
    textColor: "{colors.success-bright}"
    typography: "{typography.metric-md}"
    rounded: "{rounded.full}"
    padding: "{spacing.md}"
  calendar-day-empty:
    backgroundColor: "{colors.heat-none}"
    rounded: "{rounded.sm}"
    size: 32px
  calendar-day-light:
    backgroundColor: "{colors.heat-light}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.sm}"
    size: 32px
  calendar-day-full:
    backgroundColor: "{colors.heat-full}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.sm}"
    size: 32px
  calendar-day-over:
    backgroundColor: "{colors.heat-over}"
    textColor: "{colors.on-primary-dark}"
    rounded: "{rounded.sm}"
    size: 32px
  banner-warning:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.warning}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "{spacing.lg}"
  scrim-overlay:
    backgroundColor: "{colors.scrim}"
---

# Mito Tracker

## Overview

An instrument, not a wellness toy. The panel it drives is a CE-marked medical-adjacent device that
people buy to change how they sleep and recover, and the app's job is to make the light easy to aim
and impossible to lose track of. Everything on screen is either a control or a fact; nothing is
decoration.

The interface is dark throughout, and that is a use-context decision rather than a fashion. A panel
is used in a dim room, often in the evening, often with the phone propped against something a metre
away while several hundred red LEDs are pointed at the user. A white screen in that room is a
second, unwanted light source, and blue-heavy UI at night undoes part of what the panel is for.

The mood is **quiet, dense and confident**. Type is compact and left-aligned, panels of information
sit on translucent dark cards, and one red is allowed to shout. Where the vendor's own app is a remote
control, this one is a logbook that also happens to be a remote control — so numbers are first-class
citizens and are set in a monospaced face so they hold still while they change.

The visual identity is Mito Light's own, taken from their published material rather than invented:
`#F50537` appears both in their website stylesheets and as the primary colour in their Android app's
theme, and Poppins is the body face in that app. This is deliberate: an app that drives somebody's
hardware should feel like it belongs next to it.

## Colors

The palette is one red family on near-black, with two greys for containment. Visible red is the
brighter channel; near-infrared is a deeper wine red so both still read as one panel.

- **Primary (#F50537):** The Mito Light red, and the only colour permitted for the primary action on
  a screen. Used for the Start button, for the filled part of a dose bar, and for the "over the
  limit" step of the calendar ramp.
- **Primary pressed (#CF0000):** The vendor app's own darker red, used for the pressed and held
  states so the interaction has somewhere to go.
- **Primary bright (#FF5A6E):** A lightened brand red for *text* and thin indicators. `#F50537` on
  `#141414` measures 4.46:1, which is under the AA minimum for body text, and this is the fix. It is
  a functional token, not a second brand colour.

  Three colours have a `-bright` counterpart for exactly this reason — `primary`, `channel-nir` and
  `success` are all saturated enough to fail as small text on a dark background while working
  perfectly as fills. The rule is uniform: **fills use the base token, text uses the bright one.**
  `on-primary-dark` (#0A0A0A) is the mirror of the same problem: on a fully saturated red *fill*,
  small text has to go darker rather than lighter to clear AA, which is why a small label on
  `heat-over` or `primary` is near-black while the same label on the darker steps is not.
- **Channel red (#EF202C) and channel near-infrared (#981B32):** The two things a 4.0 panel can be
  told to do, and the six wavelengths a 5.0 splits them into. Near-infrared at 810–850 nm is
  invisible; the deeper red distinguishes its control without introducing a second colour family.
  `channel-nir-bright` is its readable-text counterpart.
- **Background (#141414):** Mito Light's own web background. Dark enough to disappear in a dim room,
  warm enough not to look like a terminal.
- **Surface (#282828) and surface high (#3D3D3D):** Both taken from the vendor app's `MitoColors`.
  Cards sit on the background at `surface`; controls that sit *on* a card — sliders, chips, inputs —
  step up to `surface-high`.
- **Success (#24C02B):** Theirs as well. Reserved for streaks and for confirming a connection, never
  for the light itself.
- **Warning (#F5A524):** Not in their palette, and needed here because this app knows something their
  app does not: how many minutes an area has already had today. Amber is the daily limit being
  approached or passed, and it is the only colour allowed to disagree with the user.
- **Error (#FF5A6E):** Deliberately the same value as `primary-bright`. A failure in this app is
  almost always a lost connection, and inventing a second red to describe it would imply a
  distinction the user does not need to make.
- **Heat ramp (#232323 → #5C0A1B → #A00A27 → #F50537):** Four steps for a day in the exposure map —
  untouched, under ten minutes, a normal day, and a day with a lot of light in it. The ramp measures
  *how much*, not *too much*: a day's total spans several areas, so 40 minutes across four of them
  breaks no limit, and a ramp pitched at the limit instead would paint four months the same colour.
  Where a limit is being approached, that is amber's job and it is said in words.
- **Depth tokens (`glass-*`, `hairline*`, `deep`, `vignette`, `glow-*`):** The translucency, edges and
  atmospheric washes described under Elevation & Depth. `hairline` is white at 5 % and does more work
  than any other value in this file: it is the only thing keeping a screen of dark cards from reading
  as one dark mass.

## Typography

Three faces, each with a job.

- **Archivo** carries headlines and button labels. Mito Light's real display face is **Aktiv Grotesk
  Extended**, licensed from Adobe and therefore unavailable to a prototype; Archivo is the closest
  free neo-grotesque, and a little positive letter-spacing on headlines echoes the extended
  proportions of the original.
- **Poppins** carries body text, at 15px, exactly as their own app sets it.
- **Roboto Mono** carries numbers — countdowns, minutes, doses, Bluetooth addresses. This is the one
  addition with a functional argument behind it: a proportional digit changes width as it changes
  value, so a running countdown visibly twitches. Monospaced digits do not.

Fourteen levels, which is at the upper end of sensible and justified by having two number scales.
Headline sizes follow the vendor app's own scale (30 / 26 / 20) so screens keep their rhythm.

`label-lg` is 20px rather than the vendor app's 16px button size, and the extra four pixels are load
bearing: white on `#F50537` measures 4.23:1, which passes WCAG AA only at large-text sizes, and bold
type qualifies as large from 18.66px upward. A 20px bold label clears that bar, suits a 52px pill,
and means the primary action of the app can stay brand red without an accessibility asterisk.

Labels at `label-sm` are set in **uppercase** with 0.15em tracking, used for section eyebrows such
as KNOWN PANELS or THIS WEEK, for the badges that name a state, and for the labels on the two wide
buttons. Nothing else is uppercase — not titles, not body text, not chips.

## Layout

A single-column phone layout with a **20px screen margin** and a **4px base grid**, both taken from
the vendor app, whose spacing scale runs 2, 4, 6, 8, 12, 16 and upwards in fours.

Content is grouped by containment rather than by rules: related facts go inside a card with 20px of
internal padding, and cards are separated by 16px. Sections are separated by 24px. A screen should
be readable at arm's length, because that is where the phone is during an exposure — so nothing
essential is smaller than 13px, and the primary controls sit in the lower half of the screen where a
thumb can reach them.

Vertical order on every screen is the same: what is happening now, then what to change, then what to
press. The countdown is never below the fold.

## Elevation & Depth

This section originally said *flat*: tonal steps only, no shadows, on the usual grounds that a shadow
on near-black is a grey smear. That is the safe answer, and it was also the reason the first build of
this design looked like a settings screen. Dark and flat is not restrained, it is absent — nothing on
the screen has an edge, so nothing has a place.

What replaced it is atmospheric, and the argument for it is the same one behind the dark theme: the
app runs in a room with several hundred red LEDs pointed into it. So:

- **The background is not one colour.** `#141414` with a `vignette` wash bled down from above the top
  of the screen, as if the panel were lighting the wall behind the interface.
- **A card is translucent.** A 135° gradient from `glass-top` to `glass-bottom` over that background,
  so the red cast shows through, with a **1px `hairline`** edge and a wide soft shadow beneath. The
  hairline is the load-bearing part: it is what gives every card a boundary in a scene with almost no
  contrast in it.
- **Anything that emits light glows.** The primary action, a slider knob, a status dot, the lit edge
  of a session in the log, and the countdown while the light is on all carry a coloured halo. On
  Android this is a coloured shadow, which the platform renders from API 28 and degrades to a plain
  dark one below that.
- **A hero card carries a wash of its own** — `glow-red` on the countdown, `glow-amber` behind a
  streak, `glow-nir` behind the dose chart — centred and faded out well before the edges.

Two constraints, both learned by building it. A glow is a shadow drawn behind the shape, so anything
small enough for the halo to fall *underneath* it must be **opaque**, or the shadow shows through the
translucent fill as a dark square — which is why a selected area tile is `surface` and not glass. And
a radial wash inside a small square reads as a second square rather than as light, so washes are for
cards, not for tiles.

A modal dims what is behind it with black at 72%, heavier than the `scrim` token, which is sized for a
sheet rather than for a question about the past — and its card is the one card that is **opaque**. A lit
control screen showing through the summary of the exposure that just ended is not atmosphere, it is two
interfaces at once.

## Shapes

Generously rounded, stopping short of playful. Cards and containers use a **24px** radius, wide buttons
**16px**, controls, inputs and body tiles **12px**, calendar cells **4px**, and small pressable things
— chips, badges, knobs, icon bubbles — are **full pills**. A radius here is a measure of size: the
bigger the surface, the rounder its corners, so a card reads as a card and a cell reads as a pixel in a
chart.

The pill remains the signal that something is interactive at small sizes. Nothing is square-cornered.

## Components

**Buttons.** One primary per screen, 60px tall on a **16px** radius, filled with a left-to-right
gradient from `primary` to `primary-pressed`, edged in `hairline-bright` and carrying a red halo. Its
label is 20px bold Archivo in uppercase, with an icon ahead of it. Secondary actions are the same
shape filled with `hairline` — white at 5%, which on this background is a pane of glass rather than a
grey — and no halo, because they are not the thing that turns the light on. Text buttons drop the shape
entirely and use `primary-bright`, never `primary`, because their labels are small. A third, quieter
wide button exists for the link a card offers at its foot: 48px, `hairline`, an uppercase `label-sm` in
`primary-bright`.

Where an action is pinned above the navigation bar it sits on a vertical gradient fading to
`background`, so content scrolls out from under it instead of colliding with it.

**Sliders.** A 6px rail in `hairline-bright`, filled to the current value in the channel's colour, and a
**24px round knob** ringed in 2px of white with a halo in the channel's colour: `channel-red` for red,
`channel-nir` for near-infrared. Material's own track is not used — on a stepped slider it stipples tick
marks along the rail and parks a dot at the far end, and both are noise on an instrument. A channel's
name sits in a 32px circular bubble tinted to its own colour, and the value reads to the right of it in
`metric-md`. This is the shape a countdown length takes and, inside a custom ML5 preset's collapsed
advanced section, the shape each wavelength takes.

**Vertical faders.** The everyday Red/NIR mix on both ML4 and ML5. On ML5 each is a master for its
three physical wavelengths; exposing six rails in Manual replaced a clear control with a mixing desk.
A column on a 12px radius over `deep` is filled from the bottom in the channel's colour, with a 5px
white lip and a halo where the fill arrives, and the value and short name — RED, NIR — in white at the
foot. A step button of 56×40px in `hairline` sits above and below, moving five per cent a tap, because a
small correction is not a drag gesture. Brightness is committed when the drag ends rather than while it
moves, because every step would otherwise be a Bluetooth write.

**Power button.** The switch, and the centre of the control screen: a 132px disc on `deep` inside a
hairline of the button's own colour at 55%, washed with that colour and haloed in it, with a 52px
pictogram. `channel-red` when it will start the panel, `primary-bright` and breathing between 18px and
40px of halo while the panel is lit (Pause), `on-surface-muted` and still when it can do nothing. An
uppercase `label-sm` caption under it says what it will do. While a countdown is running, the remaining
fraction is drawn as a 5px arc around the disc, clockwise from twelve. Ending the session is a separate
full-width secondary under the disc — not a caption beside "I turned around" — so Pause stays the one
control you can hit without looking. The aim badge above it and the slots below it stay reserved, so
START ↔ PAUSE ↔ RESUME never moves the disc.

**Countdown.** The hero of the control screen: `metric-lg` — 56px, weight 300 — in `primary-bright`
above a badge naming the state, with the room glow behind it rather than a card of its own. It shows
minutes and seconds remaining while the light is on. When the panel is idle it shows the length that is
*set*, labelled as such and in `on-surface` rather than red: a stopped panel keeps reporting the
leftovers of its last run, and repeating that number as though it were live would be a lie.

**Badges.** Two kinds, both pills. A tinted one — colour at 12% with a 22% border and an uppercase
`label-sm` in that colour — names a judgement: OPTIMAL, NEARING LIMIT, 20 MIN LEFT. A neutral one on
`deep` with a glowing dot names the panel's own state: READY, LIT, CONNECTED. The dot carries the
colour so the word can stay white.

**Dose bar.** A 10px pill: `hairline-bright` for the whole day's allowance, `primary` for minutes
already used with a halo and a white gradient tip where the fill arrives, `warning` for anything past
the limit. Always labelled with real numbers in `metric-md`, as in `22 / 20 min`, because a bar alone
cannot say how close is too close. An area with nothing on it today gets no bar at all — a column of
empty troughs says less than the month's figure does.

**Calendar.** Square cells on the heat ramp with a 4px radius and a `hairline` edge, seven to a row,
**carrying no dates**: the point is the shape of a month of use, and thirty-five numbers in a grid is a
spreadsheet. Today is a 2px `primary-bright` ring, and days that have not happened yet are empty with
the same hairline. The ramp is spelled out in a legend pill beside the heading, because a heat map
without a key is decoration. A streak is a run of adjacent non-empty cells and is worth stating in
words as well, in a `streak-badge`.

**Chips and tiles.** Chips are pills for filters and short choices: unselected is `hairline` with muted
text, selected fills to `surface-high` with full-strength text. Twelve body areas are too many to read
as text at arm's length, so they are **square tiles** instead, four to a row, each with a pictogram and
a label; the selected one is opaque `surface` with a `channel-red` edge, a red icon and a red halo.
Either way, selection is never shown by colour alone — the chosen one is also the only filled one.

**Lists.** Rows are cards, 12px apart, no dividers. A row that is about one thing carries a **6px lit
edge** down its left side in the colour of that thing — red, near-infrared, or both — with a short wash
beside it. Numbers that can be read down a column, such as a session's length, sit in a fixed-width
`metric-md` field so the rule beside them lands in the same place on every row. A row's secondary line
is `body-sm` in `on-surface-muted`; technical identifiers such as a Bluetooth address use `metric-sm`.

**App bar.** Present on every screen: the wordmark in `channel-red`, a circular button on the left for
the panel — carrying a glowing dot when one is connected, red while it is lit — and one on the right
for settings. It is the only place a running exposure is visible from every screen.

## Do's and Don'ts

- Do keep exactly one `primary`-filled element per screen. If two things are red, neither is the
  action.
- Don't set body text in `primary` — it measures 4.46:1 on the background and fails AA. Use
  `primary-bright` for red text, and reserve `primary` for fills, large state headlines and
  indicators. The same applies to `channel-nir` and `success`: fills use the base token, text uses
  the `-bright` one.
- Do treat one contrast warning as known and accepted: white on `#F50537` is 4.23:1, which is why the
  primary button's label is 20px bold — large enough for AA's large-text threshold. A linter cannot
  see font size, so it will flag `button-primary` anyway. Every other pairing in this file passes at
  body-text size.
- Do set every number that changes over time in a `metric-*` level. Countdowns, minutes, doses,
  percentages.
- Don't use `success` green for the light being on. The light being on is red, always; green is for
  streaks and for a healthy connection.
- Do let `warning` amber be the only colour that contradicts the user, and only on the authority of
  Mito Light's own manual — 10 to 20 minutes per area per day, no more than 30 if the same area is
  lit twice.
- Do give a card an edge. A `hairline` border is not optional trim — without it a screen of dark cards
  on a dark background has no boundaries in it at all.
- Don't put a glow on something opaque and small unless it is meant to be lit, and don't put a
  translucent fill on something small that *is* lit: the halo will show through as a dark square.
- Don't spend the glow. Two or three lit things per screen, and each of them either emits light in
  reality or is the button that makes it do so. Beyond that it stops meaning anything.
- Do keep the primary control within thumb reach in the lower half of the screen, and the countdown
  above the fold. The phone is on a table two feet away during an exposure.
- Don't communicate state by colour alone. "Lit" is a word as well as a red headline; a selected chip
  is filled as well as brighter.
- Do write minutes as `22 / 20 min` — the recommended figure, not the ceiling — times as `4:32`, and
  wavelengths as `810 nm` with a space.

## Screens

Not part of the DESIGN.md schema, and kept here because the same file is used to brief the tool that
sketches these. The vocabulary — Panel, Channel, Session, Exposure, Area, Dose, Limit, Preset — is
defined in [`CONTEXT.md`](CONTEXT.md).

Five of them are tabs — Panels, Control, History, Stats, Settings — under an app bar that is the same
on all of them, whose right-hand button is a second way to Settings and whose left-hand one goes to
Panels. **The app opens on Control**, whether or not a panel has answered yet: it is opened to turn a
light on, so it opens on the screen that does that, and the state of the link is reported by the switch
rather than by which screen is showing. Output, Target Area and a finished session's summary are
reached from within Control, not from the bar.

**Panels.** Choosing what to drive. A KNOWN PANELS section of remembered panels, each reconnecting
without a scan, then a Scan button and whatever it finds. Empty and searching states matter here: a
panel only advertises while nothing else is connected to it.

**Control.** The countdown as the hero and, at the centre of the screen, **one round button** that
starts the panel with whatever it is already set to. Nothing stands between that button and the light.
The button is the stop control while the panel is lit, and carries the countdown around its edge as a
ring. It is drawn in every state of the link, captioned with what it will do — start, stop, connecting,
or tap to pick a panel — because "there is no panel yet" is a state of a switch and not a different
screen. Beneath it, one card reports what the panel is set to and leads to Output. This screen must
survive being opened onto a panel that is already lit and part way through a countdown.

**Output.** Reached from Control. Red and NIR brightness as two **vertical faders** on either panel,
filled to their level, with a step button above and below — plus the countdown length, and a Presets tab
where one tap loads a named routine and hands straight back to the start button. Ten compact included
routines sit below the user's own presets; their cards retain areas, time and output without repeating
ten dose bars. A New preset action opens an editor for its name, body areas, output and countdown;
custom presets are kept across app restarts. On ML5 its six wavelengths belong in a collapsed advanced
section of that editor, not in Manual. Configuration lives here so that the start screen can be a
switch instead of a mixing desk.

**Target Area.** Only ever reached mid-session, because a body part is not the price of turning a lamp
on: either the app arrived at a panel already lit, or somebody turned around, or a run underway is
being claimed for the first time. Areas as tiles, with today's dose beside the limit for each one
chosen. Naming an area credits the run from its start; turning around keeps the minutes already spent
where they are, which is why the one link into this screen is labelled for whichever of the two it is
about to do.

**Session summary.** A modal when an exposure ends, and **the app's main way of attributing a run**:
what was recorded, a chance to correct the areas, and a way to throw it away. Choosing nothing is a
supported answer and files the session as ambient light.

**History.** Sessions in reverse order, each with its date, length, areas and channel brightnesses.
Grouped by day, with the day's total.

**Stats.** The consistency streak, a thirty-day exposure map with its legend, and minutes per area
against the limit. This is the screen the vendor app has no equivalent of at all.

**Settings.** Panel behaviour, exposure limits, the demo and export controls, Health Connect, and
notifications — one card per group, reached from the app bar.
