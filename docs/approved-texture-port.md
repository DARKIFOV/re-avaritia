# Approved texture port

## Alignment correction

The first 0.6.70 test port used crop bounds that were too wide for several item icons. Parts of neighbouring reference tiles entered the crops, which made some textures look clipped and shifted toward the lower-right corner.

The corrected 0.6.71 test JAR uses:

- exact frame bounds for every approved texture;
- edge-background removal only where it is safe;
- proportional centered fitting for item icons;
- direct full-frame fitting for the pattern encoder block and blank/encoded pattern icons;
- equal margins around all inventory icons;
- 128x128 active block/item textures and 16x16 fallback textures.

Corrected sets:

- Neutronium Combiner block faces and three upgrades;
- Greenhouse block faces and all modules, including Infinite Water;
- Extreme Pattern Encoder block faces, blank pattern and encoded pattern.

Validation:

- 24 primary resources re-cropped and centered;
- six greenhouse tier variants regenerated from the corrected base icons;
- two fallback textures regenerated;
- all Java classes left unchanged;
- ZIP, PNG and JSON validation passed.

Corrected test JAR SHA-256: `6248fe7c7a48fc2ef741f326fbdaba95ef6b963fbe4724cea2eea163cfcf9dc6`.

The previous 0.6.70 JAR is superseded and should not be used.
