package com.ogzhngms.sorgez

import java.text.Normalizer
import java.util.Locale

// A point on the globe in degrees, north and east positive.
class Place(val lat: Float, val lon: Float)

private fun p(lat: Double, lon: Double, vararg names: String) = names.map { normalize(it) to Place(lat.toFloat(), lon.toFloat()) }

// Popular cities and countries by their English, Turkish and local names; the destination step's globe
// flies to them. ponytail: a fixed offline list, swap for a geocoding API if unknown places matter.
private val PLACES: Map<String, Place> = listOf(
    // Turkey
    p(41.01, 28.98, "İstanbul", "Istanbul", "Стамбул", "イスタンブール", "伊斯坦布尔", "이스탄불", "إسطنبول"),
    p(39.93, 32.86, "Ankara"), p(38.42, 27.14, "İzmir", "Izmir"), p(36.90, 30.70, "Antalya"),
    p(38.64, 34.83, "Kapadokya", "Cappadocia", "Capadocia", "Kappadokien", "Cappadoce", "Capadócia", "Каппадокия", "كابادوكيا", "कप्पादोकिया", "卡帕多奇亚", "カッパドキア", "카파도키아", "Kapadokia", "Kappadokiya", "Göreme", "Nevşehir", "Ürgüp"),
    p(37.03, 27.43, "Bodrum"), p(36.62, 29.12, "Fethiye", "Ölüdeniz"), p(41.00, 39.72, "Trabzon"), p(36.85, 28.27, "Marmaris"),
    p(36.20, 29.64, "Kaş", "Kas"), p(36.54, 32.00, "Alanya"), p(40.19, 29.06, "Bursa"), p(37.31, 40.74, "Mardin"),
    p(37.92, 29.12, "Pamukkale", "Denizli"), p(38.32, 26.30, "Çeşme", "Alaçatı"), p(39.78, 30.52, "Eskişehir"), p(41.02, 40.52, "Rize"),
    p(37.07, 37.38, "Gaziantep", "Antep"), p(37.16, 38.79, "Şanlıurfa", "Urfa"), p(38.49, 43.38, "Van"), p(37.87, 32.48, "Konya"),
    p(41.25, 32.69, "Safranbolu"), p(39.32, 26.69, "Ayvalık", "Cunda"), p(36.72, 27.69, "Datça"), p(37.86, 27.26, "Kuşadası"),
    p(37.94, 27.34, "Efes", "Ephesus", "Selçuk"), p(39.90, 41.27, "Erzurum"), p(40.60, 43.10, "Kars"), p(40.69, 30.27, "Sapanca"),
    p(39.83, 26.07, "Bozcaada"), p(40.15, 26.41, "Çanakkale", "Truva", "Troy"), p(41.68, 26.56, "Edirne"),
    p(39.00, 35.00, "Türkiye", "Turkey", "Turkiye"),
    // Europe
    p(41.90, 12.50, "Roma", "Rome", "Rom", "Рим", "روما", "रोम", "罗马", "ローマ", "로마"),
    p(45.46, 9.19, "Milano", "Milan", "Mailand"), p(45.44, 12.32, "Venedik", "Venice", "Venezia", "Venedig", "Venise"),
    p(43.77, 11.26, "Floransa", "Florence", "Firenze", "Florenz"), p(40.85, 14.27, "Napoli", "Naples", "Neapel"),
    p(40.63, 14.60, "Amalfi", "Positano"), p(37.60, 14.00, "Sicilya", "Sicily", "Sicilia", "Sizilien"), p(42.80, 12.80, "İtalya", "Italy", "Italia", "Italien", "Italie"),
    p(48.86, 2.35, "Paris", "Parigi", "Париж", "باريس", "पेरिस", "巴黎", "パリ", "파리"),
    p(43.70, 7.27, "Nice", "Nis"), p(45.76, 4.84, "Lyon"), p(46.60, 2.40, "Fransa", "France", "Frankreich", "Francia"),
    p(51.51, -0.13, "Londra", "London", "Londres"), p(55.95, -3.19, "Edinburgh", "Edinburg"), p(53.35, -6.26, "Dublin"),
    p(52.50, -1.50, "İngiltere", "England", "UK", "United Kingdom", "Birleşik Krallık"), p(56.80, -4.20, "İskoçya", "Scotland"), p(53.20, -8.00, "İrlanda", "Ireland"),
    p(52.37, 4.90, "Amsterdam"), p(52.20, 5.50, "Hollanda", "Netherlands", "Holland"),
    p(50.85, 4.35, "Brüksel", "Brussels", "Bruxelles", "Brüssel"), p(51.21, 3.22, "Brugge", "Bruges"), p(50.60, 4.60, "Belçika", "Belgium"),
    p(52.52, 13.40, "Berlin"), p(48.14, 11.58, "Münih", "Munich", "München"), p(53.55, 9.99, "Hamburg"), p(50.11, 8.68, "Frankfurt"),
    p(50.94, 6.96, "Köln", "Cologne"), p(51.10, 10.40, "Almanya", "Germany", "Deutschland", "Alemania", "Allemagne"),
    p(48.21, 16.37, "Viyana", "Vienna", "Wien"), p(47.81, 13.04, "Salzburg"), p(47.60, 14.10, "Avusturya", "Austria", "Österreich"),
    p(47.38, 8.54, "Zürih", "Zurich", "Zürich"), p(46.20, 6.14, "Cenevre", "Geneva", "Genève", "Genf"), p(46.69, 7.86, "Interlaken"),
    p(46.80, 8.20, "İsviçre", "Switzerland", "Schweiz", "Suisse"),
    p(50.08, 14.44, "Prag", "Prague", "Praha"), p(49.80, 15.50, "Çekya", "Czechia", "Czech Republic"),
    p(47.50, 19.04, "Budapeşte", "Budapest"), p(47.20, 19.40, "Macaristan", "Hungary"),
    p(52.23, 21.01, "Varşova", "Warsaw", "Warszawa"), p(50.06, 19.94, "Krakov", "Krakow", "Kraków"), p(52.00, 19.40, "Polonya", "Poland"),
    p(41.39, 2.17, "Barselona", "Barcelona", "Barcelone", "Barcellona", "Барселона", "برشلونة", "बार्सिलोना", "巴塞罗那", "バルセロナ", "바르셀로나"),
    p(40.42, -3.70, "Madrid"), p(37.39, -5.98, "Sevilla", "Seville"), p(37.18, -3.60, "Granada"), p(39.47, -0.38, "Valencia"),
    p(36.72, -4.42, "Malaga", "Málaga"), p(39.57, 2.65, "Mallorca", "Mayorka", "Palma"), p(38.91, 1.43, "İbiza", "Ibiza"), p(28.29, -16.63, "Tenerife", "Kanarya Adaları", "Canary Islands"),
    p(40.20, -3.70, "İspanya", "Spain", "España", "Spanien", "Espagne"),
    p(38.72, -9.14, "Lizbon", "Lisbon", "Lisboa", "Lissabon"), p(41.15, -8.61, "Porto"), p(39.60, -8.00, "Portekiz", "Portugal"),
    p(37.98, 23.73, "Atina", "Athens", "Athen", "Athènes"), p(36.39, 25.46, "Santorini"), p(37.45, 25.33, "Mikonos", "Mykonos"),
    p(35.24, 24.80, "Girit", "Crete", "Kreta"), p(36.43, 28.22, "Rodos", "Rhodes"), p(40.64, 22.94, "Selanik", "Thessaloniki"),
    p(39.10, 22.00, "Yunanistan", "Greece", "Griechenland", "Grèce"),
    p(55.68, 12.57, "Kopenhag", "Copenhagen", "København"), p(56.00, 10.00, "Danimarka", "Denmark"),
    p(59.33, 18.07, "Stockholm"), p(62.00, 15.00, "İsveç", "Sweden"), p(59.91, 10.75, "Oslo"), p(61.00, 9.00, "Norveç", "Norway"),
    p(60.17, 24.94, "Helsinki"), p(62.00, 26.00, "Finlandiya", "Finland"), p(66.50, 25.73, "Rovaniemi", "Laponya", "Lapland"),
    p(64.15, -21.94, "Reykjavik", "Reykjavík"), p(64.90, -18.60, "İzlanda", "Iceland"), p(69.65, 18.96, "Tromsø", "Tromso"),
    p(42.65, 18.09, "Dubrovnik"), p(43.51, 16.44, "Split"), p(44.50, 16.00, "Hırvatistan", "Croatia"),
    p(44.79, 20.45, "Belgrad", "Belgrade", "Beograd"), p(44.00, 21.00, "Sırbistan", "Serbia"),
    p(43.86, 18.41, "Saraybosna", "Sarajevo"), p(43.34, 17.81, "Mostar"), p(43.90, 17.70, "Bosna Hersek", "Bosnia"),
    p(42.43, 18.77, "Kotor"), p(42.70, 19.40, "Karadağ", "Montenegro"), p(46.06, 14.51, "Ljubljana"), p(46.36, 14.09, "Bled"),
    p(41.33, 19.82, "Tiran", "Tirana"), p(41.20, 20.20, "Arnavutluk", "Albania"), p(41.12, 20.80, "Ohri", "Ohrid"),
    p(41.99, 21.43, "Üsküp", "Skopje"), p(44.43, 26.10, "Bükreş", "Bucharest", "București"), p(45.90, 25.00, "Romanya", "Romania"),
    p(42.70, 23.32, "Sofya", "Sofia"), p(42.70, 25.50, "Bulgaristan", "Bulgaria"),
    p(41.72, 44.79, "Tiflis", "Tbilisi"), p(41.64, 41.64, "Batum", "Batumi"), p(42.20, 43.50, "Gürcistan", "Georgia"),
    p(40.41, 49.87, "Bakü", "Baku", "Bakı"), p(40.30, 47.70, "Azerbaycan", "Azerbaijan", "Azərbaycan"), p(40.18, 44.51, "Erivan", "Yerevan"),
    p(55.76, 37.62, "Moskova", "Moscow", "Moskau", "Москва"), p(59.93, 30.34, "St Petersburg", "Saint Petersburg", "Sankt Petersburg", "Санкт-Петербург"),
    p(56.00, 40.00, "Rusya", "Russia", "Россия"), p(50.45, 30.52, "Kiev", "Kyiv"), p(48.40, 31.20, "Ukrayna", "Ukraine"),
    p(56.95, 24.11, "Riga"), p(59.44, 24.75, "Tallinn"), p(54.69, 25.28, "Vilnius"),
    p(35.90, 14.51, "Malta", "Valletta"), p(35.13, 33.43, "Kıbrıs", "Cyprus"), p(35.34, 33.32, "Girne", "Kyrenia"), p(43.74, 7.42, "Monako", "Monaco"),
    // Middle East and Africa
    p(25.20, 55.27, "Dubai", "Dubái", "Dubaï", "Dubay", "Дубай", "دبي", "दुबई", "迪拜", "ドバイ", "두바이"),
    p(24.45, 54.38, "Abu Dabi", "Abu Dhabi"), p(24.00, 54.00, "BAE", "UAE", "Birleşik Arap Emirlikleri", "United Arab Emirates"),
    p(25.29, 51.53, "Doha"), p(25.30, 51.20, "Katar", "Qatar"), p(21.39, 39.86, "Mekke", "Mecca", "Makkah", "مكة"), p(24.47, 39.61, "Medine", "Medina"),
    p(24.00, 45.00, "Suudi Arabistan", "Saudi Arabia"), p(31.77, 35.21, "Kudüs", "Jerusalem"), p(31.95, 35.93, "Amman"), p(30.33, 35.44, "Petra"),
    p(31.00, 36.00, "Ürdün", "Jordan"), p(33.89, 35.50, "Beyrut", "Beirut"), p(33.90, 35.90, "Lübnan", "Lebanon"),
    p(30.04, 31.24, "Kahire", "Cairo", "القاهرة"), p(25.69, 32.64, "Luksor", "Luxor"), p(27.92, 34.33, "Şarm El-Şeyh", "Sharm el-Sheikh"),
    p(27.26, 33.81, "Hurghada"), p(26.80, 30.80, "Mısır", "Egypt"),
    p(31.63, -8.00, "Marakeş", "Marrakech", "Marrakesh"), p(33.57, -7.59, "Kazablanka", "Casablanca"), p(34.03, -5.00, "Fes", "Fez"),
    p(31.80, -7.10, "Fas", "Morocco", "Maroc"), p(36.81, 10.18, "Tunus", "Tunis"), p(34.00, 9.50, "Tunisia"),
    p(-33.92, 18.42, "Cape Town", "Cape Town"), p(-26.20, 28.05, "Johannesburg"), p(-30.60, 22.90, "Güney Afrika", "South Africa"),
    p(-1.29, 36.82, "Nairobi"), p(0.00, 37.90, "Kenya"), p(-6.17, 39.20, "Zanzibar"), p(-6.40, 34.90, "Tanzanya", "Tanzania"),
    p(35.69, 51.39, "Tahran", "Tehran"), p(32.65, 51.67, "İsfahan", "Isfahan"), p(32.40, 53.70, "İran", "Iran"),
    p(23.59, 58.41, "Maskat", "Muscat"), p(21.50, 55.90, "Umman", "Oman"),
    // Asia
    p(35.68, 139.69, "Tokyo", "Tokio", "Tóquio", "Токио", "طوكيو", "टोक्यो", "东京", "東京", "도쿄"),
    p(35.01, 135.77, "Kyoto", "Kioto"), p(34.69, 135.50, "Osaka"), p(36.20, 138.30, "Japonya", "Japan", "Japón", "Japon"),
    p(37.57, 126.98, "Seul", "Seoul", "Séoul", "서울"), p(35.18, 129.08, "Busan"), p(36.50, 127.90, "Güney Kore", "South Korea", "Kore", "Korea"),
    p(39.90, 116.41, "Pekin", "Beijing", "Peking"), p(31.23, 121.47, "Şanghay", "Shanghai"), p(22.32, 114.17, "Hong Kong"),
    p(25.03, 121.57, "Taipei"), p(35.90, 104.20, "Çin", "China"),
    p(13.76, 100.50, "Bangkok"), p(7.88, 98.39, "Phuket", "Puket"), p(18.79, 98.98, "Chiang Mai"), p(15.90, 100.90, "Tayland", "Thailand"),
    p(1.35, 103.82, "Singapur", "Singapore"), p(3.14, 101.69, "Kuala Lumpur"), p(4.20, 102.00, "Malezya", "Malaysia"),
    p(-8.41, 115.19, "Bali"), p(-6.21, 106.85, "Cakarta", "Jakarta"), p(-2.50, 118.00, "Endonezya", "Indonesia"),
    p(21.03, 105.85, "Hanoi"), p(10.82, 106.63, "Ho Chi Minh", "Saigon"), p(16.00, 107.80, "Vietnam"),
    p(14.60, 120.98, "Manila"), p(12.90, 121.80, "Filipinler", "Philippines"),
    p(28.61, 77.21, "Delhi", "Yeni Delhi", "New Delhi"), p(19.08, 72.88, "Mumbai", "Bombay"), p(15.30, 74.12, "Goa"), p(27.18, 78.01, "Agra", "Tac Mahal", "Taj Mahal"),
    p(22.00, 79.00, "Hindistan", "India"), p(27.72, 85.32, "Katmandu", "Kathmandu"), p(28.40, 84.10, "Nepal"),
    p(4.18, 73.51, "Maldivler", "Maldives", "Male"), p(7.90, 80.80, "Sri Lanka"),
    p(39.65, 66.96, "Semerkant", "Samarkand"), p(41.30, 69.24, "Taşkent", "Tashkent"), p(41.40, 64.60, "Özbekistan", "Uzbekistan"),
    p(43.24, 76.89, "Almatı", "Almaty"), p(51.17, 71.45, "Astana"), p(48.00, 67.00, "Kazakistan", "Kazakhstan"), p(42.87, 74.59, "Bişkek", "Bishkek"),
    // Americas and Oceania
    p(40.71, -74.01, "New York", "NYC", "Nueva York", "Нью-Йорк", "纽约", "ニューヨーク", "뉴욕"),
    p(34.05, -118.24, "Los Angeles"), p(37.77, -122.42, "San Francisco"), p(36.17, -115.14, "Las Vegas"), p(25.76, -80.19, "Miami"),
    p(41.88, -87.63, "Chicago"), p(38.91, -77.04, "Washington"), p(42.36, -71.06, "Boston"), p(28.54, -81.38, "Orlando"),
    p(21.31, -157.86, "Hawaii", "Hawai", "Honolulu"), p(39.80, -98.60, "ABD", "USA", "United States", "Amerika", "America"),
    p(43.65, -79.38, "Toronto"), p(49.28, -123.12, "Vancouver"), p(45.50, -73.57, "Montreal", "Montréal"), p(56.10, -106.30, "Kanada", "Canada"),
    p(19.43, -99.13, "Mexico City", "Meksiko"), p(21.16, -86.85, "Cancun", "Cancún"), p(23.60, -102.60, "Meksika", "Mexico", "México"),
    p(23.11, -82.37, "Havana", "Havana", "La Habana"), p(21.50, -79.00, "Küba", "Cuba"),
    p(-22.91, -43.17, "Rio de Janeiro", "Rio"), p(-23.55, -46.63, "São Paulo", "Sao Paulo"), p(-14.20, -51.90, "Brezilya", "Brazil", "Brasil"),
    p(-34.60, -58.38, "Buenos Aires"), p(-38.40, -63.60, "Arjantin", "Argentina"), p(-49.30, -72.90, "Patagonya", "Patagonia"),
    p(-12.05, -77.04, "Lima"), p(-13.53, -71.97, "Cusco", "Cuzco"), p(-13.16, -72.55, "Machu Picchu"), p(-9.20, -75.00, "Peru"),
    p(-33.45, -70.67, "Santiago"), p(-35.70, -71.50, "Şili", "Chile"), p(4.71, -74.07, "Bogota", "Bogotá"), p(10.39, -75.48, "Cartagena"),
    p(4.60, -74.30, "Kolombiya", "Colombia"),
    p(-33.87, 151.21, "Sidney", "Sydney"), p(-37.81, 144.96, "Melbourne"), p(-25.30, 133.80, "Avustralya", "Australia"),
    p(-36.85, 174.76, "Auckland"), p(-45.03, 168.66, "Queenstown"), p(-41.50, 172.80, "Yeni Zelanda", "New Zealand"),
).flatten().toMap()

// Lower case without accents, so "İSTANBUL", "istanbul" and "Istanbul" are one key.
internal fun normalize(text: String): String =
    Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('ı', 'i')
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ")

// The first known name inside what was typed, longest run of words first, so "Roma, İtalya" is Rome,
// "New York City" is New York and "İstanbul'a" is Istanbul.
fun findPlace(text: String): Place? {
    val words = normalize(text).split(' ').filter { it.isNotEmpty() }
    for (length in words.size downTo 1) {
        for (start in 0..words.size - length) {
            PLACES[words.subList(start, start + length).joinToString(" ")]?.let { return it }
        }
    }
    return null
}
