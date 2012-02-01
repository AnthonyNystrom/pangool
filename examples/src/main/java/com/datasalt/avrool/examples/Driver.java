package com.datasalt.avrool.examples;

import org.apache.hadoop.util.ProgramDriver;

import com.datasalt.avrool.examples.wordcount.AvroolWordCount;
import com.datasalt.avrool.examples.wordcount.MapRedWordCount;
import com.datasalt.avrool.examples.wordcount.OldAvroWordCount;
import com.datasalt.avrool.examples.wordcount.PlainAvroWordCount;


/**
 * <p>This is Hadoop's main entry point - here we'll add 
 * all the different programs that we want to execute with Hadoop.</p>
 * 
 */
public class Driver extends ProgramDriver {

	public Driver() throws Throwable {
		super();
		//addClass("pangoolcount", WordCount.class, "Typical word count in Pangool");
		addClass("avroolcount", AvroolWordCount.class, "Typical word count in Avrool");
		addClass("plainavrocount", PlainAvroWordCount.class, "Typical word count using plain Avro with Hadoop");
		addClass("oldavrocount", OldAvroWordCount.class, "word count using AvroMapper etc..");
		addClass("hadoopcount", MapRedWordCount.class, "Typical word count in plain-vanilla Hadoop");
	}
	
	public static void main(String[] args) throws Throwable {
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
		Driver driver = new Driver();
		driver.driver(args);
		System.exit(0);
	}
}