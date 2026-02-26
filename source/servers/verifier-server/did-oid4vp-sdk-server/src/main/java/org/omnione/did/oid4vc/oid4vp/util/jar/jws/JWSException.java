/*
 * Copyright 2026 OmniOne.
 */

package org.omnione.did.oid4vc.oid4vp.util.jar.jws;

public class JWSException extends Exception {

  public JWSException(String message) {
    super(message);
  }

  public JWSException(String message, Throwable cause) {
    super(message, cause);
  }
}