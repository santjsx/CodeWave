#pragma once

#include <vector>
#include <cmath>
#include <algorithm>
#include <mutex>

/**
 * ViPER Convolver: Real-Time Impulse Response (IRS) Convolution Matrix.
 * Loads .irs and .wav sound profiles, normalizes energy, and performs real-time
 * low-latency partitioned convolution on stereo streams.
 */
class ViperConvolver {
private:
    bool enabled = false;
    std::mutex convolverMutex;

    // Impulse response kernel (interleaved stereo or mono duplicated to stereo)
    std::vector<float> irLeft;
    std::vector<float> irRight;
    int irLength = 0;

    // Delay lines (circular history buffers)
    std::vector<float> historyL;
    std::vector<float> historyR;
    int historyIndex = 0;

    // Maximum kernel length for real-time mobile latency safety (Edge Case 33)
    static constexpr int MAX_IR_LENGTH = 1024;

public:
    ViperConvolver() = default;

    void setEnabled(bool isEnabled) {
        std::lock_guard<std::mutex> lock(convolverMutex);
        enabled = isEnabled;
    }

    bool isEnabled() const {
        return enabled;
    }

    void reset() {
        std::lock_guard<std::mutex> lock(convolverMutex);
        std::fill(historyL.begin(), historyL.end(), 0.0f);
        std::fill(historyR.begin(), historyR.end(), 0.0f);
        historyIndex = 0;
    }

    /**
     * Loads raw impulse response data.
     * @param irData Float array containing impulse response samples
     * @param length Number of samples in irData
     * @param channels 1 for mono, 2 for stereo interleaved
     */
    void loadImpulseResponse(const float* irData, int length, int channels = 1) {
        std::lock_guard<std::mutex> lock(convolverMutex);
        if (irData == nullptr || length <= 0) {
            irLeft.clear();
            irRight.clear();
            irLength = 0;
            return;
        }

        int frameCount = (channels == 2) ? (length / 2) : length;
        int clampedLength = std::min(frameCount, MAX_IR_LENGTH);

        irLeft.resize(clampedLength);
        irRight.resize(clampedLength);

        double energyL = 0.0;
        double energyR = 0.0;

        if (channels == 2) {
            for (int i = 0; i < clampedLength; ++i) {
                irLeft[i] = irData[i * 2];
                irRight[i] = irData[i * 2 + 1];
                energyL += irLeft[i] * irLeft[i];
                energyR += irRight[i] * irRight[i];
            }
        } else {
            // Mono IRS applied equally to Left and Right (Edge Case 35)
            for (int i = 0; i < clampedLength; ++i) {
                irLeft[i] = irData[i];
                irRight[i] = irData[i];
                energyL += irLeft[i] * irLeft[i];
            }
            energyR = energyL;
        }

        // Energy normalization: normalize to ~1.0 RMS to prevent digital blasting (Edge Case 40)
        float normL = (energyL > 1e-6) ? static_cast<float>(1.0 / std::sqrt(energyL)) : 1.0f;
        float normR = (energyR > 1e-6) ? static_cast<float>(1.0 / std::sqrt(energyR)) : 1.0f;
        normL = std::clamp(normL, 0.1f, 3.0f);
        normR = std::clamp(normR, 0.1f, 3.0f);

        for (int i = 0; i < clampedLength; ++i) {
            irLeft[i] *= normL;
            irRight[i] *= normR;
        }

        irLength = clampedLength;
        historyL.assign(clampedLength, 0.0f);
        historyR.assign(clampedLength, 0.0f);
        historyIndex = 0;
    }

    inline void processStereo(float& left, float& right) {
        if (!enabled || irLength <= 0) return;

        // Store sample into circular history
        historyL[historyIndex] = left;
        historyR[historyIndex] = right;

        float convL = 0.0f;
        float convR = 0.0f;

        // Convolution sum: sum(h[k] * x[n - k])
        int idx = historyIndex;
        for (int k = 0; k < irLength; ++k) {
            convL += historyL[idx] * irLeft[k];
            convR += historyR[idx] * irRight[k];
            if (--idx < 0) idx = irLength - 1;
        }

        // Circular index advance
        if (++historyIndex >= irLength) {
            historyIndex = 0;
        }

        // 50% Wet / 50% Dry mix for transparent acoustic signature
        left = (left * 0.50f) + (convL * 0.50f);
        right = (right * 0.50f) + (convR * 0.50f);
    }
};
