from pathlib import Path

STORE = Path("app/src/main/java/com/sonharf/game/PremiumStoreScreen.kt")
PROFILE_FRAMES = Path("app/src/main/java/com/sonharf/game/ProfileFramesV2.kt")
PRODUCT_CATALOG = Path("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt")
DRAWABLE = Path("app/src/main/res/drawable")

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

# Focused release-contract checks. Fail before Gradle if the V2 integration is incomplete.
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
]
for fragment in required_frame_fragments:
    if fragment not in frames_text:
        raise SystemExit(f"ProfileFramesV2 missing: {fragment}")

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

print("Profile Frames V2 post-integration hardening checks passed")
