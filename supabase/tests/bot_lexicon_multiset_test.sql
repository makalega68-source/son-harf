-- Verify the multiset check refuses to lend a letter the rack does not carry
-- and accepts words that fit even when extra letters are unused.
-- All rows must return true.

select public.word_fits_letter_multiset_v1('kaka','ak')   = false as kaka_ak_rejected;
select public.word_fits_letter_multiset_v1('kaka','akak') = true  as kaka_akak_ok;
select public.word_fits_letter_multiset_v1('ağaç','çağax') = true as agac_extra_ok;
select public.word_fits_letter_multiset_v1('şişe','şie')  = false as sise_missing_second_s;
