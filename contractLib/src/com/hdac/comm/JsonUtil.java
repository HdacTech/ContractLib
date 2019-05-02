/* 
 * Copyright(c) 2018-2019 hdactech.com
 * Original code was distributed under the MIT software license.
 *
 */
package com.hdac.comm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * This class will be moved to the contractlib
 * Deprecated
 * 
 * @version 0.8
 * 
 * @see     java.util.ArrayList
 * @see     java.util.Collection
 * @see     java.util.HashMap
 * @see     java.util.Iterator
 * @see     java.util.List
 * @see     java.util.Map
 * @see     org.json.JSONArray
 * @see     org.json.JSONException
 * @see     org.json.JSONObject
 */
public class JsonUtil
{
	public static JSONObject toJsonString(Map<String, Object> map)
    {
		JSONObject jsonObject = new JSONObject();
		try
		{
			for (Map.Entry<String, Object> entry : map.entrySet())
			{
				String key = entry.getKey();
				Object value = entry.getValue();

				if (value instanceof String)
				{
					jsonObject.put(key, (String)value);
				}
				else if (value instanceof Number)
				{
					jsonObject.put(key, (Number)value);
				}
				else if (value instanceof Collection)
				{
					jsonObject.put(key, (Collection<?>)value);
				}
				else
				{
					jsonObject.put(key, value);
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
        return jsonObject;
    }

	public static Map<String, Object> fromJsonString(String str)
	{
		return fromJsonObject(new JSONObject(str));
	}
	
    public static boolean isNumber(String str)
    {
        boolean result = false;
        try
        {
        	Integer.parseInt(str);
            result = true;
        }
        catch(Exception e)
        {
        }
        return result;
    }

	public static JSONArray toJsonArray(List<Map<String, Object>> maps)
	{
	   JSONArray jsonObjectArray = new JSONArray();
	   for (Map<String, Object> map : maps)
	   {
	      jsonObjectArray.put(toJsonString(map));
	   }
	   return jsonObjectArray;
	}
	
	public static void fromJsonObject(Map<String, Object> map, JSONObject jsonObject) throws JSONException
	{
		if ((map == null) || (jsonObject == null))
			return;

		map.clear();

		Iterator<String> i = jsonObject.keys();
		while (i.hasNext())
		{
			String key = i.next();
			Object value = jsonObject.get(key);

			map.put(key, value);
		}
	}

	public static Map<String, Object> fromJsonObject(JSONObject jsonObject) throws JSONException
	{
		Map<String, Object> map = new HashMap<String, Object>();               
		if (jsonObject != null)
		{
			Iterator<String> i = jsonObject.keys();
			while (i.hasNext())
			{
				String key = i.next();
				Object value = jsonObject.get(key);

				map.put(key, value);
			}
		}
		return map;
	}	

	public static List<Map<String, Object>> fromJsonArray(JSONArray jsonarray) throws JSONException
	{
		List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
		int len = jsonarray.length();
		for (int i = 0; i < len; i++)
		{
			JSONObject jsonobj = jsonarray.getJSONObject(i);
			listMap.add(fromJsonObject(jsonobj));
		}
		return listMap; 
	}
}