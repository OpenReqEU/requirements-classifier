package com.example.mahout;

import com.example.mahout.entity.Requirement;
import com.example.mahout.util.Control;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import java.io.IOException;
import java.util.List;


public class ToSeqFile {

    private ToSeqFile() {
        //utility class
    }

    public static void reqToSeq(List<Requirement> requirements, String pathSequential) throws IOException {

        Configuration configuration = new Configuration();
        FileSystem fs = FileSystem.get(configuration);
        SequenceFile.Writer writer = new SequenceFile.Writer(fs, configuration, new Path(pathSequential + "/chunk-0"),
                Text.class, Text.class);

        int count = 0;

        Text key = new Text();
        Text value = new Text();


        for (int k = 0; k < requirements.size(); ++k) {
            Requirement requirement = requirements.get(k);

            String category = requirement.getRequirementType();
            String id = requirement.getId();
            String req = requirement.getText();

            key.set("/" + category + "/" + id);
            value.set(req);
            writer.append(key, value);
            count++;
        }
        writer.close();
        Control.getInstance().showInfoMessage("Wrote " + count + " entries.");
    }

}