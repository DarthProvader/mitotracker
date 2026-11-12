# A session may have no exposure

The rule that every **Session** carries at least one **Exposure** is what lets the app ask
"which **Area**?" before the light goes on, and it is the reason dose can be trusted: minutes
always belong to a body part. Enforcing it everywhere, though, makes the app lie in one real
case.

Mito Light ship an **Ambient Light** mode — 1 % red for twenty minutes — and a panel is a large
warm light on a wall, so people use it as one. That run is not therapy aimed at anything. The
three ways out were to demand an Area anyway, which files ambient light against somebody's
shoulders and inflates their dose; to invent an Area called "Ambient", which keeps the invariant
by breaking the definition of Area as a part of the body; or to let the Session stand with no
Exposure at all.

We let it stand. A Session with no Exposures is **unattributed**: the app records that the panel
ran, for how long and at what **Brightness**, and claims nothing about who was in front of it.
It contributes to no Dose and trips no **Limit**.

The cost is one sentence in [`CONTEXT.md`](../../CONTEXT.md) and the loss of a database
constraint we would otherwise have enjoyed. The benefit is that the history stays true, which
matters more for a device sold on medical-adjacent claims than a tidy schema does — and it is
the same instinct as ADR 0003, where the panel rather than our intention is what gets believed.

Dose queries need no special case: they sum Exposures, and a Session with none contributes
nothing by construction. The one place it surfaces is the user interface, where an unattributed
Session has to look deliberate rather than broken, so it is labelled as such in history and the
choice not to attribute it is offered plainly at the moment the light starts.
