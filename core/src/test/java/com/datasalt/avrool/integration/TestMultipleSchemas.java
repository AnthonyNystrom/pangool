//package com.datasalt.avrool.integration;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.charset.Charset;
//
//import org.apache.hadoop.fs.FileSystem;
//import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.io.LongWritable;
//import org.apache.hadoop.io.NullWritable;
//import org.apache.hadoop.io.Text;
//import org.apache.hadoop.mapreduce.Job;
//import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
//import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
//import org.junit.Test;
//
//import com.datasalt.avrool.CoGrouper;
//import com.datasalt.avrool.CoGrouperConfig;
//import com.datasalt.avrool.CoGrouperConfigBuilder;
//import com.datasalt.avrool.CoGrouperException;
//import com.datasalt.avrool.PangoolSchema;
//import com.datasalt.avrool.SortingBuilder;
//import com.datasalt.avrool.SortCriteria.SortOrder;
//import com.datasalt.avrool.api.GroupHandler;
//import com.datasalt.avrool.api.InputProcessor;
//import com.datasalt.avrool.commons.HadoopUtils;
//import com.datasalt.avrool.io.tuple.ITuple;
//import com.datasalt.avrool.io.tuple.Tuple;
//import com.datasalt.avrool.io.tuple.ITuple.InvalidFieldException;
//import com.datasalt.avrool.test.AbstractHadoopTestLibrary;
//import com.google.common.io.Files;
//
//public class TestMultipleSchemas extends AbstractHadoopTestLibrary {
//
//	public static class FirstInputProcessor extends InputProcessor<LongWritable, Text> {
//
//		@Override
//		public void process(LongWritable key, Text value, CoGrouperContext context, Collector collector) throws IOException, InterruptedException {
//
//			Tuple tuple = new Tuple();
//			tuple.setString("name", "Pere");
//			tuple.setInt("money", 100);
//			tuple.setString("country", "ES");
//
//			collector.write(0, tuple);
//
//			tuple.setString("name", "Iván");
//			tuple.setInt("money", 50);
//			tuple.setString("country", "ES");
//
//			collector.write(0, tuple);
//
//			tuple.setString("name", "Eric");
//			tuple.setInt("money", 150);
//			tuple.setString("country", "FR");
//
//			collector.write(0, tuple);
//
//			tuple = new Tuple();
//			tuple.setString("country", "ES");
//			tuple.setInt("averageSalary", 1000);
//
//			collector.write(1, tuple);
//
//			tuple.setString("country", "FR");
//			tuple.setInt("averageSalary", 1500);
//
//			collector.write(1, tuple);
//		}
//	}
//
//	public static class MyGroupHandler extends GroupHandler<Object, Object> {
//
//		@Override
//		public void onGroupElements(ITuple group, Iterable<ITuple> tuples, CoGrouperContext<Object, Object> context, 
//		    Collector<Object, Object> collector) throws IOException, InterruptedException, CoGrouperException {
//			
//			System.out.println("Group " + group);
//			for(Object tuple : tuples) {
//				System.out.println(tuple);
//			}
//		}
//	}
//
//	@Test
//	public void test() throws CoGrouperException, InvalidFieldException, IOException, InterruptedException,
//	    ClassNotFoundException {
//		CoGrouperConfig config = new CoGrouperConfigBuilder()
//		    .addSource(0, PangoolSchema.parse("name:string, money:int, country:string"))
//		    .addSource(1, PangoolSchema.parse("country:string, averageSalary:int"))
//		    .setGroupByFields("country")
//		    .setSorting(
//		        new SortingBuilder().add("country").addSourceId(SortOrder.ASC).secondarySort(0).add("money").buildSorting())
//		    .build();
//
//		Files.write("foo", new File("test-input"), Charset.forName("UTF-8"));
//		HadoopUtils.deleteIfExists(FileSystem.get(getConf()), new Path("test-output"));
//
//		Job job = new CoGrouper(config, getConf())
//		    .addInput(new Path("test-input"), TextInputFormat.class, FirstInputProcessor.class)
//		    .setGroupHandler(MyGroupHandler.class)
//		    .setOutput(new Path("test-output"), TextOutputFormat.class, NullWritable.class, NullWritable.class).createJob();
//
//		job.waitForCompletion(true);
//
//		HadoopUtils.deleteIfExists(FileSystem.get(getConf()), new Path("test-input"));
//	}
//}