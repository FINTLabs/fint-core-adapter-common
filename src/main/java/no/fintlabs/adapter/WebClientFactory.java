package no.fintlabs.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class WebClientFactory {

    private final WebClient.Builder builder;
    private final ClientHttpConnector clientHttpConnector;
    private final ReactiveClientRegistrationRepository clientRegistrationRepository;
    private final ReactiveOAuth2AuthorizedClientService authorizedClientService;

    public WebClient webClient(AdapterInstanceProperties props) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();

        ReactiveOAuth2AuthorizedClientManager authorizedClientManager = authorizedClientManager(props);
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Client = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Client.setDefaultClientRegistrationId(props.getRegistrationId());

        return builder
                .clientConnector(clientHttpConnector)
                .exchangeStrategies(exchangeStrategies)
                .filter(oauth2Client)
                .baseUrl(props.getBaseUrl())
                .build();
    }

    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(AdapterInstanceProperties props) {

        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .password()
                .refreshToken()
                .build();

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService
        );

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        authorizedClientManager.setContextAttributesMapper(oAuth2AuthorizeRequest -> Mono.just(Map.of(
                OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, props.getUsername(),
                OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, props.getPassword()
        )));

        return authorizedClientManager;
    }
}
