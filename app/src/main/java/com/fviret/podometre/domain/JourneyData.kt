package com.fviret.podometre.domain

import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyCategory
import com.fviret.podometre.domain.model.Milestone
import java.util.UUID

/**
 * Catalogue des 19 trajets virtuels de l'application.
 * Équivalent iOS : JourneyData.swift
 *
 * Chaque trajet dispose de jalons kilométriques répartis sur sa distance totale.
 * Les UUIDs sont fixes (déterministes) pour garantir la stabilité des progressions persistées.
 */
object JourneyData {

    val all: List<Journey> by lazy {
        listOf(
            // ── PROMENADES ──────────────────────────────────────────────────
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                name = "Tour des Jardins de Paris",
                subtitle = "De Montmartre aux Tuileries",
                totalKm = 12.0,
                category = JourneyCategory.WALK,
                emoji = "🌸",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000001"), 3.0, "Jardin du Palais-Royal", "Tu découvres les arcades du Palais-Royal, un havre de paix au cœur de Paris."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000002"), 6.0, "Jardin des Tuileries", "Bienvenue dans le plus ancien jardin public de Paris, créé pour Catherine de Médicis."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000003"), 9.0, "Parc des Buttes-Chaumont", "Ce parc vallonné avec son lac et son temple de la Sibylle offre une vue imprenable."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000004"), 12.0, "Jardin du Luxembourg", "Tu arrives au jardin favori des Parisiens, avec ses fontaines et ses chaises vertes."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                name = "Bords de Seine",
                subtitle = "De Notre-Dame à la Tour Eiffel",
                totalKm = 8.0,
                category = JourneyCategory.WALK,
                emoji = "🌊",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000011"), 2.0, "Île de la Cité", "Le cœur historique de Paris, berceau de la ville il y a plus de 2 000 ans."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000012"), 4.0, "Musée d'Orsay", "Tu longes l'ancienne gare reconvertie en temple de l'impressionnisme."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000013"), 6.0, "Pont Alexandre III", "Le pont le plus orné de Paris, avec ses lampadaires dorés et ses angelots."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000014"), 8.0, "Tour Eiffel", "Tu arrives au pied de la Dame de Fer, construite pour l'Exposition universelle de 1889."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                name = "Promenade des Anglais",
                subtitle = "Nice, le long de la Méditerranée",
                totalKm = 7.0,
                category = JourneyCategory.WALK,
                emoji = "🏖️",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000021"), 2.0, "Plage Beau Rivage", "Les galets niçois et l'eau turquoise de la Méditerranée s'étendent devant toi."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000022"), 5.0, "Hôtel Negresco", "Tu passes devant ce palace mythique avec son dôme rose, symbole de la Côte d'Azur."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000023"), 7.0, "Vieux-Nice", "Tu plonges dans les ruelles colorées et le marché du Cours Saleya."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000004"),
                name = "Chemin des Lavandes",
                subtitle = "Plateau de Valensole, Provence",
                totalKm = 15.0,
                category = JourneyCategory.WALK,
                emoji = "💜",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000031"), 4.0, "Premiers champs de lavande", "Un tapis violet à perte de vue, le parfum envahit l'air chaud de Provence."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000032"), 9.0, "Village de Valensole", "Ce village perché domine la plaine, avec son église romane et ses ruelles ombragées."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000033"), 15.0, "Coopérative de la lavande", "Tu arrives à la distillerie où l'huile essentielle est extraite selon des méthodes ancestrales."),
                )
            ),

            // ── SENTIERS ────────────────────────────────────────────────────
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000005"),
                name = "Tour du Mont-Blanc",
                subtitle = "Entre France, Italie et Suisse",
                totalKm = 170.0,
                category = JourneyCategory.TRAIL,
                emoji = "🏔️",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000041"), 25.0, "Les Houches", "Tu quitters Chamonix, le Mont-Blanc (4 808 m) domine l'horizon."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000042"), 60.0, "Courmayeur", "Tu passes en Italie ! La Vallée d'Aoste t'accueille avec sa cuisine et ses vins."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000043"), 110.0, "Champex-Lac", "Côté suisse, ce lac de montagne offre un reflet parfait des sommets environnants."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000044"), 170.0, "Chamonix", "Tu boucles la boucle ! 170 km, 3 pays, un chef-d'œuvre alpin accompli."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000006"),
                name = "GR20 — Corse",
                subtitle = "La traversée mythique de l'île",
                totalKm = 180.0,
                category = JourneyCategory.TRAIL,
                emoji = "🌿",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000051"), 30.0, "Refuge de Ciottulu", "Les premières crêtes de Haute-Corse, avec vue sur les pozzines, prairies d'altitude."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000052"), 90.0, "Vizzavona", "Passage en Corse du Sud, la forêt de pins laricio offre une fraîcheur bienvenue."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000053"), 140.0, "Monte Incudine", "Le plus haut sommet du sud, à 2 134 m, panorama à 360° sur toute l'île."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000054"), 180.0, "Conca", "Tu termines le GR20, l'un des sentiers les plus difficiles d'Europe. Bravo !"),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000007"),
                name = "Sentier des Douaniers",
                subtitle = "Bretagne, entre falaises et mer",
                totalKm = 45.0,
                category = JourneyCategory.TRAIL,
                emoji = "🌊",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000061"), 10.0, "Cap Fréhel", "Ces falaises de grès rose dominant la mer à 70 m sont un spectacle saisissant."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000062"), 25.0, "Fort La Latte", "Ce château médiéval suspendu au-dessus de la mer semble sorti d'un conte."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000063"), 45.0, "Saint-Malo", "Tu arrives dans la cité corsaire, ses remparts te surplombent depuis la mer."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000008"),
                name = "Gorges du Verdon",
                subtitle = "Le Grand Canyon européen",
                totalKm = 25.0,
                category = JourneyCategory.TRAIL,
                emoji = "🏞️",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000071"), 6.0, "Belvédère de Mayreste", "Première vue plongeante sur le canyon aux eaux émeraude."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000072"), 14.0, "Sentier Martel", "Tu plonges dans les entrailles des gorges sur ce sentier mythique."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000073"), 25.0, "Lac de Sainte-Croix", "Le lac artificiel aux eaux turquoise clôture cette aventure provençale."),
                )
            ),

            // ── HISTOIRE ────────────────────────────────────────────────────
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000009"),
                name = "La Route des Cathédrales",
                subtitle = "Chartres à Reims par les flèches gothiques",
                totalKm = 280.0,
                category = JourneyCategory.HISTORY,
                emoji = "⛪",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000081"), 50.0, "Cathédrale de Chartres", "Chef-d'œuvre du gothique, ses vitraux du XIIe siècle sont parmi les plus beaux du monde."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000082"), 130.0, "Cathédrale de Sens", "La première cathédrale gothique construite en France, modèle pour Canterbury et Notre-Dame."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000083"), 210.0, "Cathédrale d'Amiens", "La plus grande cathédrale de France par son volume, sublime labyrinthe de pierre."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000084"), 280.0, "Cathédrale de Reims", "Lieu du sacre des rois de France, ses anges souriants te regardent franchir le portail."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000010"),
                name = "Les Châteaux de la Loire",
                subtitle = "De Chambord à Chenonceau",
                totalKm = 120.0,
                category = JourneyCategory.HISTORY,
                emoji = "🏰",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000091"), 20.0, "Château de Chambord", "Le chef-d'œuvre de François Ier, avec son escalier à double révolution attribué à Léonard de Vinci."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000092"), 55.0, "Château de Blois", "Quatre siècles d'architecture royale réunis dans un même château, un livre d'histoire de pierre."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000093"), 90.0, "Château d'Amboise", "Ici repose Léonard de Vinci, décédé en 1519 dans le manoir du Clos Lucé tout proche."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000094"), 120.0, "Château de Chenonceau", "Le château des dames, qui enjambe le Cher sur 60 mètres. Un rêve de pierre sur l'eau."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Sur les Pas de Napoléon",
                subtitle = "De l'Élba aux Cent-Jours",
                totalKm = 320.0,
                category = JourneyCategory.HISTORY,
                emoji = "⚔️",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000101"), 60.0, "Golfe Juan", "C'est ici que Napoléon débarqua le 1er mars 1815 avec 700 hommes pour reconquérir la France."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000102"), 140.0, "Grenoble", "La ville se rallie à l'Empereur sans coup de feu. Les soldats envoyés l'arrêter passent dans son camp."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000103"), 220.0, "Lyon", "Napoléon entre en triomphateur dans la capitale des Gaules. Paris n'est plus très loin."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000104"), 320.0, "Paris — Tuileries", "Le 20 mars 1815, Napoléon reprend son trône aux Tuileries. Les Cent-Jours commencent."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000012"),
                name = "La Route des Vins d'Alsace",
                subtitle = "De Strasbourg à Colmar",
                totalKm = 70.0,
                category = JourneyCategory.HISTORY,
                emoji = "🍷",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000111"), 15.0, "Obernai", "Village emblématique avec son puits à six seaux et ses maisons à colombages du XVIIe siècle."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000112"), 35.0, "Ribeauvillé", "Cité médiévale dominée par trois châteaux en ruines et réputée pour son Riesling."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000113"), 55.0, "Riquewihr", "Le village le plus photographié d'Alsace, inchangé depuis le XVIe siècle."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000114"), 70.0, "Colmar", "La Petite Venise alsacienne, avec ses canaux et son marché couvert. Un voyage dans le temps."),
                )
            ),

            // ── MYTHES & ÉPOPÉES ────────────────────────────────────────────
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000013"),
                name = "Le Chemin de Saint-Jacques",
                subtitle = "Via Turonensis, Tours à Roncevaux",
                totalKm = 750.0,
                category = JourneyCategory.MYTH,
                emoji = "🐚",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000121"), 100.0, "Poitiers", "La basilique Saint-Hilaire-le-Grand marque la première grande étape de ce pèlerinage millénaire."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000122"), 280.0, "Bordeaux", "La cathédrale Saint-André, où se marièrent Louis VII et Aliénor d'Aquitaine en 1137."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000123"), 480.0, "Dax", "Les thermes romains et la cathédrale gothique marquent l'entrée dans le Pays basque."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000124"), 650.0, "Saint-Jean-Pied-de-Port", "Le dernier village français avant les Pyrénées. Demain, l'Espagne."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000125"), 750.0, "Col de Roncevaux", "Tu franchis les Pyrénées sur les traces de Roland, neveu de Charlemagne. L'Espagne s'ouvre à toi."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000014"),
                name = "L'Odyssée d'Ulysse",
                subtitle = "De Troie à Ithaque",
                totalKm = 3000.0,
                category = JourneyCategory.MYTH,
                emoji = "⚓",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000131"), 400.0, "Île des Cyclopes", "Tu échappes à Polyphème grâce à la ruse d'Ulysse : personne — c'est mon nom !"),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000132"), 900.0, "Île de Circé", "La magicienne transforme les compagnons en porcs. Ulysse résiste grâce à l'herbe moly."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000133"), 1600.0, "Charybde et Scylla", "Tu navigues entre le gouffre et le monstre à six têtes. La mer Ionienne tremble."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000134"), 2200.0, "Île de Calypso", "La nymphe retient Ulysse 7 ans. Même l'immortalité ne peut effacer le mal du pays."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000135"), 3000.0, "Ithaque", "Ulysse retrouve Pénélope après 20 ans d'absence. L'Odyssée s'achève, la légende commence."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000015"),
                name = "La Quête du Graal",
                subtitle = "Sur les traces des Chevaliers de la Table Ronde",
                totalKm = 500.0,
                category = JourneyCategory.MYTH,
                emoji = "🏆",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000141"), 80.0, "Camelot", "Tu quittes la cour du Roi Arthur, les chevaliers se dispersent aux quatre vents."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000142"), 200.0, "Forêt de Brocéliande", "La forêt enchantée où Merlin fut emprisonné par la fée Viviane dans un arbre ou un rocher."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000143"), 350.0, "Château de Corbenic", "Le château du Graal, gardé par le Roi Pêcheur, où Perceval pose enfin la bonne question."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000144"), 500.0, "Sarras", "La cité mystique où Galaad, le chevalier pur, contemple le Graal et monte au ciel."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000016"),
                name = "Les Travaux d'Hercule",
                subtitle = "De Némée à l'Olympe",
                totalKm = 1200.0,
                category = JourneyCategory.MYTH,
                emoji = "🦁",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000151"), 150.0, "Lion de Némée", "Tu terrassess la bête invulnérable à mains nues. Sa peau devient ton armure."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000152"), 380.0, "Hydre de Lerne", "Neuf têtes repoussent à chaque coup. Iolas brûle les cous : une victoire par l'intelligence."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000153"), 650.0, "Écuries d'Augias", "Tu détournes deux fleuves pour nettoyer 30 ans de fumier en un seul jour. Génie !"),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000154"), 950.0, "Pommes des Hespérides", "Au bout du monde, tu soutiens le ciel pendant qu'Atlas cueille les pommes d'or."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000155"), 1200.0, "Cerbère", "Tu ramènes le chien à trois têtes des Enfers. Le dernier travail, le plus redoutable."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000017"),
                name = "Le Tour du Monde en 80 Jours",
                subtitle = "Sur les traces de Phileas Fogg",
                totalKm = 40000.0,
                category = JourneyCategory.MYTH,
                emoji = "🌍",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000161"), 5000.0, "Suez", "Tu traverses le canal et entres en Asie. Passepartout réalise qu'il part vraiment."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000162"), 12000.0, "Bombay", "L'Inde ! Fogg sauve Aouda d'un sati. Une compagne pour le reste du voyage."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000163"), 22000.0, "Yokohama", "Le Japon en transit. Un cirque, un acrobate, et la traversée du Pacifique en vue."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000164"), 32000.0, "New York", "L'Atlantique à traverser en urgence. Fogg affrète un paquebot et brûle le navire pour accélérer."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000165"), 40000.0, "Londres — Reform Club", "Fogg arrive avec 5 secondes d'avance. Il a gagné le pari, et bien plus encore."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000018"),
                name = "La Route de la Soie",
                subtitle = "De Venise à Pékin, sur les traces de Marco Polo",
                totalKm = 8000.0,
                category = JourneyCategory.MYTH,
                emoji = "🏺",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000171"), 1000.0, "Constantinople", "La ville-carrefour entre Orient et Occident, porte d'entrée vers l'Asie."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000172"), 3000.0, "Samarcande", "La cité légendaire de Tamerlan, ses mosquées à coupoles bleues brillent sous le soleil d'Asie centrale."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000173"), 5500.0, "Dunhuang", "Les grottes de Mogao et leurs 492 temples bouddhistes creusés dans la falaise depuis le IVe siècle."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000174"), 8000.0, "Pékin — Cour de Kublai Khan", "Marco Polo arrive enfin à la cour du Grand Khan après 4 ans de voyage. La légende est accomplie."),
                )
            ),
            Journey(
                id = UUID.fromString("00000000-0000-0000-0000-000000000019"),
                name = "L'Expédition Shackleton",
                subtitle = "À l'assaut de l'Antarctique",
                totalKm = 1300.0,
                category = JourneyCategory.MYTH,
                emoji = "🧊",
                milestones = listOf(
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000181"), 200.0, "Mer de Weddell", "L'Endurance est prise dans les glaces. Shackleton décide : sauver chaque homme."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000182"), 500.0, "Île Éléphant", "Après 5 mois sur la banquise, les 28 hommes posent pied sur la première terre ferme."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000183"), 900.0, "Géorgie du Sud", "Shackleton traverse 1 300 km d'océan antarctique dans un canot de 7 mètres. L'exploit absolu."),
                    Milestone(UUID.fromString("10000000-0000-0000-0000-000000000184"), 1300.0, "Station baleinière de Stromness", "Shackleton arrive épuisé mais victorieux. Il retournera chercher ses hommes. Aucun ne mourra."),
                )
            ),
        )
    }

    /** Retourne un trajet par son UUID, ou null s'il n'existe pas. */
    fun findById(id: String): Journey? =
        all.firstOrNull { it.id.toString() == id }
}
