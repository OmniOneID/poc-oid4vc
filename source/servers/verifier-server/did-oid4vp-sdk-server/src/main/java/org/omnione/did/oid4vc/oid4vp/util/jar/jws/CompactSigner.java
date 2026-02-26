package org.omnione.did.oid4vc.oid4vp.util.jar.jws;

@FunctionalInterface
public interface CompactSigner {
     byte[] sign(String keyId, byte[] hash) throws Exception;
}
