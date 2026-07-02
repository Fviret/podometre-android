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
                id = UUID.fromString("a0000000-0000-0000-0000-000000000001"),
                name = "Tour des Tuileries",
                subtitle = "Du Louvre à la Concorde",
                totalKm = 2.5,
                category = JourneyCategory.WALK,
                emoji = "🌿",
                milestones = listOf(
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000001"), 0.8, "Pyramide du Louvre", "L'emblématique pyramide de verre de I.M. Pei marque le début de ta promenade royale."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000002"), 1.5, "Jardin des Tuileries", "Tu entres dans le plus ancien jardin public de Paris, créé au XVIe siècle pour Catherine de Médicis."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000003"), 2.5, "Place de la Concorde", "La plus grande place de Paris, où le Grand Axe relie le Louvre à l'Arc de Triomphe."),
                )
            ),
            Journey(
                id = UUID.fromString("a0000000-0000-0000-0000-000000000002"),
                name = "Berges de la Seine",
                subtitle = "De Notre-Dame à la Tour Eiffel",
                totalKm = 5.0,
                category = JourneyCategory.WALK,
                emoji = "🌊",
                milestones = listOf(
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000004"), 1.5, "Île de la Cité", "Le berceau de Paris, où Notre-Dame de Paris se relève fièrement de ses cendres."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000005"), 3.0, "Musée d'Orsay", "L'ancienne gare reconvertie en temple de l'impressionnisme avec ses immenses horloges."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000006"), 5.0, "Tour Eiffel", "Tu arrives au pied de la Dame de Fer, icône de Paris depuis l'Exposition universelle de 1889."),
                )
            ),
            Journey(
                id = UUID.fromString("a0000000-0000-0000-0000-000000000003"),
                name = "Boucle de Central Park",
                subtitle = "Tour du parc new-yorkais",
                totalKm = 10.0,
                category = JourneyCategory.WALK,
                emoji = "🍂",
                milestones = listOf(
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000007"), 2.5, "Conservatory Garden", "Les jardins formels du nord du parc, un havre de tranquillité loin de l'agitation new-yorkaise."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000008"), 5.0, "The Great Lawn", "La grande pelouse où des millions de New-Yorkais pique-niquent et assistent à des concerts."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000009"), 7.5, "Bethesda Fountain", "La fontaine emblématique du parc, au bord du lac, immortalisée dans d'innombrables films."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000010"), 10.0, "Strawberry Fields", "Le mémorial dédié à John Lennon, où des fans du monde entier viennent se recueillir."),
                )
            ),
            Journey(
                id = UUID.fromString("a0000000-0000-0000-0000-000000000004"),
                name = "Semi-marathon de Paris",
                subtitle = "De l'Hôtel de Ville aux Champs-Élysées",
                totalKm = 21.0,
                category = JourneyCategory.WALK,
                emoji = "🏃",
                milestones = listOf(
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000011"), 5.0, "Place de la République", "Ce carrefour symbolique de Paris marque le premier quart de ton semi-marathon."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000012"), 10.5, "Place de la Bastille", "Mi-parcours ! Là où la Révolution française débuta en 1789 avec la prise de la Bastille."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000013"), 16.0, "Jardin des Tuileries", "Les grilles du jardin en vue, tu entres dans la dernière ligne droite."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000014"), 21.0, "Arc de Triomphe", "Tu arrives au sommet des Champs-Élysées. Félicitations, tu as couru un semi-marathon !"),
                )
            ),
            Journey(
                id = UUID.fromString("a0000000-0000-0000-0000-000000000005"),
                name = "Marathon de Paris",
                subtitle = "La boucle mythique",
                totalKm = 42.0,
                category = JourneyCategory.WALK,
                emoji = "🏅",
                milestones = listOf(
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000015"), 10.0, "Place de la Bastille", "Premier quart de marathon. Le soleil se lève sur la colonne de Juillet."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000016"), 21.0, "Bois de Vincennes", "Mi-parcours dans ce poumon vert à l'est de Paris, avec son château médiéval."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000017"), 30.0, "Tour Eiffel", "Tu passes au pied de la Dame de Fer. Les jambes chauffent, mais Paris te porte."),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000018"), 37.0, "Avenue Foch", "La montée finale vers l'Arc de Triomphe commence. Plus que 5 km !"),
                    Milestone(UUID.fromString("b0000000-0000-0000-0000-000000000019"), 42.0, "Arc de Triomphe", "Tu traverses la ligne d'arrivée sur les Champs-Élysées. Paris t'acclame !"),
                )
            ),

            // ── SENTIERS ────────────────────────────────────────────────────
            Journey(
                id = UUID.fromString("c0000000-0000-0000-0000-000000000001"),
                name = "GR20 Complet",
                subtitle = "Calenzana → Conca, la traversée mythique de la Corse",
                totalKm = 180.0,
                category = JourneyCategory.TRAIL,
                emoji = "🏔️",
                milestones = listOf(
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000001"), 60.0, "Refuge de Ciottulu di i Mori", "Les premières crêtes de Haute-Corse avec vue sur les pozzines, prairies d'altitude parsemées de lacs."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000002"), 120.0, "Vizzavona", "Passage en Corse du Sud, la forêt de pins laricio offre une fraîcheur bienvenue à mi-parcours."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000003"), 180.0, "Conca", "Tu termines le GR20, l'un des sentiers les plus difficiles d'Europe. Un exploit dont tu seras fier toute ta vie."),
                )
            ),
            Journey(
                id = UUID.fromString("c0000000-0000-0000-0000-000000000002"),
                name = "Camino Francés — Tronçon final",
                subtitle = "Sarria → Santiago de Compostela",
                totalKm = 111.0,
                category = JourneyCategory.TRAIL,
                emoji = "⚜️",
                milestones = listOf(
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000004"), 22.0, "Portomarín", "Traversée du río Miño sur un pont romain. Le village a été déplacé pierre par pierre lors de la construction du barrage."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000005"), 45.0, "Palas de Rei", "Tu entres en plein cœur de la Galice, la terre des pèlerins. Les eucalyptus remplacent les chênes."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000006"), 67.0, "Melide", "Ici convergent plusieurs chemins. Arrête-toi pour manger un pulpo a feira — la pieuvre galicienne est incontournable."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000007"), 90.0, "Pedrouzo", "Dernière étape avant Santiago. Cette nuit, des centaines de pèlerins vibrent à l'unisson."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000008"), 111.0, "Cathédrale de Santiago", "Tu entres dans la Plaza del Obradoiro. La façade baroque s'impose, les bras du Bernin t'accueillent. Buen Camino !"),
                )
            ),
            Journey(
                id = UUID.fromString("c0000000-0000-0000-0000-000000000003"),
                name = "Camino Francés Complet",
                subtitle = "Saint-Jean-Pied-de-Port → Santiago de Compostela",
                totalKm = 780.0,
                category = JourneyCategory.TRAIL,
                emoji = "🐚",
                milestones = listOf(
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000009"), 25.0, "Col de Roncevaux", "Tu franchis les Pyrénées et entres en Espagne sur les traces de Roland, neveu de Charlemagne."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000010"), 200.0, "Burgos", "La cathédrale gothique de Burgos, chef-d'œuvre du XIIIe siècle, domine la ville et les pèlerins."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000011"), 400.0, "León", "La cathédrale de León et ses 1 800 m² de vitraux médiévaux baignent la nef dans une lumière irréelle."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000012"), 600.0, "O Cebreiro", "Le col qui marque l'entrée en Galice. Brume, granite et petites chapelles romanes t'accueillent."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000013"), 780.0, "Cathédrale de Santiago", "Après 780 km, tu traverses la Plaza del Obradoiro. Le Botafumeiro se balance dans la cathédrale. Tu l'as fait."),
                )
            ),
            Journey(
                id = UUID.fromString("c0000000-0000-0000-0000-000000000004"),
                name = "Via de la Plata",
                subtitle = "Séville → Santiago, le chemin de l'argent",
                totalKm = 1000.0,
                category = JourneyCategory.TRAIL,
                emoji = "🌿",
                milestones = listOf(
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000014"), 100.0, "Mérida", "L'ancienne capitale romaine avec son théâtre antique et son aqueduc remarquablement conservés depuis le Ier siècle."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000015"), 300.0, "Cáceres", "La vieille ville médiévale classée à l'UNESCO, dont le centre historique est inchangé depuis le XVe siècle."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000016"), 500.0, "Salamanque", "L'université la plus ancienne d'Espagne (fondée en 1218) et sa Plaza Mayor, doyenne des grandes places espagnoles."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000017"), 750.0, "Zamora", "La ville aux 23 églises romanes, le plus grand patrimoine roman du monde concentré en si peu d'espace."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000018"), 1000.0, "Cathédrale de Santiago", "Tu arrives par le Camino le plus solitaire et le plus sauvage. Santiago n'en a que plus de saveur."),
                )
            ),
            Journey(
                id = UUID.fromString("c0000000-0000-0000-0000-000000000005"),
                name = "Tour du Mont Blanc",
                subtitle = "Entre France, Italie et Suisse",
                totalKm = 170.0,
                category = JourneyCategory.TRAIL,
                emoji = "🏔️",
                milestones = listOf(
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000019"), 40.0, "Les Houches", "Tu quittes Chamonix, le Mont-Blanc (4 808 m) domine l'horizon de toute sa majesté."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000020"), 85.0, "Courmayeur", "Tu passes en Italie ! La Vallée d'Aoste t'accueille avec sa cuisine et ses vins généreux."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000021"), 130.0, "Champex-Lac", "Côté suisse, ce lac de montagne offre un reflet parfait des sommets environnants."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000022"), 170.0, "Chamonix", "Tu boucles la boucle ! 170 km, 3 pays, 10 000 m de dénivelé positif. Un chef-d'œuvre alpin accompli."),
                )
            ),
            Journey(
                id = UUID.fromString("c0000000-0000-0000-0000-000000000006"),
                name = "Via Francigena — Tronçon final",
                subtitle = "Lucques → Rome, sur les traces des pèlerins médiévaux",
                totalKm = 420.0,
                category = JourneyCategory.TRAIL,
                emoji = "🕊️",
                milestones = listOf(
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000023"), 80.0, "Sienne", "La place del Campo, l'une des plus belles d'Europe, avec sa tour del Mangia qui défie le ciel toscan."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000024"), 160.0, "Bolsena", "Le lac volcanique de Bolsena, ses eaux translucides et ses îles médiévales marquent l'entrée dans le Latium."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000025"), 250.0, "Viterbo", "La cité des papes au XIIIe siècle, avec son quartier médiéval parfaitement conservé de San Pellegrino."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000026"), 350.0, "Sutri", "L'amphithéâtre romain creusé dans le tuf volcanique est l'un des secrets les mieux gardés du Latium."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000027"), 420.0, "Saint-Pierre de Rome", "Tu arrives place Saint-Pierre, la colonnade du Bernin t'embrasse. La Via Francigena s'achève ici."),
                )
            ),

            // ── HISTOIRE ────────────────────────────────────────────────────
            Journey(
                id = UUID.fromString("e0000000-0000-0000-0000-000000000001"),
                name = "Route Royale Perse",
                subtitle = "Suse → Sardes, la grande artère de l'Empire achéménide",
                totalKm = 2700.0,
                category = JourneyCategory.HISTORY,
                emoji = "👑",
                milestones = listOf(
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000001"), 500.0, "Persépolis", "La cité impériale d'Achéménès, ses palais à colonnes et ses reliefs racontent 2 500 ans d'histoire perse."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000002"), 1200.0, "Babylone", "La cité de Nabuchodonosor, ses murailles décorées de lions émaillés et ses jardins suspendus légendaires."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000003"), 2000.0, "Damas", "Carrefour des caravanes depuis l'Antiquité, la plus ancienne ville continuellement habitée du monde."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000004"), 2700.0, "Sardes", "La capitale du roi Crésus, dont la fortune légendaire donnera l'expression « riche comme Crésus »."),
                )
            ),
            Journey(
                id = UUID.fromString("e0000000-0000-0000-0000-000000000002"),
                name = "Alexandre — Campagne Perse",
                subtitle = "La conquête de l'empire achéménide",
                totalKm = 5000.0,
                category = JourneyCategory.HISTORY,
                emoji = "⚔️",
                milestones = listOf(
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000005"), 500.0, "Issos", "La bataille décisive où Alexandre défait Darius III. Le roi perse s'enfuit, laissant sa famille en otage."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000006"), 1200.0, "Tyr", "Le siège de 7 mois de cette île-forteresse. Alexandre construit une digue pour y accéder. Génie militaire."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000007"), 2000.0, "Memphis", "Alexandre libère l'Égypte des Perses et est sacré pharaon. Il fonde Alexandrie sur la côte méditerranéenne."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000008"), 3500.0, "Gaugamèles", "La bataille finale contre Darius. L'empire perse s'effondre, Alexandre devient maître de l'Asie."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000009"), 5000.0, "Persépolis", "Alexandre brûle le palais de Xerxès en représailles à l'incendie d'Athènes. La vengeance grecque accomplie."),
                )
            ),
            Journey(
                id = UUID.fromString("e0000000-0000-0000-0000-000000000003"),
                name = "Alexandre — Épopée Complète",
                subtitle = "La conquête du monde connu, de la Grèce à l'Indus",
                totalKm = 35000.0,
                category = JourneyCategory.HISTORY,
                emoji = "🌍",
                milestones = listOf(
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000010"), 500.0, "Issos", "La grande bataille contre Darius III marque le début de la conquête de l'Orient."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000011"), 2000.0, "Alexandrie d'Égypte", "Alexandre fonde sa ville, dessinant lui-même son plan. Elle deviendra la capitale du monde hellénistique."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000012"), 5000.0, "Persépolis", "L'empire achéménide tombe. Alexandre est désormais roi d'Asie."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000013"), 8000.0, "Samarcande", "Aux confins du monde connu, Alexandre épouse Roxane et pousse ses hommes à l'extrême limite."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000014"), 12000.0, "Indus", "La dernière frontière. Alexandre veut aller plus loin, mais ses soldats refusent. Il pleure sur la rive."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000015"), 18000.0, "Gédrosie", "Le désert de Makran, la traversée la plus meurtrière de toute la campagne. Des milliers meurent de soif."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000016"), 28000.0, "Babylone", "Alexandre rentre dans sa capitale. Ses projets sont immenses : Arabie, Méditerranée occidentale..."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000017"), 35000.0, "Babylone, 323 av. J.-C.", "Alexandre meurt à 32 ans sans désigner d'héritier. Son empire se fragmente. La légende, elle, est éternelle."),
                )
            ),
            Journey(
                id = UUID.fromString("e0000000-0000-0000-0000-000000000004"),
                name = "Route de la Soie",
                subtitle = "Xi'an → Rome, l'artère du commerce antique",
                totalKm = 6400.0,
                category = JourneyCategory.HISTORY,
                emoji = "🧵",
                milestones = listOf(
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000018"), 800.0, "Dunhuang", "Les grottes de Mogao, 492 temples bouddhistes creusés dans la falaise depuis le IVe siècle."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000019"), 2000.0, "Samarcande", "La cité légendaire de Tamerlan, ses mosquées à coupoles bleues brillent sous le soleil d'Asie centrale."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000020"), 3000.0, "Ctésiphon", "La capitale des Parthes puis des Sassanides, où l'arc de Khosrow défie encore le temps."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000021"), 4000.0, "Antioche", "La troisième ville de l'Empire romain, carrefour stratégique entre Orient et Occident."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000022"), 5500.0, "Constantinople", "La Nouvelle Rome au carrefour des continents, gardienne de la route entre Asie et Europe."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000023"), 6400.0, "Rome", "La soie de Chine, les épices d'Inde, les perles de Perse : tous les trésors du monde aboutissent ici."),
                )
            ),
            Journey(
                id = UUID.fromString("e0000000-0000-0000-0000-000000000005"),
                name = "Marco Polo",
                subtitle = "Venise → Pékin, sur les traces du grand voyageur",
                totalKm = 12000.0,
                category = JourneyCategory.HISTORY,
                emoji = "🗺️",
                milestones = listOf(
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000024"), 1000.0, "Constantinople", "La ville-carrefour entre Orient et Occident, première grande étape du voyage des frères Polo."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000025"), 3000.0, "Hormuz", "Le détroit par lequel transitent toutes les richesses de l'Orient. Marco renonce à la mer et choisit la terre."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000026"), 5500.0, "Samarcande", "La cité légendaire aux mosquées de faïence bleue, carrefour des routes d'Asie centrale."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000027"), 8000.0, "Dunhuang", "Les grottes de Mogao et leurs milliers de bouddhas sculptés dans la falaise depuis le IVe siècle."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000028"), 12000.0, "Khanbaliq (Pékin)", "Marco Polo arrive à la cour de Kublai Khan après 4 ans de voyage. Il y restera 17 ans. La légende commence."),
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
