import bpy
import os
import sys


def arg(name: str) -> str:
    token = f"--{name}"
    if token not in sys.argv:
        raise SystemExit(f"missing {token}")
    return sys.argv[sys.argv.index(token) + 1]


base_file = arg("base")
out_file = arg("out")
anim_dir = arg("anim-dir")

bpy.ops.wm.open_mainfile(filepath=base_file)

armatures = [o for o in bpy.data.objects if o.type == "ARMATURE"]
meshes = [o for o in bpy.data.objects if o.type == "MESH"]
if not armatures or not meshes:
    raise SystemExit("fox-cat.blend did not contain a usable armature + mesh")

armature = max(armatures, key=lambda o: len(o.data.bones))
armature.animation_data_create()

print("TARGET_ARMATURE", armature.name, "BONES", len(armature.data.bones))
print("MESHES", [o.name for o in meshes])
print("MATERIALS_BEFORE", [m.name for m in bpy.data.materials])

# Clean, soft mascot palette. Preserve named nose/pupil details when possible.
for mat in bpy.data.materials:
    name = mat.name.lower()
    if "eye" in name or "iris" in name:
        color = (0.025, 0.28, 0.95, 1.0)
        roughness = 0.18
        metallic = 0.03
    elif "pupil" in name:
        color = (0.005, 0.008, 0.012, 1.0)
        roughness = 0.22
        metallic = 0.0
    elif "nose" in name or "tongue" in name or "mouth" in name:
        color = (0.86, 0.20, 0.38, 1.0)
        roughness = 0.46
        metallic = 0.0
    else:
        color = (0.93, 0.95, 0.98, 1.0)
        roughness = 0.64
        metallic = 0.0

    mat.diffuse_color = color
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF") if mat.node_tree else None
    if bsdf:
        if "Base Color" in bsdf.inputs:
            bsdf.inputs["Base Color"].default_value = color
        if "Roughness" in bsdf.inputs:
            bsdf.inputs["Roughness"].default_value = roughness
        if "Metallic" in bsdf.inputs:
            bsdf.inputs["Metallic"].default_value = metallic

# Import compatible CC0 fox-family actions and attach each as its own NLA track.
clips = [
    ("animation-fox-idle.blend", "idle_base"),
    ("animation-fox-idle-alert.blend", "idle_alert"),
    ("animation-fox-walk.blend", "walk"),
    ("animation-fox-run.blend", "react_streak"),
    ("animation-fox-jump.blend", "react_correct"),
    ("animation-fox-sit.blend", "react_wrong"),
    ("animation-fox-sneak.blend", "react_nervous"),
    ("animation-fox-fetch.blend", "collect"),
    ("animation-fox-howl.blend", "victory"),
]

# Remove any existing target tracks; source rest action is not used as a user-visible clip.
for track in list(armature.animation_data.nla_tracks):
    armature.animation_data.nla_tracks.remove(track)

loaded_names = []
for filename, clip_name in clips:
    path = os.path.join(anim_dir, filename)
    if not os.path.exists(path):
        print("SKIP_MISSING", path)
        continue
    before = {a.as_pointer() for a in bpy.data.actions}
    with bpy.data.libraries.load(path, link=False) as (data_from, data_to):
        data_to.actions = list(data_from.actions)
    candidates = [a for a in bpy.data.actions if a.as_pointer() not in before]
    if not candidates:
        print("SKIP_NO_ACTION", filename)
        continue
    action = max(candidates, key=lambda a: max(1.0, a.frame_range[1] - a.frame_range[0]))
    action.name = clip_name
    start = int(action.frame_range[0])
    track = armature.animation_data.nla_tracks.new()
    track.name = clip_name
    strip = track.strips.new(clip_name, start, action)
    strip.action_frame_start = action.frame_range[0]
    strip.action_frame_end = action.frame_range[1]
    loaded_names.append(clip_name)
    print("CLIP", clip_name, tuple(action.frame_range))

if not loaded_names:
    raise SystemExit("No animation clips could be attached to kitten rig")

# The glTF exporter reads named NLA tracks as distinct animation clips.
os.makedirs(os.path.dirname(out_file), exist_ok=True)
kwargs = dict(
    filepath=out_file,
    export_format="GLB",
    use_selection=False,
    export_animations=True,
    export_nla_strips=True,
    export_skins=True,
    export_morph=True,
    export_yup=True,
)
try:
    bpy.ops.export_scene.gltf(**kwargs)
except TypeError:
    # Older distro Blender versions may not expose every keyword above.
    kwargs.pop("export_morph", None)
    bpy.ops.export_scene.gltf(**kwargs)

if not os.path.exists(out_file) or os.path.getsize(out_file) < 1024:
    raise SystemExit("GLB export failed")

print("EXPORTED", out_file, os.path.getsize(out_file), "bytes", "CLIPS", loaded_names)
