# Store facts, not computed dose

Dosage in version one is simply elapsed minutes per **Area** per day, which is what Mito
Light's own manual states its limits in. That definition is nonetheless wrong in an obvious way — twenty minutes at 1 %
is not twenty minutes at 100 % — and the definition the field actually uses, J/cm², needs
irradiance figures only the manufacturer has.

So an **Exposure** records the facts that were true when it happened: which Area, how long,
each **Channel**'s **Brightness**, and the distance if the user gave one. Dose is derived at
read time and never stored. When the formula improves, or irradiance figures become
available, the entire history can be recomputed instead of being stranded in units we no longer
believe.
