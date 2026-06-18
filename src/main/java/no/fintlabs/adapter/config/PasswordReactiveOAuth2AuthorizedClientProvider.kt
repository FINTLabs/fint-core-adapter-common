package no.fintlabs.adapter.config

import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.Duration
import java.time.Instant

class PasswordReactiveOAuth2AuthorizedClientProvider(
    private val webClient: WebClient,
    private val username: String,
    private val password: String,
    private val clockSkew: Duration = Duration.ofSeconds(60),
    private val clock: Clock = Clock.systemUTC(),
) : ReactiveOAuth2AuthorizedClientProvider {

    override fun authorize(context: OAuth2AuthorizationContext): Mono<OAuth2AuthorizedClient> {
        val registration = context.clientRegistration
        val current: OAuth2AuthorizedClient? = context.authorizedClient

        if (current != null && !current.accessToken.isExpired()) return Mono.empty()
        if (current?.refreshToken != null) return Mono.empty()

        return webClient.post()
            .uri(registration.providerDetails.tokenUri)
            .headers { it.setBasicAuth(registration.clientId, registration.clientSecret) }
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(createForm(registration)))
            .exchangeToMono { response ->
                if (response.statusCode().is2xxSuccessful)
                    response.body(OAuth2BodyExtractors.oauth2AccessTokenResponse())
                else
                    response.createException().flatMap { Mono.error(it) }
            }
            .map { token ->
                OAuth2AuthorizedClient(registration, context.principal.name, token.accessToken, token.refreshToken)
            }
    }

    private fun createForm(registration: ClientRegistration) = LinkedMultiValueMap<String, String>().apply {
        add("grant_type", "password")
        add("username", username)
        add("password", password)
        registration.scopes?.takeIf { it.isNotEmpty() }?.let { add("scope", it.joinToString(" ")) }
    }

    private fun OAuth2AccessToken.isExpired(): Boolean {
        val expiresAt = expiresAt ?: return true
        return Instant.now(clock).isAfter(expiresAt.minus(clockSkew))
    }
}
