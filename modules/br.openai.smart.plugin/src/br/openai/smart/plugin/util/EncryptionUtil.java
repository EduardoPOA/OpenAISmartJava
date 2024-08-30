package br.openai.smart.plugin.util;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class EncryptionUtil {

	/**
	 * Criptografa o valor fornecido usando a chave e o algoritmo fornecidos.
	 *
	 * @author Eduardo Oliveira
	 *
	 * @param valor
	 * @retornar
	 * @throws Exceção
	 */
	public static String encrypt(String value) throws Exception {
		Key key = new SecretKeySpec(OpenAiConstants.KEY, OpenAiConstants.ALGORITHM);
		Cipher cipher = Cipher.getInstance(OpenAiConstants.ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] encryptedValue = cipher.doFinal(value.getBytes());
		return Base64.encodeBase64String(encryptedValue);
	}

	/**
	 * Descriptografa o valor fornecido usando a chave e o algoritmo fornecidos.
	 *
	 * @author Eduardo Oliveira
	 *
	 * @param valor
	 * @retornar
	 * @throws Exceção
	 */
	public static String decrypt(String value) throws Exception {
		Key key = new SecretKeySpec(OpenAiConstants.KEY, OpenAiConstants.ALGORITHM);
		Cipher cipher = Cipher.getInstance(OpenAiConstants.ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, key);
		byte[] decryptedValue = cipher.doFinal(Base64.decodeBase64(value));
		return new String(decryptedValue);
	}
}
