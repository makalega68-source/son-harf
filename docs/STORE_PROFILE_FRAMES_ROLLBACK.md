# Store frame rollback

If a release regression is detected, set the six new `shop_items.active` values to `false` first. Existing ownership rows must not be deleted. The client will keep owned frames in Profile → Collection, while new sales stop immediately. Reverting the Android changes is safe only after the storefront is disabled. PRO Golden is outside this rollback scope.
