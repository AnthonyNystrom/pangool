/**
 * Copyright [2011] [Datasalt Systems S.L.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datasalt.pangolin.grouper;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.log4j.Logger;

public class GrouperPartitioner extends Partitioner<Tuple,NullWritable> implements Configurable{

	private static Logger log = Logger.getLogger(GrouperPartitioner.class);
	
	private Configuration conf;
	private Schema schema;
	private int[] groupFieldsIndexes;
	
	@Override
	public int getPartition(Tuple key, NullWritable value, int numPartitions) {
		return key.partialHashCode(groupFieldsIndexes) % numPartitions;
	}

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public void setConf(Configuration conf) {
		if (conf != null){
			this.conf = conf;
			String schemaString = conf.get(Grouper.CONF_SCHEMA);
			this.schema = Schema.parse(schemaString);
		}
		
		String fieldsGroupStr = conf.get(Grouper.CONF_FIELDS_GROUP);
		//TODO do check if they match schema
		String[] fieldsGroup = fieldsGroupStr.split(",");
		groupFieldsIndexes = new int[fieldsGroup.length];
		for (int i=0 ; i < fieldsGroup.length;i++){
			groupFieldsIndexes[i] = schema.getIndexByFieldName(fieldsGroup[i]);
		}
		
	}

	

	
}
