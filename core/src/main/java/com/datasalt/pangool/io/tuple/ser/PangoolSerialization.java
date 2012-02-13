/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datasalt.pangool.io.tuple.ser;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Serializer;

import com.datasalt.pangool.CoGrouperConfig;
import com.datasalt.pangool.CoGrouperException;
import com.datasalt.pangool.Schema;
import com.datasalt.pangool.Schema.Field;
import com.datasalt.pangool.io.tuple.DatumWrapper;
import com.datasalt.pangool.io.tuple.ITuple;

/**
 * A {@link Serialization} for {@link DatumWrapper} 
 * <p>
 * To use this serialization with Hadoop, use the method {@link #enableSerialization(Configuration)} over the Hadoop
 * configuration.
 */
public class PangoolSerialization implements Serialization<DatumWrapper<ITuple>>, Configurable {

	private Configuration conf;
	private com.datasalt.pangool.io.Serialization ser;
	private CoGrouperConfig pangoolConfig;

	public PangoolSerialization() {
	}

	@Override
	public boolean accept(Class<?> c) {
		return DatumWrapper.class.isAssignableFrom(c);
	}

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public void setConf(Configuration thatConf) {
		try {
			if(thatConf != null) {
				this.conf = new Configuration(thatConf);

				// Mega tricky!!!!. This is to avoid recursive serialization instantiation!!
				disableSerialization(this.conf);

				this.pangoolConfig = CoGrouperConfig.get(conf);
				this.ser = new com.datasalt.pangool.io.Serialization(this.conf);
			}
		} catch(CoGrouperException e) {
			throw new RuntimeException(e);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Serializer<DatumWrapper<ITuple>> getSerializer(Class<DatumWrapper<ITuple>> c) {
		return new PangoolSerializer(this.ser, this.pangoolConfig);
	}

	@Override
	public Deserializer<DatumWrapper<ITuple>> getDeserializer(Class<DatumWrapper<ITuple>> c) {
		return new PangoolDeserializer(this.ser, this.pangoolConfig);
	}

	/**
	 * Caches the values from the enum fields. This is done just once for efficiency since it uses reflection.
	 * 
	 */
	public static Map<String, Enum<?>[]> getEnums(CoGrouperConfig grouperConfig) {
		Map<String, Enum<?>[]> result = new HashMap<String, Enum<?>[]>();
		for(Schema s : grouperConfig.getSourceSchemas()) {
			extractEnumsFromSchema(result, s);
		}
		return result;
	}

	public static void extractEnumsFromSchema(Map<String, Enum<?>[]> mapToFill, Schema schema) {
		try {
			for(Field field : schema.getFields()) {
				Class<?> type = field.getType();
				if(type.isEnum()) {
					Method method = type.getMethod("values", (Class<?>[]) null);
					Object values = method.invoke(null);
					mapToFill.put(field.name(), (Enum[]) values);
				}
			}

		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Use this method to enable this serialization in Hadoop
	 */
	public static void enableSerialization(Configuration conf) {
		String serClass = PangoolSerialization.class.getName();
		Collection<String> currentSers = conf.getStringCollection("io.serializations");

		if(currentSers.size() == 0) {
			conf.set("io.serializations", serClass);
			return;
		}

		// Check if it is already present
		if(!currentSers.contains(serClass)) {
			currentSers.add(serClass);
			conf.setStrings("io.serializations", currentSers.toArray(new String[] {}));
		}
	}

	/**
	 * Use this method to disable this serialization in Hadoop
	 */
	public static void disableSerialization(Configuration conf) {
		String ser = conf.get("io.serializations").trim();
		String stToSearch = Pattern.quote("," + PangoolSerialization.class.getName());
		ser = ser.replaceAll(stToSearch, "");
		conf.set("io.serializations", ser);
	}

}