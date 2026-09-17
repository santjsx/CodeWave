#pragma once

#ifndef _USE_MATH_DEFINES
#define _USE_MATH_DEFINES
#endif
#include <cmath>
#include <algorithm>
#include "BiquadFilter.hpp"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

/**
 * ViPER Clarity: Harmonic Definition & Vocal Presence Restorer.
 * Extracts high-frequency transients above 3.5kHz, applies soft dynamic harmonic
 * excitation, and blends transparent high-end sheen without sibilance or ear fatigue.
 */
class ViperClarity {
private:
    bool enabled = false;
    float clarityGain = 1.0f;
    double sampleRate = 44100.0;

    // 3.5kHz 2nd-order High-Pass Filter
    BiquadFilter highPassFilter;

public:
    ViperClarity() : highPassFilter(FilterType::HIGH_SHELF) {}

    void init(double sRate) {
        sampleRate = (sRate > 0.0) ? sRate : 44100.0;
        highPassFilter.setSampleRate(sampleRate);
        recalculateFilter();
        reset();
    }

    void reset() {
        highPassFilter.reset();
    }

    void setEnabled(bool isEnabled) {
        enabled = isEnabled;
    }

    void setGainDb(float gainDb) {
        float clamped = std::clamp(gainDb, 0.0f, 12.0f);
        clarityGain = std::pow(10.0f, clamped / 20.0f);
    }

    void recalculateFilter() {
        // High-pass filter at 3500 Hz (Butterworth Q = 0.7071)
        double f0 = std::min(3500.0, 0.45 * sampleRate);
        double q = 0.7071;
        double omega = 2.0 * M_PI * f0 / sampleRate;
        double sn = std::sin(omega);
        double cs = std::cos(omega);
        double alpha = sn / (2.0 * q);

        double a0 = 1.0 + alpha;
        highPassFilter.b0 = ((1.0 + cs) / 2.0) / a0;
        highPassFilter.b1 = (-(1.0 + cs)) / a0;
        highPassFilter.b2 = ((1.0 + cs) / 2.0) / a0;
        highPassFilter.a1 = (-2.0 * cs) / a0;
        highPassFilter.a2 = (1.0 - alpha) / a0;
        highPassFilter.reset();
    }

    inline void processStereo(float& left, float& right) {
        if (!enabled) return;

        // 1. Isolate high frequencies above 3.5kHz
        float highL = left;
        float highR = right;
        highPassFilter.processStereo(highL, highR);

        // 2. Soft dynamic excitation with anti-sibilance ceiling (Edge Case 68)
        float exciterL = highL * (clarityGain - 1.0f);
        float exciterR = highR * (clarityGain - 1.0f);

        // Soft compression on exciter to prevent harsh sibilance
        exciterL = std::tanh(exciterL * 1.2f) * 0.83f;
        exciterR = std::tanh(exciterR * 1.2f) * 0.83f;

        // 3. Transparently inject harmonic air back into master stereo stream
        left += exciterL * 0.35f;
        right += exciterR * 0.35f;
    }
};
