package br.com.rinha.pagamentos.health;

import br.com.rinha.pagamentos.model.HealthCheckResponse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Objects;

@Component
public class ProcessorHealthMonitor implements MessageListener {

	private static final String HEALTH_STATUS_DEFAULT_KEY = "health:status:default";
	private static final String HEALTH_NOTIFICATION_CHANNEL = "health:notifications";
	private static final ChannelTopic NOTIFICATION_TOPIC = new ChannelTopic(HEALTH_NOTIFICATION_CHANNEL);

	private final WebClient webClient;
	private final ReactiveStringRedisTemplate reactiveRedisTemplate;
	private final String defaultHealthUrl;

	private volatile boolean isDefaultAvailable = false;

	public ProcessorHealthMonitor(
			WebClient.Builder webClientBuilder,
			@Qualifier("reactivePersistedRedisTemplate") ReactiveStringRedisTemplate reactiveRedisTemplate,
			@Value("${processor.default.health.url}") String defaultHealthUrl) {

		this.webClient = webClientBuilder.build();
		this.reactiveRedisTemplate = reactiveRedisTemplate;
		this.defaultHealthUrl = defaultHealthUrl;

		syncStateFromRedis().subscribe();
	}

	@Async("virtualThreadExecutor")
	@Scheduled(fixedRate = 5150)
	@SchedulerLock(name = "processorHealthCheckLock", lockAtMostFor = "4s", lockAtLeastFor = "4s")
	public void scheduleHealthCheck() {
		performHealthCheckAndNotify().subscribe();
	}

	public Mono<Void> performHealthCheckAndNotify() {
		return checkHealthAsync(defaultHealthUrl)
				.flatMap(isDefaultOk ->
						reactiveRedisTemplate.opsForValue().set(HEALTH_STATUS_DEFAULT_KEY, isDefaultOk ? "1" : "0")
				)
				.onErrorResume(e ->
						reactiveRedisTemplate.opsForValue().set(HEALTH_STATUS_DEFAULT_KEY, "0")
				)
				.then(reactiveRedisTemplate.convertAndSend(HEALTH_NOTIFICATION_CHANNEL, "updated"))
				.then();
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		syncStateFromRedis().subscribe();
	}

	private Mono<Void> syncStateFromRedis() {
		return reactiveRedisTemplate.opsForValue().get(HEALTH_STATUS_DEFAULT_KEY)
				.doOnSuccess(status -> {
					this.isDefaultAvailable = Objects.equals(status, "1");
				})
				.doOnError(e -> {
					this.isDefaultAvailable = false;
				})
				.switchIfEmpty(Mono.fromRunnable(() -> this.isDefaultAvailable = false))
				.then();
	}

	private Mono<Boolean> checkHealthAsync(String url) {
		return webClient.get()
				.uri(url)
				.retrieve()
				.bodyToMono(HealthCheckResponse.class)
				.map(response -> !response.isFailing())
				.onErrorReturn(false);
	}

	public ChannelTopic getTopic() {
		return NOTIFICATION_TOPIC;
	}

	public boolean isDefaultProcessorAvailable() {
		return isDefaultAvailable;
	}

}
