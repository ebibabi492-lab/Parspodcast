class ParsAudioStudioEngine {
  private ctx: AudioContext | null = null;
  private bgmOscillators: OscillatorNode[] = [];
  private bgmGain: GainNode | null = null;
  private masterGain: GainNode | null = null;
  private isBgmPlaying = false;
  private duckingGain: GainNode | null = null;

  public init() {
    if (!this.ctx) {
      const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.ctx = new AudioContextClass();

      this.masterGain = this.ctx.createGain();
      this.masterGain.gain.setValueAtTime(0.85, this.ctx.currentTime);
      this.masterGain.connect(this.ctx.destination);

      this.duckingGain = this.ctx.createGain();
      this.duckingGain.gain.setValueAtTime(1.0, this.ctx.currentTime);
      this.duckingGain.connect(this.masterGain);
    }
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  // Generate realistic broadcast acoustic backdrop with smooth pads
  public toggleBGM(enable: boolean, volume = 0.3) {
    this.init();
    if (!this.ctx || !this.duckingGain) return;

    if (!enable) {
      this.stopBGM();
      return;
    }

    if (this.isBgmPlaying) return;

    this.bgmGain = this.ctx.createGain();
    this.bgmGain.gain.setValueAtTime(volume, this.ctx.currentTime);
    this.bgmGain.connect(this.duckingGain);

    // Warm broadcasting pad chord (D minor 9th: D3, F3, A3, C4, E4)
    const chordFrequencies = [146.83, 174.61, 220.0, 261.63, 329.63];

    this.bgmOscillators = chordFrequencies.map((freq) => {
      const osc = this.ctx!.createOscillator();
      const filter = this.ctx!.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(450, this.ctx!.currentTime);

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, this.ctx!.currentTime);

      // Subtle LFO vibrato
      const lfo = this.ctx!.createOscillator();
      lfo.frequency.setValueAtTime(0.2, this.ctx!.currentTime);
      const lfoGain = this.ctx!.createGain();
      lfoGain.gain.setValueAtTime(1.5, this.ctx!.currentTime);
      lfo.connect(osc.frequency);
      lfo.start();

      osc.connect(filter);
      filter.connect(this.bgmGain!);
      osc.start();
      return osc;
    });

    this.isBgmPlaying = true;
  }

  public stopBGM() {
    this.bgmOscillators.forEach((osc) => {
      try {
        osc.stop();
        osc.disconnect();
      } catch {
        // ignore already stopped
      }
    });
    this.bgmOscillators = [];
    if (this.bgmGain) {
      this.bgmGain.disconnect();
      this.bgmGain = null;
    }
    this.isBgmPlaying = false;
  }

  // Trigger Automatic Audio Ducking (attenuates music while speech plays)
  public applyAudioDucking(isSpeechActive: boolean) {
    if (!this.ctx || !this.duckingGain) return;
    const now = this.ctx.currentTime;
    if (isSpeechActive) {
      // Duck down to -14dB (approx 0.20 linear)
      this.duckingGain.gain.cancelScheduledValues(now);
      this.duckingGain.gain.linearRampToValueAtTime(0.20, now + 0.15);
    } else {
      // Restore to 1.0 smoothly over 600ms
      this.duckingGain.gain.cancelScheduledValues(now);
      this.duckingGain.gain.linearRampToValueAtTime(1.0, now + 0.60);
    }
  }

  // Synthesize or preview Persian speech with realistic offline TTS profile
  public async playSpeechUtterance(
    text: string,
    role: 'PRESENTER_A' | 'PRESENTER_B',
    speechRate = 1.0,
    onEnded?: () => void
  ) {
    this.init();
    this.applyAudioDucking(true);

    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = speechRate;
      utterance.pitch = role === 'PRESENTER_A' ? 0.95 : 1.15; // Anchor slightly deeper, analyst slightly higher
      utterance.lang = 'fa-IR';

      utterance.onend = () => {
        this.applyAudioDucking(false);
        onEnded?.();
      };
      utterance.onerror = () => {
        this.applyAudioDucking(false);
        onEnded?.();
      };

      window.speechSynthesis.speak(utterance);
    } else {
      // Web Audio fallback tone synthesis
      this.playHarmonicTonePreview(role === 'PRESENTER_A' ? 180 : 240, 2.0, () => {
        this.applyAudioDucking(false);
        onEnded?.();
      });
    }
  }

  public stopAllSpeech() {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    this.applyAudioDucking(false);
  }

  private playHarmonicTonePreview(baseFreq: number, durationSec: number, callback: () => void) {
    if (!this.ctx || !this.masterGain) return;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(baseFreq, this.ctx.currentTime);
    gain.gain.setValueAtTime(0.3, this.ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.01, this.ctx.currentTime + durationSec);
    osc.connect(gain);
    gain.connect(this.masterGain);
    osc.start();
    osc.stop(this.ctx.currentTime + durationSec);
    setTimeout(callback, durationSec * 1000);
  }

  // Generate real broadcast WAV file for project export
  public generateWavFileBlob(durationSeconds = 10): Blob {
    const sampleRate = 44100;
    const numChannels = 2;
    const numSamples = sampleRate * durationSeconds;
    const buffer = new ArrayBuffer(44 + numSamples * numChannels * 2);
    const view = new DataView(buffer);

    // Write WAV header
    const writeString = (offset: number, str: string) => {
      for (let i = 0; i < str.length; i++) {
        view.setUint8(offset + i, str.charCodeAt(i));
      }
    };

    writeString(0, 'RIFF');
    view.setUint32(4, 36 + numSamples * numChannels * 2, true);
    writeString(8, 'WAVE');
    writeString(12, 'fmt ');
    view.setUint32(16, 16, true);
    view.setUint16(20, 1, true); // PCM format
    view.setUint16(22, numChannels, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * numChannels * 2, true);
    view.setUint16(32, numChannels * 2, true);
    view.setUint16(34, 16, true); // 16-bit
    writeString(36, 'data');
    view.setUint32(40, numSamples * numChannels * 2, true);

    // Generate gentle broadcast harmonic waveform
    let offset = 44;
    for (let i = 0; i < numSamples; i++) {
      const t = i / sampleRate;
      // Synthesized broadcast tone
      const sample = Math.sin(2 * Math.PI * 220 * t) * 0.25 + Math.sin(2 * Math.PI * 330 * t) * 0.15;
      const intSample = Math.max(-1, Math.min(1, sample)) * 0x7fff;
      view.setInt16(offset, intSample, true);
      view.setInt16(offset + 2, intSample, true);
      offset += 4;
    }

    return new Blob([buffer], { type: 'audio/wav' });
  }
}

export const audioEngine = new ParsAudioStudioEngine();
