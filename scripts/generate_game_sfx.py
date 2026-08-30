#!/usr/bin/env python3
import math, random, wave, struct
from pathlib import Path

SR = 44100
OUT = Path("app/src/main/res/raw")
OUT.mkdir(parents=True, exist_ok=True)
random.seed(20260830)

def clamp(x):
    return max(-1.0, min(1.0, x))

def wav(name, data):
    peak = max(1e-9, max(abs(x) for x in data))
    gain = min(0.94 / peak, 1.0)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SR)
        frames = bytearray()
        for x in data:
            frames += struct.pack("<h", int(clamp(x * gain) * 32767))
        f.writeframes(frames)

def env(t, attack=.003, decay=12.0):
    a = min(1.0, t / max(attack, 1e-6))
    return a * math.exp(-decay * t)

def tone(duration, freqs, gains=None, decay=8.0, attack=.003, noise=0.0, sweep=0.0):
    n = int(SR * duration)
    gains = gains or [1.0] * len(freqs)
    out = []
    lp = 0.0
    for i in range(n):
        t = i / SR
        e = env(t, attack, decay)
        s = 0.0
        for f, g in zip(freqs, gains):
            ff = f * (1.0 + sweep * t / max(duration, .001))
            s += math.sin(2*math.pi*ff*t) * g
            s += math.sin(2*math.pi*ff*2.01*t) * g * .15
        if noise:
            white = random.uniform(-1,1)
            lp = lp * .72 + white * .28
            s += lp * noise
        out.append(s * e)
    return out

def mix(parts, duration):
    n = int(SR * duration)
    out = [0.0] * n
    for start, data, gain in parts:
        offset = int(start * SR)
        for i, x in enumerate(data):
            j = offset + i
            if 0 <= j < n:
                out[j] += x * gain
    return out

# tactile keyboard click: crisp but soft
wav("sfx_key_click", mix([
    (0, tone(.055,[1300,2600],[.65,.22],decay=42,attack=.001,noise=.22), .55),
    (0, tone(.035,[180],[.4],decay=55,attack=.001), .18),
], .07))

# generic UI tap
wav("sfx_ui_tap", mix([
    (0, tone(.09,[520,1040],[.55,.22],decay=28,attack=.002,noise=.08), .50),
], .10))

# accepted word: short premium upward chime
wav("sfx_word_accepted", mix([
    (0.00, tone(.22,[660,1320],[.55,.13],decay=10,attack=.004), .52),
    (.08, tone(.28,[990,1980],[.55,.10],decay=9,attack=.004), .50),
], .38))

# notification
wav("sfx_soft_notify", mix([
    (0.00, tone(.18,[760,1520],[.55,.10],decay=11), .40),
    (.07, tone(.20,[1140,2280],[.45,.08],decay=11), .36),
], .30))

# bonus sparkle
wav("sfx_bonus", mix([
    (0.00, tone(.20,[784,1568],[.50,.10],decay=10), .42),
    (.09, tone(.22,[1046,2092],[.50,.10],decay=9), .44),
    (.18, tone(.30,[1568,3136],[.44,.08],decay=8), .42),
], .56))

# warning / invalid
wav("sfx_warning", mix([
    (0.00, tone(.16,[185,370],[.70,.18],decay=9,attack=.002,noise=.05), .50),
    (.11, tone(.15,[165,330],[.65,.16],decay=10,attack=.002), .42),
], .30))

# countdown tick
wav("sfx_countdown", mix([
    (0.00, tone(.075,[900,1800],[.45,.12],decay=38,attack=.001,noise=.05), .40),
], .085))

# heartbeat: two warm low thumps
beat1 = tone(.16,[58,116],[.9,.16],decay=20,attack=.003,noise=.015)
beat2 = tone(.14,[52,104],[.85,.14],decay=22,attack=.003,noise=.01)
wav("sfx_heartbeat", mix([(0,beat1,.65),(.13,beat2,.48)], .36))

# explosion / bomb loss: sub boom + filtered noise tail
n = int(SR * .72)
out=[]
lp=0.0
for i in range(n):
    t=i/SR
    white=random.uniform(-1,1)
    lp=lp*.86+white*.14
    boom=math.sin(2*math.pi*(72-34*t)*t)*math.exp(-7*t)
    snap=white*math.exp(-18*t)
    rumble=lp*math.exp(-3.8*t)
    out.append(boom*.72 + snap*.24 + rumble*.28)
wav("sfx_explosion", out)

# victory
wav("sfx_victory", mix([
    (0.00,tone(.32,[523,1046],[.55,.10],decay=7),.45),
    (.13,tone(.36,[659,1318],[.55,.10],decay=6),.48),
    (.28,tone(.55,[784,1568],[.58,.10],decay=5),.55),
], .92))

# defeat
wav("sfx_defeat", mix([
    (0.00,tone(.28,[330,660],[.55,.10],decay=7,sweep=-.18),.42),
    (.16,tone(.40,[247,494],[.55,.08],decay=6,sweep=-.22),.45),
], .65))

print("generated", len(list(OUT.glob("sfx_*.wav"))), "wav files")
