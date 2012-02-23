/**
 * Copyright [2012] [Datasalt Systems S.L.]
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
package com.datasalt.pangool.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import com.datasalt.pangool.cogroup.TupleMRBuilder;
import com.datasalt.pangool.cogroup.TupleMRException;
import com.datasalt.pangool.cogroup.processors.TupleCombiner;
import com.datasalt.pangool.cogroup.processors.TupleReducer;
import com.datasalt.pangool.cogroup.processors.TupleMapper;
import com.datasalt.pangool.io.tuple.ITuple;
import com.datasalt.pangool.io.tuple.Schema;
import com.datasalt.pangool.io.tuple.Tuple;
import com.datasalt.pangool.io.tuple.Schema.Field;

/**
 * Original Hadoop's WordCount implemented in Pangool.
 */
public class WordCount {

	@SuppressWarnings("serial")
	public static class Split extends TupleMapper<LongWritable, Text> {

		private Tuple tuple;
		
		public void setup(TupleMRContext context, Collector collector) throws IOException, InterruptedException {
			tuple = new Tuple(context.getTupleMRConfig().getIntermediateSchema(0));
			tuple.set("count", 1);
		}
		
		@Override
		public void map(LongWritable key, Text value, TupleMRContext context, Collector collector)
		    throws IOException, InterruptedException {
			StringTokenizer itr = new StringTokenizer(value.toString());
			while(itr.hasMoreTokens()) {
				tuple.set("word", itr.nextToken());
				collector.write(tuple);
			}
		}
	}

	@SuppressWarnings("serial")
	public static class CountCombiner extends TupleCombiner {

		private Tuple tuple;
		
		public void setup(TupleMRContext context, Collector collector) throws IOException, InterruptedException {
			this.tuple = new Tuple(context.getCoGrouperConfig().getIntermediateSchema("schema"));
		}

		@Override
		public void onGroupElements(ITuple group, Iterable<ITuple> tuples, TupleMRContext context, Collector collector)
		    throws IOException, InterruptedException, TupleMRException {
			int count = 0;
			tuple.set("word", group.get("word"));
			for(ITuple tuple : tuples) {
				count += (Integer) tuple.get(1);
			}
			tuple.set("count", count);
			collector.write(this.tuple);
		}
	}

	@SuppressWarnings("serial")
	public static class Count extends TupleReducer<Text, IntWritable> {

		@Override
		public void reduce(ITuple group, Iterable<ITuple> tuples, TupleMRContext context, Collector collector)
		    throws IOException, InterruptedException, TupleMRException {
			int count = 0;
			for(ITuple tuple : tuples) {
				count += (Integer) tuple.get(1);
			}
			Text text = (Text)group.get("word");
			collector.write(text, new IntWritable(count));
		}
	}

	public Job getJob(Configuration conf, String input, String output) throws  TupleMRException,
	    IOException {
		FileSystem fs = FileSystem.get(conf);
		fs.delete(new Path(output), true);

		List<Field> fields = new ArrayList<Field>();
		fields.add(new Field("word",String.class));
		fields.add(new Field("count",Integer.class));
		
		TupleMRBuilder cg = new TupleMRBuilder(conf);
		cg.addIntermediateSchema(new Schema("schema",fields));
		cg.setJarByClass(WordCount.class);
		cg.addInput(new Path(input), TextInputFormat.class, new Split());
		cg.setOutput(new Path(output), TextOutputFormat.class, Text.class, Text.class);
		cg.setGroupByFields("word");
		cg.setTupleReducer(new Count());
		cg.setTupleCombiner(new CountCombiner());

		return cg.createJob();
	}

	private static final String HELP = "Usage: WordCount [input_path] [output_path]";

	public static void main(String args[]) throws TupleMRException, IOException, InterruptedException,
	    ClassNotFoundException {
		if(args.length != 2) {
			System.err.println("Wrong number of arguments");
			System.err.println(HELP);
			System.exit(-1);
		}

		Configuration conf = new Configuration();
		new WordCount().getJob(conf, args[0], args[1]).waitForCompletion(true);
	}
}