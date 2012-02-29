package com.datasalt.pangool.examples.topicalwordcount;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.junit.Test;

import com.datasalt.pangool.utils.test.AbstractHadoopTestLibrary;
import com.google.common.io.Files;

public class TestTopicalWordCountWithStopWords extends AbstractHadoopTestLibrary {
	
	private final static String STOP_WORDS = TestTopicalWordCountWithStopWords.class.getName() + "-stop-words.txt";
	private final static String INPUT = TestTopicalWordCountWithStopWords.class.getName() + "-input";
	private final static String OUTPUT = TestTopicalWordCountWithStopWords.class.getName() + "-output";
	
	@Test
	public void test() throws Exception {
		trash(OUTPUT);
		
		Configuration conf = new Configuration();
		
		TestTopicalWordCount.createInput(INPUT);

		Files.touch(new File(STOP_WORDS));
		ToolRunner.run( new TopicalWordCountWithStopWords(), new String[] { INPUT, OUTPUT, STOP_WORDS });
		
		assertEquals(6, TestTopicalWordCount.assertOutput(OUTPUT + "/part-r-00000", conf));
		
		// Stop words: bar, bloh
		Files.write(("bar" + "\n" + "bloh").getBytes("UTF-8"), new File(STOP_WORDS));
		ToolRunner.run( new TopicalWordCountWithStopWords(), new String[] { INPUT, OUTPUT, STOP_WORDS });

		assertEquals(3, TestTopicalWordCount.assertOutput(OUTPUT + "/part-r-00000", conf));
		
		trash(INPUT, STOP_WORDS, OUTPUT);
	}
}