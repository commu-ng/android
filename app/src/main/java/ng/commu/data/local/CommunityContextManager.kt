package ng.commu.data.local

import ng.commu.data.model.Community
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityContextManager @Inject constructor() {
    companion object {
        private const val CONSOLE_DOMAIN = "commu.ng"
    }

    @Volatile
    var currentCommunity: Community? = null
        private set

    fun setCommunity(community: Community?) {
        currentCommunity = community
    }

    fun buildOriginHeader(): String? {
        val community = currentCommunity ?: return null

        // Prefer custom domain if available and verified
        if (!community.customDomain.isNullOrEmpty() &&
            !community.domainVerified.isNullOrEmpty()) {
            return "https://${community.customDomain}"
        }

        // Otherwise use subdomain format
        return "https://${community.slug}.$CONSOLE_DOMAIN"
    }
}
