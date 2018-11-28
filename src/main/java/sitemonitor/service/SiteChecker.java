package sitemonitor.service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import sitemonitor.repository.Event;
import sitemonitor.repository.Site;


@Service
public class SiteChecker {
	public static final int REQUEST_TIMEOUT_SECONDS = 10;
	private Log logger = LogFactory.getLog(getClass());
	
	@PostConstruct
	public void init() {
	}
	
	@Async
	public Future<Event> handleSiteCheck(Site site) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("SiteChecker.handleSiteCheck() [" + site.getName() + "]");
		}

		long start = System.currentTimeMillis();
		String status = null;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("SiteChecker.handleSiteCheck() site:" + site.getName() + ", " + site.getAssertText());
			}
			SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
			TcpClient tcpClient = TcpClient.create().secure( t -> t.sslContext(sslContext) );
			HttpClient httpClient = HttpClient.from(tcpClient); 
			ClientHttpConnector httpConnector = new ReactorClientHttpConnector(httpClient);
			WebClient webClient = WebClient.builder().clientConnector(httpConnector).build();
			
			//WebClient webClient = WebClient.create(site.getUrl());
			Map<String,Object> responsemap = new HashMap<String,Object>();
			String response = webClient.get()
					.uri(site.getUrl())
					.exchange()
					.doOnSuccess(r -> responsemap.put("status", r.statusCode()))
					.block()
					.bodyToMono(String.class)
					.timeout(Duration.ofSeconds(60))
					.block();
			
			HttpStatus httpStatus = (HttpStatus) responsemap.get("status");
			status = httpStatus.name();
			
			if (logger.isDebugEnabled()) {
				logger.debug("SiteChecker.handleSiteCheck() site:" + site.getName() + ", httpStatus: " + httpStatus);
			}

			if (status == null || !"OK".equals(status)) {
				status = "FAIL status: " + status;
			} else {
				if (StringUtils.isNotEmpty(site.getAssertText())) {
					if (StringUtils.contains(response, site.getAssertText())) {
						status = "OK";
					} else {
						status = "FAIL";
					}
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("SiteChecker.handleSiteCheck() [" + site.getName() + "] Problem", e);
			}
			status = "FAIL " + e.getMessage();
		} finally {

		}

		site.setResponseTime(System.currentTimeMillis() - start);
		
		Event event = new Event();
		if (!status.equals(site.getStatus())) {
			event.setStatusChange("Y");
		}
		event.setSite(site);
		event.setDescription(site.getName() + " " + status);
		event.setEventTime(new Date());
		event.setState(status);
		event.setResponseTime(site.getResponseTime());

		site.setStatus(status);
		if ("OK".equals(status)) {
			site.setFailures(0);
		} else {
			site.setFailures(site.getFailures() + 1);
		}
		
		return new AsyncResult<Event>(event);
	}
}
