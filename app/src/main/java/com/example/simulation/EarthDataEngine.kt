package com.example.simulation

data class LocationHub(
    val name: String,
    val country: String,
    val type: String, // "Monumento", "Aeropuerto", "Estación", "Puerto", "Ciudad"
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val timezone: String,
    val description: String
)

object EarthDataEngine {
    val hubs = listOf(
        LocationHub(
            name = "París - Torre Eiffel",
            country = "Francia",
            type = "Monumento",
            latitude = 48.8584,
            longitude = 2.2945,
            altitudeMeters = 35.0,
            timezone = "UTC+1",
            description = "La Torre Eiffel es una estructura de hierro de 330 metros que simboliza a París y a Francia en todo el mundo."
        ),
        LocationHub(
            name = "París - Aeropuerto Charles de Gaulle (CDG)",
            country = "Francia",
            type = "Aeropuerto",
            latitude = 49.0097,
            longitude = 2.5479,
            altitudeMeters = 119.0,
            timezone = "UTC+1",
            description = "El principal aeropuerto de Francia y uno de los mayores centros de conexión aérea de Europa continental."
        ),
        LocationHub(
            name = "París - Estación de Gare de Lyon",
            country = "Francia",
            type = "Estación",
            latitude = 48.8443,
            longitude = 2.3744,
            altitudeMeters = 38.0,
            timezone = "UTC+1",
            description = "Histórica estación de tren terminal con conexiones de alta velocidad (TGV) con el sur de Francia, Italia y Suiza."
        ),
        LocationHub(
            name = "Nueva York - Estatua de la Libertad",
            country = "Estados Unidos",
            type = "Monumento",
            latitude = 40.6892,
            longitude = -74.0445,
            altitudeMeters = 2.0,
            timezone = "UTC-5",
            description = "Monumento colosal obsequiado por Francia que conmemora la alianza revolucionaria y la libertad en Nueva York."
        ),
        LocationHub(
            name = "Nueva York - Aeropuerto JFK",
            country = "Estados Unidos",
            type = "Aeropuerto",
            latitude = 40.6413,
            longitude = -73.7781,
            altitudeMeters = 4.0,
            timezone = "UTC-5",
            description = "El aeropuerto internacional icónico que sirve de puerta de entrada global a América del Norte."
        ),
        LocationHub(
            name = "El Cairo - Pirámides de Giza",
            country = "Egipto",
            type = "Monumento",
            latitude = 29.9792,
            longitude = 31.1342,
            altitudeMeters = 130.0,
            timezone = "UTC+2",
            description = "La Gran Pirámide es la única superviviente de las Siete Maravillas del Mundo Antiguo, con más de 4500 años de antigüedad."
        ),
        LocationHub(
            name = "Tokio - Templo Senso-ji",
            country = "Japón",
            type = "Monumento",
            latitude = 35.7148,
            longitude = 139.7967,
            altitudeMeters = 5.0,
            timezone = "UTC+9",
            description = "El templo budista más antiguo e icónico de Tokio, dedicado al Bodhisattva Kannon, fundado en el año 645 d.C."
        ),
        LocationHub(
            name = "Tokio - Aeropuerto de Haneda (HND)",
            country = "Japón",
            type = "Aeropuerto",
            latitude = 35.5494,
            longitude = 139.7798,
            altitudeMeters = 6.0,
            timezone = "UTC+9",
            description = "Uno de los aeropuertos más transitados y puntuales del globo, localizado a orillas de la Bahía de Tokio."
        ),
        LocationHub(
            name = "Sídney - Ópera de Sídney",
            country = "Australia",
            type = "Monumento",
            latitude = -33.8568,
            longitude = 151.2153,
            altitudeMeters = 4.0,
            timezone = "UTC+10",
            description = "Obra maestra de la arquitectura expresionista del siglo XX, diseñada por Jørn Utzon, con sus icónicas conchas flotantes."
        ),
        LocationHub(
            name = "Río de Janeiro - Cristo Redentor",
            country = "Brasil",
            type = "Monumento",
            latitude = -22.9519,
            longitude = -43.2105,
            altitudeMeters = 710.0,
            timezone = "UTC-3",
            description = "Estatua Art Déco de Jesucristo de 30 metros, situada en la cima del cerro del Corcovado dominando la bahía de Río."
        ),
        LocationHub(
            name = "Río de Janeiro - Puerto de Río",
            country = "Brasil",
            type = "Puerto",
            latitude = -22.8967,
            longitude = -43.1901,
            altitudeMeters = 2.0,
            timezone = "UTC-3",
            description = "Puerto comercial e industrial histórico dentro de la icónica bahía de Guanabara de Río de Janeiro."
        ),
        LocationHub(
            name = "Londres - Big Ben & Westminster",
            country = "Reino Unido",
            type = "Monumento",
            latitude = 51.5007,
            longitude = -0.1246,
            altitudeMeters = 15.0,
            timezone = "UTC+0",
            description = "La torre de reloj neogótica del Palacio de Westminster es el emblema definitivo de Londres desde 1859."
        ),
        LocationHub(
            name = "Londres - Aeropuerto de Heathrow (LHR)",
            country = "Reino Unido",
            type = "Aeropuerto",
            latitude = 51.4700,
            longitude = -0.4543,
            altitudeMeters = 25.0,
            timezone = "UTC+0",
            description = "El principal aeropuerto de Londres, siendo el nodo de transporte aéreo más activo de todo el Reino Unido."
        ),
        LocationHub(
            name = "Roma - Coliseo Romano",
            country = "Italia",
            type = "Monumento",
            latitude = 41.8902,
            longitude = 12.4922,
            altitudeMeters = 52.0,
            timezone = "UTC+1",
            description = "El anfiteatro más grande jamás construido en el Imperio Romano, inaugurado en el año 80 d.C. para juegos de gladiadores."
        ),
        LocationHub(
            name = "Roma - Estación Termini",
            country = "Italia",
            type = "Estación",
            latitude = 41.9014,
            longitude = 12.5019,
            altitudeMeters = 58.0,
            timezone = "UTC+1",
            description = "El nudo de transportes principal de Roma que opera servicios interurbanos rápidos y el metro metropolitano."
        ),
        LocationHub(
            name = "Pekín - Ciudad Prohibida",
            country = "China",
            type = "Monumento",
            latitude = 39.9163,
            longitude = 116.3972,
            altitudeMeters = 44.0,
            timezone = "UTC+8",
            description = "Complejo de palacios imperiales chinos de las dinastías Ming y Qing, que contiene 980 edificios históricos."
        ),
        LocationHub(
            name = "Dubái - Burj Khalifa",
            country = "Emiratos Árabes Unidos",
            type = "Monumento",
            latitude = 25.1972,
            longitude = 55.2744,
            altitudeMeters = 11.0,
            timezone = "UTC+4",
            description = "El rascacielos más alto del planeta Tierra, que se eleva a una asombrosa cota de 828 metros en la metrópolis de Dubái."
        ),
        LocationHub(
            name = "Estambul - Santa Sofía",
            country = "Turquía",
            type = "Monumento",
            latitude = 41.0086,
            longitude = 28.9802,
            altitudeMeters = 30.0,
            timezone = "UTC+3",
            description = "Monumento bizantino cumbre erigido originalmente como catedral en el siglo VI, luego mezquita y ahora museo."
        ),
        LocationHub(
            name = "Atenas - Acrópolis",
            country = "Grecia",
            type = "Monumento",
            latitude = 37.9715,
            longitude = 23.7257,
            altitudeMeters = 150.0,
            timezone = "UTC+2",
            description = "La antigua ciudadela griega sobre una colina rocosa que alberga el legendario templo del Partenón."
        ),
        LocationHub(
            name = "Ciudad del Cabo - Montaña de la Mesa",
            country = "Sudáfrica",
            type = "Monumento",
            latitude = -33.9628,
            longitude = 18.4251,
            altitudeMeters = 1085.0,
            timezone = "UTC+2",
            description = "Una imponente montaña de cima plana que domina la ciudad, declarada una de las siete maravillas naturales del mundo."
        ),
        LocationHub(
            name = "Moscú - Plaza Roja",
            country = "Rusia",
            type = "Monumento",
            latitude = 55.7539,
            longitude = 37.6208,
            altitudeMeters = 125.0,
            timezone = "UTC+3",
            description = "La icónica gran plaza central adyacente al Kremlin y adornada por las cúpulas de colores de San Basilio."
        ),
        LocationHub(
            name = "Bombay - Puerta de la India",
            country = "India",
            type = "Monumento",
            latitude = 18.9220,
            longitude = 72.8347,
            altitudeMeters = 5.0,
            timezone = "UTC+5.5",
            description = "Un arco de triunfo construido a orillas del mar Arábigo en conmemoración de la visita real de Jorge V en 1911."
        ),
        LocationHub(
            name = "Singapur - Marina Bay Sands",
            country = "Singapur",
            type = "Monumento",
            latitude = 1.2829,
            longitude = 103.8584,
            altitudeMeters = 15.0,
            timezone = "UTC+8",
            description = "Icono moderno del perfil de Singapur, con un parque elevado de tres hectáreas y piscina volada infinita."
        ),
        LocationHub(
            name = "Vancouver - Parque Stanley",
            country = "Canadá",
            type = "Monumento",
            latitude = 49.3021,
            longitude = -123.1432,
            altitudeMeters = 10.0,
            timezone = "UTC-8",
            description = "Uno de los parques urbanos más extensos de Norteamérica, con frondosos bosques de cedros costeros y tótems indígenas."
        ),
        LocationHub(
            name = "Buenos Aires - Obelisco",
            country = "Argentina",
            type = "Monumento",
            latitude = -34.6037,
            longitude = -58.3816,
            altitudeMeters = 25.0,
            timezone = "UTC-3",
            description = "Monumento erigido en la Avenida 9 de Julio para conmemorar el cuarto centenario de la fundación de la ciudad."
        ),
        LocationHub(
            name = "Buenos Aires - Puerto Madero",
            country = "Argentina",
            type = "Puerto",
            latitude = -34.6133,
            longitude = -58.3639,
            altitudeMeters = 3.0,
            timezone = "UTC-3",
            description = "Antiguo embarcadero de Buenos Aires reacondicionado hoy en un sofisticado barrio gastronómico y corporativo al agua."
        ),
        LocationHub(
            name = "Dublín - Trinity College",
            country = "Irlanda",
            type = "Monumento",
            latitude = 53.3438,
            longitude = -6.2546,
            altitudeMeters = 8.0,
            timezone = "UTC+0",
            description = "Prestigiosa universidad fundada en 1592, que salvaguarda la histórica e imponente Antigua Biblioteca y el Libro de Kells."
        ),
        LocationHub(
            name = "Cuzco - Machu Picchu",
            country = "Perú",
            type = "Monumento",
            latitude = -13.1631,
            longitude = -72.5450,
            altitudeMeters = 2430.0,
            timezone = "UTC-5",
            description = "Sanctuario histórico e imponente ciudadela inca construida en el siglo XV en una cresta andina espectacular."
        ),
        LocationHub(
            name = "Madrid - Puerta del Sol",
            country = "España",
            type = "Monumento",
            latitude = 40.4167,
            longitude = -3.7037,
            altitudeMeters = 650.0,
            timezone = "UTC+1",
            description = "El Km 0 de la red radial de carreteras españolas y el emblemático lugar de celebración de las campanadas de Año Nuevo."
        ),
        LocationHub(
            name = "Madrid - Estación de Atocha",
            country = "España",
            type = "Estación",
            latitude = 40.4067,
            longitude = -3.6903,
            altitudeMeters = 618.0,
            timezone = "UTC+1",
            description = "Estación ferroviaria central célebre por su espectacular jardín botánico tropical cubierto de 4000 metros."
        ),
        LocationHub(
            name = "Barcelona - Sagrada Familia",
            country = "España",
            type = "Monumento",
            latitude = 41.4036,
            longitude = 2.1744,
            altitudeMeters = 45.0,
            timezone = "UTC+1",
            description = "La majestuosa basílica modernista inacabada diseñada por Antoni Gaudí, Patrimonio de la Humanidad por la UNESCO."
        ),
        LocationHub(
            name = "Barcelona - Puerto de Barcelona",
            country = "España",
            type = "Puerto",
            latitude = 41.3592,
            longitude = 2.1524,
            altitudeMeters = 2.0,
            timezone = "UTC+1",
            description = "Uno de los puertos más importantes del mar Mediterráneo para el transporte de mercancías y turismo de cruceros."
        ),
        LocationHub(
            name = "Reykjavík - Iglesia Hallgrímskirkja",
            country = "Islandia",
            type = "Monumento",
            latitude = 64.1420,
            longitude = -21.9265,
            altitudeMeters = 40.0,
            timezone = "UTC+0",
            description = "Iglesia luterana icónica diseñada en estilo expresionista inspirándose en las columnas de basalto de los volcanes de Islandia."
        ),
        LocationHub(
            name = "Honolulu - Playa de Waikiki",
            country = "Estados Unidos",
            type = "Monumento",
            latitude = 21.2764,
            longitude = -157.8281,
            altitudeMeters = 1.0,
            timezone = "UTC-10",
            description = "Famosa playa de Hawái encaramada junto al cráter de Diamond Head, cuna del surf moderno."
        ),
        LocationHub(
            name = "Anchorage - Puerto de Anchorage",
            country = "Estados Unidos",
            type = "Puerto",
            latitude = 61.2408,
            longitude = -149.8883,
            altitudeMeters = 10.0,
            timezone = "UTC-9",
            description = "Puerto subártico estratégico fundamental de Alaska que recibe la mayor parte de suministros invernales del estado."
        )
    )

    fun search(query: String): List<LocationHub> {
        if (query.isBlank()) return emptyList()
        val cleaned = query.lowercase().trim()
        
        // Match by coordinate first if format matches 'lat, lng'
        val coords = cleaned.split(",")
        if (coords.size == 2) {
            val lat = coords[0].trim().toDoubleOrNull()
            val lng = coords[1].trim().toDoubleOrNull()
            if (lat != null && lng != null) {
                return listOf(
                    LocationHub(
                        name = "Coordenadas GPS",
                        country = "Global",
                        type = "Ciudad",
                        latitude = lat,
                        longitude = lng,
                        altitudeMeters = 100.0,
                        timezone = "UTC",
                        description = "Ubicación personalizada mediante coordenadas geográficas directas Latitude/Longitude."
                    )
                )
            }
        }

        return hubs.filter {
            it.name.lowercase().contains(cleaned) ||
            it.country.lowercase().contains(cleaned) ||
            it.type.lowercase().contains(cleaned)
        }
    }
}
