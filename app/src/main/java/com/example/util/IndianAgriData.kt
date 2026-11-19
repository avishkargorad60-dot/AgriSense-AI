package com.example.util

data class DistrictLocation(
  val state: String,
  val district: String,
  val latitude: Double,
  val longitude: Double,
)

enum class IndianSoilType(
  val labelEn: String,
  val labelHi: String,
  val labelMr: String,
  val idealPhRange: ClosedFloatingPointRange<Double>,
  val waterRetention: String,
  val characteristics: String,
) {
  BLACK(
    "Black Soil (Regur)",
    "काली मिट्टी (रेगुर)",
    "काळी माती (रेगूर)",
    6.5..8.2,
    "High moisture retention; swells when wet and cracks when dry.",
    "Rich in lime, iron, magnesia, and alumina; ideal for cotton, soybean, and pulses."
  ),
  ALLUVIAL(
    "Alluvial Soil",
    "जलोढ़ मिट्टी",
    "गाळाची माती",
    6.2..7.8,
    "Moderate to good drainage; deep fertile profile.",
    "Rich in potash and lime; excellent for wheat, rice, sugarcane, and vegetables."
  ),
  RED(
    "Red Soil",
    "लाल मिट्टी",
    "तांबडी माती",
    5.5..7.0,
    "Low to moderate water holding; drains rapidly.",
    "Porous sandy-loam texture rich in iron; suits groundnut, millets, pulses, and turmeric."
  ),
  LATERITE(
    "Laterite Soil",
    "लैटेराइट मिट्टी",
    "जांभी माती",
    5.0..6.5,
    "Fast draining; benefits strongly from organic mulch.",
    "Acidic soil common in Konkan/Western Ghats; great for mango, cashew, banana, and rice."
  ),
  ARID(
    "Arid / Desert Soil",
    "मरुस्थलीय मिट्टी",
    "वाळवंटी / कोरडवाहू माती",
    7.2..8.6,
    "Very low moisture retention; drip irrigation recommended.",
    "Sandy and saline-prone; suitable for bajra, guar, cumin, and mustard."
  ),
  MOUNTAIN(
    "Mountain / Forest Soil",
    "पर्वतीय मिट्टी",
    "डोंगराळ माती",
    5.2..6.8,
    "Moderate drainage with rich humus in valleys.",
    "High organic matter on slopes; suitable for maize, ginger, tea, and temperate fruits."
  );

  fun localizedLabel(lang: AppLanguage): String = when (lang) {
    AppLanguage.EN -> labelEn
    AppLanguage.HI -> labelHi
    AppLanguage.MR -> labelMr
  }
}

enum class IndianSeason(val labelEn: String, val labelHi: String, val labelMr: String, val months: String) {
  KHARIF("Kharif (Monsoon)", "खरीफ (मानसून)", "खरीप (पावसाळा)", "Jun – Oct"),
  RABI("Rabi (Winter)", "रबी (सर्दी)", "रब्बी (हिवाळा)", "Nov – Mar"),
  ZAID("Zaid (Summer)", "जायद (गर्मी)", "उन्हाळी (झैद)", "Mar – Jun");

  fun localizedLabel(lang: AppLanguage): String = when (lang) {
    AppLanguage.EN -> labelEn
    AppLanguage.HI -> labelHi
    AppLanguage.MR -> labelMr
  }
}

enum class IrrigationType(val labelEn: String, val labelHi: String, val labelMr: String) {
  RAINFED("Rainfed Only", "वर्षा आधारित", "फक्त पावसावर आधारित"),
  CANAL("Canal / River", "नहर सिंचाई", "कालवा / नदी सिंचन"),
  BOREWELL("Borewell / Open Well", "बोरवेल / कुआं", "बोअरवेल / विहीर"),
  DRIP("Drip / Micro-Irrigation", "ड्रिप / टपक सिंचाई", "ठिबक सिंचन");

  fun localizedLabel(lang: AppLanguage): String = when (lang) {
    AppLanguage.EN -> labelEn
    AppLanguage.HI -> labelHi
    AppLanguage.MR -> labelMr
  }
}

data class IndianCropProfile(
  val name: String,
  val hindiName: String,
  val marathiName: String,
  val seasons: List<IndianSeason>,
  val compatibleSoils: List<IndianSoilType>,
  val tempRangeC: ClosedFloatingPointRange<Double>,
  val rainfallRangeMm: ClosedFloatingPointRange<Double>,
  val phRange: ClosedFloatingPointRange<Double>,
  val waterRequirement: String,
  val requiresAssuredIrrigation: Boolean,
  val durationDays: String,
  val guidance: String,
)

data class KnowledgeArticle(
  val id: String,
  val category: String,
  val title: String,
  val summary: String,
  val content: String,
  val isGovtScheme: Boolean = false,
  val officialUrl: String? = null,
)

object IndianAgriData {
  val districtLocations = listOf(
    DistrictLocation("Maharashtra", "Pune", 18.5204, 73.8567),
    DistrictLocation("Maharashtra", "Nashik", 19.9975, 73.7898),
    DistrictLocation("Maharashtra", "Nagpur", 21.1458, 79.0882),
    DistrictLocation("Maharashtra", "Kolhapur", 16.7050, 74.2433),
    DistrictLocation("Maharashtra", "Chhatrapati Sambhajinagar", 19.8762, 75.3433),
    DistrictLocation("Maharashtra", "Satara", 17.6805, 74.0183),
    DistrictLocation("Punjab", "Ludhiana", 30.9010, 75.8573),
    DistrictLocation("Punjab", "Amritsar", 31.6340, 74.8723),
    DistrictLocation("Haryana", "Karnal", 29.6857, 76.9905),
    DistrictLocation("Uttar Pradesh", "Lucknow", 26.8467, 80.9462),
    DistrictLocation("Uttar Pradesh", "Varanasi", 25.3176, 82.9739),
    DistrictLocation("Madhya Pradesh", "Indore", 22.7196, 75.8577),
    DistrictLocation("Madhya Pradesh", "Bhopal", 23.2599, 77.4126),
    DistrictLocation("Gujarat", "Rajkot", 22.3039, 70.8022),
    DistrictLocation("Gujarat", "Anand", 22.5645, 72.9289),
    DistrictLocation("Rajasthan", "Jaipur", 26.9124, 75.7873),
    DistrictLocation("Rajasthan", "Jodhpur", 26.2389, 73.0243),
    DistrictLocation("Karnataka", "Bengaluru Rural", 13.2257, 77.5750),
    DistrictLocation("Karnataka", "Belagavi", 15.8497, 74.4977),
    DistrictLocation("Telangana", "Warangal", 17.9689, 79.5941),
    DistrictLocation("Andhra Pradesh", "Guntur", 16.3067, 80.4365),
    DistrictLocation("Tamil Nadu", "Thanjavur", 10.7870, 79.1378),
    DistrictLocation("Bihar", "Patna", 25.5941, 85.1376),
    DistrictLocation("West Bengal", "Bardhaman", 23.2324, 87.8615),
    DistrictLocation("Odisha", "Cuttack", 20.4625, 85.8830),
  )

  val states: List<String> = districtLocations.map { it.state }.distinct().sorted()

  fun districtsForState(state: String): List<DistrictLocation> =
    districtLocations.filter { it.state.equals(state, ignoreCase = true) }

  fun findCoordinates(state: String, district: String): Pair<Double, Double> {
    val exact = districtLocations.firstOrNull {
      it.state.equals(state, ignoreCase = true) && it.district.equals(district, ignoreCase = true)
    }
    val byState = districtLocations.firstOrNull { it.state.equals(state, ignoreCase = true) }
    val match = exact ?: byState ?: districtLocations.first()
    return match.latitude to match.longitude
  }

  val crops = listOf(
    IndianCropProfile(
      "Soybean", "सोयाबीन", "सोयाबीन",
      listOf(IndianSeason.KHARIF),
      listOf(IndianSoilType.BLACK, IndianSoilType.ALLUVIAL, IndianSoilType.RED),
      22.0..34.0, 550.0..1100.0, 6.0..7.5,
      "Moderate (450–650 mm); critical at flowering and pod-filling.", false, "95–110 days",
      "Use Rhizobium seed treatment; maintain 45 cm row spacing and ensure drainage during heavy rain."
    ),
    IndianCropProfile(
      "Cotton (Bt / Desi)", "कपास", "कापूस",
      listOf(IndianSeason.KHARIF),
      listOf(IndianSoilType.BLACK, IndianSoilType.ALLUVIAL),
      24.0..38.0, 500.0..1000.0, 6.5..8.2,
      "Moderate (600–850 mm); avoid waterlogging in black soil.", false, "150–180 days",
      "Adopt pheromone traps for pink bollworm monitoring; split nitrogen application at 30 and 60 DAS."
    ),
    IndianCropProfile(
      "Wheat", "गेहूं", "गहू",
      listOf(IndianSeason.RABI),
      listOf(IndianSoilType.ALLUVIAL, IndianSoilType.BLACK),
      12.0..26.0, 150.0..600.0, 6.2..7.8,
      "Moderate-High (4–6 irrigations at CRI, tillering, flowering, milking).", true, "115–135 days",
      "Sow in November for optimal crown root initiation (CRI irrigation at 21 days after sowing)."
    ),
    IndianCropProfile(
      "Paddy (Rice)", "धान", "भात / तांदूळ",
      listOf(IndianSeason.KHARIF, IndianSeason.ZAID),
      listOf(IndianSoilType.ALLUVIAL, IndianSoilType.LATERITE, IndianSoilType.BLACK),
      22.0..36.0, 900.0..2200.0, 5.5..7.2,
      "High (1000–1400 mm); Alternate Wetting and Drying saves 25% water.", true, "120–145 days",
      "Transplant 21-day seedlings; adopt SRI spacing or AWD irrigation to reduce methane and save water."
    ),
    IndianCropProfile(
      "Sugarcane", "गन्ना", "ऊस",
      listOf(IndianSeason.KHARIF, IndianSeason.RABI),
      listOf(IndianSoilType.BLACK, IndianSoilType.ALLUVIAL, IndianSoilType.RED),
      20.0..36.0, 750.0..1800.0, 6.0..8.0,
      "High (1500–2200 mm); drip fertigation highly recommended.", true, "300–365 days",
      "Use trash mulching between ridges to conserve soil moisture and control weeds naturally."
    ),
    IndianCropProfile(
      "Chickpea (Chana)", "चना", "हरभरा",
      listOf(IndianSeason.RABI),
      listOf(IndianSoilType.BLACK, IndianSoilType.ALLUVIAL, IndianSoilType.RED),
      14.0..28.0, 200.0..600.0, 6.0..7.8,
      "Low (1–2 light irrigations at pre-flowering and pod development).", false, "100–120 days",
      "Ideal residual-moisture Rabi pulse; avoid excess irrigation which promotes wilt."
    ),
    IndianCropProfile(
      "Tur / Pigeon Pea", "अरहर / तुअर", "तूर",
      listOf(IndianSeason.KHARIF),
      listOf(IndianSoilType.BLACK, IndianSoilType.RED, IndianSoilType.ALLUVIAL),
      22.0..35.0, 500.0..1000.0, 6.0..7.8,
      "Low-Moderate; deep taproot tolerates dry spells well.", false, "150–175 days",
      "Intercrop with soybean or green gram; ridge-and-furrow planting prevents Phytophthora blight."
    ),
    IndianCropProfile(
      "Maize", "मक्का", "मका",
      listOf(IndianSeason.KHARIF, IndianSeason.RABI, IndianSeason.ZAID),
      listOf(IndianSoilType.ALLUVIAL, IndianSoilType.RED, IndianSoilType.MOUNTAIN, IndianSoilType.BLACK),
      18.0..34.0, 450.0..1000.0, 5.8..7.5,
      "Moderate; sensitive to both drought and standing water at tasseling.", false, "95–115 days",
      "Keep fields weed-free for the first 35 days and monitor whorls early for Fall Armyworm."
    ),
    IndianCropProfile(
      "Bajra (Pearl Millet)", "बाजरा", "बाजरी",
      listOf(IndianSeason.KHARIF, IndianSeason.ZAID),
      listOf(IndianSoilType.ARID, IndianSoilType.RED, IndianSoilType.BLACK),
      25.0..40.0, 250.0..650.0, 6.5..8.4,
      "Low (250–400 mm); highly drought resilient.", false, "80–95 days",
      "Best choice for low-rainfall zones and sandy/arid soils; respond well to zinc micro-nutrient."
    ),
    IndianCropProfile(
      "Jowar (Sorghum)", "ज्वार", "ज्वारी",
      listOf(IndianSeason.KHARIF, IndianSeason.RABI),
      listOf(IndianSoilType.BLACK, IndianSoilType.RED, IndianSoilType.ARID),
      20.0..36.0, 350.0..800.0, 6.0..8.2,
      "Low (300–500 mm); great dryland cereal + fodder crop.", false, "105–125 days",
      "Rabi jowar thrives on stored moisture in medium-deep black soils with wide spacing."
    ),
    IndianCropProfile(
      "Groundnut", "मूंगफली", "भुईमूग",
      listOf(IndianSeason.KHARIF, IndianSeason.ZAID),
      listOf(IndianSoilType.RED, IndianSoilType.ALLUVIAL, IndianSoilType.ARID),
      24.0..35.0, 450.0..950.0, 5.8..7.2,
      "Moderate; apply gypsum (400 kg/ha) at pegging stage.", false, "105–125 days",
      "Loose sandy loam or red soil allows easy peg penetration and clean pod harvesting."
    ),
    IndianCropProfile(
      "Mustard (Sarson)", "सरसों", "मोहरी",
      listOf(IndianSeason.RABI),
      listOf(IndianSoilType.ALLUVIAL, IndianSoilType.ARID),
      12.0..26.0, 150.0..500.0, 6.0..8.2,
      "Low (1–2 irrigations at branching and siliqua formation).", false, "110–130 days",
      "Apply sulphur-rich fertilizer for higher oil percentage; monitor aphids during cloudy weather."
    ),
    IndianCropProfile(
      "Onion", "प्याज", "कांदा",
      listOf(IndianSeason.KHARIF, IndianSeason.RABI, IndianSeason.ZAID),
      listOf(IndianSoilType.BLACK, IndianSoilType.ALLUVIAL, IndianSoilType.RED),
      16.0..32.0, 350.0..800.0, 6.0..7.5,
      "Moderate-Frequent light irrigations; stop 15 days before harvest.", true, "110–130 days",
      "Raised-bed cultivation with drip irrigation reduces purple blotch and improves bulb grading."
    ),
    IndianCropProfile(
      "Tomato", "टमाटर", "टोमॅटो",
      listOf(IndianSeason.KHARIF, IndianSeason.RABI, IndianSeason.ZAID),
      listOf(IndianSoilType.RED, IndianSoilType.ALLUVIAL, IndianSoilType.BLACK),
      18.0..31.0, 400.0..900.0, 6.0..7.2,
      "Moderate-High; consistent drip moisture prevents blossom-end rot.", true, "90–120 days",
      "Stake indeterminate hybrids, mulch beds, and watch for early blight after humid spells."
    ),
    IndianCropProfile(
      "Moong (Green Gram)", "मूंग", "मूग",
      listOf(IndianSeason.KHARIF, IndianSeason.ZAID),
      listOf(IndianSoilType.ALLUVIAL, IndianSoilType.RED, IndianSoilType.BLACK),
      24.0..38.0, 300.0..750.0, 6.2..7.8,
      "Low; quick 65-day catch crop that fixes atmospheric nitrogen.", false, "60–70 days",
      "Excellent summer/Zaid or Kharif intercrop to enrich soil organic nitrogen between cereals."
    ),
    IndianCropProfile(
      "Turmeric", "हल्दी", "हळद",
      listOf(IndianSeason.KHARIF),
      listOf(IndianSoilType.RED, IndianSoilType.LATERITE, IndianSoilType.BLACK),
      20.0..34.0, 900.0..1800.0, 5.5..7.0,
      "Moderate-High; raised beds with organic mulch essential.", true, "210–250 days",
      "Treat mother rhizomes with Trichoderma before planting on broad ridges."
    ),
    IndianCropProfile(
      "Banana", "केला", "केळी",
      listOf(IndianSeason.KHARIF, IndianSeason.RABI),
      listOf(IndianSoilType.ALLUVIAL, IndianSoilType.BLACK, IndianSoilType.LATERITE),
      20.0..36.0, 800.0..2000.0, 6.0..7.5,
      "High; regular drip fertigation gives uniform bunch weight.", true, "300–340 days",
      "Use tissue-culture G9 saplings, windbreak borders, and potassium-rich fertigation."
    ),
  )

  val knowledgeArticles = listOf(
    KnowledgeArticle(
      id = "scheme_pm_kisan",
      category = "Government Schemes",
      title = "PM-KISAN Samman Nidhi Yojana",
      summary = "Direct income support of ₹6,000/year in three equal installments for eligible landholding farmer families.",
      content = "Under Pradhan Mantri Kisan Samman Nidhi (PM-KISAN), eligible farmer families receive ₹2,000 every four months directly into their Aadhaar-seeded bank accounts. Ensure e-KYC and land seeding are completed via CSC or the official portal.",
      isGovtScheme = true,
      officialUrl = "https://pmkisan.gov.in/"
    ),
    KnowledgeArticle(
      id = "scheme_pmfby",
      category = "Government Schemes",
      title = "Pradhan Mantri Fasal Bima Yojana (PMFBY)",
      summary = "Crop insurance protection against natural calamities, unseasonal rain, pests, and post-harvest losses.",
      content = "PMFBY limits farmer premium to 2% for Kharif crops, 1.5% for Rabi crops, and 5% for annual commercial/horticultural crops. Report localized hailstorms or inundation within 72 hours via the Crop Insurance app or bank.",
      isGovtScheme = true,
      officialUrl = "https://pmfby.gov.in/"
    ),
    KnowledgeArticle(
      id = "scheme_shc",
      category = "Government Schemes",
      title = "Soil Health Card (SHC) Scheme",
      summary = "Free laboratory soil testing every 2 years for pH, EC, organic carbon, NPK, and micronutrients.",
      content = "Collect composite soil samples from 15 cm depth across 5–6 spots in a zig-zag pattern, dry in shade, and submit to your nearest district Soil Testing Lab for balanced fertilizer recommendations.",
      isGovtScheme = true,
      officialUrl = "https://soilhealth.dac.gov.in/"
    ),
    KnowledgeArticle(
      id = "scheme_enam",
      category = "Government Schemes",
      title = "e-NAM (National Agriculture Market)",
      summary = "Pan-India electronic trading portal linking APMC mandis for transparent price discovery.",
      content = "Farmers can register on e-NAM using Aadhaar and bank details to check live mandi arrivals, assaying grades, and receive digital payments directly after auction.",
      isGovtScheme = true,
      officialUrl = "https://www.enam.gov.in/"
    ),
    KnowledgeArticle(
      id = "soil_organic",
      category = "Soil Management",
      title = "Improving Soil Organic Carbon & Structure",
      summary = "Practical ways to rebuild humus, water retention, and microbial life in Indian soils.",
      content = "1. Apply 4–5 tonnes/acre of well-decomposed FYM or vermicompost before monsoon.\n2. Practice green manuring with Dhaincha or Sunhemp and incorporate at 45 days.\n3. Avoid burning crop stubble; shred and decompose with bio-decomposer.\n4. Rotate cereals with legumes (Soybean, Tur, Moong, Chana) to fix natural nitrogen."
    ),
    KnowledgeArticle(
      id = "fertilizer_npk",
      category = "Fertilizer Guide",
      title = "Balanced NPK & Micronutrient Application",
      summary = "Timing basal doses, split nitrogen top-dressing, and zinc/boron correction.",
      content = "Apply 100% Phosphorus (DAP/SSP) and Potash (MOP) as basal dose near the root zone at sowing. Split Nitrogen (Urea) into 2–3 doses coinciding with active vegetative and flowering stages. Use neem-coated urea and avoid broadcasting before heavy rain."
    ),
    KnowledgeArticle(
      id = "pest_ipm",
      category = "Pest & Disease IPM",
      title = "Integrated Pest Management (IPM) Basics",
      summary = "Monitor pests early using traps and botanical sprays before resorting to chemicals.",
      content = "Install 5–8 pheromone traps and 10 yellow sticky traps per acre. Conserve ladybird beetles and Trichogramma wasps. Use Neem oil (1500 ppm Azadirachtin @ 3 ml/L) as a first-line preventive spray against sucking pests."
    ),
    KnowledgeArticle(
      id = "irrigation_drip",
      category = "Irrigation Methods",
      title = "Drip, Sprinkler & Mulching Conservation",
      summary = "Save 35–55% irrigation water while boosting fertilizer uptake efficiency.",
      content = "Drip irrigation delivers water directly to the root zone, cutting evaporation and weed growth. Combine with straw or silver-black plastic mulching to reduce soil temperature spikes and cut irrigation frequency by up to 30%."
    ),
    KnowledgeArticle(
      id = "calendar_seasons",
      category = "Seasonal Calendar",
      title = "Indian Cropping Seasons: Kharif, Rabi & Zaid",
      summary = "Key sowing and harvesting windows across India's three agricultural cycles.",
      content = "• Kharif (Jun–Oct): Monsoon-fed crops — Paddy, Soybean, Cotton, Maize, Tur, Bajra, Groundnut.\n• Rabi (Nov–Mar): Winter crops — Wheat, Chickpea, Mustard, Rabi Jowar, Onion.\n• Zaid (Mar–Jun): Summer irrigated crops — Moong, Watermelon, Cucumber, Fodder Maize."
    ),
  )
}
