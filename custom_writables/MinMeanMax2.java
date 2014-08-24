package com.guavus.training;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

class MMMMapper2 extends Mapper<Object, Text, IntWritable, DoubleWritable> {

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

		/*-
		outKey += 100000;
		iwk.set(outKey);
		dwv.set(netOut);

		context.write(iwk, dwv);
		 */
	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		iwk = new IntWritable();
		dwv = new DoubleWritable();
		System.out.println("Started mapper for file" + context.getInputSplit().toString());
	}
}

class Triplet implements Writable {
	double first;
	double second;
	double third;

	Triplet(double d1, double d2, double d3) {
		first = d1;
		second = d2;
		third = d3;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(Double.toString(first));
		sb.append(',');
		sb.append(Double.toString(second));
		sb.append(',');
		sb.append(Double.toString(third));
		sb.append(',');
		return sb.toString();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(toString());
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		String s = in.readUTF();
		String[] token = s.split(",");
		first = Double.parseDouble(token[0]);
		second = Double.parseDouble(token[1]);
		third = Double.parseDouble(token[2]);
	}
}

class MMMReducer2 extends Reducer<IntWritable, DoubleWritable, IntWritable, Triplet> {
	ArrayWritable value = null;
	IntWritable outKey = null;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		value = new ArrayWritable(DoubleWritable.class);
		outKey = new IntWritable();
		System.out.println("Started mapper for file" + context.getJobName());
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		System.out.println("Done with the reducer id" + context.getJobID() + " name " + context.getJobName());
	}

	@Override
	protected void reduce(IntWritable key, Iterable<DoubleWritable> valIter1, Context context) throws IOException, InterruptedException {

		Iterator<DoubleWritable> iter = valIter1.iterator();

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		double sum = 0;
		int count = 0;

		while (iter.hasNext()) {
			double val = iter.next().get();
			if (min > val) {
				min = val;
			}
			if (max < val) {
				max = val;
			}
			sum += val;
			count++;
		}

		Triplet value = new Triplet(sum / count, min, max);
		context.write(key, value);
	}
}

public class MinMeanMax2 {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		Configuration conf = new Configuration();

		Job job = new Job(conf, "MinMeanMax");
		job.setMapperClass(MMMMapper2.class);
		job.setReducerClass(MMMReducer2.class);

		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(ArrayWritable.class);

		//job.setNumReduceTasks(0);//, value)
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
