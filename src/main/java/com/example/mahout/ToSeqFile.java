package com.example.mahout;

import com.example.mahout.entity.Requirement;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import java.util.List;


public class ToSeqFile {

    public static void ReqToSeq(List<Requirement> requirements, String path_sequential) throws Exception {
        String outputDirName = path_sequential;

        Configuration configuration = new Configuration();
        FileSystem fs = FileSystem.get(configuration);
        SequenceFile.Writer writer = new SequenceFile.Writer(fs, configuration, new Path(outputDirName + "/chunk-0"),
                Text.class, Text.class);

        int count = 0;

        Text key = new Text();
        Text value = new Text();


        for (int k = 0; k < requirements.size(); ++k) {
            Requirement requirement = requirements.get(k);

            String category = requirement.getRequirement_type();
            String id = requirement.getId();
            String req = requirement.getText();

            key.set("/" + category + "/" + id);
            value.set(req);
            writer.append(key, value);
            count++;
        }
        writer.close();
        System.out.println("Wrote " + count + " entries.");
    }

}