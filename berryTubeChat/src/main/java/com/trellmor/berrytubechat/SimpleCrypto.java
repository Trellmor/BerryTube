/*
 * BerryTubeChat android client
 * Copyright (C) 2012-2013 Daniel Triendl <trellmor@trellmor.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.trellmor.berrytubechat;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Usage:
 * 
 * <pre>
 * String crypto = SimpleCrypto.encrypt(masterpassword, cleartext)
 * ...
 * String cleartext = SimpleCrypto.decrypt(masterpassword, crypto)
 * </pre>
 * 
 * @author ferenc.hechler
 */
class SimpleCrypto {

	static String encrypt(String key, String cleartext)
			throws GeneralSecurityException {
		byte[] rawKey = toByte(key);
		byte[] result = encrypt(rawKey, cleartext.getBytes());
		return toHex(result);
	}

	static String decrypt(String key, String encrypted)
			throws GeneralSecurityException {
		byte[] rawKey = toByte(key);
		byte[] enc = toByte(encrypted);
		byte[] result = decrypt(rawKey, enc);
		return new String(result);
	}

	public static String generateKey() throws NoSuchAlgorithmException {
		final int outputKeyLength = 128; // 192 and 256 bits may not be
											// available

		SecureRandom sr = new SecureRandom();
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(outputKeyLength, sr);
		SecretKey key = kg.generateKey();
		byte[] raw = key.getEncoded();
		return toHex(raw);
	}

	private static byte[] encrypt(byte[] raw, byte[] clear)
			throws GeneralSecurityException {
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		return cipher.doFinal(clear);
	}

	private static byte[] decrypt(byte[] raw, byte[] encrypted)
			throws GeneralSecurityException {
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		return cipher.doFinal(encrypted);
	}


	public static String fromHex(String hex) {
		return new String(toByte(hex));
	}

	private static byte[] toByte(String hexString) {
		int len = hexString.length() / 2;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++)
			result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
					16).byteValue();
		return result;
	}

	private static String toHex(byte[] buf) {
		if (buf == null)
			return "";
		StringBuffer result = new StringBuffer(2 * buf.length);
		for (byte b : buf) {
			appendHex(result, b);
		}
		return result.toString();
	}

	private final static String HEX = "0123456789ABCDEF";

	private static void appendHex(StringBuffer sb, byte b) {
		sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
	}
}
