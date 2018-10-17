/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.jsonrpc.websocket.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.websocket.subscription.SubscriptionManager;
import tech.pegasys.pantheon.ethereum.jsonrpc.websocket.subscription.SubscriptionNotFoundException;
import tech.pegasys.pantheon.ethereum.jsonrpc.websocket.subscription.request.InvalidSubscriptionRequestException;
import tech.pegasys.pantheon.ethereum.jsonrpc.websocket.subscription.request.SubscriptionRequestMapper;
import tech.pegasys.pantheon.ethereum.jsonrpc.websocket.subscription.request.UnsubscribeRequest;

import io.vertx.core.json.Json;
import org.junit.Before;
import org.junit.Test;

public class EthUnsubscribeTest {

  private EthUnsubscribe ethUnsubscribe;
  private SubscriptionManager subscriptionManagerMock;
  private SubscriptionRequestMapper mapperMock;
  private final String CONNECTION_ID = "test-connection-id";

  @Before
  public void before() {
    subscriptionManagerMock = mock(SubscriptionManager.class);
    mapperMock = mock(SubscriptionRequestMapper.class);
    ethUnsubscribe = new EthUnsubscribe(subscriptionManagerMock, mapperMock);
  }

  @Test
  public void nameIsEthUnsubscribe() {
    assertThat(ethUnsubscribe.getName()).isEqualTo("eth_unsubscribe");
  }

  @Test
  public void responseContainsUnsubscribeStatus() {
    final JsonRpcRequest request = createJsonRpcRequest();
    final UnsubscribeRequest unsubscribeRequest = new UnsubscribeRequest(1L, CONNECTION_ID);
    when(mapperMock.mapUnsubscribeRequest(eq(request))).thenReturn(unsubscribeRequest);
    when(subscriptionManagerMock.unsubscribe(eq(unsubscribeRequest))).thenReturn(true);

    final JsonRpcSuccessResponse expectedResponse =
        new JsonRpcSuccessResponse(request.getId(), true);

    assertThat(ethUnsubscribe.response(request)).isEqualTo(expectedResponse);
  }

  @Test
  public void invalidUnsubscribeRequestReturnsInvalidRequestResponse() {
    final JsonRpcRequest request = createJsonRpcRequest();
    when(mapperMock.mapUnsubscribeRequest(any()))
        .thenThrow(new InvalidSubscriptionRequestException());

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_REQUEST);

    assertThat(ethUnsubscribe.response(request)).isEqualTo(expectedResponse);
  }

  @Test
  public void whenSubscriptionNotFoundReturnError() {
    final JsonRpcRequest request = createJsonRpcRequest();
    when(mapperMock.mapUnsubscribeRequest(any())).thenReturn(mock(UnsubscribeRequest.class));
    when(subscriptionManagerMock.unsubscribe(any()))
        .thenThrow(new SubscriptionNotFoundException(1L));

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.SUBSCRIPTION_NOT_FOUND);

    assertThat(ethUnsubscribe.response(request)).isEqualTo(expectedResponse);
  }

  @Test
  public void uncaughtErrorOnSubscriptionManagerReturnsInternalErrorResponse() {
    final JsonRpcRequest request = createJsonRpcRequest();
    when(mapperMock.mapUnsubscribeRequest(any())).thenReturn(mock(UnsubscribeRequest.class));
    when(subscriptionManagerMock.unsubscribe(any())).thenThrow(new RuntimeException());

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.INTERNAL_ERROR);

    assertThat(ethUnsubscribe.response(request)).isEqualTo(expectedResponse);
  }

  private JsonRpcRequest createJsonRpcRequest() {
    return Json.decodeValue(
        "{\"id\": 1, \"method\": \"eth_unsubscribe\", \"params\": [\"0x0\"]}",
        JsonRpcRequest.class);
  }
}
