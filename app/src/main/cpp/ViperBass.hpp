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
 * ViPER Bass: Psychoacoustic Sub-Bass Reinforcement Engine.
 * Isolates sub-80Hz content, generates musical sub-harmonics via non-linear
 * hyperbolic saturation, applies DC blocking, and blends phantom bass into the audio stream.
 */
class ViperBass {
private:
    bool enabled = false;
    float bassGain = 1.0f; // Multiplier from gain in dB
    double sampleRate = 44100.0;

    // 80Hz 2nd-order Low-Pass Filter (Q = 0.707 Butterworth)
    BiquadFilter lowPassFilter;

    // 5Hz DC Blocker to eliminate saturation offsets (Edge Case 72)
    float dcPrevX[2] = {0.0f, 0.0f};
    float dcPrevY[2] = {0.0f, 0.0f};

public:
    ViperBass() : lowPassFilter(FilterType::LOW_SHELF) {}

    void init(double sRate) {
        sampleRate = (sRate > 0.0) ? sRate : 44100.0;
        lowPassFilter.setSampleRate(sampleRate);
        recalculateFilter();
        reset();
    }

    void reset() {
        lowPassFilter.reset();
        dcPrevX[0] = 0.0f; dcPrevX[1] = 0.0f;
        dcPrevY[0] = 0.0f; dcPrevY[1] = 0.0f;
    }

    void setEnabled(bool isEnabled) {
        enabled = isEnabled;
    }

    void setGainDb(float gainDb) {
        float clamped = std::clamp(gainDb, 0.0f, 14.0f);
        bassGain = std::pow(10.0f, clamped / 20.0f);
    }

    void recalculateFilter() {
        // Standard 2nd order Butterworth Low-Pass at 80Hz
        double f0 = 80.0;
        double q = 0.7071;
        double omega = 2.0 * M_PI * f0 / sampleRate;
        double sn = std::sin(omega);
        double cs = std::cos(omega);
        double alpha = sn / (2.0 * q);

        double a0 = 1.0 + alpha;
        lowPassFilter.b0 = ((1.0 - cs) / 2.0) / a0;
        lowPassFilter.b1 = (1.0 - cs) / a0;
        lowPassFilter.b2 = ((1.0 - cs) / 2.0) / a0;
        lowPassFilter.a1 = (-2.0 * cs) / a0;
        lowPassFilter.a2 = (1.0 - alpha) / a0;
        lowPassFilter.reset();
    }

    inline void processStereo(float& left, float& right) {
        if (!enabled) return;

        // 1. Isolate sub-80Hz low frequencies
        float lowL = left;
        float lowR = right;
        lowPassFilter.processStereo(lowL, lowR);

        // 2. Generate non-linear psychoacoustic harmonics via tanh
        // Drive limited to +/- 10.0 to prevent numerical overflow (Edge Case 14)
        float driveL = std::clamp(lowL * bassGain, -10.0f, 10.0f);
        float driveR = std::clamp(lowR * bassGain, -10.0f, 10.0f);

        float harmonicsL = std::tanh(driveL);
        float harmonicsR = std::tanh(driveR);

        // 3. DC Blocker filter (5Hz pole: R = 1 - (2*PI*5/Fs))
        float R = static_cast<float>(1.0 - (2.0 * M_PI * 5.0 / sampleRate));
        float dcL = harmonicsL - dcPrevX[0] + R * dcPrevY[0];
        dcPrevX[0] = harmonicsL; dcPrevY[0] = dcL;

        float dcR = harmonicsR - dcPrevX[1] + R * dcPrevY[1];
        dcPrevX[1] = harmonicsR; dcPrevY[1] = dcR;

        // 4. Blend synthesized phantom harmonics back into the signal
        left += dcL * 0.40f;
        right += dcR * 0.40f;
    }
};
