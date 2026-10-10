"""Original offline game audio: wood/ceramic contacts, plucked rewards and soft scene swishes.
No borrowed audio; deterministic renders, short transients, no runtime synthesis.
"""
from pathlib import Path
import wave
import numpy as np

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
RNG = np.random.default_rng(2601002)


def voice(hz, duration, kind='pluck'):
    t = np.arange(int(duration * RATE)) / RATE
    attack = 1 - np.exp(-t / .0025)
    tail = np.minimum(1, (duration-t)/.035).clip(0,1)
    if kind == 'wood':
        # Inharmonic damped body modes, rather than a pitched UI beep.
        v = sum(a*np.sin(2*np.pi*hz*r*t)*np.exp(-t/d) for r,a,d in [(1,.7,.018),(2.76,.22,.009),(4.1,.09,.005)])
        n = RNG.normal(0,1,len(t)); n=np.convolve(n,np.ones(9)/9,mode='same')
        v += n*.18*np.exp(-t/.008)
    elif kind == 'drum':
        phase=2*np.pi*(hz*t+hz*.012*(1-np.exp(-t/.012)))
        v=np.sin(phase)*np.exp(-t/.045)
    else:
        # Damped mallet/plucked-string modes with slightly detuned upper partials.
        v = sum(a*np.sin(2*np.pi*hz*r*t)*np.exp(-t/(d*duration)) for r,a,d in [(1,.63,.32),(2.003,.21,.16),(3.01,.10,.09),(4.15,.035,.05)])
        v += .025*np.convolve(RNG.normal(0,1,len(t)),np.ones(15)/15,mode='same')*np.exp(-t/.014)
    return v*attack*tail


def render(notes, duration, sweep=False):
    x=np.zeros(int(duration*RATE))
    for hz,start,length,gain,kind in notes:
        v=voice(hz,length,kind)*gain
        i=int(start*RATE);n=min(len(v),len(x)-i)
        x[i:i+n]+=v[:n]
    if sweep:
        t=np.arange(len(x))/RATE
        noise=np.convolve(RNG.normal(0,1,len(x)),np.ones(45)/45,mode='same')
        x+=.20*noise*np.sin(np.pi*np.minimum(t/.24,1))**2*np.exp(-t/.12)
    dry=x.copy()
    for lag,gain in [(0.033,.09),(.061,.045),(.097,.018)]:
        i=int(lag*RATE);x[i:]+=dry[:-i]*gain
    # Conservative master ceiling; leave transient headroom and fades on both ends.
    x=np.tanh(x)*.82
    x[:180]*=np.linspace(0,1,180);x[-1500:]*=np.linspace(1,0,1500)
    assert np.max(np.abs(x))<.82
    return (x*32767).astype('<i2')

# Contact, gesture and reward have distinct physical timbres; no melody on every key.
CUES={
 'key_click':(.09,[(580,0,.07,.55,'wood')],False),
 'ui_tap':(.14,[(390,0,.11,.55,'wood'),(165,.005,.09,.18,'drum')],False),
 'word_accepted':(.46,[(392,0,.30,.65,'pluck'),(587.33,.065,.32,.45,'pluck'),(120,0,.12,.14,'drum')],True),
 'soft_notify':(.40,[(523.25,0,.32,.48,'pluck'),(659.25,.08,.25,.22,'pluck')],False),
 'bonus':(.79,[(392,0,.38,.43,'pluck'),(493.88,.07,.4,.46,'pluck'),(587.33,.14,.42,.43,'pluck'),(783.99,.23,.44,.35,'pluck')],True),
 'warning':(.24,[(180,0,.12,.40,'wood'),(150,.09,.12,.34,'wood')],False),
 'heartbeat':(.28,[(76,0,.10,.52,'drum'),(92,.12,.09,.34,'drum')],False),
 'victory':(1.32,[(196,0,.36,.32,'pluck'),(392,.07,.46,.50,'pluck'),(493.88,.18,.46,.46,'pluck'),(587.33,.30,.50,.45,'pluck'),(783.99,.48,.63,.46,'pluck'),(392,.48,.68,.23,'pluck'),(493.88,.48,.62,.18,'pluck')],True),
 'defeat':(.87,[(392,0,.45,.43,'pluck'),(329.63,.15,.47,.35,'pluck'),(293.66,.30,.48,.34,'pluck'),(146.83,.30,.46,.14,'pluck')],False),
 'countdown':(.11,[(420,0,.08,.48,'wood')],False),
 'turn_start':(.37,[(330,0,.12,.38,'wood'),(392,.03,.24,.42,'pluck')],True),
 'rival_move':(.24,[(310,0,.14,.40,'wood'),(140,.008,.16,.14,'drum')],True),
 'round_win':(.72,[(392,0,.37,.52,'pluck'),(493.88,.09,.38,.45,'pluck'),(587.33,.19,.42,.46,'pluck')],True),
 'round_lost':(.58,[(329.63,0,.34,.41,'pluck'),(293.66,.14,.35,.34,'pluck')],False),
 'streak':(.56,[(493.88,0,.30,.46,'pluck'),(587.33,.06,.34,.46,'pluck'),(783.99,.14,.35,.40,'pluck')],True),
}
if __name__=='__main__':
    for name,(duration,notes,sweep) in CUES.items():
        pcm=render(notes,duration,sweep)
        with wave.open(str(ROOT/f'sfx_{name}.wav'),'wb') as out:
            out.setparams((1,2,RATE,0,'NONE','not compressed'));out.writeframes(pcm.tobytes())
    print(f'Rendered {len(CUES)} original game cues at {RATE} Hz')
