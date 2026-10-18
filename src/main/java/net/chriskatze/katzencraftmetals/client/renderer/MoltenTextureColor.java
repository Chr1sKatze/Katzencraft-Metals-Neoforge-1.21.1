package net.chriskatze.katzencraftmetals.client.renderer;

/** Deterministic CPU-only highlight analysis, independent of Minecraft rendering state. */
final class MoltenTextureColor {
    static final int FALLBACK = 0xFFD080;

    record Profile(double hue, double saturation, double median, double highlight, boolean neutral) {}
    record Reference(Profile texture, int approvedTint) {}

    static int derive(int[] argb) {
        return derive(analyze(argb));
    }

    static Profile analyze(int[] argb) {
        double[] brightness = new double[256];
        double total = 0;
        // Bound work for high-resolution resource packs while sampling the whole animation strip.
        int step = Math.max(1, (int) Math.ceil(argb.length / 262144.0));
        for (int i = 0; i < argb.length; i += step) {
            int p = argb[i], alpha = p >>> 24;
            if (alpha < 32) continue;
            int value = Math.max((p >> 16) & 255, Math.max((p >> 8) & 255, p & 255));
            brightness[value] += alpha / 255.0;
            total += alpha / 255.0;
        }
        if (total == 0) return null;
        double median = percentile(brightness, total, .50);
        double highlight = percentile(brightness, total, .85);
        double[] weights = new double[24], hueX = new double[24], hueY = new double[24], saturation = new double[24];
        double chroma = 0;
        for (int i = 0; i < argb.length; i += step) {
            int p = argb[i], alpha = p >>> 24;
            if (alpha < 32) continue;
            double r = ((p >> 16) & 255) / 255.0, g = ((p >> 8) & 255) / 255.0, b = (p & 255) / 255.0;
            double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
            double sat = max == 0 ? 0 : delta / max;
            if (sat < .05) continue;
            double hue = max == r ? (g - b) / delta : max == g ? 2 + (b - r) / delta : 4 + (r - g) / delta;
            hue = (hue / 6 + 1) % 1;
            int bin = Math.min(23, (int) (hue * 24));
            // Saturation weighting rejects gray/white specks; brighter colored pixels
            // contribute more, but sparse highlights cannot dominate the distribution.
            double weight = alpha / 255.0 * sat * sat * (.35 + .65 * Math.min(max, highlight));
            weights[bin] += weight;
            hueX[bin] += Math.cos(hue * 2 * Math.PI) * weight;
            hueY[bin] += Math.sin(hue * 2 * Math.PI) * weight;
            saturation[bin] += sat * weight;
            chroma += weight;
        }
        if (chroma / total < .02) {
            return new Profile(.055, 0, median, highlight, true);
        }
        int dominant = 0;
        for (int i = 1; i < 24; i++) if (weights[i] > weights[dominant]) dominant = i;
        double x = 0, y = 0, sat = 0, weight = 0;
        for (int offset = -1; offset <= 1; offset++) {
            int bin = (dominant + offset + 24) % 24;
            x += hueX[bin]; y += hueY[bin]; sat += saturation[bin]; weight += weights[bin];
        }
        double hue = (Math.atan2(y, x) / (2 * Math.PI) + 1) % 1;
        return new Profile(hue, sat / weight, median, highlight, false);
    }

    static int derive(Profile texture, Reference... references) {
        if (texture == null) return FALLBACK;
        double saturation = baseSaturation(texture), value = baseValue(texture);
        double totalWeight = 0, saturationShift = 0, valueShift = 0, neutralHueX = 0, neutralHueY = 0;
        for (Reference reference : references) {
            if (reference == null || reference.texture() == null) continue;
            Profile sample = reference.texture();
            // Compare darkness, saturation and highlight spread, never interpolate RGB.
            double distance = 4 * square(texture.median() - sample.median())
                    + 2 * square(texture.saturation() - sample.saturation())
                    + square((texture.highlight() - texture.median()) - (sample.highlight() - sample.median()));
            double weight = 1 / (.002 + distance);
            int rgb = reference.approvedTint();
            double max = Math.max((rgb >> 16) & 255, Math.max((rgb >> 8) & 255, rgb & 255)) / 255.0;
            double min = Math.min((rgb >> 16) & 255, Math.min((rgb >> 8) & 255, rgb & 255)) / 255.0;
            saturationShift += weight * ((max == 0 ? 0 : (max - min) / max) - baseSaturation(sample));
            valueShift += weight * (max - baseValue(sample));
            double approvedHue = hueOf(rgb);
            neutralHueX += weight * Math.cos(approvedHue * 2 * Math.PI);
            neutralHueY += weight * Math.sin(approvedHue * 2 * Math.PI);
            totalWeight += weight;
        }
        double hue = texture.hue();
        if (totalWeight > 0) {
            saturation = clamp(saturation + saturationShift / totalWeight, .30, .75);
            value = clamp(value + valueShift / totalWeight, .82, .98);
            if (texture.neutral()) hue = (Math.atan2(neutralHueY, neutralHueX) / (2 * Math.PI) + 1) % 1;
        }
        // Chromatic texture hue is kept exactly, including blue/purple/yellow metals.
        // Only effectively neutral textures borrow a warm reference hue.
        return hsv(hue, saturation, value);
    }

    private static double baseSaturation(Profile texture) {
        return texture.neutral() ? .58 + .10 * (1 - texture.median())
                : clamp(.24 + .62 * texture.saturation() + .08 * (1 - texture.median()), .36, .72);
    }

    private static double baseValue(Profile texture) {
        return texture.neutral() ? .86 + .10 * texture.highlight() : .84 + .12 * texture.highlight();
    }

    private static double hueOf(int rgb) {
        double r = ((rgb >> 16) & 255) / 255.0, g = ((rgb >> 8) & 255) / 255.0, b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b)), delta = max - Math.min(r, Math.min(g, b));
        if (delta == 0) return .055;
        double hue = max == r ? (g - b) / delta : max == g ? 2 + (b - r) / delta : 4 + (r - g) / delta;
        return (hue / 6 + 1) % 1;
    }

    private static double square(double value) { return value * value; }

    private static double percentile(double[] histogram, double total, double fraction) {
        double sum = 0;
        for (int i = 0; i < histogram.length; i++) {
            sum += histogram[i];
            if (sum >= total * fraction) return i / 255.0;
        }
        return 1;
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private static int hsv(double hue, double saturation, double value) {
        double h = hue * 6, f = h - Math.floor(h), p = value * (1 - saturation);
        double q = value * (1 - f * saturation), t = value * (1 - (1 - f) * saturation);
        double r, g, b;
        switch ((int) h % 6) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return ((int) Math.round(r * 255) << 16) | ((int) Math.round(g * 255) << 8) | (int) Math.round(b * 255);
    }

    private MoltenTextureColor() {}
}
