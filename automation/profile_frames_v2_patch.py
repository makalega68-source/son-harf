from pathlib import Path

STORE = Path("app/src/main/java/com/sonharf/game/PremiumStoreScreen.kt")
PROFILE_FRAMES = Path("app/src/main/java/com/sonharf/game/ProfileFramesV2.kt")
PRODUCT_CATALOG = Path("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt")
DRAWABLE = Path("app/src/main/res/drawable")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# The V2 frames are Google Play non-consumables rendered by ProfileFramesV2StoreRow.
# They must never also enter PremiumStoreScreen's generic Son Coin purchase list.
store_text = STORE.read_text(encoding="utf-8")
filter_line = "                .filterNot { it.id in ProfileFrameV2Catalog.paidIds }\n"
anchor = "            products = b.getShopItems()\n"

if filter_line not in store_text:
    if store_text.count(anchor) != 1:
        raise SystemExit(f"PremiumStoreScreen: expected one getShopItems anchor, found {store_text.count(anchor)}")
    store_text = store_text.replace(anchor, anchor + filter_line, 1)
    STORE.write_text(store_text, encoding="utf-8")
    print("Added V2 Google Play frame exclusion to generic store catalog")
else:
    print("V2 Google Play frame exclusion already present")

# Store cards must preview the same photo aperture used by the actual runtime frame.
frames_text = PROFILE_FRAMES.read_text(encoding="utf-8")
preview_marker = "val previewVisual = remember(spec.productId) { ProfileFrameV2Catalog.visual(spec.productId, false) }"
if preview_marker not in frames_text:
    frames_text = replace_once(
        frames_text,
        """    val subtitleEn: String,\n    @DrawableRes val drawable: Int,\n    val accent: Color,\n)""",
        """    val subtitleEn: String,\n    val accent: Color,\n)""",
        "ProfileFrameStoreSpec drawable removal",
    )
    for drawable_line in [
        "        R.drawable.profile_frame_shop_pink_blossom,\n",
        "        R.drawable.profile_frame_shop_blue_royal,\n",
        "        R.drawable.profile_frame_shop_amethyst,\n",
        "        R.drawable.profile_frame_shop_emerald,\n",
    ]:
        count = frames_text.count(drawable_line)
        if count != 1:
            raise SystemExit(f"Store frame drawable argument: expected one {drawable_line.strip()}, found {count}")
        frames_text = frames_text.replace(drawable_line, "", 1)
    frames_text = replace_once(
        frames_text,
        """                val product = products[spec.productId]\n                val realPrice = product?.oneTimePurchaseOfferDetails?.formattedPrice\n                Surface(""",
        """                val product = products[spec.productId]\n                val realPrice = product?.oneTimePurchaseOfferDetails?.formattedPrice\n                val previewVisual = remember(spec.productId) { ProfileFrameV2Catalog.visual(spec.productId, false) }\n                Surface(""",
        "Store preview visual lookup",
    )
    frames_text = replace_once(
        frames_text,
        """                            Image(\n                                painter = painterResource(spec.drawable),\n                                contentDescription = null,\n                                modifier = Modifier.size(104.dp),\n                                contentScale = ContentScale.Fit,\n                            )""",
        """                            Surface(\n                                modifier = Modifier.size(104.dp * previewVisual.photoRatio),\n                                shape = CircleShape,\n                                color = spec.accent.copy(alpha = .14f),\n                            ) {\n                                Box(contentAlignment = Alignment.Center) {\n                                    Text(\n                                        \"A\",\n                                        color = spec.accent,\n                                        fontSize = 16.sp,\n                                        fontWeight = FontWeight.Black,\n                                    )\n                                }\n                            }\n                            Image(\n                                painter = painterResource(previewVisual.drawable),\n                                contentDescription = null,\n                                modifier = Modifier.size(104.dp),\n                                contentScale = ContentScale.Fit,\n                            )""",
        "Store profile-photo frame preview",
    )
    PROFILE_FRAMES.write_text(frames_text, encoding="utf-8")
    print("Added profile-photo fit preview to V2 store cards")
else:
    print("V2 store photo-fit previews already present")

# Focused release-contract checks.
store_text = STORE.read_text(encoding="utf-8")
frames_text = PROFILE_FRAMES.read_text(encoding="utf-8")
catalog_text = PRODUCT_CATALOG.read_text(encoding="utf-8")

required_store_fragments = [
    "ProfileFramesV2StoreRow(",
    ".filterNot { it.id in ProfileFrameV2Catalog.paidIds }",
]
for fragment in required_store_fragments:
    if fragment not in store_text:
        raise SystemExit(f"PremiumStoreScreen missing: {fragment}")

required_product_ids = [
    "profile_frame_pink_blossom",
    "profile_frame_blue_royal",
    "profile_frame_amethyst",
    "profile_frame_emerald",
]
for product_id in required_product_ids:
    if product_id not in catalog_text:
        raise SystemExit(f"ProductCatalog missing V2 product: {product_id}")

required_frame_fragments = [
    "const val PINK_BLOSSOM = ProductCatalog.PROFILE_FRAME_PINK_BLOSSOM",
    "const val BLUE_ROYAL = ProductCatalog.PROFILE_FRAME_BLUE_ROYAL",
    "const val AMETHYST = ProductCatalog.PROFILE_FRAME_AMETHYST",
    "const val EMERALD = ProductCatalog.PROFILE_FRAME_EMERALD",
    "PlayPurchaseVerification.verify(productId, purchase.purchaseToken)",
    "billing.launchProduct(host, product)",
    "fun ownedPaidFrame(equippedId: String?, ownedIds: Set<String>)",
    "val previewVisual = remember(spec.productId) { ProfileFrameV2Catalog.visual(spec.productId, false) }",
    "Modifier.size(104.dp * previewVisual.photoRatio)",
    "painterResource(previewVisual.drawable)",
    "ProfilePhotoRuntime.load(avatarPath)",
]
for fragment in required_frame_fragments:
    if fragment not in frames_text:
        raise SystemExit(f"ProfileFramesV2 missing: {fragment}")

# V2 must render the raw photo directly. Reusing the historical avatar composable would add
# another sweep-gradient ring plus padding underneath the PNG and produce a double-frame halo.
if "ProfilePhotoAvatarWithGender(" in frames_text:
    raise SystemExit("ProfileFramesV2 must not call ProfilePhotoAvatarWithGender; legacy ring would be rendered under V2 artwork")

required_assets = [
    "profile_frame_default_gray.png",
    "profile_frame_pro_gold.png",
    "profile_frame_shop_pink_blossom.png",
    "profile_frame_shop_blue_royal.png",
    "profile_frame_shop_amethyst.png",
    "profile_frame_shop_emerald.png",
]
missing_assets = [name for name in required_assets if not (DRAWABLE / name).is_file()]
if missing_assets:
    raise SystemExit(f"Missing Profile Frames V2 assets: {', '.join(missing_assets)}")

call_sites = []
for kotlin in Path("app/src/main/java").rglob("*.kt"):
    for line_no, line in enumerate(kotlin.read_text(encoding="utf-8").splitlines(), start=1):
        if "FramedProfilePhotoAvatar(" in line and kotlin.name != "FramedProfileAvatar.kt":
            call_sites.append(f"{kotlin}:{line_no}:{line.strip()}")
print(f"FramedProfilePhotoAvatar runtime call sites: {len(call_sites)}")
for call_site in call_sites:
    print(f"  {call_site}")

print("Profile Frames V2 post-integration hardening checks passed")