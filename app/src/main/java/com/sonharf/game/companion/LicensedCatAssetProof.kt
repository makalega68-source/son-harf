package com.sonharf.game.companion

/** Audit-only provenance for the purchased companion visual used by the 2D runtime. */
internal object LicensedCatAssetProof {
    const val RUNTIME_ASSET = "drawable-nodpi/licensed_cat_companion_atlas.png"
    const val SHA256 = "0af65dd6a1bd537be61a617516487b275fc2b3442a18c86ece998ff086b7363a"
    val OWNER_SUPPLIED_SOURCES = listOf(
        "magecat.unitypackage",
        "mage_cat.fbx",
        "mage_cat_textures.zip",
    )

    const val NOTE =
        "2D expression atlas derived from the owner-supplied purchased Mage Cat package; " +
            "no Unity Editor/Readme/native executable content is shipped by this Android runtime."
}
