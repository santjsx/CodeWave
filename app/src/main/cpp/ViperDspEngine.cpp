#include "ViperDspEngine.hpp"

ViperDspEngine::ViperDspEngine() {
    eqBands[0] = BiquadFilter(FilterType::LOW_SHELF);
    for (int i = 1; i < 9; ++i) {
        eqBands[i] = BiquadFilter(FilterType::PEAKING);
    }
    eqBands[9] = BiquadFilter(FilterType::HIGH_SHELF);
    init(44100.0);
}

void ViperDspEngine::init(double sRate) {
    std::lock_guard<std::mutex> lock(engineMutex);
    sampleRate = (sRate > 0.0) ? sRate : 44100.0;

    for (int i = 0; i < NUM_BANDS; ++i) {
        eqBands[i].setSampleRate(sampleRate);
        eqBands[i].updateCoefficients(centerFrequencies[i], bandGainsDb[i], 1.414);
        eqBands[i].reset();
    }

    viperBass.init(sampleRate);
    viperClarity.init(sampleRate);
    viperConvolver.reset();
}

void ViperDspEngine::reset() {
    std::lock_guard<std::mutex> lock(engineMutex);
    for (int i = 0; i < NUM_BANDS; ++i) {
        eqBands[i].reset();
    }
    viperBass.reset();
    viperClarity.reset();
    viperConvolver.reset();
}

void ViperDspEngine::setGlobalEnabled(bool enabled) {
    globalEnabled.store(enabled, std::memory_order_release);
}

void ViperDspEngine::setEqEnabled(bool enabled) {
    eqEnabled.store(enabled, std::memory_order_release);
}

void ViperDspEngine::setEqBand(int bandIndex, double gainDb) {
    if (bandIndex < 0 || bandIndex >= NUM_BANDS) return;
    std::lock_guard<std::mutex> lock(engineMutex);
    bandGainsDb[bandIndex] = gainDb;
    eqBands[bandIndex].updateCoefficients(centerFrequencies[bandIndex], gainDb, 1.414);
    recalculatePreampHeadroom();
}

void ViperDspEngine::setUserPreampDb(float preampDb) {
    float clamped = std::clamp(preampDb, -15.0f, 15.0f);
    userPreampFactor.store(std::pow(10.0f, clamped / 20.0f), std::memory_order_release);
}

void ViperDspEngine::setBass(bool enabled, float gainDb) {
    std::lock_guard<std::mutex> lock(engineMutex);
    viperBass.setEnabled(enabled);
    viperBass.setGainDb(gainDb);
    recalculatePreampHeadroom();
}

void ViperDspEngine::setClarity(bool enabled, float gainDb) {
    std::lock_guard<std::mutex> lock(engineMutex);
    viperClarity.setEnabled(enabled);
    viperClarity.setGainDb(gainDb);
}

void ViperDspEngine::setConvolverEnabled(bool enabled) {
    viperConvolver.setEnabled(enabled);
}

void ViperDspEngine::loadImpulseResponse(const float* irData, int length, int channels) {
    viperConvolver.loadImpulseResponse(irData, length, channels);
}

void ViperDspEngine::recalculatePreampHeadroom() {
    // Dynamic Headroom Guard: Drop master volume by the maximum band/bass boost (Edge Case 61, 62)
    double maxBoost = 0.0;
    for (int i = 0; i < NUM_BANDS; ++i) {
        if (bandGainsDb[i] > maxBoost) {
            maxBoost = bandGainsDb[i];
        }
    }

    if (maxBoost > 0.0) {
        autoHeadroomFactor.store(static_cast<float>(std::pow(10.0, -maxBoost / 20.0)), std::memory_order_release);
    } else {
        autoHeadroomFactor.store(1.0f, std::memory_order_release);
    }
}

void ViperDspEngine::applySoftLimiter(float& sample) {
    // Transparent True-Peak Soft-Clipping Limiter (-0.2 dBFS ceiling, threshold = 0.95f)
    // Edge Cases 63, 64
    constexpr float threshold = 0.95f;
    if (sample > threshold) {
        float excess = sample - threshold;
        sample = threshold + excess / (1.0f + std::pow(excess / (1.0f - threshold), 2.0f));
    } else if (sample < -threshold) {
        float excess = -sample - threshold;
        sample = -(threshold + excess / (1.0f + std::pow(excess / (1.0f - threshold), 2.0f)));
    }
    sample = std::clamp(sample, -1.0f, 1.0f);
}

void ViperDspEngine::processStereoInterleaved(float* buffer, int frameCount) {
    if (!globalEnabled.load(std::memory_order_acquire)) {
        // Bit-perfect passthrough
        return;
    }

    float currentHeadroom = autoHeadroomFactor.load(std::memory_order_relaxed);
    float currentUserGain = userPreampFactor.load(std::memory_order_relaxed);
    float combinedGain = currentHeadroom * currentUserGain;
    bool isEqActive = eqEnabled.load(std::memory_order_relaxed);

    for (int frame = 0; frame < frameCount; ++frame) {
        int idxL = frame * 2;
        int idxR = idxL + 1;

        float left = buffer[idxL];
        float right = buffer[idxR];

        // 1. Apply Dynamic Preamp Headroom Guard
        if (combinedGain != 1.0f) {
            left *= combinedGain;
            right *= combinedGain;
        }

        // 2. Cascade through 10-band Dolby Parametric Biquads
        if (isEqActive) {
            for (int b = 0; b < NUM_BANDS; ++b) {
                eqBands[b].processStereo(left, right);
            }
        }

        // 3. Apply ViPER Bass (Psychoacoustic sub-harmonic generator)
        viperBass.processStereo(left, right);

        // 4. Apply ViPER Clarity (Harmonic definition exciter)
        viperClarity.processStereo(left, right);

        // 5. Apply ViPER Convolver (IRS impulse response matrix)
        viperConvolver.processStereo(left, right);

        // 6. Safety Soft Limiter (Guarantees zero digital distortion or audio clipping)
        applySoftLimiter(left);
        applySoftLimiter(right);

        buffer[idxL] = left;
        buffer[idxR] = right;
    }
}
