"""Rebuild the shared, warm game cue palette (no third-party samples)."""
from array import array
from math import sin, cos, exp, pi
from pathlib import Path
import wave

RATE = 22050
ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
# Gentle electric-piano voices in a common D major pentatonic palette.
CUES = {
    'key_click': [(440, 0, .13, .23)],
    'ui_tap': [(293.66, 0, .19, .30)],
    'word_accepted': [(369.99, 0, .36, .34), (554.37, .09, .38, .26)],
    'soft_notify': [(329.63, 0, .42, .30), (440, .12, .44, .22)],
    'bonus': [(293.66, 0, .48, .26), (369.99, .10, .45, .24), (554.37, .20, .50, .21)],
    'warning': [(246.94, 0, .36, .25), (220, .12, .40, .20)],
    'heartbeat': [(146.83, 0, .22, .27), (146.83, .24, .25, .20)],
    'victory': [(293.66, 0, .65, .24), (369.99, .14, .65, .22), (440, .28, .72, .20), (587.33, .43, .85, .18)],
    'defeat': [(293.66, 0, .65, .22), (246.94, .20, .70, .21), (220, .40, .80, .18)],
    'countdown': [(293.66, 0, .20, .19)],
    'turn_start': [(293.66, 0, .38, .28), (440, .12, .42, .22)],
    'rival_move': [(329.63, 0, .32, .25), (293.66, .08, .36, .20)],
    'round_win': [(369.99, 0, .52, .26), (440, .13, .56, .22), (554.37, .26, .62, .20)],
    'round_lost': [(293.66, 0, .52, .24), (246.94, .18, .58, .20)],
    'streak': [(440, 0, .40, .25), (554.37, .09, .46, .22), (587.33, .19, .55, .18)],
}

def render(notes):
    length = int((max(start + duration for _, start, duration, _ in notes) + .14) * RATE)
    samples = [0.0] * length
    for hz, start, duration, level in notes:
        offset = int(start * RATE)
        count = int(duration * RATE)
        for i in range(count):
            t = i / RATE
            attack = .5 - .5 * cos(pi * min(t / .018, 1))
            tail = .5 - .5 * cos(pi * min((duration - t) / .08, 1))
            envelope = attack * tail * exp(-4.2 * t / duration)
            voice = sin(2 * pi * hz * t) + .12 * sin(4 * pi * hz * t) + .025 * sin(6 * pi * hz * t)
            samples[offset + i] += level * envelope * voice
    # A subtle short room tail with a smooth envelope; never normalize up quiet cues.
    dry = samples[:]
    for delay, gain in [(.045, .10), (.085, .045)]:
        shift = int(delay * RATE)
        for i in range(shift, length):
            samples[i] += dry[i - shift] * gain
    assert max(abs(v) for v in samples) < .65
    return array('h', (round(v * 32767) for v in samples))

if __name__ == '__main__':
    for name, notes in CUES.items():
        pcm = render(notes)
        with wave.open(str(ROOT / f'sfx_{name}.wav'), 'wb') as out:
            out.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
            out.writeframes(pcm.tobytes())
    print(f'Rendered {len(CUES)} soft cues')
