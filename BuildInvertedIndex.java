import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

import java.io.IOException;
import java.util.*;

public class BuildInvertedIndex {

    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        private final static IntWritable one = new IntWritable(1);
        //word is key anf fileval is value
        private Text word = new Text();
        private Text fileVal = new Text();

        public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            FileSplit fileSplit = (FileSplit) reporter.getInputSplit();
            String fileName = fileSplit.getPath().getName();
            fileVal.set(fileName + "@" + key + ", ");

            String line = value.toString();
            StringTokenizer tokenizer = new StringTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                String str = tokenizer.nextToken();
                word.set(str.replaceAll("[^a-zA-Z]", "").trim());
                //The (key, value) pair will look like ("word : ", "filename@lineoffset")
                output.collect(new Text(word.toString() + " : "), fileVal);
            }
        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            int sum = 0;
            String tmpOut = "";
            String tmpTest = "";
            List<String> LList = new LinkedList<String>();
            //Create a list of the incoming values
            while (values.hasNext()) {
                tmpOut = values.next().toString();
                if (!(LList.contains(tmpOut)) && (tmpOut != "")) {
                    LList.add(tmpOut);
                }
            }
            //Sort the list
            Collections.sort(LList);
            tmpOut = "";
            ListIterator itr = LList.listIterator(0);
            //Append all the values together to create a new string which is sent out as value
            while (itr.hasNext()) {
                tmpTest = itr.next().toString();
                if (tmpTest != "")
                    tmpOut += tmpTest;
            }
            output.collect(key, new Text("" + tmpOut));
        }
    }

    public static void main(String[] args) throws Exception {
        JobConf conf = new JobConf(BuildInvertedIndex.class);
        conf.setJobName("wordcount");

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);

        conf.setMapperClass(Map.class);
        conf.setCombinerClass(Reduce.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        JobClient.runJob(conf);
    }
}
