package org.myorg;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class Cat{
    public static void main (String [] args) throws Exception{
            try{
                    Path pt=new Path("hdfs://localhost:54310/user/mahtab.singh/nobots_ip_country_tsv.txt");
                    FileSystem fs = FileSystem.get(new Configuration());
                    BufferedReader br=new BufferedReader(new InputStreamReader(fs.open(pt)));
                    String line;
                    line=br.readLine();
                    while (line != null){
                            System.out.println(line);
                            line=br.readLine();
                    }
            }catch(Exception e){
            	e.printStackTrace();
            }
    }
}
