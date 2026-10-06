"""Original word-game audio palette, synthesised from scratch (no sampled or borrowed audio).

The feel follows what players know from popular word games: a soft wooden tile tap per key,
a bright two-note marimba for an accepted word, glockenspiel sparkle for bonuses, a muted
low "bonk" (never a buzzer) for a wrong word and short marimba fanfares for wins.
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


def main():
    # Typing and taps: quiet, short, woody.
    write('sfx_key_click', wood(1250, .05), peak=.45)
    write('sfx_ui_tap', pop(), peak=.40)
    write('sfx_rival_move', wood(820, .07, noise=.15), peak=.45)
    write('sfx_countdown', wood(1700, .045, noise=.1), peak=.40)
    write('sfx_heartbeat', mix([(0, 1, bonk(110, .16)), (.17, .7, bonk(98, .16))], .36), peak=.50)

    # Accepted word: the classic bright upward two-note marimba.
    write('sfx_word_accepted', mix([(0, .9, marimba(NOTE['C6'], .32)), (.075, 1, marimba(NOTE['G6'], .42))], .5))
    # A notification: one soft glockenspiel note.
    write('sfx_soft_notify', glock(NOTE['E6'], .7), peak=.45)
    # Your turn: gentle G-C chime.
    write('sfx_turn_start', mix([(0, .8, glock(NOTE['G5'], .5)), (.11, 1, glock(NOTE['C6'], .6))], .75), peak=.50)
    # Wrong word: two soft low bonks.
    write('sfx_warning', mix([(0, 1, bonk(196, .18)), (.13, .85, bonk(165, .22))], .38), peak=.55)
    # Bonus: quick glockenspiel sparkle arpeggio.
    write('sfx_bonus', seq(['C6', 'E6', 'G6', 'C7'], glock, .055, .95, ring=.75))
    # Streak / combo: rising marimba run topped with a glock note.
    write('sfx_streak', mix([(0, .8, marimba(NOTE['E5'], .3)), (.06, .85, marimba(NOTE['G5'], .3)),
                             (.12, .9, marimba(NOTE['C6'], .35)), (.18, .7, glock(NOTE['E7'], .5))], .75))
    # Round won: marimba arpeggio resolving on a chord.
    write('sfx_round_win', mix([(0, .8, marimba(NOTE['C5'], .4)), (.09, .85, marimba(NOTE['E5'], .4)),
                                (.18, .9, marimba(NOTE['G5'], .5)), (.30, 1, marimba(NOTE['C6'], .6)),
                                (.30, .5, glock(NOTE['E6'], .7))], 1.05))
    # Round lost: mellow falling third, low and soft.
    write('sfx_round_lost', mix([(0, .9, marimba(NOTE['E5'], .45)), (.17, 1, marimba(NOTE['C5'], .6))], .85), peak=.50)
    # Match won: a short marimba fanfare with a glockenspiel crown.
    write('sfx_victory', mix([(0, .7, marimba(NOTE['C5'], .4)), (.10, .75, marimba(NOTE['E5'], .4)),
                              (.20, .8, marimba(NOTE['G5'], .4)), (.32, .9, marimba(NOTE['C6'], .5)),
                              (.46, .8, marimba(NOTE['E6'], .5)), (.60, 1, marimba(NOTE['G6'], .8)),
                              (.60, .6, marimba(NOTE['C6'], .8)), (.60, .55, glock(NOTE['C7'], 1.0)),
                              (.60, .35, marimba(NOTE['C5'], .9))], 1.75), peak=.66)
    # Match lost: slow, gentle descending marimba, not a sad trombone.
    write('sfx_defeat', mix([(0, .8, marimba(NOTE['G5'], .5)), (.20, .85, marimba(NOTE['E5'], .5)),
                             (.40, .9, marimba(NOTE['D5'], .5)), (.62, 1, marimba(NOTE['C5'], .9)),
                             (.62, .4, marimba(NOTE['G4'], .9))], 1.6), peak=.52)


if __name__ == '__main__':
    main()
