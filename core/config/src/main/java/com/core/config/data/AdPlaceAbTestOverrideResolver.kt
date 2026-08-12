package com.core.config.data

import com.core.config.data.model.AdPlaceModel

/**
 * Replaces complete ad-place models selected for Remote Config A/B testing.
 *
 * Overrides are deliberately not merged field-by-field: an accepted override becomes the
 * complete source of truth for that place. Invalid or unavailable overrides leave the base
 * model untouched.
 */
internal object AdPlaceAbTestOverrideResolver {

    fun resolve(
        basePlaces: List<AdPlaceModel>,
        configuredPlaceNames: List<String>,
        overrideProvider: (String) -> AdPlaceModel?,
    ): List<AdPlaceModel> {
        if (basePlaces.isEmpty() || configuredPlaceNames.isEmpty()) return basePlaces

        val basePlaceNames = basePlaces.mapNotNull { it.adPlace }.toSet()
        val overridesByPlaceName = configuredPlaceNames
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .filter { it in basePlaceNames }
            .mapNotNull { expectedPlaceName ->
                overrideProvider(expectedPlaceName)
                    ?.takeIf { it.isValidCompleteOverrideFor(expectedPlaceName) }
                    ?.let { expectedPlaceName to it }
            }
            .toMap()

        if (overridesByPlaceName.isEmpty()) return basePlaces

        return basePlaces.map { basePlace ->
            overridesByPlaceName[basePlace.adPlace] ?: basePlace
        }
    }

    private fun AdPlaceModel.isValidCompleteOverrideFor(expectedPlaceName: String): Boolean {
        return adPlace == expectedPlaceName &&
            !adId.isNullOrBlank() &&
            !adType.isNullOrBlank() &&
            isEnable != null
    }
}
