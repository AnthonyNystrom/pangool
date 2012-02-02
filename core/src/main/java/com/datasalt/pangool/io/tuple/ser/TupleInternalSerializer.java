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

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.io.VLongWritable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.serializer.Serializer;

import com.datasalt.pangool.CoGrouperConfig;
import com.datasalt.pangool.Schema;
import com.datasalt.pangool.Schema.Field;
import com.datasalt.pangool.io.Serialization;
import com.datasalt.pangool.io.tuple.ITuple;
import com.datasalt.pangool.io.tuple.ITupleInternal;

class TupleInternalSerializer implements Serializer<ITupleInternal> {

	private Serialization ser;

	private DataOutputStream out;
	private CoGrouperConfig coGrouperConfig;
	private Text text = new Text();

	private DataOutputBuffer tmpOutputBuffer = new DataOutputBuffer();

	TupleInternalSerializer(Serialization ser, CoGrouperConfig pangoolConfig) {
		this.coGrouperConfig = pangoolConfig;
		this.ser = ser;
	}

	public void open(OutputStream out) {
		this.out = new DataOutputStream(out);
	}

	public void serialize(ITupleInternal tuple) throws IOException {
		// First we write common schema
		Schema commonSchema = coGrouperConfig.getCommonOrderedSchema();
		int sourceId = write(commonSchema, tuple, 0, this.out);

		if(coGrouperConfig.getnSchemas() > 1) {
			// Now we write specific part if needed.
			Schema schema = coGrouperConfig.getSpecificOrderedSchema(sourceId);
			write(schema, tuple, commonSchema.getFields().length, this.out);
		}
	}

	public void close() throws IOException {
		this.out.close();
	}

	public int write(Schema schema, ITuple tuple, int index, DataOutput output) throws IOException {
		int sourceId = 0;
		for(Field field : schema.getFields()) {
			String fieldName = field.getName();
			if(fieldName == Field.SOURCE_ID_FIELD_NAME) {
				sourceId = tuple.getInt(index);
				WritableUtils.writeVInt(output, sourceId);
				continue;
			}
			Class<?> fieldType = field.getType();
			Object element;
			element = tuple.getArray()[index];
			try {
				if(fieldType == VIntWritable.class) {
					WritableUtils.writeVInt(output, (Integer) element);
				} else if(fieldType == VLongWritable.class) {
					WritableUtils.writeVLong(output, (Long) element);
				} else if(fieldType == Integer.class) {
					output.writeInt((Integer) element);
				} else if(fieldType == Long.class) {
					output.writeLong((Long) element);
				} else if(fieldType == Double.class) {
					output.writeDouble((Double) element);
				} else if(fieldType == Float.class) {
					output.writeFloat((Float) element);
				} else if(fieldType == String.class) {
					byte[] bytes = (byte[]) element;
					text.set(bytes, 0, bytes.length);
					text.write(output);
				} else if(fieldType == Boolean.class) {
					output.write((Boolean) element ? 1 : 0);
				} else if(fieldType.isEnum()) {
					Enum<?> e = (Enum<?>) element;
					if(e.getClass() != fieldType) {
						throw new IOException("Field '" + fieldName + "' contains '" + element + "' which is "
						    + element.getClass().getName() + ".The expected type is " + fieldType.getName());
					}
					WritableUtils.writeVInt(output, e.ordinal());
				} else {
					if(element == null) {
						WritableUtils.writeVInt(output, 0);
					} else {
						tmpOutputBuffer.reset();
						ser.ser(element, tmpOutputBuffer);
						WritableUtils.writeVInt(output, tmpOutputBuffer.getLength());
						output.write(tmpOutputBuffer.getData(), 0, tmpOutputBuffer.getLength());
					}
				}
			} catch(ClassCastException e) {
				throw new IOException("Field '" + fieldName + "' contains '" + element + "' which is "
				    + element.getClass().getName() + ".The expected type is " + fieldType.getName());
			} // end for
			index++;
		} 
		return sourceId;
	}
}