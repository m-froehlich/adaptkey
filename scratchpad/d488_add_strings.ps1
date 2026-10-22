# D-488: appends the 15 new blacklist-confirmation/provenance strings to every values-*/strings.xml.
# Drafts by Claude (established D-478 rule); generator-checked: the UiLocaleConsistencyTest enforces
# that every locale defines exactly English's string names with matching placeholders.
$ErrorActionPreference = 'Stop'
$root = "D:\workspace-ai\adapt-key\app\src\main\res"

# name => per-language map of XML text (pre-escaped: apostrophes as \')
$L = @{}

$L['values-de'] = @{
suggestion_blacklist_confirm_label='Blacklist: %1$s'; suggestion_keep_label='Behalten'
learned_words_removed_pending='%1$s vergessen. Tippt du es wieder, fragt AdaptKey nach.'
blacklist_entry_info_action='Herkunft anzeigen'; blacklist_info_origin_label='Ursprung'
blacklist_info_added_label='Hinzugefügt'; blacklist_info_copy_action='Kopieren'; blacklist_info_close_action='Schließen'
blacklist_origin_gesture_g04='Aus der Vorschlagsleiste entfernt (bestätigt)'
blacklist_origin_escalation='Wiederkehrendes zuvor vergessenes Wort (bestätigt)'
blacklist_origin_w03_forbid='Gelerntes Wort verboten (bestätigt)'
blacklist_origin_editor_manual='Manuell im Blacklist-Editor hinzugefügt'
blacklist_origin_backup_import='Aus Backup wiederhergestellt'
blacklist_origin_bundled_seed='Mit der App mitgeliefert'
blacklist_origin_unknown='Unbekannt (aus einer früheren Version)' }

$L['values-el'] = @{
suggestion_blacklist_confirm_label='Λίστα αποκλεισμού: %1$s'; suggestion_keep_label='Διατήρηση'
learned_words_removed_pending='%1$s ξεχάστηκε. Αν το πληκτρολογήσεις ξανά, το AdaptKey θα σε ρωτήσει.'
blacklist_entry_info_action='Προβολή προέλευσης'; blacklist_info_origin_label='Προέλευση'
blacklist_info_added_label='Προστέθηκε'; blacklist_info_copy_action='Αντιγραφή'; blacklist_info_close_action='Κλείσιμο'
blacklist_origin_gesture_g04='Αφαιρέθηκε από τη γραμμή προτάσεων (επιβεβαιωμένο)'
blacklist_origin_escalation='Επαναλαμβανόμενη λέξη μετά από Απομάθηση (επιβεβαιωμένο)'
blacklist_origin_w03_forbid='Απαγορευμένη μαθημένη λέξη (επιβεβαιωμένο)'
blacklist_origin_editor_manual='Προστέθηκε χειροκίνητα στον επεξεργαστή λίστας αποκλεισμού'
blacklist_origin_backup_import='Επαναφέρθηκε από αντίγραφο ασφαλείας'
blacklist_origin_bundled_seed='Συμβαδίλειο με την εφαρμογή'
blacklist_origin_unknown='Άγνωστο (από προηγούμενη έκδοση)' }

$L['values-fr'] = @{
suggestion_blacklist_confirm_label='Liste noire : %1$s'; suggestion_keep_label='Garder'
learned_words_removed_pending='%1$s oublié. Si vous le retapez, AdaptKey vous demandera confirmation.'
blacklist_entry_info_action='Afficher l''origine'; blacklist_info_origin_label='Origine'
blacklist_info_added_label='Ajouté'; blacklist_info_copy_action='Copier'; blacklist_info_close_action='Fermer'
blacklist_origin_gesture_g04='Supprimé de la barre de suggestions (confirmé)'
blacklist_origin_escalation='Mot récurrent après Oublier (confirmé)'
blacklist_origin_w03_forbid='Mot appris interdit (confirmé)'
blacklist_origin_editor_manual='Ajouté manuellement dans l''éditeur de liste noire'
blacklist_origin_backup_import='Restauré depuis une sauvegarde'
blacklist_origin_bundled_seed='Fourni avec l''application'
blacklist_origin_unknown='Inconnu (d''une version antérieure)' }

$L['values-es'] = @{
suggestion_blacklist_confirm_label='Lista negra: %1$s'; suggestion_keep_label='Conservar'
learned_words_removed_pending='%1$s olvidada. Si la vuelves a escribir, AdaptKey te preguntará.'
blacklist_entry_info_action='Mostrar origen'; blacklist_info_origin_label='Origen'
blacklist_info_added_label='Añadida'; blacklist_info_copy_action='Copiar'; blacklist_info_close_action='Cerrar'
blacklist_origin_gesture_g04='Eliminada de la barra de sugerencias (confirmado)'
blacklist_origin_escalation='Palabra recurrente tras Olvidar (confirmado)'
blacklist_origin_w03_forbid='Palabra aprendida prohibida (confirmado)'
blacklist_origin_editor_manual='Añadida manualmente en el editor de la lista negra'
blacklist_origin_backup_import='Restaurada desde una copia de seguridad'
blacklist_origin_bundled_seed='Incluida con la aplicación'
blacklist_origin_unknown='Desconocido (de una versión anterior)' }

$L['values-it'] = @{
suggestion_blacklist_confirm_label='Lista nera: %1$s'; suggestion_keep_label='Mantieni'
learned_words_removed_pending='%1$s dimenticata. Se la digiti di nuovo, AdaptKey te lo chiederà.'
blacklist_entry_info_action='Mostra origine'; blacklist_info_origin_label='Origine'
blacklist_info_added_label='Aggiunta'; blacklist_info_copy_action='Copia'; blacklist_info_close_action='Chiudi'
blacklist_origin_gesture_g04='Rimossa dalla barra dei suggerimenti (confermato)'
blacklist_origin_escalation='Parola ricorrente dopo Dimentica (confermato)'
blacklist_origin_w03_forbid='Parola appresa vietata (confermato)'
blacklist_origin_editor_manual='Aggiunta manualmente nell''editor della lista nera'
blacklist_origin_backup_import='Ripristinata da un backup'
blacklist_origin_bundled_seed='Inclusa con l''app'
blacklist_origin_unknown='Sconosciuto (da una versione precedente)' }

$L['values-pt'] = @{
suggestion_blacklist_confirm_label='Lista de bloqueio: %1$s'; suggestion_keep_label='Manter'
learned_words_removed_pending='%1$s esquecida. Se a escrever novamente, o AdaptKey perguntará.'
blacklist_entry_info_action='Mostrar origem'; blacklist_info_origin_label='Origem'
blacklist_info_added_label='Adicionada'; blacklist_info_copy_action='Copiar'; blacklist_info_close_action='Fechar'
blacklist_origin_gesture_g04='Removida da barra de sugestões (confirmado)'
blacklist_origin_escalation='Palavra recorrente após Esquecer (confirmado)'
blacklist_origin_w03_forbid='Palavra aprendida proibida (confirmado)'
blacklist_origin_editor_manual='Adicionada manualmente no editor da lista de bloqueio'
blacklist_origin_backup_import='Restaurada de um backup'
blacklist_origin_bundled_seed='Incluída com a aplicação'
blacklist_origin_unknown='Desconhecido (de uma versão anterior)' }

$L['values-nl'] = @{
suggestion_blacklist_confirm_label='Zwarte lijst: %1$s'; suggestion_keep_label='Behouden'
learned_words_removed_pending='%1$s vergeten. Als je het opnieuw typt, vraagt AdaptKey ernaar.'
blacklist_entry_info_action='Herkomst tonen'; blacklist_info_origin_label='Herkomst'
blacklist_info_added_label='Toegevoegd'; blacklist_info_copy_action='Kopiëren'; blacklist_info_close_action='Sluiten'
blacklist_origin_gesture_g04='Verwijderd uit de suggestiebalk (bevestigd)'
blacklist_origin_escalation='Terugkerend woord na Vergeten (bevestigd)'
blacklist_origin_w03_forbid='Leerwoord verbannen (bevestigd)'
blacklist_origin_editor_manual='Handmatig toegevoegd in de zwarte-lijst-editor'
blacklist_origin_backup_import='Terugezet vanuit een back-up'
blacklist_origin_bundled_seed='Ingebouwd in de app'
blacklist_origin_unknown='Onbekend (uit een eerdere versie)' }

$L['values-pl'] = @{
suggestion_blacklist_confirm_label='Czarna lista: %1$s'; suggestion_keep_label='Zachowaj'
learned_words_removed_pending='Zapomniano %1$s. Jeśli wpiszesz je ponownie, AdaptKey zapyta.'
blacklist_entry_info_action='Pokaż pochodzenie'; blacklist_info_origin_label='Pochodzenie'
blacklist_info_added_label='Dodano'; blacklist_info_copy_action='Kopiuj'; blacklist_info_close_action='Zamknij'
blacklist_origin_gesture_g04='Usunięto z paska podpowiedzi (potwierdzone)'
blacklist_origin_escalation='Powtarzające się słowo po Zapomnij (potwierdzone)'
blacklist_origin_w03_forbid='Zabroniono wyuczonego słowa (potwierdzone)'
blacklist_origin_editor_manual='Dodano ręcznie w edytorze czarnej listy'
blacklist_origin_backup_import='Przywrócono z kopii zapasowej'
blacklist_origin_bundled_seed='Dołączone do aplikacji'
blacklist_origin_unknown='Nieznane (z wcześniejszej wersji)' }

$L['values-tr'] = @{
suggestion_blacklist_confirm_label='Kara liste: %1$s'; suggestion_keep_label='Koru'
learned_words_removed_pending='%1$s unutuldu. Tekrar yazarsan AdaptKey sana sorar.'
blacklist_entry_info_action='Kaynağı göster'; blacklist_info_origin_label='Kaynak'
blacklist_info_added_label='Eklendi'; blacklist_info_copy_action='Kopyala'; blacklist_info_close_action='Kapat'
blacklist_origin_gesture_g04='Öneri çubuğundan kaldırıldı (onaylandı)'
blacklist_origin_escalation='Unutulduktan sonra tekrarlanan kelime (onaylandı)'
blacklist_origin_w03_forbid='Öğrenilen kelime yasaklandı (onaylandı)'
blacklist_origin_editor_manual='Kara liste düzenleyicisinde elle eklendi'
blacklist_origin_backup_import='Bir yedekten geri yüklendi'
blacklist_origin_bundled_seed='Uygulamayla birlikte gelir'
blacklist_origin_unknown='Bilinmiyor (önceki sürümden)' }

$L['values-sv'] = @{
suggestion_blacklist_confirm_label='Svartlista: %1$s'; suggestion_keep_label='Behåll'
learned_words_removed_pending='%1$s glömt. Om du skriver det igen frågar AdaptKey.'
blacklist_entry_info_action='Visa ursprung'; blacklist_info_origin_label='Ursprung'
blacklist_info_added_label='Tillagd'; blacklist_info_copy_action='Kopiera'; blacklist_info_close_action='Stäng'
blacklist_origin_gesture_g04='Borttagen från förslagsraden (bekräftat)'
blacklist_origin_escalation='Återkommande ord efter Glöm (bekräftat)'
blacklist_origin_w03_forbid='Inlärt ord förbjudet (bekräftat)'
blacklist_origin_editor_manual='Tillagd manuellt i redigeraren för svartlistan'
blacklist_origin_backup_import='Återställd från en säkerhetskopia'
blacklist_origin_bundled_seed='Ingår i appen'
blacklist_origin_unknown='Okänt (från en tidigare version)' }

$L['values-nb'] = @{
suggestion_blacklist_confirm_label='Svarteliste: %1$s'; suggestion_keep_label='Behold'
learned_words_removed_pending='%1$s glemt. Hvis du skriver det igjen, spør AdaptKey.'
blacklist_entry_info_action='Vis opprinnelse'; blacklist_info_origin_label='Opprinnelse'
blacklist_info_added_label='Lagt til'; blacklist_info_copy_action='Kopier'; blacklist_info_close_action='Lukk'
blacklist_origin_gesture_g04='Fjernet fra forslagsraden (bekreftet)'
blacklist_origin_escalation='Gjentagende ord etter Glemt (bekreftet)'
blacklist_origin_w03_forbid='Innlært ord forbudt (bekreftet)'
blacklist_origin_editor_manual='Lagt til manuelt i svarteliste-redigereren'
blacklist_origin_backup_import='Gjenopprettet fra en sikkerhetskopi'
blacklist_origin_bundled_seed='Leveres med appen'
blacklist_origin_unknown='Ukjent (fra en tidligere versjon)' }

$L['values-da'] = @{
suggestion_blacklist_confirm_label='Sortliste: %1$s'; suggestion_keep_label='Behold'
learned_words_removed_pending='%1$s glemt. Hvis du skriver den igen, spørger AdaptKey.'
blacklist_entry_info_action='Vis oprindelse'; blacklist_info_origin_label='Oprindelse'
blacklist_info_added_label='Tilføjet'; blacklist_info_copy_action='Kopiér'; blacklist_info_close_action='Luk'
blacklist_origin_gesture_g04='Fjernet fra forslagslinjen (bekræftet)'
blacklist_origin_escalation='Tilbagevendende ord efter Glem (bekræftet)'
blacklist_origin_w03_forbid='Indlært ord forbudt (bekræftet)'
blacklist_origin_editor_manual='Tilføjet manuelt i sortliste-editoren'
blacklist_origin_backup_import='Gendannet fra en sikkerhedskopi'
blacklist_origin_bundled_seed='Leveres med appen'
blacklist_origin_unknown='Ukendt (fra en tidligere version)' }

$L['values-fi'] = @{
suggestion_blacklist_confirm_label='Musta lista: %1$s'; suggestion_keep_label='Säilytä'
learned_words_removed_pending='%1$s unohdettu. Jos kirjoitat sen uudelleen, AdaptKey kysyy.'
blacklist_entry_info_action='Näytä alkuperä'; blacklist_info_origin_label='Alkuperä'
blacklist_info_added_label='Lisätty'; blacklist_info_copy_action='Kopioi'; blacklist_info_close_action='Sulje'
blacklist_origin_gesture_g04='Poistettu ehdotusrivistä (vahvistettu)'
blacklist_origin_escalation='Toistuva sana Unohtamisen jälkeen (vahvistettu)'
blacklist_origin_w03_forbid='Opittu sana kielletty (vahvistettu)'
blacklist_origin_editor_manual='Lisätty manuaalisesti mustan listan muokkaimessa'
blacklist_origin_backup_import='Palautettu varmuuskopiosta'
blacklist_origin_bundled_seed='Sisältyy sovellukseen'
blacklist_origin_unknown='Tuntematon (aiemmasta versiosta)' }

$L['values-cs'] = @{
suggestion_blacklist_confirm_label='Černá listina: %1$s'; suggestion_keep_label='Ponechat'
learned_words_removed_pending='%1$s zapomenuto. Pokud jej napíšete znovu, AdaptKey se zeptá.'
blacklist_entry_info_action='Zobrazit původ'; blacklist_info_origin_label='Původ'
blacklist_info_added_label='Přidáno'; blacklist_info_copy_action='Kopírovat'; blacklist_info_close_action='Zavřít'
blacklist_origin_gesture_g04='Odebráno z lišty návrhů (potvrzeno)'
blacklist_origin_escalation='Opakované slovo po Zapomenout (potvrzeno)'
blacklist_origin_w03_forbid='Naučené slovo zakázáno (potvrzeno)'
blacklist_origin_editor_manual='Ručně přidáno v editoru černé listiny'
blacklist_origin_backup_import='Obnoveno ze zálohy'
blacklist_origin_bundled_seed='Součást aplikace'
blacklist_origin_unknown='Neznámý (ze starší verze)' }

$L['values-sk'] = @{
suggestion_blacklist_confirm_label='Čierna listina: %1$s'; suggestion_keep_label='Ponechať'
learned_words_removed_pending='%1$s zabudnuté. Ak ho napíšete znova, AdaptKey sa opýta.'
blacklist_entry_info_action='Zobraziť pôvod'; blacklist_info_origin_label='Pôvod'
blacklist_info_added_label='Pridané'; blacklist_info_copy_action='Kopírovať'; blacklist_info_close_action='Zavrieť'
blacklist_origin_gesture_g04='Odstránené z lišty návrhov (potvrdené)'
blacklist_origin_escalation='Opakované slovo po Zabudnúť (potvrdené)'
blacklist_origin_w03_forbid='Naučené slovo zakázané (potvrdené)'
blacklist_origin_editor_manual='Ručne pridané v editore čiernej listiny'
blacklist_origin_backup_import='Obnovené zo zálohy'
blacklist_origin_bundled_seed='Súčasť aplikácie'
blacklist_origin_unknown='Neznámy (zo staršej verzie)' }

$L['values-hu'] = @{
suggestion_blacklist_confirm_label='Feketelista: %1$s'; suggestion_keep_label='Megtartás'
learned_words_removed_pending='%1$s elfelejtve. Ha újra beírja, az AdaptKey megkérdezi.'
blacklist_entry_info_action='Eredet megjelenítése'; blacklist_info_origin_label='Eredet'
blacklist_info_added_label='Hozzáadva'; blacklist_info_copy_action='Másolás'; blacklist_info_close_action='Bezárás'
blacklist_origin_gesture_g04='Eltávolítva a javaslatcsíkról (megerősítve)'
blacklist_origin_escalation='Ismétlődő szó az Elfelejtés után (megerősítve)'
blacklist_origin_w03_forbid='Megtanult szó tiltva (megerősítve)'
blacklist_origin_editor_manual='Kézzel hozzáadva a feketelista-szerkesztőben'
blacklist_origin_backup_import='Biztonsági mentésből visszaállítva'
blacklist_origin_bundled_seed='Az alkalmazással érkezik'
blacklist_origin_unknown='Ismeretlen (korábbi verzióból)' }

$L['values-ro'] = @{
suggestion_blacklist_confirm_label='Listă neagră: %1$s'; suggestion_keep_label='Păstrează'
learned_words_removed_pending='%1$s uitat. Dacă îl scrieți din nou, AdaptKey vă va întreba.'
blacklist_entry_info_action='Afișează originea'; blacklist_info_origin_label='Origine'
blacklist_info_added_label='Adăugat'; blacklist_info_copy_action='Copiază'; blacklist_info_close_action='Închide'
blacklist_origin_gesture_g04='Eliminat din bara de sugestii (confirmat)'
blacklist_origin_escalation='Cuvânt recurent după Uită (confirmat)'
blacklist_origin_w03_forbid='Cuvânt învățat interzis (confirmat)'
blacklist_origin_editor_manual='Adăugat manual în editorul listei negre'
blacklist_origin_backup_import='Restaurat dintr-o copie de siguranță'
blacklist_origin_bundled_seed='Inclus în aplicație'
blacklist_origin_unknown='Necunoscut (dintr-o versiune anterioară)' }

$L['values-hr'] = @{
suggestion_blacklist_confirm_label='Crna lista: %1$s'; suggestion_keep_label='Zadrži'
learned_words_removed_pending='%1$s zaboravljeno. Ako ga ponovno upišete, AdaptKey će pitati.'
blacklist_entry_info_action='Prikaži podrijetlo'; blacklist_info_origin_label='Podrijetlo'
blacklist_info_added_label='Dodano'; blacklist_info_copy_action='Kopiraj'; blacklist_info_close_action='Zatvori'
blacklist_origin_gesture_g04='Uklonjeno iz trake prijedloga (potvrđeno)'
blacklist_origin_escalation='Ponavljajuća riječ nakon Zaboravi (potvrđeno)'
blacklist_origin_w03_forbid='Naučena riječ zabranjena (potvrđeno)'
blacklist_origin_editor_manual='Ručno dodano u uređivaču crne liste'
blacklist_origin_backup_import='Vraćeno iz sigurnosne kopije'
blacklist_origin_bundled_seed='Isporučuje se s aplikacijom'
blacklist_origin_unknown='Nepoznato (iz starije verzije)' }

$L['values-bs'] = @{
suggestion_blacklist_confirm_label='Crna lista: %1$s'; suggestion_keep_label='Zadrži'
learned_words_removed_pending='%1$s zaboravljeno. Ako ga ponovo ukucate, AdaptKey će pitati.'
blacklist_entry_info_action='Prikaži porijeklo'; blacklist_info_origin_label='Porijeklo'
blacklist_info_added_label='Dodano'; blacklist_info_copy_action='Kopiraj'; blacklist_info_close_action='Zatvori'
blacklist_origin_gesture_g04='Uklonjeno iz trake prijedloga (potvrđeno)'
blacklist_origin_escalation='Ponavljajuća riječ nakon Zaboravi (potvrđeno)'
blacklist_origin_w03_forbid='Naučena riječ zabranjena (potvrđeno)'
blacklist_origin_editor_manual='Ručno dodano u uređivaču crne liste'
blacklist_origin_backup_import='Vraćeno iz sigurnosne kopije'
blacklist_origin_bundled_seed='Isporučuje se s aplikacijom'
blacklist_origin_unknown='Nepoznato (iz starije verzije)' }

$L['values-sr'] = @{
suggestion_blacklist_confirm_label='Црна листа: %1$s'; suggestion_keep_label='Задржи'
learned_words_removed_pending='%1$s заборављено. Ако га поново укуцате, AdaptKey ће питати.'
blacklist_entry_info_action='Прикажи порекло'; blacklist_info_origin_label='Порекло'
blacklist_info_added_label='Додато'; blacklist_info_copy_action='Копирај'; blacklist_info_close_action='Затвори'
blacklist_origin_gesture_g04='Уклоњено из траке предлога (потврђено)'
blacklist_origin_escalation='Понављајућа реч након Заборави (потврђено)'
blacklist_origin_w03_forbid='Научена реч забрањена (потврђено)'
blacklist_origin_editor_manual='Ручно додато у уређивачу црне листе'
blacklist_origin_backup_import='Обновљено из резервне копије'
blacklist_origin_bundled_seed='Испоручује се са апликацијом'
blacklist_origin_unknown='Непознато (из старије верзије)' }

$L['values-et'] = @{
suggestion_blacklist_confirm_label='Must nimekiri: %1$s'; suggestion_keep_label='Säilita'
learned_words_removed_pending='%1$s unustatud. Kui sa selle uuesti kirjutad, küsib AdaptKey.'
blacklist_entry_info_action='Näita päritolu'; blacklist_info_origin_label='Päritolu'
blacklist_info_added_label='Lisatud'; blacklist_info_copy_action='Kopeeri'; blacklist_info_close_action='Sulge'
blacklist_origin_gesture_g04='Eemaldatud soovitusribalt (kinnitatud)'
blacklist_origin_escalation='Korduv sõna pärast Unusta (kinnitatud)'
blacklist_origin_w03_forbid='Õpitud sõna keelatud (kinnitatud)'
blacklist_origin_editor_manual='Lisatud käsitsi musta nimekirja redaktoris'
blacklist_origin_backup_import='Taastatud varukopiost'
blacklist_origin_bundled_seed='Kaasas rakendusega'
blacklist_origin_unknown='Teadmata (varasemast versioonist)' }

$L['values-lv'] = @{
suggestion_blacklist_confirm_label='Melnais saraksts: %1$s'; suggestion_keep_label='Paturēt'
learned_words_removed_pending='%1$s aizmirsts. Ja to ierakstīsiet vēlreiz, AdaptKey pajautās.'
blacklist_entry_info_action='Rādīt izcelsmi'; blacklist_info_origin_label='Izcelsme'
blacklist_info_added_label='Pievienots'; blacklist_info_copy_action='Kopēt'; blacklist_info_close_action='Aizvērt'
blacklist_origin_gesture_g04='Noņemts no ieteikumu joslas (apstiprināts)'
blacklist_origin_escalation='Atkārtojies vārds pēc Aizmirst (apstiprināts)'
blacklist_origin_w03_forbid='Apmācīts vārds aizliegts (apstiprināts)'
blacklist_origin_editor_manual='Manuāli pievienots melnā saraksta redaktorā'
blacklist_origin_backup_import='Atjaunots no dublējuma'
blacklist_origin_bundled_seed='Iekļauts lietotnē'
blacklist_origin_unknown='Nezināms (no iepriekšējās versijas)' }

$L['values-lt'] = @{
suggestion_blacklist_confirm_label='Juodasis sąrašas: %1$s'; suggestion_keep_label='Palikti'
learned_words_removed_pending='%1$s pamiršta. Jei parašysite dar kartą, AdaptKey paklaus.'
blacklist_entry_info_action='Rodyti kilmę'; blacklist_info_origin_label='Kilmė'
blacklist_info_added_label='Pridėta'; blacklist_info_copy_action='Kopijuoti'; blacklist_info_close_action='Uždaryti'
blacklist_origin_gesture_g04='Pašalinta iš siūlymų juostos (patvirtinta)'
blacklist_origin_escalation='Pasikartojantis žodis po Pamiršti (patvirtinta)'
blacklist_origin_w03_forbid='Išmoktas žodis uždraustas (patvirtinta)'
blacklist_origin_editor_manual='Rankiniu būdu pridėta juodojo sąrašo rengyklėje'
blacklist_origin_backup_import='Atkurta iš atsarginės kopijos'
blacklist_origin_bundled_seed='Pateikiama su programa'
blacklist_origin_unknown='Nežinoma (iš ankstesnės versijos)' }

$L['values-id'] = @{
suggestion_blacklist_confirm_label='Daftar hitam: %1$s'; suggestion_keep_label='Simpan'
learned_words_removed_pending='%1$s dilupakan. Jika Anda mengetiknya lagi, AdaptKey akan bertanya.'
blacklist_entry_info_action='Tampilkan asal'; blacklist_info_origin_label='Asal'
blacklist_info_added_label='Ditambahkan'; blacklist_info_copy_action='Salin'; blacklist_info_close_action='Tutup'
blacklist_origin_gesture_g04='Dihapus dari bilah saran (dikonfirmasi)'
blacklist_origin_escalation='Kata berulang setelah Lupakan (dikonfirmasi)'
blacklist_origin_w03_forbid='Kata yang dipelajari dilarang (dikonfirmasi)'
blacklist_origin_editor_manual='Ditambahkan manual di editor daftar hitam'
blacklist_origin_backup_import='Dipulihkan dari cadangan'
blacklist_origin_bundled_seed='Termasuk dalam aplikasi'
blacklist_origin_unknown='Tidak diketahui (dari versi sebelumnya)' }

$L['values-ms'] = @{
suggestion_blacklist_confirm_label='Senarai hitam: %1$s'; suggestion_keep_label='Simpan'
learned_words_removed_pending='%1$s dilupakan. Jika anda menaipnya lagi, AdaptKey akan bertanya.'
blacklist_entry_info_action='Tunjukkan asalan'; blacklist_info_origin_label='Asalan'
blacklist_info_added_label='Ditambah'; blacklist_info_copy_action='Salin'; blacklist_info_close_action='Tutup'
blacklist_origin_gesture_g04='Dibuang dari bar cadangan (disahkan)'
blacklist_origin_escalation='Perkataan berulang selepas Lupakan (disahkan)'
blacklist_origin_w03_forbid='Perkataan dipelajari dilarang (disahkan)'
blacklist_origin_editor_manual='Ditambah secara manual dalam editor senarai hitam'
blacklist_origin_backup_import='Dipulihkan daripada sandaran'
blacklist_origin_bundled_seed='Disertakan bersama apl'
blacklist_origin_unknown='Tidak diketahui (daripada versi terdahulu)' }

$L['values-sw'] = @{
suggestion_blacklist_confirm_label='Orodha nyeusi: %1$s'; suggestion_keep_label='Weka'
learned_words_removed_pending='%1$s imesahaulishwa. Ukiandika tena, AdaptKey itakuuliza.'
blacklist_entry_info_action='Onyesha chanzo'; blacklist_info_origin_label='Chanzo'
blacklist_info_added_label='Imeongezwa'; blacklist_info_copy_action='Nakili'; blacklist_info_close_action='Funga'
blacklist_origin_gesture_g04='Imeondolewa kutoka kwenye utepe wa mapendekezo (imethibitishwa)'
blacklist_origin_escalation='Neno linalojirudia baada ya Sahau (limethibitishwa)'
blacklist_origin_w03_forbid='Neno lililojifunza limezuiwa (limethibitishwa)'
blacklist_origin_editor_manual='Imeongezwa kwa mkono kwenye kihariri cha orodha nyeusi'
blacklist_origin_backup_import='Imerejeshwa kutoka kwenye nakala rudufu'
blacklist_origin_bundled_seed='Inakuja na programu'
blacklist_origin_unknown='Haijulikani (kutoka toleo la awali)' }

$L['values-fil'] = @{
suggestion_blacklist_confirm_label='Blacklist: %1$s'; suggestion_keep_label='Panatilihin'
learned_words_removed_pending='Nakalimutan ang %1$s. Kung i-type muli ito, magtatanong ang AdaptKey.'
blacklist_entry_info_action='Ipakita ang pinagmulan'; blacklist_info_origin_label='Pinagmulan'
blacklist_info_added_label='Naidagdag'; blacklist_info_copy_action='Kopyahin'; blacklist_info_close_action='Isara'
blacklist_origin_gesture_g04='Inalis mula sa suggestion bar (kumpirmado)'
blacklist_origin_escalation='Paulit-ulit na salita pagkatapos ng Kalimutan (kumpirmado)'
blacklist_origin_w03_forbid='Ipinagbawal ang natutunang salita (kumpirmado)'
blacklist_origin_editor_manual='Manual na idinagdag sa blacklist editor'
blacklist_origin_backup_import='Naibalik mula sa backup'
blacklist_origin_bundled_seed='Kasama ng app'
blacklist_origin_unknown='Hindi kilala (mula sa mas lumang bersyon)' }

$L['values-ru'] = @{
suggestion_blacklist_confirm_label='Чёрный список: %1$s'; suggestion_keep_label='Оставить'
learned_words_removed_pending='%1$s забыто. Если вы введёте его снова, AdaptKey спросит.'
blacklist_entry_info_action='Показать источник'; blacklist_info_origin_label='Источник'
blacklist_info_added_label='Добавлено'; blacklist_info_copy_action='Копировать'; blacklist_info_close_action='Закрыть'
blacklist_origin_gesture_g04='Удалено из панели подсказок (подтверждено)'
blacklist_origin_escalation='Повторное слово после «Забыть» (подтверждено)'
blacklist_origin_w03_forbid='Заученное слово запрещено (подтверждено)'
blacklist_origin_editor_manual='Добавлено вручную в редакторе чёрного списка'
blacklist_origin_backup_import='Восстановлено из резервной копии'
blacklist_origin_bundled_seed='Поставляется с приложением'
blacklist_origin_unknown='Неизвестно (из более ранней версии)' }

$L['values-uk'] = @{
suggestion_blacklist_confirm_label='Чорний список: %1$s'; suggestion_keep_label='Залишити'
learned_words_removed_pending='%1$s забуто. Якщо ви введете його знову, AdaptKey запитає.'
blacklist_entry_info_action='Показати походження'; blacklist_info_origin_label='Походження'
blacklist_info_added_label='Додано'; blacklist_info_copy_action='Копіювати'; blacklist_info_close_action='Закрити'
blacklist_origin_gesture_g04='Вилучено з панелі підказок (підтверджено)'
blacklist_origin_escalation='Повторне слово після «Забути» (підтверджено)'
blacklist_origin_w03_forbid='Вивчене слово заборонено (підтверджено)'
blacklist_origin_editor_manual='Додано вручну в редакторі чорного списку'
blacklist_origin_backup_import='Відновлено з резервної копії'
blacklist_origin_bundled_seed='Постачається з програмою'
blacklist_origin_unknown='Невідомо (з попередньої версії)' }

$L['values-az'] = @{
suggestion_blacklist_confirm_label='Qara siyahı: %1$s'; suggestion_keep_label='Saxla'
learned_words_removed_pending='%1$s unuduldu. Yenidən yazsanız, AdaptKey soruşacaq.'
blacklist_entry_info_action='Mənbəyi göstər'; blacklist_info_origin_label='Mənbə'
blacklist_info_added_label='Əlavə edildi'; blacklist_info_copy_action='Kopyala'; blacklist_info_close_action='Bağla'
blacklist_origin_gesture_g04='Təklif panelindən silindi (təsdiqləndi)'
blacklist_origin_escalation='Unutduqdan sonra təkrarlanan söz (təsdiqləndi)'
blacklist_origin_w03_forbid='Öyrənilən söz qadağan edildi (təsdiqləndi)'
blacklist_origin_editor_manual='Qara siyahı redaktorunda əl ilə əlavə edildi'
blacklist_origin_backup_import='Ehtiyat nüsxədən bərpa edildi'
blacklist_origin_bundled_seed='Tətbiqlə birlikdə gəlir'
blacklist_origin_unknown='Naməlum (əvvəlki versiyadan)' }

$L['values-uz'] = @{
suggestion_blacklist_confirm_label='Qora roʻyxat: %1$s'; suggestion_keep_label='Saqlash'
learned_words_removed_pending='%1$s unutilildi. Yana kiritsangiz, AdaptKey soʻraydi.'
blacklist_entry_info_action='Manbani koʻrsatish'; blacklist_info_origin_label='Manba'
blacklist_info_added_label='Qoʻshildi'; blacklist_info_copy_action='Nusxalash'; blacklist_info_close_action='Yopish'
blacklist_origin_gesture_g04='Taklif panelidan olib tashlandi (tasdiqlandi)'
blacklist_origin_escalation='Unutilgandan keyin takrorlangan soʻz (tasdiqlandi)'
blacklist_origin_w03_forbid='Oʻrganilgan soʻz taqiqlandi (tasdiqlandi)'
blacklist_origin_editor_manual='Qora roʻyxat muharririga qoʻlda qoʻshildi'
blacklist_origin_backup_import='Zaxira nusxadan tiklandi'
blacklist_origin_bundled_seed='Ilova bilan birga keladi'
blacklist_origin_unknown='Nomaʼlum (avvalgi versiyadan)' }

$order = @(
'suggestion_blacklist_confirm_label','suggestion_keep_label','learned_words_removed_pending',
'blacklist_entry_info_action','blacklist_info_origin_label','blacklist_info_added_label',
'blacklist_info_copy_action','blacklist_info_close_action','blacklist_origin_gesture_g04',
'blacklist_origin_escalation','blacklist_origin_w03_forbid','blacklist_origin_editor_manual',
'blacklist_origin_backup_import','blacklist_origin_bundled_seed','blacklist_origin_unknown')

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$changed = 0
foreach ($dir in $L.Keys) {
    $path = Join-Path $root "$dir\strings.xml"
    if (-not (Test-Path -LiteralPath $path)) { Write-Output "MISSING: $path"; continue }
    $content = [System.IO.File]::ReadAllText($path)
    if ($content -match 'blacklist_origin_unknown') { Write-Output "SKIP (already present): $dir"; continue }
    if ($content -notmatch '</resources>') { Write-Output "NO CLOSE TAG: $dir"; continue }
    $block = "`n    <!-- D-488: blacklist confirmation chip, its reject zone, and per-entry provenance (drafts). -->`n"
    foreach ($name in $order) {
        $value = $L[$dir][$name]
        if ($null -eq $value) { Write-Output "MISSING KEY: $dir/$name"; return }
        # Android string escaping: a literal apostrophe must be written as \' in the XML.
        $value = $value -replace "'", "\'"
        $block += "    <string name=`"$name`">$value</string>`n"
    }
    $content = $content -replace '</resources>\s*$', ($block + '</resources>')
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
    $changed++
}
Write-Output "Updated $changed locale files."
