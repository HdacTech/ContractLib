/* 
 * Copyright(c) 2018-2019 hdactech.com
 * Original code was distributed under the MIT software license.
 *
 */
package com.hdac.comm;

import java.text.DecimalFormat;

/**
 * This class provide String method 
 * 
 * @version 0.8
 * 
 * @see     java.text.DecimalFormat
 */
public class StringUtil
{
	public static String nvl(Object obj)
	{
		return nvl(obj, "");
	}

	public static String nvl(Object obj, String defaultStr)
	{
		if (obj == null)
			return defaultStr;

		return obj.toString();
	}

	public static String toSmallLetter(String str, int beginIndex)
	{
		if (str == null)
			return str;

		return toSmallLetter(str, beginIndex, str.length());
	}

	public static String toSmallLetter(String str, int beginIndex, int endIndex)
	{
		if (str == null)
			return str;
		if (beginIndex < 0)
			return str;
		if (endIndex > str.length())
			return str;
		if (beginIndex > endIndex)
			return str;

		String small = str.substring(beginIndex, endIndex).toLowerCase();
		str = small.concat(str.substring(endIndex));

		return str;
	}

	public static String toBigLetter(String str, int beginIndex)
	{
		if (str == null)
			return str;

		return toBigLetter(str, beginIndex, str.length());
	}

	public static String toBigLetter(String str, int beginIndex, int endIndex)
	{
		if (str == null)
			return str;
		if (beginIndex < 0)
			return str;
		if (endIndex > str.length())
			return str;
		if (beginIndex > endIndex)
			return str;

		String small = str.substring(beginIndex, endIndex).toUpperCase();
		str = small.concat(str.substring(endIndex));

		return str;
	}
	
	public static String toNumber(double num)
	{
		String formatMask = "###############";	
		DecimalFormat df = new DecimalFormat(formatMask);

		return df.format(num);
	}

	public static String toHexString(String str)
	{
		if (str == null)
			return str;

		StringBuilder sb = new StringBuilder();
		int size = str.length();
		for (int i = 0; i < size; i++)
		{
			char ch = str.charAt(i);
			sb.append(String.format("%02X", (int)ch));
		}
		return sb.toString();
	}

	public static String toHexString(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder(); 
		for (byte b : bytes)
		{
			sb.append(String.format("%02X", b & 0xff)); 
		}
		return sb.toString();
	}

	public static String hexToString(String hexStr)
	{
		if (hexStr == null)
			return hexStr;

		StringBuilder sb = new StringBuilder();

		if (hexStr.startsWith("0x"))
			hexStr = hexStr.substring(2);

		int size = hexStr.length();
		for (int i = 0; i < size; i += 2)
		{
			String str = hexStr.substring(i, i + 2);
			sb.append((char)Integer.parseInt(str, 16));
		}

		return sb.toString();
	}
	
	public static String reverseHexString(String hexStr)
	{
	    StringBuilder result = new StringBuilder();
	    for (int i = 0; i <=hexStr.length()-2; i=i+2)
	    {
	        result.append(new StringBuilder(hexStr.substring(i,i+2)).reverse());
	    }
	    return result.reverse().toString();
	}

	public static String toLittleEndian(long value)
	{
		byte[] a = getLittleEndian(value);

		StringBuilder sb = new StringBuilder(a.length * 2);
		for (byte b : a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	private static byte[] getLittleEndian(long v)
	{
		byte[] buf = new byte[8];
		buf[0] = (byte)((v >>> 000) & 0xFF);
		buf[1] = (byte)((v >>> 010) & 0xFF);
		buf[2] = (byte)((v >>> 020) & 0xFF);
		buf[3] = (byte)((v >>> 030) & 0xFF);
		buf[4] = (byte)((v >>> 040) & 0xFF);
		buf[5] = (byte)((v >>> 050) & 0xFF);
		buf[6] = (byte)((v >>> 060) & 0xFF);
		buf[7] = (byte)((v >>> 070) & 0xFF);
		return buf;
	}

	public static byte[] toByteArray(String str)
	{
		byte[] bytes = new byte[str.length() / 2];
		for (int i = 0; i < bytes.length; i++)
		{
			bytes[i] = (byte)Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
		}
		return bytes;
	}
}