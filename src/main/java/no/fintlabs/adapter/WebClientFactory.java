package no.fintlabs.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Component
public class WebClientFactory {

    private final WebClient.Builder builder;
    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    private final ClientHttpConnector clientHttpConnector;

    public WebClient webClient(AdapterProperties props) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Client = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Client.setDefaultClientRegistrationId(props.getRegistrationId());

        return builder
                .clientConnector(clientHttpConnector)
                .exchangeStrategies(exchangeStrategies)
                .filter(oauth2Client)
                .baseUrl(props.getBaseUrl())
                .build();
    }
}
