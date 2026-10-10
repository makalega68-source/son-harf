"""Original word-game audio palette, synthesised from scratch (no sampled or borrowed audio).

The feel follows what players know from popular word games, kept deliberately soft: a wooden
tile tap per key, bubble pops for accepted words and bonuses, a muted low "bonk" (never a
buzzer) for a wrong word and warm, rounded tones for wins. No bells or bright metal.
Pure standard library so it runs anywhere: python3 scripts/generate_word_game_sfx.py
"""
from pathlib import Path
import math
import random
import struct
import wave

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
RNG = random.Random(2610061)

# Equal-tempered note frequencies used below.
NOTE = {
    'C4': 261.63, 'G4': 392.00, 'A4': 440.00, 'C5': 523.25, 'D5': 587.33, 'E5': 659.25,
    'G5': 783.99, 'A5': 880.00, 'C6': 1046.50, 'D6': 1174.66, 'E6': 1318.51, 'G6': 1567.98,
    'C7': 2093.00, 'E7': 2637.02,
}


def marimba(hz, length):
    """Struck wooden bar: fundamental, the bar's 4x overtone and a short mallet click."""
    n = int(length * RATE)
    decay = max(.10, min(.55, 260.0 / hz))      # low bars ring longer
    out = []
    for i in range(n):
        t = i / RATE
        a = 1 - math.exp(-t / .0015)
        v = (.78 * math.sin(2 * math.pi * hz * t) * math.exp(-t / decay)
             + .20 * math.sin(2 * math.pi * hz * 3.93 * t) * math.exp(-t / (decay * .22))
             + .05 * math.sin(2 * math.pi * hz * 9.2 * t) * math.exp(-t / .006))
        out.append(v * a)
    return out


def glock(hz, length):
    """Metal bar: bright, long ringing partials, a gentle sparkle."""
    n = int(length * RATE)
    out = []
    for i in range(n):
        t = i / RATE
        a = 1 - math.exp(-t / .001)
        v = (.62 * math.sin(2 * math.pi * hz * t) * math.exp(-t / .55)
             + .22 * math.sin(2 * math.pi * hz * 2.76 * t) * math.exp(-t / .18)
             + .08 * math.sin(2 * math.pi * hz * 5.40 * t) * math.exp(-t / .07))
        out.append(v * a)
    return out


def wood(hz, length, noise=.22):
    """Wooden tile on a board: inharmonic body modes and a tiny filtered click."""
    n = int(length * RATE)
    out, lp = [], 0.0
    for i in range(n):
        t = i / RATE
        lp += .35 * (RNG.uniform(-1, 1) - lp)
        v = (.70 * math.sin(2 * math.pi * hz * t) * math.exp(-t / .016)
             + .24 * math.sin(2 * math.pi * hz * 2.31 * t) * math.exp(-t / .008)
             + .10 * math.sin(2 * math.pi * hz * 3.87 * t) * math.exp(-t / .004)
             + noise * lp * math.exp(-t / .004))
        out.append(v * (1 - math.exp(-t / .0006)))
    return out


def bonk(hz, length):
    """Soft, low, rounded thud with a slight downward bend: 'not quite', never harsh."""
    n = int(length * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        f = hz * (1 - .12 * (1 - math.exp(-t / .05)))
        phase += 2 * math.pi * f / RATE
        v = (.85 * math.sin(phase) + .12 * math.sin(2 * phase)) * math.exp(-t / .09)
        out.append(v * (1 - math.exp(-t / .002)))
    return out


def pop(length=.05):
    """Bubble pop for buttons: a fast upward sine glide."""
    n = int(length * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        f = 520 + 520 * (t / length)
        phase += 2 * math.pi * f / RATE
        out.append(math.sin(phase) * math.exp(-t / .014) * (1 - math.exp(-t / .0008)))
    return out


def mix(parts, length):
    buf = [0.0] * int(length * RATE)
    for start, gain, voice in parts:
        s = int(start * RATE)
        for i, v in enumerate(voice):
            if s + i >= len(buf):
                break
            buf[s + i] += v * gain
    return buf


def write(name, buf, peak=.62):
    m = max(1e-9, max(abs(v) for v in buf))
    fade = int(.012 * RATE)
    for i in range(min(fade, len(buf))):
        buf[-1 - i] *= i / fade
    frames = b''.join(struct.pack('<h', int(max(-1, min(1, v / m * peak)) * 32767)) for v in buf)
    with wave.open(str(ROOT / f'{name}.wav'), 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(frames)


def seq(notes, voice, step, length, gain=1.0, ring=.6):
    return mix([(i * step, gain, voice(NOTE[n], ring)) for i, n in enumerate(notes)], length)


def bubble(f0, f1, length, decay=.035):
    """Soft bubble: a rounded sine glide, no bright overtones."""
    n = int(length * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        f = f0 + (f1 - f0) * min(1.0, t / max(length * .6, 1e-3))
        phase += 2 * math.pi * f / RATE
        out.append(math.sin(phase) * math.exp(-t / decay) * (1 - math.exp(-t / .003)))
    return out


def soft(hz, length, decay=.16):
    """Soft rounded tone: a pure sine with a slow attack and a whisper of the octave."""
    n = int(length * RATE)
    out = []
    for i in range(n):
        t = i / RATE
        v = (.9 * math.sin(2 * math.pi * hz * t) + .08 * math.sin(4 * math.pi * hz * t) * math.exp(-t / .03))
        out.append(v * math.exp(-t / decay) * (1 - math.exp(-t / .006)))
    return out


def lowpass(buf, k=.25):
    """Gentle one-pole low-pass to take any edge off."""
    out, y = [], 0.0
    for v in buf:
        y += k * (v - y)
        out.append(y)
    return out


def main():
    # Every cue is built from the same soft family as the key tap and the bubble pop:
    # no bells, no bright metal, fundamentals kept low, rounded attacks, gentle peaks.
    write('sfx_key_click', wood(1250, .05), peak=.42)
    write('sfx_ui_tap', pop(), peak=.38)
    write('sfx_rival_move', wood(820, .07, noise=.15), peak=.40)
    write('sfx_countdown', wood(1100, .045, noise=.1), peak=.36)
    write('sfx_heartbeat', mix([(0, 1, bonk(110, .16)), (.17, .7, bonk(98, .16))], .36), peak=.45)

    # Accepted word: two quick rising bubbles.
    write('sfx_word_accepted', lowpass(mix([(0, .85, bubble(420, 620, .09)), (.07, 1, bubble(560, 840, .11))], .22)), peak=.48)
    # Notification: one soft bubble.
    write('sfx_soft_notify', lowpass(bubble(480, 660, .12, decay=.05)), peak=.38)
    # Your turn: soft two-note tone, low register.
    write('sfx_turn_start', lowpass(mix([(0, .8, soft(NOTE['G4'], .28, .09)), (.10, 1, soft(NOTE['C5'], .34, .11))], .48)), peak=.40)
    # Wrong word: two soft low bonks.
    write('sfx_warning', mix([(0, 1, bonk(196, .18)), (.13, .85, bonk(165, .22))], .38), peak=.48)
    # Bonus: a quick run of three rising bubbles.
    write('sfx_bonus', lowpass(mix([(0, .8, bubble(420, 600, .08)), (.06, .9, bubble(520, 740, .08)),
                                    (.12, 1, bubble(620, 900, .11))], .28)), peak=.48)
    # Streak: four bubbles, a touch faster.
    write('sfx_streak', lowpass(mix([(i * .05, .75 + i * .08, bubble(380 + i * 90, 560 + i * 110, .08)) for i in range(4)], .3)), peak=.48)
    # Round won: soft rising tones resolving gently.
    write('sfx_round_win', lowpass(mix([(0, .8, soft(NOTE['C5'], .3, .10)), (.10, .85, soft(NOTE['E5'], .3, .10)),
                                        (.20, 1, soft(NOTE['G5'], .55, .18)), (.20, .45, soft(NOTE['C5'], .55, .18))], .8)), peak=.44)
    # Round lost: soft falling pair, low.
    write('sfx_round_lost', lowpass(mix([(0, .9, soft(NOTE['E5'], .3, .12)), (.16, 1, soft(NOTE['C5'], .5, .18))], .72)), peak=.40)
    # Match won: bubbles rising into a warm soft chord.
    write('sfx_victory', lowpass(mix([(0, .7, bubble(380, 560, .08)), (.07, .75, bubble(460, 680, .08)),
                                      (.14, .8, bubble(560, 820, .09)),
                                      (.26, 1, soft(NOTE['C5'], .9, .30)), (.26, .7, soft(NOTE['E5'], .9, .30)),
                                      (.26, .6, soft(NOTE['G5'], .9, .30))], 1.25)), peak=.46)
    # Match lost: slow soft descending tones.
    write('sfx_defeat', lowpass(mix([(0, .85, soft(NOTE['G4'] * 1.5, .4, .14)), (.22, .9, soft(NOTE['E5'], .4, .14)),
                                     (.44, 1, soft(NOTE['C5'], .8, .28))], 1.3)), peak=.40)


if __name__ == '__main__':
    main()
