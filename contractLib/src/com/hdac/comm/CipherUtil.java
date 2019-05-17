/* 
 * Copyright(c) 2018-2019 hdactech.com
 * Original code was distributed under the MIT software license.
 *
 */
package com.hdac.comm;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class provide crypto method(Encode/Decode) 
 * 
 * 
 * @version 0.8
 * @see     javax.crypto.Cipher
 * @see     javax.crypto.spec.IvParameterSpec
 * @see     javax.crypto.spec.SecretKeySpec
 */
public class CipherUtil
{
	public static String AesEncode(String str, String key)
	{
		try
		{
			byte[] ip = getIp(key);
			SecretKeySpec keySpec = getSecretKeySpec();

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(ip));

			byte[] encrypted = cipher.doFinal(str.getBytes("UTF-8"));
			return StringUtil.toHexString(encrypted);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
 
	public static String AesDecode(String str, String key)
	{
		try
		{
			byte[] ip = getIp(key);
			SecretKeySpec keySpec = getSecretKeySpec();

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ip));
 
			byte[] byteStr = StringUtil.toByteArray(str);
			return new String(cipher.doFinal(byteStr), "UTF-8");
 		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private static byte[] getIp(String key)
	{
		while (key.length() < 16)
			key += "|" + key;
			
		return key.substring(0, 16).getBytes();
	}

	private static SecretKeySpec getSecretKeySpec()
	{
		byte[] keyBytes = new byte[16];
		return new SecretKeySpec(keyBytes, "AES");
	}
}