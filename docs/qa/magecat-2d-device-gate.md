# 2D companion real-device gate

This package must not be merged as a production integration until the following checks are completed on at least one physical Android device.

- Verify the 3x3 licensed atlas renders one expression at a time without seams, corruption or excessive memory use.
- Verify TalkBack announces only the supplied contextual label and does not create duplicate decorative image announcements.
- Verify system font scale / large text does not clip the optional message bubble.
- Verify a narrow phone viewport does not place the companion over the primary CTA, board or software keyboard.
- Verify Android reduced-motion / disabled animator scale produces no companion movement.
- Verify lobby/loading/result/league surfaces remain responsive and no frame drops or crashes are introduced.
- Verify no network request, backend write, economy mutation, score/timer/matchmaking mutation or polling originates from the companion package.

Source/provenance:
- Owner-supplied purchased package inputs: `magecat.unitypackage`, `mage_cat.fbx`, `mage_cat_textures.zip`.
- Runtime atlas SHA-256: `0af65dd6a1bd537be61a617516487b275fc2b3442a18c86ece998ff086b7363a`.
- Unity Editor/Readme/native executable content is intentionally not part of the Android runtime package.

Until this checklist is recorded as passed, the current stable production UI remains authoritative and the companion integration stays device-gated.
