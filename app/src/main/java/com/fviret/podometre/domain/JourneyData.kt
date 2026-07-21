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
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000001"), 16.0, "Carrozzu", "Premier refuge après Calenzana, au cœur des aiguilles de Bonifatu. Le canyon et le pont de singe resteront gravés dans ta mémoire."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000028"), 33.0, "U Vallone", "Refuge suspendu entre lacs et crêtes vertigineuses. Tu as déjà dépassé le point de non-retour du GR20."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000029"), 49.0, "Refuge de Ciottulu di i Mori", "Les premières crêtes de Haute-Corse avec vue sur les pozzines, prairies d'altitude parsemées de lacs."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000030"), 66.0, "Col de Vergio", "Point culminant de la partie nord, 1 477 m d'altitude. La vue plonge sur les deux versants de l'île de Beauté."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000031"), 82.0, "Manganu", "Vaste plateau d'altitude cerné de pozzines. Le silence n'est brisé que par le vent et les chevaux sauvages."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000032"), 98.0, "Petra Piana", "Le refuge se découpe sur un ciel souvent violet au coucher. Tu entres dans le cœur sauvage du GR20."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000002"), 117.0, "Vizzavona", "Passage en Corse du Sud, la forêt de pins laricio offre une fraîcheur bienvenue à mi-parcours."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000033"), 133.0, "Prati", "Premier refuge du GR20 sud, perché au-dessus des vallées. Le massif du Monte Renoso se profile à l'horizon."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000034"), 150.0, "Usciolu", "Refuge mythique accroché à un éperon rocheux. La Vue sur le golfe d'Ajaccio récompense chaque effort."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000035"), 163.0, "Asinau", "Les aiguilles de Bavella se dressent comme des cathédrales de granite. Le sentier devient artistique."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000036"), 173.0, "Paliri", "Dernier refuge avant Conca. Tu sens déjà la mer et la fin du voyage mythique."),
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
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000037"), 11.0, "Morgade", "La borne des 100 km s'approche. Les chemins de pierre s'enfoncent dans les forêts de chênes galiciens."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000004"), 22.0, "Portomarín", "Traversée du río Miño sur un pont romain. Le village a été déplacé pierre par pierre lors de la construction du barrage."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000005"), 45.0, "Palas de Rei", "Tu entres en plein cœur de la Galice, la terre des pèlerins. Les eucalyptus remplacent les chênes."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000038"), 55.0, "Eirexe", "Hameau de granite et mousse. Le sentier serpente entre horreos, ces greniers sur piliers typiques de Galice."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000006"), 67.0, "Melide", "Ici convergent plusieurs chemins. Arrête-toi pour manger un pulpo a feira — la pieuvre galicienne est incontournable."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000039"), 78.0, "O Pedrouzo", "Dernière grande étape avant Santiago. Cette nuit, des centaines de pèlerins vibrent à l'unisson."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000007"), 90.0, "Monte do Gozo", "La colline de la joie — d'ici tu aperçois pour la première fois les tours de la cathédrale de Santiago."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000040"), 100.0, "Borne des 100 km", "Tu as parcouru 100 km depuis Sarria. La compostela officielle est à portée de main."),
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
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000019"), 15.0, "Les Houches", "Tu quittes Chamonix, le Mont-Blanc (4 808 m) domine l'horizon de toute sa majesté."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000041"), 32.0, "Les Contamines-Montjoie", "Charmant village savoyard au pied du dôme de Miage. Les alpages ouvrent sur le col du Bonhomme."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000042"), 50.0, "Les Chapieux", "Hameau isolé à 1 554 m. Le col de la Seigne et la frontière italienne s'offrent dès le lendemain matin."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000043"), 65.0, "Refuge Elisabetta", "Premier refuge côté italien, face aux glaciers du Mont-Blanc. L'Italie commence dans un décor de haute montagne."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000020"), 82.0, "Courmayeur", "Tu passes en Italie ! La Vallée d'Aoste t'accueille avec sa cuisine et ses vins généreux."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000044"), 98.0, "Refuge Bonatti", "Belvédère sur les Grandes Jorasses. Le panorama depuis la terrasse du refuge est parmi les plus beaux des Alpes."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000045"), 114.0, "La Fouly", "Premier village suisse du Tour. La Suisse commence par des prairies fleuries et des chalets impeccables."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000021"), 130.0, "Champex-Lac", "Côté suisse, ce lac de montagne offre un reflet parfait des sommets environnants."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000046"), 148.0, "Col de la Forclaz", "La descente vers Martigny s'amorce. La vue sur le val de Bagnes et le glacier du Trient est hypnotique."),
                    Milestone(UUID.fromString("d0000000-0000-0000-0000-000000000047"), 160.0, "Tré-le-Champ", "Côté français à nouveau. Les aiguilles Rouges et les Grandes Montets encadrent le chemin du retour."),
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
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000029"), 250.0, "Pasargades", "La première capitale achéménide, fondée par Cyrus le Grand. Son tombeau de marbre blanc veille encore sur la plaine."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000001"), 500.0, "Persépolis", "La cité impériale d'Achéménès, ses palais à colonnes et ses reliefs racontent 2 500 ans d'histoire perse."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000030"), 750.0, "Ecbatane", "La capitale d'été des rois perses, perchée à 1 800 m d'altitude. Ses sept murailles concentriques brillaient de couleurs différentes."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000002"), 1200.0, "Babylone", "La cité de Nabuchodonosor, ses murailles décorées de lions émaillés et ses jardins suspendus légendaires."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000031"), 1500.0, "Ninive", "L'ancienne capitale assyrienne, dont les lions de pierre gardaient les palais de Sennachérib. Aujourd'hui silencieuse."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000032"), 1800.0, "Gordion", "La cité du nœud gordien que nul ne pouvait défaire. Le carrefour phygien entre Orient et Occident."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000003"), 2000.0, "Damas", "Carrefour des caravanes depuis l'Antiquité, la plus ancienne ville continuellement habitée du monde."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000033"), 2200.0, "Antioche", "La troisième ville de l'Empire romain, bâtie au débouché de la vallée de l'Oronte sur la Méditerranée."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000034"), 2400.0, "Sardes-sur-route", "Les caravanes traversent la Lydie, le pays de l'or electrum, avant d'atteindre la capitale."),
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
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000035"), 200.0, "Granique", "La première bataille sur le sol perse. Alexandre charge personnellement la cavalerie ennemie. La conquête commence."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000036"), 400.0, "Gordion", "Alexandre tranche le nœud gordien : celui qui le défait régnera sur l'Asie. La prophétie est accomplie."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000005"), 500.0, "Issos", "La bataille décisive où Alexandre défait Darius III. Le roi perse s'enfuit, laissant sa famille en otage."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000006"), 1200.0, "Tyr", "Le siège de 7 mois de cette île-forteresse. Alexandre construit une digue pour y accéder. Génie militaire."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000037"), 1600.0, "Alexandrie d'Égypte", "Alexandre trace de sa lance le plan de la future capitale. La cité qu'il imagine deviendra la première ville du monde."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000007"), 2000.0, "Memphis", "Alexandre libère l'Égypte des Perses et est sacré pharaon. Il fonde Alexandrie sur la côte méditerranéenne."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000038"), 2600.0, "Babylone", "La cité millénaire se rend sans combat. Alexandre y entre triomphalement, adopte les mœurs babyloniennes."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000008"), 3500.0, "Gaugamèles", "La bataille finale contre Darius. L'empire perse s'effondre, Alexandre devient maître de l'Asie."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000039"), 4000.0, "Portes Persiques", "Le défilé montagnard où les Perses tentent leur dernière résistance. Alexandre les contourne de nuit."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000040"), 4500.0, "Pasargades", "Alexandre visite le tombeau de Cyrus et ordonne qu'il soit restauré. Respect du conquérant pour son prédécesseur."),
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
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000041"), 1500.0, "Tyr", "Sept mois de siège pour prendre cette île inexpugnable. Alexandre ordonne de construire une digue en pleine mer."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000011"), 2000.0, "Alexandrie d'Égypte", "Alexandre fonde sa ville, dessinant lui-même son plan. Elle deviendra la capitale du monde hellénistique."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000042"), 3500.0, "Gaugamèles", "La bataille décisive qui met fin à l'empire perse. Darius s'enfuit pour la dernière fois."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000012"), 5000.0, "Persépolis", "L'empire achéménide tombe. Alexandre est désormais roi d'Asie."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000043"), 6500.0, "Ecbatane", "La trésorerie royale perse tombe aux mains d'Alexandre. Ses finances lui permettent de conquérir le monde."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000013"), 8000.0, "Samarcande", "Aux confins du monde connu, Alexandre épouse Roxane et pousse ses hommes à l'extrême limite."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000044"), 9500.0, "Bactres", "La ville de Bactriane où Alexandre installe des colonies grecques. L'hellénisme atteint l'Asie centrale."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000045"), 10500.0, "Khyber", "Alexandre franchit le col de Khyber et entre dans le sous-continent indien. Nul Grec ne l'a fait avant lui."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000014"), 12000.0, "Indus", "La dernière frontière. Alexandre veut aller plus loin, mais ses soldats refusent. Il pleure sur la rive."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000046"), 13500.0, "Hydaspe", "La dernière grande bataille, contre le roi Poros et ses éléphants. La cavalerie macédonienne n'a jamais failli."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000047"), 16000.0, "Noces de Suse", "Alexandre organise les noces de Suse : 10 000 Macédoniens épousent des Persianes. Fusion des mondes."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000015"), 18000.0, "Gédrosie", "Le désert de Makran, la traversée la plus meurtrière de toute la campagne. Des milliers meurent de soif."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000048"), 22000.0, "Carmania", "Alexandre retrouve le reste de l'armée après la traversée du désert. Fête de plusieurs jours dans la province fertile."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000049"), 26000.0, "Persépolis revisitée", "Alexandre revient sur les ruines qu'il a lui-même brûlées. Il dit avoir regretté cet acte."),
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
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000050"), 400.0, "Lanzhou", "La porte de la Route de la Soie, où le fleuve Jaune marque la limite entre la Chine des Han et les steppes."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000018"), 800.0, "Dunhuang", "Les grottes de Mogao, 492 temples bouddhistes creusés dans la falaise depuis le IVe siècle."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000051"), 1100.0, "Tourfan", "L'oasis du Taklamakan, ses ruines de cités uighours ensablées et ses vignes qui prospèrent sous 40 degrés."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000052"), 1400.0, "Kashgar", "Le grand marché dominical où se croisent Chinois, Perses, Arabes et Romains depuis deux millénaires."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000053"), 1800.0, "Merv", "L'oasis la plus puissante d'Asie centrale. Carrefour incontournable des routes nord-sud et est-ouest."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000019"), 2000.0, "Samarcande", "La cité légendaire de Tamerlan, ses mosquées à coupoles bleues brillent sous le soleil d'Asie centrale."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000054"), 2500.0, "Boukhara", "La ville sainte d'Asie centrale, ses 140 médersas et son minaret Kalon qui servait de phare aux caravanes."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000055"), 2800.0, "Hérat", "La perle du Khorasan, ville de poètes et de miniaturistes. Tamerlane y fit construire ses plus beaux monuments."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000020"), 3000.0, "Ctésiphon", "La capitale des Parthes puis des Sassanides, où l'arc de Khosrow défie encore le temps."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000056"), 3400.0, "Bagdad", "Fondée au VIIIe siècle, la « Ville ronde » des Abbassides concentrait la moitié de la richesse mondiale."),
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
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000057"), 1800.0, "Trébizonde", "Port byzantin sur la mer Noire, tête de pont de la route persane. Marco y observe la rencontre de deux mondes."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000058"), 2400.0, "Tabriz", "La grande cité marchande de Perse, où transitent les pierres précieuses et les soies de toute l'Asie."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000025"), 3000.0, "Hormuz", "Le détroit par lequel transitent toutes les richesses de l'Orient. Marco renonce à la mer et choisit la terre."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000059"), 3800.0, "Khorasan", "La province perse aux villes légendaires. Marco décrit ses habitants comme « les plus beaux de la terre »."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000060"), 4500.0, "Badakhshan", "Le pays des rubis et du lapis-lazuli. Marco y séjourne un an, malade. Les montagnes l'ont guéri, dit-il."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000061"), 5200.0, "Pamir", "Le « Toit du monde », plateau glacial à 4 500 m où Marco voit pour la première fois des moutons aux cornes géantes."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000026"), 5500.0, "Samarcande", "La cité légendaire aux mosquées de faïence bleue, carrefour des routes d'Asie centrale."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000062"), 6200.0, "Taklamakan", "Le désert « d'où l'on ne revient pas ». Marco le longe par le nord. Des voix y attirent les voyageurs à la mort."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000027"), 8000.0, "Dunhuang", "Les grottes de Mogao et leurs milliers de bouddhas sculptés dans la falaise depuis le IVe siècle."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000063"), 9000.0, "Shanzhou", "La cité-frontière entre désert et Chine des Han. Marco aperçoit pour la première fois des Chinois."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000064"), 10500.0, "Shangdu", "La capitale d'été de Kublai Khan, immortalisée par Coleridge : « Xanadu ». Marco est reçu en audience."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000065"), 11200.0, "Le Grand Canal", "L'œuvre hydraulique des Song : 1 800 km de canaux reliant Pékin au Yangtsé. Marco n'en croit pas ses yeux."),
                    Milestone(UUID.fromString("f0000000-0000-0000-0000-000000000028"), 12000.0, "Khanbaliq (Pékin)", "Marco Polo arrive à la cour de Kublai Khan après 4 ans de voyage. Il y restera 17 ans. La légende commence."),
                )
            ),

            // ── MYTHES & ÉPOPÉES ────────────────────────────────────────────
            Journey(
                id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
                name = "L'Odyssée — Trajet Réel",
                subtitle = "Troie → Ithaque, la route reconstituée",
                totalKm = 900.0,
                category = JourneyCategory.MYTH,
                emoji = "🌊",
                milestones = listOf(
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000001"), 100.0, "Ismaros", "Ulysse saccage la ville des Kikoniens. Premier arrêt, premier désastre : leurs alliés arrivent et massacrent ses hommes."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000002"), 250.0, "Détroit des Dardanelles", "Tu traverses le détroit là où Troie veillait sur le passage entre deux mondes depuis des siècles."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000003"), 450.0, "Cap Malée", "Le cap le plus redouté des marins grecs. Une tempête ici envoie Ulysse bien au-delà de sa route prévue."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000004"), 600.0, "Mer Ionienne", "Tu navigues dans les eaux qui séparent la Grèce de l'Italie, où Charybde et Scylla guettent les imprudents."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000005"), 750.0, "Corfou (Phéaciens)", "Les Phéaciens t'accueillent. Nausicaa trouve Ulysse sur la plage. Le retour est enfin possible."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000006"), 900.0, "Ithaque", "Ulysse rentre incognito après 20 ans d'absence. Il bande son arc, perce les anneaux. Pénélope est libre."),
                )
            ),
            Journey(
                id = UUID.fromString("10000000-0000-0000-0000-000000000002"),
                name = "L'Odyssée — Voyage Complet",
                subtitle = "L'errance mythique d'Ulysse à travers la Méditerranée",
                totalKm = 8000.0,
                category = JourneyCategory.MYTH,
                emoji = "🔱",
                milestones = listOf(
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000007"), 500.0, "Île des Lotophages", "Un peuple qui mange le lotus et oublie tout désir de rentrer. Ulysse doit traîner ses hommes de force."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000008"), 1200.0, "Île des Cyclopes", "Tu échappes à Polyphème grâce à la ruse d'Ulysse : « Personne » — c'est mon nom !"),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000009"), 2000.0, "Île d'Éole", "Le dieu des vents offre une outre renfermant tous les vents contraires. Les marins l'ouvrent. Désastre."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000010"), 3000.0, "Île de Circé", "La magicienne transforme les compagnons en porcs. Ulysse résiste grâce à l'herbe moly d'Hermès."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000011"), 4000.0, "Les Enfers", "Ulysse convoque les morts pour consulter Tirésias. Il y revoit Achille, Ajax, et sa propre mère."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000012"), 5500.0, "Charybde et Scylla", "Tu navigues entre le gouffre tourbillonnant et le monstre à six têtes. La mer tremble de colère."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000013"), 6500.0, "Île de Calypso", "La nymphe retient Ulysse 7 ans. Même l'immortalité ne peut effacer le mal du pays."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000014"), 8000.0, "Ithaque", "Après 10 ans d'errance et 20 ans d'absence, Ulysse retrouve Pénélope et Télémaque. L'Odyssée s'achève."),
                )
            ),
            Journey(
                id = UUID.fromString("10000000-0000-0000-0000-000000000003"),
                name = "L'Iliade — Siège de Troie",
                subtitle = "Mycènes → Troie, la guerre qui façonna l'Occident",
                totalKm = 400.0,
                category = JourneyCategory.MYTH,
                emoji = "🐴",
                milestones = listOf(
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000015"), 100.0, "Aulis", "La flotte grecque de mille navires se rassemble ici. Agamemnon sacrifie Iphigénie pour que les vents soufflent."),
                    Milestone(UUID.fromString("11000000-0000-0000-0000-000000000016"), 400.0, "Troie (Ilion)", "Dix ans de siège, la colère d'Achille, la mort d'Hector, le cheval de bois. La cité brûle. La légende commence."),
                )
            ),
        )
    }

    /** Retourne un trajet par son UUID, ou null s'il n'existe pas. */
    fun findById(id: String): Journey? =
        all.firstOrNull { it.id.toString() == id }
}
