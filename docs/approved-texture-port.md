# Approved texture port

## Native 12-pixel left shift

The approved texture sets are stored as native 128x128 game PNGs. The previous correction still left visible empty space on the left and caused the artwork to appear clipped on the right.

The corrected 0.6.73 test JAR applies a native horizontal shift of 12 pixels to every approved 128x128 texture:

- the left strip is moved to the right side instead of being discarded;
- no pixels are removed;
- no image is enlarged or resampled;
- all 128x128 dimensions remain unchanged;
- the two 16x16 fallback textures receive an equivalent two-pixel shift.

Corrected sets:

- Neutronium Combiner block faces and three upgrades;
- Greenhouse block faces and every module, including Infinite Water;
- Extreme Pattern Encoder block faces, blank pattern and encoded pattern.

Validation:

- 30 primary 128x128 resources shifted left by 12 pixels;
- two 16x16 fallback resources shifted left by two pixels;
- 103 PNG files and 199 JSON files validated;
- all Java classes and gameplay logic left unchanged;
- ZIP validation passed.

Corrected test JAR SHA-256: `3f697c879a2f54187df9a4b150c344397d0b6164732dfabc42ab60c2da001d6f`.

The 0.6.70, 0.6.71 and intermediate 0.6.72 texture test JARs are superseded and should not be used.
