package com.sonharf.game

// ============================================================================
// BURASI DOLDURULACAK TEK YER. BAŞKA HİÇBİR DOSYAYA DOKUNMA.
// ============================================================================
object Config {
    const val SUPABASE_URL = "https://XXXXXXXXXXXX.supabase.co"
    const val SUPABASE_ANON_KEY = "YOUR_ANON_KEY_HERE"

    // AdMob — test kimlikleriyle bırakılırsa uygulama yine sorunsuz çalışır.
    const val ADMOB_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val ADMOB_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Play Billing — PRO abonelik ürün kimliği (Play Console'da aynı olmalı)
    const val PRO_SUB_ID = "sonharf_pro_monthly"

    const val TURN_SECONDS = 20
    const val MATCH_TURNS = 12
}
