#pragma once

#ifndef _USE_MATH_DEFINES
#define _USE_MATH_DEFINES
#endif
#include <cmath>
#include <algorithm>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

enum class FilterType {
    LOW_SHELF,
    PEAKING,
    HIGH_SHELF
};

/**
 * High-Precision Stereo Parametric Biquad Filter
 * Implements Direct Form I difference equations with independent stereo history
 * and exponential coefficient smoothing to eliminate zipper noise.
 */
class BiquadFilter {
public:
    FilterType type;
    double sampleRate = 44100.0;

    // Target coefficients
    double b0 = 1.0, b1 = 0.0, b2 = 0.0;
    double a1 = 0.0, a2 = 0.0;

    // Smoothed active coefficients
    double curB0 = 1.0, curB1 = 0.0, curB2 = 0.0;
    double curA1 = 0.0, curA2 = 0.0;

    // Independent history for stereo (0 = Left, 1 = Right)
    double x1[2] = {0.0, 0.0};
    double x2[2] = {0.0, 0.0};
    double y1[2] = {0.0, 0.0};
    double y2[2] = {0.0, 0.0};

    static constexpr double SMOOTHING_FACTOR = 0.005;

    explicit BiquadFilter(FilterType fType = FilterType::PEAKING, double sRate = 44100.0)
        : type(fType), sampleRate(sRate > 0.0 ? sRate : 44100.0) {}

    void reset() {
        x1[0] = 0.0; x1[1] = 0.0;
        x2[0] = 0.0; x2[1] = 0.0;
        y1[0] = 0.0; y1[1] = 0.0;
        y2[0] = 0.0; y2[1] = 0.0;
        curB0 = b0; curB1 = b1; curB2 = b2;
        curA1 = a1; curA2 = a2;
    }

    void setSampleRate(double sRate) {
        sampleRate = (sRate > 0.0) ? sRate : 44100.0;
    }

    void updateCoefficients(double centerFreq, double gainDb, double qFactor) {
        // Edge cases 1, 2, 12: Nyquist safety clamp, sub-hertz guard, and Q-factor bounds
        double nyquist = 0.49 * sampleRate;
        double f0 = std::clamp(centerFreq, 10.0, nyquist);
        double q = std::clamp(qFactor, 0.1, 10.0);

        double a = std::pow(10.0, gainDb / 40.0);
        double omega = 2.0 * M_PI * f0 / sampleRate;
        double sn = std::sin(omega);
        double cs = std::cos(omega);
        double alpha = sn / (2.0 * q);

        double normB0 = 1.0, normB1 = 0.0, normB2 = 0.0;
        double normA0 = 1.0, normA1 = 0.0, normA2 = 0.0;

        switch (type) {
            case FilterType::PEAKING: {
                normB0 = 1.0 + alpha * a;
                normB1 = -2.0 * cs;
                normB2 = 1.0 - alpha * a;
                normA0 = 1.0 + alpha / a;
                normA1 = -2.0 * cs;
                normA2 = 1.0 - alpha / a;
                break;
            }
            case FilterType::LOW_SHELF: {
                double aMin1 = a - 1.0;
                double aPlus1 = a + 1.0;
                double beta = 2.0 * std::sqrt(a) * alpha;

                normB0 = a * (aPlus1 - aMin1 * cs + beta);
                normB1 = 2.0 * a * (aMin1 - aPlus1 * cs);
                normB2 = a * (aPlus1 - aMin1 * cs - beta);
                normA0 = aPlus1 + aMin1 * cs + beta;
                normA1 = -2.0 * (aMin1 + aPlus1 * cs);
                normA2 = aPlus1 + aMin1 * cs - beta;
                break;
            }
            case FilterType::HIGH_SHELF: {
                double aMin1 = a - 1.0;
                double aPlus1 = a + 1.0;
                double beta = 2.0 * std::sqrt(a) * alpha;

                normB0 = a * (aPlus1 + aMin1 * cs + beta);
                normB1 = -2.0 * a * (aMin1 + aPlus1 * cs);
                normB2 = a * (aPlus1 + aMin1 * cs - beta);
                normA0 = aPlus1 - aMin1 * cs + beta;
                normA1 = 2.0 * (aMin1 - aPlus1 * cs);
                normA2 = aPlus1 - aMin1 * cs - beta;
                break;
            }
        }

        if (std::abs(normA0) > 1e-12) {
            b0 = normB0 / normA0;
            b1 = normB1 / normA0;
            b2 = normB2 / normA0;
            a1 = normA1 / normA0;
            a2 = normA2 / normA0;
        }
    }

    /**
     * Process an interleaved stereo audio frame.
     * @param left Left channel sample (in/out)
     * @param right Right channel sample (in/out)
     */
    inline void processStereo(float& left, float& right) {
        // Smooth coefficients toward target (anti-zipper interpolation)
        curB0 += (b0 - curB0) * SMOOTHING_FACTOR;
        curB1 += (b1 - curB1) * SMOOTHING_FACTOR;
        curB2 += (b2 - curB2) * SMOOTHING_FACTOR;
        curA1 += (a1 - curA1) * SMOOTHING_FACTOR;
        curA2 += (a2 - curA2) * SMOOTHING_FACTOR;

        // Process Left Channel
        double inL = left;
        double outL = (curB0 * inL) + (curB1 * x1[0]) + (curB2 * x2[0]) - (curA1 * y1[0]) - (curA2 * y2[0]);
        if (!std::isfinite(outL)) outL = 0.0;
        x2[0] = x1[0]; x1[0] = inL;
        y2[0] = y1[0]; y1[0] = outL;
        left = static_cast<float>(outL);

        // Process Right Channel
        double inR = right;
        double outR = (curB0 * inR) + (curB1 * x1[1]) + (curB2 * x2[1]) - (curA1 * y1[1]) - (curA2 * y2[1]);
        if (!std::isfinite(outR)) outR = 0.0;
        x2[1] = x1[1]; x1[1] = inR;
        y2[1] = y1[1]; y1[1] = outR;
        right = static_cast<float>(outR);
    }
};
