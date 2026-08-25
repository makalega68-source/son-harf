# Eve AI Mascot implementation plan

Work in progress on `feature/eve-ai-mascot`.

Goals:
- Replace the existing pet/mascot implementation with Eve.
- Render Eve as a real-time 3D GLB/glTF model using the existing SceneView/Filament stack.
- Provide a dedicated mascot screen with text chat and thought/speech bubbles.
- Add an AI conversation backend contract designed for warm, emotionally attentive day-to-day conversation without manipulative dependency patterns.
- Map AI response state to Eve animation cues such as idle, look-around, rest and reactive motions.
- Keep model-provider secrets server-side.

Acceptance order:
1. Eve asset imports and renders.
2. Idle animation plays reliably.
3. Existing pet UI/code is removed or disconnected.
4. Mascot chat screen works with a mock/local provider.
5. Secure AI backend integration is wired.
6. Animation cue selection and conversation context are connected.
7. QA build passes.
