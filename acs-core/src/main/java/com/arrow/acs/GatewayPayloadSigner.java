/*******************************************************************************
 * Copyright (c) 2018 Arrow Electronics, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License 2.0
 * which accompanies this distribution, and is available at
 * http://apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Arrow Electronics, Inc.
 *******************************************************************************/
package com.arrow.acs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

public class GatewayPayloadSigner extends Loggable {
	public static final String PAYLOAD_SIGNATURE_VERSION_1 = "1";
	private String secretKey;
	private String hid;
	private String name;
	private boolean encrypted;
	private String apiKey;
	private List<String> parameters = new ArrayList<>();

	private GatewayPayloadSigner(String secretKey) {
		this.secretKey = secretKey;
	}

	public static GatewayPayloadSigner create(String secretKey) {
		AcsUtils.notEmpty(secretKey, "secretKey is empty");
		return new GatewayPayloadSigner(secretKey);
	}

	public GatewayPayloadSigner withHid(String hid) {
		AcsUtils.notEmpty(hid, "hid is empty");
		this.hid = hid;
		return this;
	}

	public GatewayPayloadSigner withName(String name) {
		AcsUtils.notEmpty(name, "name is empty");
		this.name = name;
		return this;
	}

	public GatewayPayloadSigner withEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
		return this;
	}

	public GatewayPayloadSigner withApiKey(String apiKey) {
		AcsUtils.notEmpty(apiKey, "apiKey is empty");
		this.apiKey = apiKey;
		return this;
	}

	public GatewayPayloadSigner withParameter(String name, String value) {
		AcsUtils.notEmpty(name, "name is empty");
		parameters.add(String.format("%s=%s", name.toLowerCase(), value));
		return this;
	}

	public String signV1() {
		String method = "signV1";
		AcsUtils.notEmpty(apiKey, "apiKey is required");
		AcsUtils.notEmpty(secretKey, "secretKey is required");

		StringBuilder stringToSign = new StringBuilder();
		stringToSign.append(hash(buildCanonicalRequest())).append('\n');
		stringToSign.append(apiKey).append('\n');
		stringToSign.append(PAYLOAD_SIGNATURE_VERSION_1);
		logDebug(method, "stringToSign: %s", stringToSign);

		String signingKey = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, PAYLOAD_SIGNATURE_VERSION_1)
		        .hmacHex(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, apiKey).hmacHex(secretKey));
		logDebug(method, "signingKey: %s", signingKey);

		String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, signingKey).hmacHex(stringToSign.toString());
		logDebug(method, "signature: %s", signature);

		return signature;
	}

	private String buildCanonicalRequest() {
		AcsUtils.notEmpty(hid, "hid is empty");
		AcsUtils.notEmpty(name, "name is empty");

		StringBuilder builder = new StringBuilder();
		builder.append(hid).append('\n').append(name).append('\n').append(encrypted).append('\n');
		if (!parameters.isEmpty()) {
			Collections.sort(parameters);
			parameters.forEach(param -> builder.append(param).append('\n'));
		}
		return builder.toString();
	}

	private static String hash(String value) {
		return DigestUtils.sha256Hex(value);
	}
}
