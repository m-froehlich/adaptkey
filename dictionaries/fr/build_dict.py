# Builds dict.tsv for the French language pack from a real, open-licensed frequency corpus
# (hermitdave/FrequencyWords, MIT licence, OpenSubtitles-2018-derived: fr_50k_raw.txt, downloaded
# verbatim and kept alongside this script for reproducibility) plus rule-based POS tagging, mirroring
# this project's own "rule-based generation + curated exception tables" method (see the Language
# Contribution Guide, step 8's pipeline description) rather than a per-word LLM pass, which does not
# scale to a word list this size. See AdaptKey-History.md for the D-number this round is filed under.

import re
from pathlib import Path

BASE = Path(__file__).parent
RAW = BASE / "fr_50k_raw.txt"
OUT = BASE / "dict.tsv"

TOP_N = 12000
# Rank-1 raw OpenSubtitles count ("de", 8,435,682) rescaled to ~1,000,000, matching the order of
# magnitude of German's own bundled dict.tsv max (1,004,234) - see AdaptKey-Language-Contribution-
# Guide.md step 1's "frequency-scale calibration" note. A plain linear rescale preserves the source
# corpus's own real Zipfian shape rather than inventing a new one.
SCALE = 1_000_000 / 8_435_682

FRENCH_LETTERS = set("abcdefghijklmnopqrstuvwxyzàâäéèêëîïôöùûüÿçœæ")
SINGLE_LETTER_WHITELIST = {"a", "à", "y", "ô"}
# Subtitle-corpus artefact: spoken-French verb-pronoun inversion questions ("pouvez-vous",
# "voulez-vous", "sommes-nous") get glued by the source tokeniser into one hyphenated "word" that is
# not actually a single French dictionary word - reject any hyphen-joined token whose last part is one
# of these.
PRONOUN_SUFFIXES = {
    "vous", "tu", "nous", "il", "elle", "ils", "elles", "on", "je", "moi", "toi", "y", "en", "ce", "il-y", "y-a"
}

APOSTROPHE_RE = re.compile(r"['’]")


def is_clean_word(w: str) -> bool:
    if not w or APOSTROPHE_RE.search(w):
        return False
    if any(ch.isdigit() for ch in w):
        return False
    parts = w.split("-")
    if len(parts) > 1 and parts[-1] in PRONOUN_SUFFIXES:
        return False
    for ch in w:
        if ch not in FRENCH_LETTERS and ch != "-":
            return False
    if len(w) == 1:
        return w in SINGLE_LETTER_WHITELIST
    return True


# --- POS tagging -------------------------------------------------------------------------------
# PartOfSpeech only has NOUN/VERB/ADJECTIVE/PREPOSITION/PROPER_NOUN/OTHER (no pronoun/article/
# conjunction/adverb of its own) - a closed-class function word that is none of the above is tagged
# OTHER, the same convention German's own dictionary uses for its own function words.

PREPOSITIONS = {
    "à", "de", "dans", "en", "sur", "sous", "avec", "sans", "pour", "par", "chez", "vers", "depuis",
    "pendant", "avant", "après", "entre", "contre", "malgré", "selon", "envers", "dès", "hors",
    "outre", "parmi", "sauf", "via", "durant", "moyennant", "concernant", "excepté", "jusque",
    "jusqu'à", "au-dessus", "au-dessous", "auprès", "autour", "devant", "derrière", "dedans",
    "dehors", "près", "loin", "au-delà", "en-dessous"
}

# High-frequency irregular verb forms (être/avoir/aller/faire/dire/pouvoir/vouloir/devoir/savoir/
# voir/venir/falloir/prendre/mettre/tenir/croire/partir/sortir/connaître/sentir/servir/valoir/
# recevoir/rendre/vendre/attendre/entendre/répondre/perdre/comprendre/apprendre/vivre/suivre/écrire/
# lire/boire/naître/mourir/courir/ouvrir/offrir/dormir/sentir), hand-curated - suffix rules cannot
# reach these since they are the irregular forms specifically.
IRREGULAR_VERB_FORMS = """
suis es est sommes êtes sont étais était étions étiez étaient fus fut fûmes fûtes furent serai seras
sera serons serez seront serais serait serions seriez seraient sois soit soyons soyez soient fusse
fusses fût fussions fussiez fussent été étant
ai as a avons avez ont avais avait avions aviez avaient eus eut eûmes eûtes eurent aurai auras aura
aurons aurez auront aurais aurait aurions auriez auraient aie aies ait ayons ayez aient eusse eusses
eût eussions eussiez eussent eu eue eus eues ayant
vais vas va allons allez vont allais allait allions alliez allaient irai iras ira irons irez iront
irais irait irions iriez iraient aille ailles allions alliez aillent allé allée allés allées allant
fais fait faisons faites font faisais faisait faisions faisiez faisaient fis fit fîmes fîtes firent
ferai feras fera ferons ferez feront ferais ferait ferions feriez feraient fasse fasses fassions
fassiez fassent fait faite faits faites faisant
dis dit disons dites disent disais disait disions disiez disaient dirai diras dira dirons direz
diront dirais dirait dirions diriez diraient dise dises disions disiez disent dit dite dits
peux peut pouvons pouvez peuvent pouvais pouvait pouvions pouviez pouvaient pus put pûmes pûtes
purent pourrai pourras pourra pourrons pourrez pourront pourrais pourrait pourrions pourriez
pourraient puisse puisses puissions puissiez puissent pu pouvant
veux veut voulons voulez veulent voulais voulait voulions vouliez voulaient voulus voulut voudrai
voudras voudra voudrons voudrez voudront voudrais voudrait voudrions voudriez voudraient veuille
veuilles veuillions veuillez veuillent voulu voulue voulant
dois doit devons devez doivent devais devait devions deviez devaient dus dut dûmes dûtes durent
devrai devras devra devrons devrez devront devrais devrait devrions devriez devraient doive doives
devions deviez doivent dû due dus dues devant
sais sait savons savez savent savais savait savions saviez savaient sus sut sûmes sûtes surent
saurai sauras saura saurons saurez sauront saurais saurait saurions sauriez sauraient sache saches
sachions sachiez sachent su sue sus sues sachant
vois voit voyons voyez voient voyais voyait voyions voyiez voyaient vis vit vîmes vîtes virent
verrai verras verra verrons verrez verront verrais verrait verrions verriez verraient voie voies
voyions voyiez voient vu vue vus vues voyant
viens vient venons venez viennent venais venait venions veniez venaient vins vint vînmes vîntes
vinrent viendrai viendras viendra viendrons viendrez viendront viendrais viendrait viendrions
viendriez viendraient vienne viennes venions veniez viennent venu venue venus venues venant
faut fallait fallut faudra faudrait faille fallu
prends prend prenons prenez prennent prenais prenait prenions preniez prenaient pris prit prîmes
prîtes prirent prendrai prendras prendra prendrons prendrez prendront prendrais prendrait
prendrions prendriez prendraient prenne prennes prenions preniez prennent prise prises prenant
mets met mettons mettez mettent mettais mettait mettions mettiez mettaient mis mit mîmes mîtes
mirent mettrai mettras mettra mettrons mettrez mettront mettrais mettrait mettrions mettriez
mettraient mette mettes mettions mettiez mettent mise mises mettant
tiens tient tenons tenez tiennent tenais tenait tenions teniez tenaient tins tint tînmes tîntes
tinrent tiendrai tiendras tiendra tiendrons tiendrez tiendront tiendrais tiendrait tiendrions
tiendriez tiendraient tienne tiennes tenions teniez tiennent tenu tenue tenant
crois croit croyons croyez croient croyais croyait croyions croyiez croyaient crus crut crûmes
crûtes crurent croirai croiras croira croirons croirez croiront croirais croirait croirions
croiriez croiraient croie croies croyions croyiez croient cru crue croyant
pars part partons partez partent partais partait partions partiez partaient partis partit partîmes
partîtes partirent partirai partiras partira partirons partirez partiront partirais partirait
partirions partiriez partiraient parte partes partions partiez partent parti partie partis parties
partant
sors sort sortons sortez sortent sortais sortait sortions sortiez sortaient sortis sortit sortîmes
sortîtes sortirent sortirai sortiras sortira sortirons sortirez sortiront sortirais sortirait
sortirions sortiriez sortiraient sorte sortes sortions sortiez sortent sorti sortie sortant
connais connaît connaissons connaissez connaissent connaissais connaissait connaissions
connaissiez connaissaient connus connut connûmes connûtes connurent connaîtrai connaîtras
connaîtra connaîtrons connaîtrez connaîtront connaîtrais connaîtrait connaîtrions connaîtriez
connaîtraient connaisse connaisses connaissions connaissiez connaissent connu connue connaissant
recois reçois reçoit recevons recevez reçoivent recevais recevait recevions receviez recevaient
reçus reçut reçûmes reçûtes reçurent recevrai recevras recevra recevrons recevrez recevront
recevrais recevrait recevrions recevriez recevraient reçoive reçoives recevions receviez
reçoivent reçu reçue recevant
vis vit vivons vivez vivent vivais vivait vivions viviez vivaient vécus vécut vécûmes vécûtes
vécurent vivrai vivras vivra vivrons vivrez vivront vivrais vivrait vivrions vivriez vivraient
vive vives vivions viviez vivent vécu vécue vivant
suis suit suivons suivez suivent suivais suivait suivions suiviez suivaient suivis suivit
suivîmes suivîtes suivirent suivrai suivras suivra suivrons suivrez suivront suivrais suivrait
suivrions suivriez suivraient suive suives suivions suiviez suivent suivi suivie suivant
ecris écris écrit écrivons écrivez écrivent écrivais écrivait écrivions écriviez écrivaient
écrivis écrivit écrivîmes écrivîtes écrivirent écrirai écriras écrira écrirons écrirez écriront
écrirais écrirait écririons écririez écriraient écrive écrives écrivions écriviez écrivent écrit
écrite écrivant
lis lit lisons lisez lisent lisais lisait lisions lisiez lisaient lus lut lûmes lûtes lurent lirai
liras lira lirons lirez liront lirais lirait lirions liriez liraient lise lises lisions lisiez
lisent lu lue lisant
bois boit buvons buvez boivent buvais buvait buvions buviez buvaient bus but bûmes bûtes burent
boirai boiras boira boirons boirez boiront boirais boirait boirions boiriez boiraient boive
boives buvions buviez boivent bu bue buvant
nais naît naissons naissez naissent naissais naissait naissions naissiez naissaient naquis naquit
naquîmes naquîtes naquirent naîtrai naîtras naîtra naîtrons naîtrez naîtront naîtrais naîtrait
naîtrions naîtriez naîtraient naisse naisses naissions naissiez naissent né née naissant
meurs meurt mourons mourez meurent mourais mourait mourions mouriez mouraient mourus mourut
mourûmes mourûtes moururent mourrai mourras mourra mourrons mourrez mourront mourrais mourrait
mourrions mourriez mourraient meure meures mourions mouriez meurent mort morte mourant
cours court courons courez courent courais courait courions couriez couraient courus courut
courûmes courûtes coururent courrai courras courra courrons courrez courront courrais courrait
courrions courriez courraient coure coures courions couriez courent couru courue courant
ouvre ouvrons ouvrez ouvrent ouvrais ouvrait ouvrions ouvriez ouvraient ouvris ouvrit ouvrîmes
ouvrîtes ouvrirent ouvrirai ouvriras ouvrira ouvrirons ouvrirez ouvriront ouvrirais ouvrirait
ouvririons ouvririez ouvriraient ouvre ouvres ouvrions ouvriez ouvrent ouvert ouverte ouvrant
dors dort dormons dormez dorment dormais dormait dormions dormiez dormaient dormis dormit
dormîmes dormîtes dormirent dormirai dormiras dormira dormirons dormirez dormiront dormirais
dormirait dormirions dormiriez dormiraient dorme dormes dormions dormiez dorment dormi dormant
""".split()
IRREGULAR_VERB_FORMS = set(IRREGULAR_VERB_FORMS)

# Common -ment nouns (deverbal nouns) - split out from the far more numerous -ment ADVERB
# derivation (any adjective + "-ment"), which is left tagged OTHER by default below.
MENT_NOUNS = {
    "gouvernement", "moment", "développement", "changement", "mouvement", "département",
    "appartement", "médicament", "monument", "document", "instrument", "testament", "sentiment",
    "tourment", "élément", "complément", "supplément", "fondement", "jugement", "logement",
    "paiement", "événement", "équipement", "campement", "commandement", "comportement",
    "règlement", "environnement", "établissement", "engagement", "enseignement", "renseignement",
    "traitement", "vêtement", "bâtiment", "armement", "financement", "licenciement",
    "recrutement", "classement", "abonnement", "aménagement", "rassemblement", "gisement"
}

# -re and -oir are deliberately NOT blanket suffix rules (unlike -er/-ir below) - both have far too
# high a noun-collision rate in French (poussière/lumière/rivière/nombre/chambre/... ; miroir/
# tiroir/couloir/...) for a plain suffix check to stay reliable. Real -re/-oir verbs are covered
# explicitly instead: irregular ones via their conjugated forms in IRREGULAR_VERB_FORMS above, plus
# every irregular/regular -re/-oir infinitive itself (not itself a conjugated form) here.
RE_OIR_INFINITIVES = {
    "prendre", "mettre", "connaître", "connaitre", "vivre", "suivre", "écrire", "lire", "boire",
    "naître", "naitre", "avoir", "savoir", "pouvoir", "vouloir", "devoir", "voir", "recevoir",
    "apercevoir", "décevoir", "concevoir", "vendre", "rendre", "attendre", "entendre", "répondre",
    "perdre", "descendre", "dépendre", "prétendre", "tendre", "fondre", "mordre", "tordre",
    "confondre", "correspondre", "défendre", "suspendre", "surprendre", "comprendre", "apprendre",
    "reprendre", "permettre", "promettre", "admettre", "soumettre", "transmettre", "commettre",
    "battre", "combattre", "abattre", "paraître", "paraitre", "disparaître", "disparaitre",
    "reconnaître", "reconnaitre", "plaire", "taire", "faire", "traduire", "conduire", "construire",
    "détruire", "produire", "réduire", "séduire", "instruire", "cuire", "suffire", "rire", "sourire",
    "croire", "croître", "croitre", "coudre", "résoudre", "moudre"
}

VERB_SUFFIXES = ("er", "ir")
# Common, high-frequency words ending in -er/-ir that are NOT infinitives - the false positives the
# plain suffix rule would otherwise introduce.
NON_VERB_SUFFIX_EXCEPTIONS = {
    "hiver", "cancer", "fer", "mer", "hier", "super", "hyper", "cher", "enfer", "univers", "revers",
    "travers", "hangar", "clair", "car", "loisir", "plaisir", "désir", "avenir", "souvenir",
    "repentir", "tir", "cuir", "clavier", "cahier", "escalier", "policier", "quartier", "métier",
    "papier", "dossier", "atelier", "hôtelier", "premier", "dernier", "entier", "léger", "étranger",
    "danger", "berger", "verger", "collier", "laser", "boitier", "boîtier", "révolver", "gangster",
    "leader", "manager", "poster", "scanner", "conteneur", "container", "ordinateur"
}

ADJ_SUFFIXES = (
    "eux", "euse", "if", "ive", "al", "ale", "aux", "el", "elle", "ique", "able", "ible", "ain",
    "aine", "ien", "ienne", "ais", "aise", "ois", "oise"
)
NON_ADJ_SUFFIX_EXCEPTIONS = {
    "si", "vie", "ici", "voici", "voilà", "chemin", "cousin", "voisin", "moins", "bien", "rien",
    "combien", "quotidien", "musique", "pratique", "critique", "boutique", "public", "logique",
    "physique", "politique", "technique", "spécifique", "pacifique", "fois", "parfois", "autrefois",
    "quelquefois", "toutefois", "hôtel", "ciel", "cheval", "journal", "canal", "signal", "animal",
    "hôpital", "métal", "carnaval", "festival", "capital", "capitale", "hotel"
}

NOUN_SUFFIXES = ("tion", "sion", "ité", "isme", "ance", "ence")

# A short list of unmistakable proper nouns that survived the frequency-word source in lower case
# (the app's own capitalisation-rule decision for French, see the D-number entry, means these are
# never auto-capitalised - PROPER_NOUN is still tagged for accuracy, matching the guide's own
# "still tag nouns/proper nouns accurately in step 3" advice).
PROPER_NOUNS = {
    "france", "paris", "dieu", "jésus", "noël", "amérique", "europe", "londres", "chine", "afrique",
    "canada", "belgique", "suisse", "espagne", "italie", "allemagne", "russie", "japon", "marie",
    "michel", "sarah", "david", "daniel", "thomas", "nicolas"
    # "pierre" (stone) and "jean" (denim) deliberately excluded despite also being common first
    # names - each collides with a genuinely common French noun, and PROPER_NOUN always forces
    # capitalisation ahead of every other rule (isProper is checked before isPureNoun), so tagging
    # either here would wrongly capitalise its far more frequent common-noun sense every time.
}


ENUM_ORDER = ["NOUN", "VERB", "ADJECTIVE", "PREPOSITION", "PROPER_NOUN", "OTHER"]


def pos_for(word: str) -> str:
    tags = set()
    is_proper = word in PROPER_NOUNS
    if is_proper:
        tags.add("PROPER_NOUN")
    if word in PREPOSITIONS:
        tags.add("PREPOSITION")
    if word in IRREGULAR_VERB_FORMS or word in RE_OIR_INFINITIVES:
        tags.add("VERB")
    elif not is_proper and word.endswith(VERB_SUFFIXES) and word not in NON_VERB_SUFFIX_EXCEPTIONS and len(word) > 3:
        tags.add("VERB")
    is_common_noun = False
    if not is_proper and word.endswith(NOUN_SUFFIXES) and len(word) > 4:
        is_common_noun = True
    elif not is_proper and word.endswith("ment") and word in MENT_NOUNS:
        is_common_noun = True
    if is_common_noun:
        tags.add("NOUN")
        # D-number (this round): CapitalisationEngine.capitalise()'s §6 rule 3 ("a pure noun is
        # capitalised automatically") is unconditional, not gated by Language - it fires for ANY
        # word whose only tags are NOUN/PROPER_NOUN. French does not capitalise common nouns
        # (unlike German), so a French common noun must never be tagged as a *pure* NOUN - pairing
        # it with OTHER keeps the real NOUN signal for A-05's split-safety gate
        # (PartOfSpeech.contains(NOUN) still true) while making isPureNoun/isAmbiguousNoun read it
        # as ambiguous, the same "no automatic correction" outcome §6 rule 5 already gives every
        # other non-noun-only French word. A genuine PROPER_NOUN is exempt (isProper already forces
        # capitalisation ahead of the pure-noun check, and French does capitalise proper nouns).
        if "PROPER_NOUN" not in tags:
            tags.add("OTHER")
    if not is_proper and word.endswith(ADJ_SUFFIXES) and word not in NON_ADJ_SUFFIX_EXCEPTIONS and len(word) > 3 and "VERB" not in tags:
        tags.add("ADJECTIVE")
    if not tags:
        return "OTHER"
    ordered = [t for t in ENUM_ORDER if t in tags]
    return ",".join(ordered)


def main():
    lines = RAW.read_text(encoding="utf-8").splitlines()
    seen = set()
    entries = []
    for line in lines:
        parts = line.split(" ")
        if len(parts) != 2:
            continue
        word, raw_count = parts[0], parts[1]
        if not is_clean_word(word):
            continue
        if word in seen:
            continue
        seen.add(word)
        try:
            count = int(raw_count)
        except ValueError:
            continue
        scaled = max(1, round(count * SCALE))
        entries.append((word, scaled))
        if len(entries) >= TOP_N:
            break

    with OUT.open("w", encoding="utf-8", newline="\n") as f:
        for word, freq in entries:
            pos = pos_for(word)
            f.write(f"{word}\t{freq}\t{pos}\n")

    print(f"Wrote {len(entries)} entries to {OUT}")
    from collections import Counter
    tag_counts = Counter(pos_for(w) for w, _ in entries)
    print("Tag distribution:", dict(tag_counts))


if __name__ == "__main__":
    main()
