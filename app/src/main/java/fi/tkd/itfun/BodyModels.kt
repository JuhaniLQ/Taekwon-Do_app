package fi.tkd.itfun

enum class BodyRegion { HEAD, TORSO, ARM, LEG }

data class BodyItem(
    val code: String,
    val region: BodyRegion,
    val canBeVitalPoint: Boolean,
    val nameEn: String,
    val nameKo: String
)
