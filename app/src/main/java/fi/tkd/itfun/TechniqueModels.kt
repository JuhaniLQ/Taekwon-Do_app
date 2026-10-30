package fi.tkd.itfun

data class TKDItem(
    val techniqueCode: Int,
    val name: String,
    val koreanName: String,
    val category: TKDItemCategory,
    val pattern: List<String>,
    val beltLevel: Int,
    val videoKey: String,
    val localVideoPath: String?
)

enum class TKDItemCategory {
    DEFENSIVE,
    OFFENSIVE,
    KICKS,
    ALL_PATTERNS,
    STANCES,
    SPECIAL,
    CRUSHING,
    SPARRING
}
