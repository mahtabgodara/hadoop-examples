package com.guavus.training;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

class MMMMapper0 extends Mapper<Object, Text, IntWritable, DoubleWritable> {

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		System.out.println("Done with the mapper id" + context.getJobID() + " split " + context.getInputSplit().toString());
	}

	IntWritable iwk = null;
	DoubleWritable dwv = null;

	@Override
	protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
		String[] tokens = value.toString().split(",");
		long ts;
		double netIn;
		try {
			ts = Long.parseLong(tokens[0]);
			netIn = Double.parseDouble(tokens[1]);
		} catch (NumberFormatException e) {
			// Ignore record
			return;
		}

		int outKey = (int) ((ts % 86400) / 60 / 60);

		iwk.set(outKey);
		dwv.set(netIn);

		context.write(iwk, dwv);
	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		iwk = new IntWritable();
		dwv = new DoubleWritable();
		System.out.println("Started mapper for file" + context.getInputSplit().toString());
	}
}

class MMMReducer0 extends Reducer<IntWritable, DoubleWritable, IntWritable, DoubleWritable> {
	DoubleWritable value = null;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		value = new DoubleWritable();
		System.out.println("Started mapper for file" + context.getTaskAttemptID() + context.toString());
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		System.out.println("Done with the reducer id" + context.getJobID() + " name " + context.getJobName());
	}

	@Override
	protected void reduce(IntWritable key, Iterable<DoubleWritable> valIter1, Context context) throws IOException, InterruptedException {
		Iterator<DoubleWritable> iter = valIter1.iterator();
		double sum = 0;
		int count = 0;
		while (iter.hasNext()) {
			sum += iter.next().get();
			count++;
		}
		value.set(sum / count);
		context.write(key, value);
	}
}

public class MinMeanMax0 {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		Configuration conf = new Configuration();

		Job job = new Job(conf, "MinMeanMax");
		job.setMapperClass(MMMMapper0.class);
		job.setReducerClass(MMMReducer0.class);

		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(DoubleWritable.class);

		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(DoubleWritable.class);

		int argc = 0;

		FileOutputFormat.setOutputPath(job, new Path(args[argc++]));

		for (; argc < args.length; ++argc) {
			FileInputFormat.addInputPath(job, new Path(args[argc]));
		}

		boolean status = job.waitForCompletion(true);
		System.exit(status ? 0 : 1);
	}
}
