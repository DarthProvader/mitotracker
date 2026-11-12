# Mito Light

Controlling a Mito Light red light therapy panel from independent software, and recording what
the light was actually used for. The hardware knows only its present state, so every notion
of history, body parts and dosage exists solely on the phone.

## Language

### The hardware

**Panel**:
One Mito Light unit — a wall-hung array of red and near-infrared LEDs with a touchscreen.
_Avoid_: device, lamp, light, machine

**Channel**:
A group of LED wavelengths the **Panel** dims as one. A two-channel Panel has red and
near-infrared; a six-channel Panel exposes each wavelength separately.
_Avoid_: spectrum, colour, band

**Brightness**:
How hard a **Channel** is driven, 0–100 %. Zero switches that Channel off.
_Avoid_: intensity, power, level

**Countdown**:
How long the **Panel** will stay lit before switching itself off. Zero means indefinitely.
_Avoid_: timer, duration, session length

### What people do with it

**Session**:
One continuous run of the **Panel**, from start until it stops or is stopped. Describes what
the hardware did, and nothing about who stood in front of it.
_Avoid_: run, treatment, workout

**Area**:
A part of the body that light is aimed at, such as knees or face. The unit that dosage
limits apply to. Areas come from a fixed list and never overlap, so that minutes can be
added up without double counting.
_Avoid_: body part, zone, target, region

**Exposure**:
A claim that one **Area** received light for a span of one **Session**. Turning around halfway
through produces two.

A Session usually has at least one Exposure, and one with none is **unattributed**: the light
was on but the app is not claiming it reached anybody. Running the panel as ambient light is
the honest case for that, and an unattributed Session contributes to no **Dose** and to no
**Limit** (ADR 0008).
_Avoid_: application, treatment, irradiation

**Dose**:
Minutes of **Exposure** an **Area** has received over some period, usually a day. Always
derived from Exposures, never recorded as a number of its own.
_Avoid_: energy, J/cm², load

**Limit**:
The most **Dose** one **Area** should receive in a day. Taken from Mito Light's published user
manual: 10–20 minutes per Area per day is the recommendation, an
Area may be lit once or twice a day, and a repeated Area should total no more than 30
minutes. A single **Session** can be set to 1–20 minutes on the hardware.
_Avoid_: cap, maximum, quota

**Preset**:
A named routine — one or more **Areas** plus the **Brightness** and **Countdown** to use on
them, such as "Knees, 10 minutes, both channels full". Starting a **Session** from a Preset
is how the app learns which Areas are being lit. A Preset covering several Areas, such as
"whole front", produces one **Exposure** each.
_Avoid_: programme, mode, profile, favourite

A Preset is not a **Panel** light mode. Six-channel Panels have their own built-in modes
(CLASSIC, INDIVIDUAL and four named presets); those live in the hardware and mean something
different.

## Example dialogue

> **Dev:** You hung the panel, ran it for twenty minutes, and did your chest for the first
> ten and your back for the second ten. How many sessions is that?
>
> **Expert:** One. I only switched it on once.
>
> **Dev:** And how many exposures?
>
> **Expert:** Two — chest and back, ten minutes each. That matters, because tomorrow the app
> should tell me my back is fine but my chest has already had its limit for the day.
>
> **Dev:** What if you set the countdown to twenty but walked off after five?
>
> **Expert:** Then the session is five minutes, not twenty. The countdown is what I asked
> for; the session is what happened.
