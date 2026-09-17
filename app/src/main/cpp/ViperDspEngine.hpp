#pragma once

#include <vector>
#include <array>
#include <atomic>
#include <mutex>
#include <cmath>
#include <algorithm>
#include "BiquadFilter.hpp"
#include "ViperBass.hpp"
#include "ViperClarity.hpp"
#include "ViperConvolver.hpp"

class ViperDspEngine {
private:
    double sampleRate = 44100.0;
    std::atomic<bool> globalEnabled{true};
    std::atomic<bool> eqEnabled{true};

    // 10 Dolby Parametric Equalizer Bands
    static constexpr int NUM_BANDS = 10;
    std::array<BiquadFilter, NUM_BANDS> eqBands;
    std::array<double, NUM_BANDS> centerFrequencies = {
        31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0
    };
    std::array<double, NUM_BANDS> bandGainsDb = {0.0};

    // Preamp Gains
    std::atomic<float> userPreampFactor{1.0f};
    std::atomic<float> autoHeadroomFactor{1.0f};

    // ViPERFX Sub-Engines
    ViperBass viperBass;
    ViperClarity viperClarity;
    ViperConvolver viperConvolver;

    std::mutex engineMutex;

public:
    ViperDspEngine();

    void init(double sRate);
    void reset();

    void setGlobalEnabled(bool enabled);
    void setEqEnabled(bool enabled);
    void setEqBand(int bandIndex, double gainDb);
    void setUserPreampDb(float preampDb);

    void setBass(bool enabled, float gainDb);
    void setClarity(bool enabled, float gainDb);
    void setConvolverEnabled(bool enabled);
    void loadImpulseResponse(const float* irData, int length, int channels);

    void processStereoInterleaved(float* buffer, int frameCount);

private:
    void recalculatePreampHeadroom();
    void applySoftLimiter(float& sample);
};
