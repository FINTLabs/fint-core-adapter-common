package no.fintlabs.adapter.config

import io.netty.channel.ChannelOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration


@Configuration
class OAuthConfiguration(
    private val props: AdapterProperties,
) {

    @Bean
    @Primary
    fun webClient(
        authorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
        clientHttpConnector: ClientHttpConnector
    ): WebClient = WebClient.builder()
        .clientConnector(clientHttpConnector)
        .exchangeStrategies(createExchangeStrategies())
        .filter(createExchangeFilterFunction(authorizedClientManager))
        .baseUrl(props.baseUrl)
        .build()

    @Bean
    fun clientHttpConnector(): ClientHttpConnector {
        return ReactorClientHttpConnector(
            HttpClient.create(
                ConnectionProvider
                    .builder("laidback")
                    .maxConnections(25)
                    .pendingAcquireMaxCount(-1)
                    .pendingAcquireTimeout(Duration.ofMinutes(15))
                    .maxLifeTime(Duration.ofMinutes(30))
                    .maxIdleTime(Duration.ofMinutes(5))
                    .build()
            )
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 900000)
                .responseTimeout(Duration.ofMinutes(10))
        )
    }

    private fun createExchangeStrategies() = ExchangeStrategies.builder()
        .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
        .build();

    private fun createExchangeFilterFunction(authorizedClientManager: ReactiveOAuth2AuthorizedClientManager) =
        ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
            .also { it.setDefaultClientRegistrationId(props.registrationId) }

    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: ReactiveClientRegistrationRepository,
        authorizedClientService: ReactiveOAuth2AuthorizedClientService,
    ): ReactiveOAuth2AuthorizedClientManager =
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService,
        ).apply {
            setAuthorizedClientProvider(
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                    .provider(passwordProvider())
                    .refreshToken()
                    .build()
            )
        }

    private fun passwordProvider() =
        PasswordReactiveOAuth2AuthorizedClientProvider(
            webClient = WebClient.builder().build(),
            username = props.username,
            password = props.password,
        )
}