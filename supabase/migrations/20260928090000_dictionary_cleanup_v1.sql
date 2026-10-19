-- Dictionary cleanup: words that a word game must not accept are switched off (kept for audit).
-- Turkish: junk entries and plainly English spellings that TDK only lists as foreign words.
update public.dictionary_words
set game_allowed = false, bot_eligible = false, reject_reason = 'tr_non_turkish_or_junk'
where language = 'tr' and normalized_word = any (array[
  'dzfgdf','cc',
  'antidumping','background','benchmarking','casting','catering','clearing','dancing','deadline','dealer','dealing',
  'dribbling','dumping','factoring','flashback','franchising','hacker','handout','hedging','internship','jersey',
  'jogging','leasing','lifting','marketing','mesh','mobbing','mouse','nickname','notebook','peeling','playback',
  'rafting','rating','roaming','slash','sticker','stretching','teenage','teenager','timing','trekking','zapping'
]);

-- English: abbreviations, Roman numerals and slurs.
update public.dictionary_words
set game_allowed = false, bot_eligible = false, reject_reason = 'en_abbreviation_numeral_or_slur'
where language = 'en' and (
  -- No vowel at all, apart from real interjections.
  (normalized_word !~ '[aeiouy]' and normalized_word <> all (array['brr','hmm','shh','psst','pst','nth','tsk','hm','mm','sh','zzz','pfft','grr','crwth','cwm']))
  -- Two-letter words: only the standard word-game list.
  or (length(normalized_word) = 2 and normalized_word <> all (array[
    'aa','ab','ad','ae','ag','ah','ai','al','am','an','ar','as','at','aw','ax','ay','ba','be','bi','bo','by','da','de','do',
    'ed','ef','eh','el','em','en','er','es','ex','fa','fe','go','ha','he','hi','hm','ho','id','if','in','is','it','jo','ka',
    'ki','la','li','lo','ma','me','mi','mm','mo','mu','my','na','ne','no','nu','od','oe','of','oh','oi','ok','om','on','op',
    'or','os','ow','ox','oy','pa','pe','pi','po','qi','re','sh','si','so','ta','ti','to','uh','um','un','up','us','ut','we',
    'wo','xi','xu','ya','ye','yo','za']))
  or normalized_word = any (array[
    -- Roman numerals
    'ii','iii','iv','vi','vii','viii','ix','xii','xiii','xiv','xv','xvi','xvii','xviii','xix','xx','xxi','xxii','xxiii',
    'xxiv','xxv','xxx','xl','lii','liv','lvi','lvii','lxi','lxii','lix','lxx','xci','xcii','xcv','cii','civ','cvi','mcm',
    -- Abbreviations
    'adj','adv','aka','alt','amt','ans','arr','aux','avg','cir','cit','dds','deg','dpi','doz','enc','esp','est','etc',
    'exp','ext','fol','fut','fwy','gov','hwy','inc','ind','inf','int','isl','liq','lix','obj','obs','opp',
    'org','qty','rel','rev','riv','rte','sci','sec','seq','sqq','std','syn','tbs','tsp','twp','uhf','ult','usu','val',
    'var','vhf','viz','vol','xor','yrs','dox','ecu','emf',
    -- Slurs
    'fag','fags','faggot','faggots','wog','wogs','wop','wops','yid','yids','gyp','gyps','kike','kikes','spic','spics',
    'chink','chinks','nigger','niggers','dyke','dykes','tranny','retard','retards'
  ])
);

-- Legacy validators only check `active`, so rejected words are switched off there too.
update public.dictionary_words set active = false
where reject_reason in ('tr_non_turkish_or_junk', 'en_abbreviation_numeral_or_slur');
