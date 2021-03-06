/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.nat;

import org.hyperledger.besu.nat.core.NatManager;
import org.hyperledger.besu.nat.core.NatMethodDetector;
import org.hyperledger.besu.nat.core.domain.NatPortMapping;
import org.hyperledger.besu.nat.core.domain.NatServiceType;
import org.hyperledger.besu.nat.core.domain.NetworkProtocol;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility class to help interacting with various {@link NatManager}. */
public class NatService {

  protected static final Logger LOG = LogManager.getLogger();

  private final NatMethod currentNatMethod;
  private final Optional<NatManager> currentNatManager;

  public NatService(final Optional<NatManager> natManager) {
    this.currentNatMethod = retrieveNatMethod(natManager);
    this.currentNatManager = natManager;
  }

  /**
   * Returns whether or not the Besu node is running under a NAT environment.
   *
   * @return true if Besu node is running under NAT environment, false otherwise.
   */
  public boolean isNatEnvironment() {
    return currentNatMethod != NatMethod.NONE;
  }

  /**
   * If nat environment is present, performs the given action, otherwise does nothing.
   *
   * @param natMethod specific on which only this action must be performed
   * @param action the action to be performed, if a nat environment is present
   */
  public void ifNatEnvironment(
      final NatMethod natMethod, final Consumer<? super NatManager> action) {
    if (isNatEnvironment()) {
      currentNatManager.filter(s -> natMethod.equals(s.getNatMethod())).ifPresent(action);
    }
  }

  /**
   * Returns the NAT method.
   *
   * @return the current NatMethod.
   */
  public NatMethod getNatMethod() {
    return currentNatMethod;
  }

  /**
   * Returns the NAT manager associated to the current NAT method.
   *
   * @return an {@link Optional} wrapping the {@link NatManager} or empty if not found.
   */
  public Optional<NatManager> getNatManager() {
    return currentNatManager;
  }

  /** Starts the manager or service. */
  public void start() {
    if (isNatEnvironment()) {
      try {
        getNatManager().orElseThrow().start();
      } catch (Exception e) {
        LOG.warn("Caught exception while trying to start the manager or service.", e);
      }
    } else {
      LOG.info("No NAT environment detected so no service could be started");
    }
  }

  /** Stops the manager or service. */
  public void stop() {
    if (isNatEnvironment()) {
      try {
        getNatManager().orElseThrow().stop();
      } catch (Exception e) {
        LOG.warn("Caught exception while trying to stop the manager or service", e);
      }
    } else {
      LOG.info("No NAT environment detected so no service could be stopped");
    }
  }

  /**
   * Returns a {@link Optional} wrapping the advertised IP address.
   *
   * @return The advertised IP address wrapped in a {@link Optional}. Empty if
   *     `isNatExternalIpUsageEnabled` is false
   */
  public Optional<String> queryExternalIPAddress() {
    if (isNatEnvironment()) {
      try {
        final NatManager natManager = getNatManager().orElseThrow();
        LOG.info(
            "Waiting for up to {} seconds to detect external IP address...",
            NatManager.TIMEOUT_SECONDS);
        return Optional.of(
            natManager.queryExternalIPAddress().get(NatManager.TIMEOUT_SECONDS, TimeUnit.SECONDS));

      } catch (Exception e) {
        LOG.warn(
            "Caught exception while trying to query NAT external IP address (ignoring): {}", e);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns a {@link Optional} wrapping the local IP address.
   *
   * @return The local IP address wrapped in a {@link Optional}.
   */
  public Optional<String> queryLocalIPAddress() throws RuntimeException {
    if (isNatEnvironment()) {
      try {
        final NatManager natManager = getNatManager().orElseThrow();
        LOG.info(
            "Waiting for up to {} seconds to detect external IP address...",
            NatManager.TIMEOUT_SECONDS);
        return Optional.of(
            natManager.queryLocalIPAddress().get(NatManager.TIMEOUT_SECONDS, TimeUnit.SECONDS));
      } catch (Exception e) {
        LOG.warn("Caught exception while trying to query local IP address (ignoring): {}", e);
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the port mapping associated to the passed service type.
   *
   * @param serviceType The service type {@link NatServiceType}.
   * @param networkProtocol The network protocol {@link NetworkProtocol}.
   * @return The port mapping {@link NatPortMapping}
   */
  public Optional<NatPortMapping> getPortMapping(
      final NatServiceType serviceType, final NetworkProtocol networkProtocol) {
    if (isNatEnvironment()) {
      try {
        final NatManager natManager = getNatManager().orElseThrow();
        return Optional.of(natManager.getPortMapping(serviceType, networkProtocol));
      } catch (Exception e) {
        LOG.warn("Caught exception while trying to query port mapping (ignoring): {}", e);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieve the current NatMethod.
   *
   * @param natManager The natManager wrapped in a {@link Optional}.
   * @return the current NatMethod.
   */
  private NatMethod retrieveNatMethod(final Optional<NatManager> natManager) {
    return natManager.map(NatManager::getNatMethod).orElse(NatMethod.NONE);
  }

  /**
   * Attempts to automatically detect the Nat method by applying nat method detectors. Will return
   * the first one that succeeds in its detection.
   *
   * @param natMethodDetectors list of nat method auto detections
   * @return a {@link NatMethod} equal to NONE if no Nat method has been detected automatically.
   */
  public static NatMethod autoDetectNatMethod(final NatMethodDetector... natMethodDetectors) {
    return Arrays.stream(natMethodDetectors)
        .flatMap(natMethodDetector -> natMethodDetector.detect().stream())
        .findFirst()
        .orElse(NatMethod.NONE);
  }
}
