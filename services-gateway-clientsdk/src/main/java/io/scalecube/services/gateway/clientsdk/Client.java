package io.scalecube.services.gateway.clientsdk;

import io.scalecube.services.gateway.clientsdk.exceptions.mappers.ClientErrorMapper;
import io.scalecube.services.gateway.clientsdk.http.HttpClientCodec;
import io.scalecube.services.gateway.clientsdk.http.HttpClientTransport;
import io.scalecube.services.gateway.clientsdk.rsocket.RSocketClientCodec;
import io.scalecube.services.gateway.clientsdk.rsocket.RSocketClientTransport;
import io.scalecube.services.gateway.clientsdk.websocket.WebsocketClientCodec;
import io.scalecube.services.gateway.clientsdk.websocket.WebsocketClientTransport;
import io.scalecube.services.methods.MethodInfo;
import io.scalecube.services.transport.api.DataCodec;
import io.scalecube.services.transport.api.HeadersCodec;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class Client {

  private final ClientTransport transport;
  private final ClientCodec codec;
  private final ClientErrorMapper errorMapper;

  private final ConcurrentHashMap<Class<?>, ? super Object> proxyMap = new ConcurrentHashMap<>();

  /**
   * Constructor for client.
   *
   * @param transport client transport
   * @param codec client message codec
   * @param errorMapper error mapper
   */
  private Client(ClientTransport transport, ClientCodec codec, ClientErrorMapper errorMapper) {
    this.transport = transport;
    this.codec = codec;
    this.errorMapper = errorMapper;
  }

  /**
   * Client on rsocket client transport.
   *
   * @param clientSettings client settings
   * @return client
   */
  public static Client onRSocket(ClientSettings clientSettings) {
    RSocketClientCodec clientCodec =
        new RSocketClientCodec(
            HeadersCodec.getInstance(clientSettings.contentType()),
            DataCodec.getInstance(clientSettings.contentType()));

    RSocketClientTransport clientTransport =
        new RSocketClientTransport(clientSettings, clientCodec, clientSettings.loopResources());

    return new Client(clientTransport, clientCodec, clientSettings.errorMapper());
  }

  /**
   * Client on websocket client transport.
   *
   * @param clientSettings client settings
   * @return client
   */
  public static Client onWebsocket(ClientSettings clientSettings) {
    WebsocketClientCodec clientCodec =
        new WebsocketClientCodec(DataCodec.getInstance(clientSettings.contentType()));

    WebsocketClientTransport clientTransport =
        new WebsocketClientTransport(clientSettings, clientCodec, clientSettings.loopResources());

    return new Client(clientTransport, clientCodec, clientSettings.errorMapper());
  }

  /**
   * Client on http client transport.
   *
   * @param clientSettings client settings
   * @return client
   */
  public static Client onHttp(ClientSettings clientSettings) {
    HttpClientCodec clientCodec =
        new HttpClientCodec(DataCodec.getInstance(clientSettings.contentType()));

    ClientTransport clientTransport =
        new HttpClientTransport(clientSettings, clientCodec, clientSettings.loopResources());

    return new Client(clientTransport, clientCodec, clientSettings.errorMapper());
  }

  /**
   * Close transport function.
   *
   * @return mono void
   */
  public Mono<Void> close() {
    return Mono.defer(transport::close);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Client{");
    sb.append("transport=").append(transport);
    sb.append(", codec=").append(codec);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Proxy creator function.
   *
   * @param serviceClazz service interface.
   * @param <T> type of service interface.
   * @return proxied service object.
   */
  public <T> T forService(Class<T> serviceClazz) {
    return forService(serviceClazz, errorMapper);
  }

  /**
   * Proxy creator function.
   *
   * @param serviceClazz service interface.
   * @param errorMapper error mapper.
   * @param <T> type of service interface.
   * @return proxied service object.
   */
  public <T> T forService(Class<T> serviceClazz, ClientErrorMapper errorMapper) {
    // noinspection unchecked
    return (T)
        proxyMap.computeIfAbsent(
            serviceClazz,
            (clazz) -> {
              Map<Method, MethodInfo> methods = Reflect.methodsInfo(serviceClazz);
              return Proxy.newProxyInstance(
                  serviceClazz.getClassLoader(),
                  new Class[] {serviceClazz},
                  new RemoteInvocationHandler(transport, methods, codec, errorMapper));
            });
  }

  /**
   * Request with mono response as response.
   *
   * @param clientMessage client request message.
   * @return mono response
   */
  public Mono<ClientMessage> requestResponse(ClientMessage clientMessage) {
    return transport.requestResponse(clientMessage);
  }

  /**
   * Request with flux stream as response.
   *
   * @param clientMessage client request message.
   * @return flux response
   */
  public Flux<ClientMessage> requestStream(ClientMessage clientMessage) {
    return transport.requestStream(clientMessage);
  }
}
